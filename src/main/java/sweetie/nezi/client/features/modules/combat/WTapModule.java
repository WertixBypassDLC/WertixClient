package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.event.events.player.world.AttackEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.math.TimerUtil;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "WTap", category = Category.COMBAT)
public class WTapModule extends Module {
    @Getter private static final WTapModule instance = new WTapModule();

    private final SliderSetting minDelay = new SliderSetting("Min Delay (ms)").value(10f).range(5f, 50f).step(1f);
    private final SliderSetting maxDelay = new SliderSetting("Max Delay (ms)").value(60f).range(20f, 120f).step(1f);

    private final TimerUtil stopTimer = new TimerUtil();
    private boolean suppressing;
    private long currentStopDuration;

    public WTapModule() {
        addSettings(minDelay, maxDelay);
    }

    @Override
    public void onDisable() {
        suppressing = false;
        restoreKeys();
    }

    public boolean isSuppressing() {
        return suppressing;
    }

    @Override
    public void onEvent() {
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || !mc.player.isSprinting()) return;

            long lo = Math.max(5L, minDelay.getValue().longValue());
            long hi = Math.max(lo + 1L, maxDelay.getValue().longValue());
            currentStopDuration = ThreadLocalRandom.current().nextLong(lo, hi);

            mc.options.forwardKey.setPressed(false);
            mc.player.setSprinting(false);

            suppressing = true;
            stopTimer.reset();
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!suppressing || mc.player == null) return;

            if (stopTimer.finished(currentStopDuration)) {
                suppressing = false;
                restoreKeys();
            } else {
                mc.options.forwardKey.setPressed(false);
            }
        }));

        addEvents(attackEvent, updateEvent);
    }

    private void restoreKeys() {
        if (mc.player == null || mc.getWindow() == null) return;

        long handle = mc.getWindow().getHandle();
        int forwardKey = mc.options.forwardKey.getDefaultKey().getCode();

        if (forwardKey > 0 && InputUtil.isKeyPressed(handle, forwardKey)) {
            mc.options.forwardKey.setPressed(true);
        }
    }
}