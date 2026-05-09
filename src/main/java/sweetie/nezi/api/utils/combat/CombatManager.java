package sweetie.nezi.api.utils.combat;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.move.SprintEvent;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.player.PlayerUtil;
import sweetie.nezi.api.utils.rotation.RaytracingUtil;
import sweetie.nezi.api.utils.rotation.rotations.FunTimeRotation;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.features.modules.combat.WTapModule;
import sweetie.nezi.client.features.modules.movement.SprintModule;
import sweetie.nezi.client.features.modules.render.SwingAnimationModule;

@Getter
@Accessors(fluent = true, chain = true)
public class CombatManager implements QuickImports {
    private static final int[] DEFAULT_ATTACK_TICKS = {10, 11};
    private static final int[] FUN_TIME_ATTACK_TICKS = {10, 11, 10, 13};

    private static final int SPRINT_RESET_DEFAULT_TICKS = 2;
    private static final int SPRINT_RESET_MIN_TICKS = 0;
    private static final int SPRINT_RESET_MAX_ARM_TICKS = 6;
    private static final int SPRINT_RESET_MAX_WAIT_TICKS = 6;
    private static final int COOLDOWN_LOOKAHEAD_TICKS = 20;
    private static final float FULL_ATTACK_COOLDOWN = 0.9F;
    private static final float CRITICAL_ATTACK_COOLDOWN = 0.9F;

    private final ClickScheduler clickScheduler = new ClickScheduler();
    private final SprintManager sprintManager = new SprintManager(SprintManager.SprintType.LEGIT);
    private final ShieldBreakManager shieldBreakManager = new ShieldBreakManager();

    private int attackCount = 0;
    private boolean sprintResetArmed = false;
    private boolean sprintResetShouldRestore = false;
    private boolean sprintKeyForcedRelease = false;
    private int sprintResetReadyTick = Integer.MIN_VALUE;
    private int sprintResetExpireTick = Integer.MIN_VALUE;

    public CombatManager() {
        SprintEvent.getInstance().subscribe(new Listener<>(1, event -> {
            boolean oneTick = clickScheduler.isOneTickBeforeAttack();

            boolean rule = configurable != null && configurable.target != null
                    && configurable.onlyCrits
                    && !mc.player.isOnGround() && !shouldCancelCrit()
                    && (oneTick && mc.player.getVelocity().y <= .16477328182606651);

            sprintManager.legitSprint(event, rule);
        }));
    }

    @Getter @Setter
    private CombatExecutor.CombatConfigurable configurable;

    public void handleAttack() {
        if (mc.player == null || mc.interactionManager == null) {
            releaseSprintReset();
            return;
        }

        sprintManager.sprintType = switch (SprintModule.getInstance().mode.getValue()) {
            case "Packet" -> SprintManager.SprintType.PACKET;
            case "Legit" -> SprintManager.SprintType.LEGIT;
            default -> SprintManager.SprintType.NONE;
        };

        if (configurable == null || configurable.target == null) {
            releaseSprintReset();
            return;
        }

        tickSprintResetTimeout();

        if (!canUseLegitSprintReset()) {
            releaseSprintReset();
        } else if (sprintResetArmed) {
            applyLegitSprintReset();
        }

        if (shouldSuppressFunTimeAttack()) {
            maybeRestoreArmedSprint();
            return;
        }

        int configuredLeadTicks = getConfiguredSprintResetTicks();
        int ticksUntilAttackWindow = getTicksUntilAttackWindow();
        boolean smartResetActive = canUseLegitSprintReset() && configurable.onlyCrits;

        if (smartResetActive && (mc.player.isSprinting() || sprintResetArmed)) {
            if (!sprintResetArmed) {
                if (ticksUntilAttackWindow <= configuredLeadTicks) {
                    int waitTicks = Math.max(SPRINT_RESET_MIN_TICKS, ticksUntilAttackWindow);
                    armSprintReset(waitTicks);
                    if (waitTicks > 0) {
                        return;
                    }
                } else {
                    releaseSprintReset();
                    return;
                }
            } else if (mc.player.age < sprintResetReadyTick) {
                return;
            }
        }

        boolean readyToAttack = ticksUntilAttackWindow <= 0 && canAttack();
        if (!readyToAttack) {
            maybeRestoreArmedSprint();
            return;
        }

        if (isRaytraceFailed()) {
            releaseSprintReset();
            return;
        }

        if (mc.player.isBlocking() && configurable.unPressShield) {
            mc.interactionManager.stopUsingItem(mc.player);
        }

        if (!smartResetActive && !sprintResetArmed && canUseLegitSprintReset() && mc.player.isSprinting()) {
            armSprintReset(configuredLeadTicks);
            if (configuredLeadTicks > 0) {
                return;
            }
        }

        if (sprintResetArmed && mc.player.age < sprintResetReadyTick) {
            return;
        }

        if (canUseLegitSprintReset() && (mc.player.isSprinting() || sprintResetArmed)) {
            applyLegitSprintReset();
        }

        mc.interactionManager.attackEntity(mc.player, configurable.target);
        mc.player.swingHand(Hand.MAIN_HAND);
        SwingAnimationModule.getInstance().notifyHit(configurable.target);
        onAttackPerformed();
        finishSprintReset(true);
    }

