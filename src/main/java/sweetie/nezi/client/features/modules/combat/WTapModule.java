package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.world.AttackEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.math.TimerUtil;

/**
 * WTap — briefly stops and re-starts sprinting on each attack to apply extra knockback.
 * The sprint is toggled off for a configurable number of milliseconds after each hit.
 */
@ModuleRegister(name = "WTap", category = Category.COMBAT)
public class WTapModule extends Module {
    @Getter private static final WTapModule instance = new WTapModule();

    private final SliderSetting delay = new SliderSetting("Delay (ms)").value(50f).range(20f, 200f).step(5f);

    private final TimerUtil tapTimer = new TimerUtil();
    private boolean waitingToResprint;

    public WTapModule() {
        addSettings(delay);
    }

    @Override
    public void onDisable() {
        waitingToResprint = false;
    }

    @Override
    public void onEvent() {
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.player.networkHandler == null) {
                return;
            }

            if (!mc.player.isSprinting()) {
                return;
            }

            // Stop sprinting via packet
            mc.player.networkHandler.sendPacket(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING)
            );
            mc.player.setSprinting(false);
            waitingToResprint = true;
            tapTimer.reset();
        }));

        EventListener updateEvent = sweetie.nezi.api.event.events.player.other.UpdateEvent.getInstance()
                .subscribe(new Listener<>(event -> {
                    if (!waitingToResprint || mc.player == null || mc.player.networkHandler == null) {
                        return;
                    }

                    if (tapTimer.finished(delay.getValue().longValue())) {
                        mc.player.networkHandler.sendPacket(
                                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING)
                        );
                        mc.player.setSprinting(true);
                        waitingToResprint = false;
                    }
                }));

        addEvents(attackEvent, updateEvent);
    }
}
