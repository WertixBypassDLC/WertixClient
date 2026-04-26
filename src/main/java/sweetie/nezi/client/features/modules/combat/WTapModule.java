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

@ModuleRegister(name = "WTap", category = Category.COMBAT)
public class WTapModule extends Module {
    @Getter private static final WTapModule instance = new WTapModule();

    private enum Phase { IDLE, SUPPRESSING }

    private volatile Phase phase = Phase.IDLE;
    private final TimerUtil phaseTimer = new TimerUtil();

    private static final long SUPPRESS_MS = 60L;

    public WTapModule() {
        setEnabled(true);
    }

    @Override
    public void onDisable() {
        cancel();
    }

    public boolean isSuppressing() {
        return phase == Phase.SUPPRESSING;
    }

    public boolean requestCritAttack(Runnable onAttack) {
        if (!isEnabled() || mc.player == null) return false;

        if (mc.player.isSprinting() || mc.options.forwardKey.isPressed()) {
            mc.options.forwardKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
            mc.player.setSprinting(false);

            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }

        phase = Phase.SUPPRESSING;
        phaseTimer.reset();

        onAttack.run();

        return true;
    }

    public void requestReset() {
        if (!isEnabled() || mc.player == null) return;

        if (mc.player.isSprinting() || mc.options.forwardKey.isPressed()) {
            mc.options.forwardKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
            mc.player.setSprinting(false);

            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }

        phase = Phase.SUPPRESSING;
        phaseTimer.reset();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            if (phase == Phase.SUPPRESSING) {
                mc.options.forwardKey.setPressed(false);
                mc.options.sprintKey.setPressed(false);
                mc.player.setSprinting(false);

                if (phaseTimer.finished(SUPPRESS_MS)) {
                    cancel();
                }
            }
        }));
        addEvents(update);
    }

    private void cancel() {
        phase = Phase.IDLE;
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