    public void resetState() {
        finishSprintReset(true);
        attackCount = 0;
        clickScheduler.reset();
    }

    public void releaseSprintReset() {
        if (WTapModule.getInstance().isSuppressing()) {
            WTapModule.getInstance().releaseReset();
        }
        finishSprintReset(true);
    }

    public boolean canAttack() {
        if (isCooldownNotComplete()) return false;
        if (configurable.noAttackIfEat && PlayerUtil.isEating()) return false;

        if (configurable.shieldBreak && configurable.target instanceof PlayerEntity player && shieldBreakManager.shouldBreakShield(player)) {
            return true;
        }

        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && configurable.onlyCrits && configurable.smartCrits)
            return true;

        if (!mc.options.jumpKey.isPressed() && PlayerUtil.isAboveWater())
            return true;

        return shouldDoHit() || !configurable.onlyCrits;
    }

    public boolean canAttackPreview() {
        return canAttackPreview(0);
    }

    public boolean canAttackPreview(int ticksAhead) {
        if (configurable == null || mc.player == null) {
            return false;
        }

        int safeTicksAhead = Math.max(0, ticksAhead);
        for (int tick = 0; tick <= safeTicksAhead; tick++) {
            if (canAttackPreviewAt(tick)) {
                return true;
            }
        }

        return false;
    }

    private void applyLegitSprintReset() {
        if (WTapModule.getInstance().requestReset()) {
            sprintResetShouldRestore = false;
            sprintKeyForcedRelease = false;
            return;
        }

        SprintModule sprintModule = SprintModule.getInstance();
        sprintModule.tickStop = Math.max(sprintModule.tickStop, 2);

        if (mc.options != null) {
            mc.options.sprintKey.setPressed(false);
            sprintKeyForcedRelease = true;
        }

        mc.player.setSprinting(false);
    }

    private boolean isRaytraceFailed() {
        if (!configurable.raytrace) {
            return false;
        }

        if (configurable.hitBox != null) {
            return !RaytracingUtil.rayTrace(configurable.rotation.getVector(), configurable.distance - 0.25F, configurable.hitBox);
        }

        EntityHitResult hitResult = RaytracingUtil.raytraceEntity(
                configurable.distance,
                configurable.rotation,
                configurable.ignoreWalls
        );

        return hitResult == null || hitResult.getEntity() != configurable.target;
    }

    private boolean isCooldownNotComplete() {
        boolean dynamicCooldown = configurable != null && configurable.dynamicCooldown;
        return !clickScheduler.isCooldownComplete(dynamicCooldown, 0);
    }

    private boolean canAttackPreviewAt(int ticksAhead) {
        boolean dynamicCooldown = configurable != null && configurable.dynamicCooldown;
        if (!clickScheduler.isCooldownComplete(dynamicCooldown, ticksAhead)) {
            return false;
        }

        if (configurable.noAttackIfEat && PlayerUtil.isEating()) {
            return false;
        }

        if (configurable.shieldBreak && configurable.target instanceof PlayerEntity player && shieldBreakManager.canBreakShield(player)) {
            return true;
        }

        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && configurable.onlyCrits && configurable.smartCrits) {
            return true;
        }

        if (!mc.options.jumpKey.isPressed() && PlayerUtil.isAboveWater()) {
            return true;
        }

        return shouldDoHit() || !configurable.onlyCrits;
    }

    private boolean shouldCancelCrit() {
        return mc.player.hasStatusEffect(StatusEffects.BLINDNESS) ||
                mc.player.hasStatusEffect(StatusEffects.LEVITATION) ||
                mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) ||
                PlayerUtil.isInWeb() ||
                mc.player.isInLava() ||
                mc.player.isClimbing() ||
                mc.player.isRiding() ||
                mc.player.hasVehicle() ||
                mc.player.isSubmergedInWater() ||
                mc.player.hasNoGravity() ||
                mc.player.getAbilities().flying;
    }

    private boolean shouldDoHit() {
        return (!mc.player.isOnGround() && mc.player.fallDistance > 0.0f) || shouldCancelCrit();
    }

    private void onAttackPerformed() {
        attackCount++;
        clickScheduler.recalculate(nextAttackDelay());
    }

    private boolean shouldSuppressFunTimeAttack() {
        AuraModule auraModule = AuraModule.getInstance();
        return auraModule != null
                && auraModule.isEnabled()
                && auraModule.getAimMode().is("Fun Time")
                && FunTimeRotation.isForceMissActive();
    }

    private long nextAttackDelay() {
        AuraModule auraModule = AuraModule.getInstance();
        int[] pattern = auraModule != null && auraModule.isEnabled() && auraModule.getAimMode().is("Fun Time")
                ? FUN_TIME_ATTACK_TICKS
                : DEFAULT_ATTACK_TICKS;

        int ticks = pattern[Math.floorMod(attackCount, pattern.length)];
        return ticks * 50L;
    }

    private boolean canUseLegitSprintReset() {
        return sprintManager.sprintType == SprintManager.SprintType.LEGIT
                && mc.player != null
                && !mc.player.isSwimming()
                && !mc.player.isGliding();
    }

    private void armSprintReset(int leadTicks) {
        if (!canUseLegitSprintReset()) {
            return;
        }

        int clampedLeadTicks = Math.max(SPRINT_RESET_MIN_TICKS, Math.min(SPRINT_RESET_MAX_ARM_TICKS, leadTicks));
        sprintResetArmed = true;
        sprintResetShouldRestore = true;
        sprintResetReadyTick = mc.player.age + clampedLeadTicks;
        sprintResetExpireTick = sprintResetReadyTick + SPRINT_RESET_MAX_WAIT_TICKS;
        applyLegitSprintReset();
    }

    private void tickSprintResetTimeout() {
        if (!sprintResetArmed || mc.player == null) {
            return;
        }

        if (mc.player.age > sprintResetExpireTick) {
            finishSprintReset(true);
        }
    }

    private void maybeRestoreArmedSprint() {
        if (!sprintResetArmed || mc.player == null) {
            return;
        }

        if (!canUseLegitSprintReset()) {
            finishSprintReset(true);
            return;
        }

        if (!configurable.onlyCrits && mc.player.age >= sprintResetReadyTick) {
            finishSprintReset(true);
            return;
        }

        if (mc.player.age > sprintResetExpireTick) {
            finishSprintReset(true);
        }
    }

    private int getTicksUntilAttackWindow() {
        long timeToNext = configurable.dynamicCooldown ? Math.max(0L, clickScheduler.toNext()) : 0L;
        int delayTicks = msToTicksCeil(timeToNext);
        float requiredCooldown = configurable.onlyCrits ? CRITICAL_ATTACK_COOLDOWN : FULL_ATTACK_COOLDOWN;
        int cooldownTicks = getTicksUntilCooldownReady(requiredCooldown);
        int critTicks = getTicksUntilEarliestCritical();
        return Math.max(delayTicks, Math.max(cooldownTicks, critTicks));
    }

    private int getConfiguredSprintResetTicks() {
        if (configurable == null) {
            return SPRINT_RESET_DEFAULT_TICKS;
        }

        return Math.max(SPRINT_RESET_MIN_TICKS, Math.min(SPRINT_RESET_MAX_ARM_TICKS, configurable.sprintResetTicks));
    }

    private static int msToTicksCeil(long ms) {
        if (ms <= 0L) {
            return 0;
        }

        return (int) ((ms + 49L) / 50L);
    }

    private int getTicksUntilCooldownReady(float requiredCooldown) {
        if (mc.player == null) {
            return 0;
        }

        if (mc.player.getAttackCooldownProgress(0.0F) >= requiredCooldown) {
            return 0;
        }

        for (int ticks = 1; ticks <= COOLDOWN_LOOKAHEAD_TICKS; ticks++) {
            if (mc.player.getAttackCooldownProgress(ticks) >= requiredCooldown) {
                return ticks;
            }
        }

        return COOLDOWN_LOOKAHEAD_TICKS;
    }

    private int getTicksUntilEarliestCritical() {
        if (mc.player == null || configurable == null) {
            return 0;
        }

        if (!configurable.onlyCrits || configurable.smartCrits || mc.player.isGliding()) {
            return 0;
        }

        if (shouldDoHit()) {
            return 0;
        }

        if (mc.player.isOnGround()) {
            return 2;
        }

        return mc.player.getVelocity().y > 0.0D ? 2 : 1;
    }

    private void finishSprintReset(boolean restoreSprint) {
        if (restoreSprint
                && sprintResetShouldRestore
                && mc.player != null
                && !mc.player.isGliding()) {
            mc.player.setSprinting(true);
        }

        restoreSprintKeyState();
        clearSprintResetState();
    }

    private void restoreSprintKeyState() {
        if (!sprintKeyForcedRelease || mc.options == null) {
            return;
        }

        boolean physicalPressed = mc.getWindow() != null
                && InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.sprintKey.getDefaultKey().getCode());
        mc.options.sprintKey.setPressed(physicalPressed);
        sprintKeyForcedRelease = false;
    }

    private void clearSprintResetState() {
        sprintResetArmed = false;
        sprintResetShouldRestore = false;
        sprintKeyForcedRelease = false;
        sprintResetReadyTick = Integer.MIN_VALUE;
        sprintResetExpireTick = Integer.MIN_VALUE;
    }
}
