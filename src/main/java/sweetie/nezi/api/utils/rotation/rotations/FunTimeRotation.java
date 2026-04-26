package sweetie.nezi.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationMode;
import sweetie.nezi.client.features.modules.combat.AuraModule;

import java.security.SecureRandom;

public class FunTimeRotation extends RotationMode implements QuickImports {
    public static final String MODE_NAME = "FunTimeSnap";
    private static final long VISUAL_FREEZE_DURATION_MS = 380L;

    private static long lastAttackTimeMs = System.currentTimeMillis();
    private static int attackCount = 0;
    private static final long FORCE_MISS_DURATION_MS = 320L;
    private static boolean forceMissActive = false;
    private static boolean forceMissSwinged = false;
    private static long forceMissEndTime = 0L;
    private static int lastForceMissAttackCount = -1;
    private static Rotation visualRotation;
    private static Rotation previousVisualRotation;

    private final SecureRandom secureRandom = new SecureRandom();

    public FunTimeRotation() {
        super(MODE_NAME);
    }

    public static void reset() {
        lastAttackTimeMs = System.currentTimeMillis();
        attackCount = 0;
        forceMissActive = false;
        forceMissSwinged = false;
        forceMissEndTime = 0L;
        lastForceMissAttackCount = -1;
        visualRotation = null;
        previousVisualRotation = null;
    }

    public static void onAttack() {
        attackCount++;
        lastAttackTimeMs = System.currentTimeMillis();
        freezeVisualRotation();
    }

    public static Vec3d getAimPoint(Entity entity) {
        if (entity == null) {
            return Vec3d.ZERO;
        }
        return entity.getPos().add(0.0, MathHelper.clamp(entity.getEyeHeight(entity.getPose()), 0.1, entity.getHeight()), 0.0);
    }

    public static Vec3d getRandomOffsetVelocity() {
        return Vec3d.ZERO;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (mc.player == null) {
            return currentRotation;
        }

        AuraModule aura = AuraModule.getInstance();
        boolean hasTarget = aura != null && aura.isEnabled() && aura.target != null;
        boolean keepVisualFreeze = shouldKeepVisualFreeze(hasTarget);
        if (!keepVisualFreeze) {
            clearVisualRotation();
        }

        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float rotationDifference = Math.max((float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta)), 1.0E-4F);
        boolean canAttack = entity != null && aura != null && aura.isEnabled() && canAttackNow(aura);

        float yawJitter = canAttack
                ? 0.0F
                : (float) (randomBetween(5.6F, 8.0F) * Math.sin(System.currentTimeMillis() / 28.0D));
        float pitchJitter = canAttack
                ? 0.0F
                : (float) (randomBetween(2.8F, 4.4F) * Math.cos(System.currentTimeMillis() / 28.0D));
        float speed = canAttack
                ? 1.0F
                : mc.player.age % 2 == 0
                ? randomBetween(0.52F, 0.72F)
                : randomBetween(0.08F, 0.22F);

        float yawLimit = Math.abs(yawDelta / rotationDifference) * 180.0F;
        float pitchLimit = Math.abs(pitchDelta / rotationDifference) * 180.0F;
        float moveYaw = MathHelper.clamp(yawDelta, -yawLimit, yawLimit);
        float movePitch = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit);

        float yaw = MathHelper.lerp(randomBetween(speed, speed + 0.24F), currentRotation.getYaw(), currentRotation.getYaw() + moveYaw) + yawJitter;
        float pitch = MathHelper.lerp(randomBetween(speed, speed + 0.24F), currentRotation.getPitch(), currentRotation.getPitch() + movePitch) + pitchJitter;

        return finalizeRotation(currentRotation, yaw, pitch);
    }

    public static Rotation getVisualRotation() {
        return visualRotation;
    }

    public static Rotation getPreviousVisualRotation() {
        return previousVisualRotation != null ? previousVisualRotation : visualRotation;
    }

    public static boolean hasVisualRotation() {
        return visualRotation != null;
    }

    private boolean canAttackNow(AuraModule aura) {
        try {
            return aura.combatExecutor.combatManager().canAttack();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean shouldKeepVisualFreeze(boolean hasTarget) {
        return hasTarget && attackCount > 0 && elapsedSinceAttack() <= VISUAL_FREEZE_DURATION_MS;
    }

    private static long elapsedSinceAttack() {
        return Math.max(0L, System.currentTimeMillis() - lastAttackTimeMs);
    }

    private Rotation finalizeRotation(Rotation currentRotation, float yaw, float pitch) {
        return new Rotation(
                applyGCD(yaw, currentRotation.getYaw()),
                MathHelper.clamp(applyGCD(pitch, currentRotation.getPitch()), -90.0F, 90.0F)
        );
    }

    private static void freezeVisualRotation() {
        if (mc == null || mc.player == null) {
            return;
        }

        Rotation frozen = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        previousVisualRotation = visualRotation != null ? visualRotation : frozen;
        visualRotation = frozen;
    }

    private static void clearVisualRotation() {
        visualRotation = null;
        previousVisualRotation = null;
    }

    private static void activateForceMiss(int currentAttackCount) {
        if (currentAttackCount == lastForceMissAttackCount) {
            return;
        }

        lastForceMissAttackCount = currentAttackCount;
        forceMissActive = true;
        forceMissSwinged = true;
        forceMissEndTime = System.currentTimeMillis() + FORCE_MISS_DURATION_MS;
    }

    public static boolean isForceMissActive() {
        if (!forceMissActive) {
            return false;
        }
        if (System.currentTimeMillis() >= forceMissEndTime) {
            forceMissActive = false;
            forceMissSwinged = false;
            forceMissEndTime = 0L;
            return false;
        }
        return true;
    }

    public static boolean consumeForceMissSwing() {
        if (!isForceMissActive()) {
            return false;
        }
        if (forceMissSwinged) {
            return false;
        }
        forceMissSwinged = true;
        return true;
    }

    private float randomBetween(float min, float max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            float swap = min;
            min = max;
            max = swap;
        }
        return MathHelper.lerp(secureRandom.nextFloat(), min, max);
    }

    private static float applyGCD(float targetRotation, float currentRotation) {
        if (mc == null || mc.options == null) {
            return targetRotation;
        }

        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        float f = sensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;
        float gcd = f1 * 0.15F;
        if (gcd <= 0.0F) {
            return targetRotation;
        }

        float delta = targetRotation - currentRotation;
        float adjustedDelta = Math.round(delta / gcd) * gcd;
        return currentRotation + adjustedDelta;
    }
}
