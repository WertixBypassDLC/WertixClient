package sweetie.nezi.api.utils.combat;

import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.client.features.modules.combat.AuraModule;

public class ClickScheduler implements QuickImports {
    private static final int[] DEFAULT_ATTACK_TICKS = {10, 11};
    private static final int[] FUN_TIME_ATTACK_TICKS = {10, 11, 10, 13};

    private long delay = 0;
    private long lastClickTime = System.currentTimeMillis();

    public boolean isCooldownComplete() {
        float currentCooldown = mc.player != null ? mc.player.getAttackCooldownProgress(0.5f) : 1.0f;
        long currentTime = System.currentTimeMillis();
        long timeSinceLastClick = currentTime - lastClickTime;

        boolean wasComplete = timeSinceLastClick >= delay && currentCooldown > 0.9f;
        return wasComplete;
    }

    public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
        if (mc.player == null) {
            return true;
        }

        boolean dynamicReady = !dynamicCooldown || hasTicksElapsedSinceLastClick(Math.max(0, tickCount() - ticks));
        return dynamicReady && mc.player.getAttackCooldownProgress(ticks) > 0.9F;
    }

    public boolean hasTicksElapsedSinceLastClick(int ticks) {
        return lastClickPassed() >= (ticks * 50L);
    }

    public long lastClickPassed() {
        return System.currentTimeMillis() - lastClickTime;
    }

    public void recalculate(long delay) {
        lastClickTime = System.currentTimeMillis();
        this.delay = delay;
    }

    public void reset() {
        delay = 0;
        lastClickTime = System.currentTimeMillis();
    }

    public boolean isOneTickBeforeAttack() {
        return willClickAt(3);
    }

    public boolean willClickAt(int tick) {
        long timeSinceLastClick = lastClickPassed();
        long time = tick * 50L;
        return timeSinceLastClick >= (delay - time) && timeSinceLastClick < delay + time;
    }

    public long toNext() {
        return delay - lastClickPassed();
    }

    public float cooldownProgress(int baseTime) {
        return (mc.player.lastAttackedTicks + baseTime) / mc.player.getAttackCooldownProgressPerTick();
    }

    private int tickCount() {
        AuraModule auraModule = AuraModule.getInstance();
        int[] pattern = auraModule != null && auraModule.isEnabled() && auraModule.getAimMode().is("Fun Time")
                ? FUN_TIME_ATTACK_TICKS
                : DEFAULT_ATTACK_TICKS;

        int attackCount = auraModule != null ? auraModule.combatExecutor.combatManager().attackCount() : 0;
        return pattern[Math.floorMod(attackCount, pattern.length)];
    }
}
