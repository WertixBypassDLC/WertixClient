package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;

@ModuleRegister(name = "Fast Break", category = Category.OTHER)
public class FastBreakModule extends Module {
    @Getter private static final FastBreakModule instance = new FastBreakModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            mc.interactionManager.blockBreakingCooldown = 0;
            mc.interactionManager.cancelBlockBreaking();
        }));

        addEvents(updateEvent);
    }
}
