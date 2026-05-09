package sweetie.nezi.client.features.modules.movement.speed;

import lombok.Getter;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.move.TravelEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.system.backend.Choice;
import sweetie.nezi.client.features.modules.movement.speed.modes.SpeedGrim;
import sweetie.nezi.client.features.modules.movement.speed.modes.SpeedShulker;
import sweetie.nezi.client.features.modules.movement.speed.modes.SpeedVanilla;

@ModuleRegister(name = "Speed", category = Category.MOVEMENT)
public class SpeedModule extends Module {
    @Getter private static final SpeedModule instance = new SpeedModule();

    private final SpeedVanilla speedVanilla = new SpeedVanilla(() -> getMode().is("Vanilla"));
    private final SpeedGrim speedGrim = new SpeedGrim(() -> getMode().is("Grim"));
    private final SpeedShulker speedShulker = new SpeedShulker(() -> getMode().is("Shulker"));

    private final SpeedMode[] modes = new SpeedMode[]{
            speedVanilla,
            speedGrim,
            speedShulker
    };

    private SpeedMode currentMode = speedVanilla;

    @Getter private final ModeSetting mode = new ModeSetting("Mode")
            .values(Choice.getValues(modes))
            .value("Vanilla")
            .onAction(() -> {
                SpeedMode m = (SpeedMode) Choice.getChoiceByName(getMode().getValue(), modes);
                if (m != null) currentMode = m;
            });

    public SpeedModule() {
        addSettings(mode);

        addSettings(speedVanilla.getSettings());
        addSettings(speedGrim.getSettings());
        addSettings(speedShulker.getSettings());

        // чтобы currentMode сразу совпадал с mode.value() / конфигом
        SpeedMode m = (SpeedMode) Choice.getChoiceByName(getMode().getValue(), modes);
        if (m != null) currentMode = m;
    }

    @Override
    public void toggle() {
        super.toggle();
        currentMode.toggle();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentMode.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentMode.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onUpdate();
        }));

        EventListener travelEvent = TravelEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onTravel();
        }));

        addEvents(updateEvent, travelEvent);
    }
}
