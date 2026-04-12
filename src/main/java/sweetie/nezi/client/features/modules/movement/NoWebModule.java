package sweetie.nezi.client.features.modules.movement;

import lombok.Getter;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.utils.player.MoveUtil;
import sweetie.nezi.api.utils.player.PlayerUtil;

@ModuleRegister(name = "No Web", category = Category.MOVEMENT)
public class NoWebModule extends Module {
    @Getter private static final NoWebModule instance = new NoWebModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (PlayerUtil.isInWeb()) {
                mc.player.setVelocity(0, 0, 0);

                double verticalSpeed = 0.995;
                double horizantalSpeed = 0.19175;

                if (mc.options.jumpKey.isPressed()) {
                    mc.player.getVelocity().y = verticalSpeed;
                } else if (mc.options.sneakKey.isPressed()) {
                    mc.player.getVelocity().y = -verticalSpeed;
                }

                MoveUtil.setSpeed(horizantalSpeed);
            }
        }));

        addEvents(updateEvent);
    }
}
