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

@ModuleRegister(name = "WTap", category = Category.COMBAT)
public class WTapModule extends Module {
    @Getter private static final WTapModule instance = new WTapModule();

    private enum Phase { IDLE, SUPPRESSING }

    private volatile Phase phase = Phase.IDLE;
    private final TimerUtil phaseTimer = new TimerUtil();

    // Сколько миллисекунд удерживать отжатым спринт для создания Knockback-эффекта
    private static final long SUPPRESS_MS = 100L;

    public WTapModule() {
        setEnabled(true);
    }

    @Override
    public void onDisable() {
        cancel();
    }

    /** Активен ли цикл подавления спринта прямо сейчас */
    public boolean isSuppressing() {
        return phase == Phase.SUPPRESSING;
    }

    /**
     * Вызывается из TriggerBot / Aura перед атакой.
     * Запускает классический WTap (сброс спринта на короткое время)
     * и сразу вызывает саму атаку.
     */
    public boolean requestCritAttack(Runnable onAttack) {
        if (!isEnabled() || mc.player == null) return false;

        phase = Phase.SUPPRESSING;
        phaseTimer.reset();

        // Глушим спринт перед ударом
        suppressSprint();

        // Сразу вызываем атаку, прыжок больше не инициируется
        onAttack.run();

        return true;
    }

    /**
     * Вызывается из старых систем для ручного сброса спринта.
     */
    public void requestReset() {
        if (!isEnabled() || mc.player == null) return;
        phase = Phase.SUPPRESSING;
        phaseTimer.reset();
        suppressSprint();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            if (phase == Phase.SUPPRESSING) {
                // Продолжаем глушить спринт заданное время
                suppressSprint();

                if (phaseTimer.finished(SUPPRESS_MS)) {
                    cancel();
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

    private void cancel() {
        phase = Phase.IDLE;
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