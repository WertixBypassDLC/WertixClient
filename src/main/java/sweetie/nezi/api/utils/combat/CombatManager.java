package sweetie.nezi.api.utils.combat;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.move.SprintEvent;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.player.PlayerUtil;
import sweetie.nezi.api.utils.rotation.RaytracingUtil;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.features.modules.movement.SprintModule;

@Getter
@Accessors(fluent = true, chain = true)
public class CombatManager implements QuickImports {
    private final ClickScheduler clickScheduler = new ClickScheduler();
    private final SprintManager sprintManager = new SprintManager(SprintManager.SprintType.LEGIT);
    private final ShieldBreakManager shieldBreakManager = new ShieldBreakManager();

    private int preAttackTicks = 0;
    private int attackCount = 0;

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
        sprintManager.sprintType = switch (SprintModule.getInstance().mode.getValue()) {
            case "Packet" -> SprintManager.SprintType.PACKET;
            case "Legit" -> SprintManager.SprintType.LEGIT;
            default -> SprintManager.SprintType.NONE;
        };

        if (preAttackTicks > 0) {
            preAttackTicks--;
            if (preAttackTicks > 0) return;

            mc.interactionManager.attackEntity(mc.player, configurable.target);
            mc.player.swingHand(Hand.MAIN_HAND);
            onAttackPerformed();
            return;
        }

        if (canAttack()) {
            if (isRaytraceFailed()) return;

            if (mc.player.isBlocking() && configurable.alwaysShield) {
                mc.interactionManager.stopUsingItem(mc.player);
            }

            if (sprintManager.sprintType == SprintManager.SprintType.LEGIT && mc.player.isSprinting() && !mc.player.isSwimming()) {
                preAttackTicks = 1;
                applyLegitSprintReset();
                return;
            }

            mc.interactionManager.attackEntity(mc.player, configurable.target);
            mc.player.swingHand(Hand.MAIN_HAND);
            onAttackPerformed();
        }
    }

    public void resetState() {
        preAttackTicks = 0;
        attackCount = 0;
        clickScheduler.reset();
    }

    private void applyLegitSprintReset() {
        SprintModule sprintModule = SprintModule.getInstance();
        sprintModule.tickStop = MathUtil.randomInRange(1, 2);
        mc.player.setSprinting(false);
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
        return !clickScheduler.isCooldownComplete();
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

    private long nextAttackDelay() {
        AuraModule auraModule = AuraModule.getInstance();
        if (auraModule != null && auraModule.isEnabled() && auraModule.getAimMode().is("Fun Time")) {
            int[] funTimeTicks = new int[]{10, 11, 10, 13};
            int ticks = funTimeTicks[Math.floorMod(attackCount, funTimeTicks.length)];
            return ticks * 50L;
        }
        return MathUtil.randomInRange(470, 520);
    }
}
