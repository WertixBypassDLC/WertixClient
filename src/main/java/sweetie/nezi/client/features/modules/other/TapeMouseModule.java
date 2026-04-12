package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.math.TimerUtil;

@ModuleRegister(name = "Tape Mouse", category = Category.OTHER)
public class TapeMouseModule extends Module {
    @Getter private static final TapeMouseModule instance = new TapeMouseModule();

    private final BooleanSetting attack = new BooleanSetting("Атака").value(true);
    private final SliderSetting attackDelay = new SliderSetting("Задержка атаки").value(10f).range(1f, 20f).step(1f).setVisible(attack::getValue);
    private final BooleanSetting use = new BooleanSetting("Использование").value(false);
    private final SliderSetting useDelay = new SliderSetting("Задержка использования").value(10f).range(1f, 20f).step(1f).setVisible(use::getValue);

    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil useTimer = new TimerUtil();

    public TapeMouseModule() {
        addSettings(attack, attackDelay, use, useDelay);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (attack.getValue()) {
                handleAction(attackDelay.getValue(), attackTimer, () -> mc.doAttack());
            }
            if (use.getValue()) {
                handleAction(useDelay.getValue(), useTimer, () -> mc.doItemUse());
            }
        }));

        addEvents(tickEvent);
    }

    private void handleAction(float delay, TimerUtil timer, Runnable action) {
        if (timer.finished(delay * 50L)) {
            action.run();
            timer.reset();
        }
    }
}
