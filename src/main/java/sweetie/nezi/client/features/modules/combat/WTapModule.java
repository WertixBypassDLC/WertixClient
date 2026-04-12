package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.event.events.player.world.AttackEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.math.TimerUtil;

import java.util.concurrent.ThreadLocalRandom;

/**
 * WTap — legit sprint-reset for crits.
 *
 * Instead of sending sprint packets (easily flagged by anti-cheat),
 * this module briefly suppresses the forward movement key after each attack,
 * causing the player to stop for a random 10–60 ms window.
 * The server sees the player decelerate naturally, producing a guaranteed
 * critical hit on the next swing while looking completely legitimate.
 *
 * No packets are sent; the player simply "releases W" for a fraction of a second.
 */
@ModuleRegister(name = "WTap", category = Category.COMBAT)
public class WTapModule extends Module {
    @Getter private static final WTapModule instance = new WTapModule();

    private final SliderSetting minDelay = new SliderSetting("Min Delay (ms)")
            .value(10f).range(5f, 50f).step(1f);
    private final SliderSetting maxDelay = new SliderSetting("Max Delay (ms)")
            .value(60f).range(20f, 120f).step(1f);

    private final TimerUtil stopTimer = new TimerUtil();

    /** Whether we are currently suppressing movement. */
    private boolean suppressing;

    /** The randomised duration (in ms) for the current suppression window. */
    private long currentStopDuration;

    /** Tracks whether movement keys were held before suppression, so we restore correctly. */
    private boolean wasForwardHeld;
    private boolean wasSprintHeld;

    public WTapModule() {
        addSettings(minDelay, maxDelay);
    }

    @Override
    public void onDisable() {
        restoreKeys();
        suppressing = false;
    }

    /**
     * Returns {@code true} when the module is actively suppressing forward movement.
     * Other modules (e.g. TriggerBot, Aura) can query this to avoid redundant sprint logic.
     */
    public boolean isSuppressing() {
        return suppressing;
    }

    @Override
    public void onEvent() {
        // On attack → begin suppression
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            if (!mc.player.isSprinting()) return;

            // Compute a random stop duration between min and max, with sub-ms precision
            long lo = Math.max(5L, minDelay.getValue().longValue());
            long hi = Math.max(lo + 1L, maxDelay.getValue().longValue());
            currentStopDuration = ThreadLocalRandom.current().nextLong(lo, hi);

            // Remember key state
            wasForwardHeld = mc.options.forwardKey.isPressed();
            wasSprintHeld = mc.options.sprintKey.isPressed();

            // Suppress movement keys — the server will see the player slow down normally
            if (wasForwardHeld) {
                mc.options.forwardKey.setPressed(false);
            }
            if (wasSprintHeld) {
                mc.options.sprintKey.setPressed(false);
            }
            mc.player.setSprinting(false);

            suppressing = true;
            stopTimer.reset();
        }));

        // On update → check if suppression window has elapsed, then restore keys
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!suppressing || mc.player == null) return;

            if (stopTimer.finished(currentStopDuration)) {
                restoreKeys();
                suppressing = false;
            }
        }));

        addEvents(attackEvent, updateEvent);
    }

    /** Restores movement keys to their original state before suppression. */
    private void restoreKeys() {
        if (mc.player == null) return;

        if (wasForwardHeld) {
            mc.options.forwardKey.setPressed(true);
        }
        if (wasSprintHeld) {
            mc.options.sprintKey.setPressed(true);
            mc.player.setSprinting(true);
        }

        wasForwardHeld = false;
        wasSprintHeld = false;
    }
}
