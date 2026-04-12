package sweetie.nezi.client.features.modules.movement.noslow;

import lombok.Getter;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.client.features.modules.movement.noslow.modes.NoSlowSlotUpdate;

@ModuleRegister(name = "No Slow", category = Category.MOVEMENT)
public class NoSlowModule extends Module {
    @Getter private static final NoSlowModule instance = new NoSlowModule();

    private final NoSlowSlotUpdate slotUpdate = new NoSlowSlotUpdate();

    public boolean doUseNoSlow() {
        return isEnabled() && mc.player.isUsingItem();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            slotUpdate.onUpdate();
        }));

        addEvents(updateEvent);
    }
}