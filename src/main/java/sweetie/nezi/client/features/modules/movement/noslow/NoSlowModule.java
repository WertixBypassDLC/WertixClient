package sweetie.nezi.client.features.modules.movement.noslow;

import lombok.Getter;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.event.events.player.move.MoveEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.system.backend.Choice;
import sweetie.nezi.client.features.modules.movement.noslow.modes.NoSlowFuntime;
import sweetie.nezi.client.features.modules.movement.noslow.modes.NoSlowFuntimeNew;
import sweetie.nezi.client.features.modules.movement.noslow.modes.NoSlowSlotUpdate;

@ModuleRegister(name = "No Slow", category = Category.MOVEMENT)
public class NoSlowModule extends Module {
    @Getter private static final NoSlowModule instance = new NoSlowModule();

    private final NoSlowSlotUpdate slotUpdate = new NoSlowSlotUpdate();
    private final NoSlowFuntime funtime = new NoSlowFuntime();
    private final NoSlowFuntimeNew funtimeNew = new NoSlowFuntimeNew();

    private final NoSlowMode[] modes = new NoSlowMode[] {
            slotUpdate, funtime, funtimeNew
    };

    private NoSlowMode currentMode = slotUpdate;
    @Getter private final ModeSetting mode = new ModeSetting("Mode").value(slotUpdate.getName())
            .values(Choice.getValues(modes));

    public NoSlowModule() {
        mode.onAction(this::switchMode);
        addSettings(mode);
    }

    public boolean doUseNoSlow() {
        return isEnabled() && mc.player != null && mc.player.isUsingItem() && currentMode.slowingCancel();
    }

    @Override
    public void onEnable() {
        currentMode.onEnable();
    }

    @Override
    public void onDisable() {
        currentMode.onDisable();
    }

    private void switchMode() {
        NoSlowMode previousMode = currentMode;
        NoSlowMode nextMode = (NoSlowMode) Choice.getChoiceByName(mode.getValue(), modes);

        if (previousMode == nextMode) {
            return;
        }

        previousMode.onDisable();
        currentMode = nextMode;

        if (isEnabled()) {
            currentMode.onEnable();
        }
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onUpdate();
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onTick();
        }));

        EventListener moveEvent = MoveEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onMove(event);
        }));

        addEvents(updateEvent, tickEvent, moveEvent);
    }
}
