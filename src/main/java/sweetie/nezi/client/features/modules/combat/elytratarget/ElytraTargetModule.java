package sweetie.nezi.client.features.modules.combat.elytratarget;

import lombok.Getter;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.other.RotationUpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;

@ModuleRegister(name = "Elytra Target", category = Category.COMBAT)
public class ElytraTargetModule extends Module {
    @Getter private static final ElytraTargetModule instance = new ElytraTargetModule();
    public final ElytraRotationProcessor elytraRotationProcessor = new ElytraRotationProcessor(this);

    public ElytraTargetModule() {
        addSettings(elytraRotationProcessor.getSettings());
    }

    @Override
    public void onEvent() {
        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            elytraRotationProcessor.processRotation();
        }));

        addEvents(rotationUpdateEvent);
    }
}
