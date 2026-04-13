package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.client.util.InputUtil;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.utils.math.TimerUtil;

import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "WTap", category = Category.COMBAT)
public class WTapModule extends Module {
    @Getter private static final WTapModule instance = new WTapModule();

    /**
     * Фаза крит-цикла:
     * IDLE        — ничего не происходит
     * JUMP        — только что инициировали прыжок
     * WAIT_FALL   — ждём, когда игрок оторвался от земли и начал падать
     * READY       — игрок падает, крит гарантирован → вызываем attackCallback
     */
    private enum CritPhase { IDLE, JUMP, WAIT_FALL, READY }

    private volatile CritPhase critPhase = CritPhase.IDLE;
    private volatile Runnable attackCallback;
    private final TimerUtil phaseTimer = new TimerUtil();

    // Сколько тиков (мс) ждём перед проверкой падения после прыжка
    private static final long JUMP_GRACE_MS = 80L;
    // Максимальное время ожидания крита (защита от зависания)
    private static final long CRIT_TIMEOUT_MS = 600L;

    public WTapModule() {
        setEnabled(true);
    }

    @Override
    public void onDisable() {
        cancelCrit();
    }

    /** Активен ли цикл WTap прямо сейчас */
    public boolean isSuppressing() {
        return critPhase != CritPhase.IDLE;
    }

    /**
     * Вызывается из TriggerBot / Aura перед атакой.
     * Вместо немедленной атаки — запускает цикл WTap:
     *   1. Сбрасываем спринт (только через setPressed/setSprinting, без пакетов)
     *   2. Прыгаем
     *   3. Ждём падения
     *   4. Вызываем attackCallback (сама атака)
     *
     * @param onCritReady — колбэк, который будет вызван в момент гарантированного крита
     * @return true  — WTap принял запрос (атаку нужно отложить)
     *         false — WTap не может принять (выключен / уже в процессе / не спринтует)
     */
    public boolean requestCritAttack(Runnable onCritReady) {
        if (!isEnabled() || mc.player == null) return false;
        // Если уже в процессе — не перебиваем
        if (critPhase != CritPhase.IDLE) return true;
        // Если игрок уже в воздухе и падает — крит и так будет, сразу атакуем
        if (!mc.player.isOnGround() && mc.player.fallDistance > 0.0f) {
            onCritReady.run();
            return true;
        }

        attackCallback = onCritReady;
        critPhase = CritPhase.JUMP;
        phaseTimer.reset();

        // Сразу глушим спринт (без пакетов)
        suppressSprint();
        // Инициируем прыжок через jump() — это легитный способ
        mc.player.jump();

        return true;
    }

    /**
     * Старый метод для обратной совместимости с Aura/TriggerBot,
     * которые не используют колбэк (они сами атакуют потом).
     * Просто глушим спринт + прыгаем. Атака должна будет
     * дождаться isSuppressing() == false.
     */
    public void requestReset() {
        if (!isEnabled() || mc.player == null) return;
        if (critPhase != CritPhase.IDLE) return;
        critPhase = CritPhase.JUMP;
        phaseTimer.reset();
        suppressSprint();
        mc.player.jump();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            switch (critPhase) {
                case IDLE -> { /* ничего */ }

                case JUMP -> {
                    // Продолжаем глушить спринт пока не взлетели
                    suppressSprint();
                    // Подождём grace-период чтобы игрок точно оторвался от земли
                    if (phaseTimer.finished(JUMP_GRACE_MS)) {
                        critPhase = CritPhase.WAIT_FALL;
                    }
                    // Таймаут защиты
                    if (phaseTimer.finished(CRIT_TIMEOUT_MS)) cancelCrit();
                }

                case WAIT_FALL -> {
                    // Продолжаем глушить спринт
                    suppressSprint();
                    // Ждём момента, когда игрок в воздухе и начинает падать (fallDistance растёт)
                    boolean inAir    = !mc.player.isOnGround();
                    boolean falling  = mc.player.fallDistance > 0.01f;
                    boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava();
                    boolean climbing = mc.player.isClimbing();

                    if (inAir && falling && !inLiquid && !climbing) {
                        critPhase = CritPhase.READY;
                    }
                    // Таймаут защиты
                    if (phaseTimer.finished(CRIT_TIMEOUT_MS)) cancelCrit();
                }

                case READY -> {
                    // Крит гарантирован! Вызываем атаку
                    if (attackCallback != null) {
                        attackCallback.run();
                        attackCallback = null;
                    }
                    critPhase = CritPhase.IDLE;
                    // Восстанавливаем клавишу движения
                    restoreKey();
                }
            }
        }));
        addEvents(update);
    }

    /** Сбросить спринт без пакетов */
    private void suppressSprint() {
        if (mc.player == null) return;
        mc.player.setSprinting(false);
    }

    private void cancelCrit() {
        critPhase = CritPhase.IDLE;
        attackCallback = null;
        restoreKey();
    }

    private void restoreKey() {
        if (mc.player == null || mc.getWindow() == null) return;
        int keyCode = mc.options.forwardKey.getDefaultKey().getCode();
        if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyCode)) {
            mc.options.forwardKey.setPressed(true);
        }
    }
}