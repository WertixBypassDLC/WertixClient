package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.client.features.modules.movement.SprintModule;

import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "WTap", category = Category.COMBAT)
public class WTapModule extends Module {
    @Getter private static final WTapModule instance = new WTapModule();

    private enum Phase { IDLE, SUPPRESSING }

    private volatile Phase phase = Phase.IDLE;
    private final TimerUtil phaseTimer = new TimerUtil();

    private long suppressMs = 60L;
    private int sprintStopTicks = 2;
    private boolean suppressForward;

    public WTapModule() {
    }

    @Override
    public void onDisable() {
        cancel();
    }

    public boolean isSuppressing() {
        return phase == Phase.SUPPRESSING;
    }

    public boolean requestCritAttack(Runnable onAttack) {
        if (!beginSuppress(true)) return false;
        onAttack.run();
        return true;
    }

    public boolean requestReset() {
        return beginSuppress(false);
    }

    public void releaseReset() {
        cancel();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            if (phase == Phase.SUPPRESSING) {
                applySuppression();

                if (phaseTimer.finished(suppressMs)) {
                    cancel();
                }
            }
        }));
        addEvents(update);
    }

    private boolean beginSuppress(boolean attacking) {
        if (!isEnabled() || mc.player == null || mc.options == null) {
            return false;
        }

        long sampledSuppressMs = ThreadLocalRandom.current().nextLong(attacking ? 46L : 38L, attacking ? 87L : 73L);
        int sampledSprintStop = ThreadLocalRandom.current().nextInt(attacking ? 2 : 1, 4);
        boolean sampledForwardSuppress = mc.player.input != null
                && mc.player.input.hasForwardMovement()
                && ThreadLocalRandom.current().nextFloat() < (attacking ? 0.58f : 0.34f);

        if (phase != Phase.SUPPRESSING) {
            phase = Phase.SUPPRESSING;
            phaseTimer.reset();
            suppressMs = sampledSuppressMs;
        } else {
            suppressMs = Math.max(suppressMs, phaseTimer.getElapsedTime() + sampledSuppressMs);
        }

        sprintStopTicks = Math.max(sprintStopTicks, sampledSprintStop);
        suppressForward = suppressForward || sampledForwardSuppress;
        applySuppression();
        return true;
    }

    private void applySuppression() {
        SprintModule.getInstance().tickStop = Math.max(SprintModule.getInstance().tickStop, sprintStopTicks);
        mc.options.sprintKey.setPressed(false);
        if (suppressForward) {
            mc.options.forwardKey.setPressed(false);
        }

        boolean wasSprinting = mc.player.isSprinting();
        mc.player.setSprinting(false);

        if (wasSprinting && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
    }

    private void cancel() {
        phase = Phase.IDLE;
        suppressMs = 60L;
        sprintStopTicks = 2;
        suppressForward = false;
        restoreKey();
    }

    private void restoreKey() {
        if (mc.player == null || mc.getWindow() == null) return;

        long handle = mc.getWindow().getHandle();
        int forwardCode = mc.options.forwardKey.getDefaultKey().getCode();
        int sprintCode = mc.options.sprintKey.getDefaultKey().getCode();

        if (InputUtil.isKeyPressed(handle, forwardCode)) {
            mc.options.forwardKey.setPressed(true);
        }
        if (InputUtil.isKeyPressed(handle, sprintCode)) {
            mc.options.sprintKey.setPressed(true);
        }
    }
}