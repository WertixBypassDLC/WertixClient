package sweetie.nezi.client.features.modules.movement.spider;

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
import sweetie.nezi.client.features.modules.movement.spider.modes.SpiderCustom;
import sweetie.nezi.client.features.modules.movement.spider.modes.SpiderCommandBlock;
import sweetie.nezi.client.features.modules.movement.spider.modes.SpiderFunTime;
import sweetie.nezi.client.features.modules.movement.spider.modes.SpiderMatrix;
import sweetie.nezi.client.features.modules.movement.spider.modes.SpiderSeaCucumber;

@ModuleRegister(name = "Spider", category = Category.MOVEMENT)
public class SpiderModule extends Module {
    @Getter private static final SpiderModule instance = new SpiderModule();

    private final SpiderFunTime spiderFunTime = new SpiderFunTime();
    private final SpiderSeaCucumber spiderSeaCucumber = new SpiderSeaCucumber();
    private final SpiderMatrix spiderMatrix = new SpiderMatrix(() -> getMode().is("Matrix"));
    private final SpiderCommandBlock spiderCommandBlock = new SpiderCommandBlock();
    private final SpiderCustom spiderCustom = new SpiderCustom(() -> getMode().is("Custom"));

    private final SpiderMode[] modes = new SpiderMode[]{
            spiderFunTime, spiderSeaCucumber, spiderMatrix, spiderCommandBlock, spiderCustom
    };

    private SpiderMode currentMode = spiderFunTime;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value(spiderFunTime.getName())
            .values(Choice.getValues(modes))
            .onAction(() -> {
                SpiderMode newMode = (SpiderMode) Choice.getChoiceByName(getMode().getValue(), modes);
                if (newMode == null || newMode == currentMode) {
                    return;
                }

                if (isEnabled()) {
                    currentMode.onDisable();
                }

                currentMode = newMode;

                if (isEnabled()) {
                    currentMode.onEnable();
                }
            });

    public SpiderModule() {
        addSettings(mode);

        for (SpiderMode spiderMode : modes) {
            addSettings(spiderMode.getSettings());
        }
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
        EventListener motionEvent = MotionEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onMotion(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onUpdate();
        }));

        addEvents(motionEvent, updateEvent);
    }
}
