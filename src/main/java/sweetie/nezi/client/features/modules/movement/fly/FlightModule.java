package sweetie.nezi.client.features.modules.movement.fly;


import lombok.Getter;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.move.MotionEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.system.backend.Choice;
import sweetie.nezi.client.features.modules.movement.fly.modes.FlightGrim;
import sweetie.nezi.client.features.modules.movement.fly.modes.FlightVanilla;

@ModuleRegister(name = "Flight", category = Category.MOVEMENT)
public class FlightModule extends Module {
    @Getter private static final FlightModule instance = new FlightModule();

    private final FlightGrim flightGrim = new FlightGrim(() -> getMode().is("Grim"), this);
    private final FlightVanilla flightVanilla = new FlightVanilla(() -> getMode().is("Vanilla"));

    private final FlightMode[] modes = new FlightMode[]{
            flightVanilla, flightGrim
    };

    private FlightMode currentMode = flightGrim;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value(flightGrim.getName())
            .values(Choice.getValues(modes))
            .onAction(() -> {
                currentMode = (FlightMode) Choice.getChoiceByName(getMode().getValue(), modes);
            });

    public FlightModule() {
        addSettings(mode);

        addSettings(flightGrim.getSettings());
        addSettings(flightVanilla.getSettings());
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

        EventListener motionEvent = MotionEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onMotion(event);
        }));

        addEvents(updateEvent, motionEvent);
    }
}