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

    private final SecureRandom random = new SecureRandom();
    private long smoothbackShakeStartMs = -1L;

    private static long snapHintExpireMs = 0L;
    private static long lastAttackTimeMs = System.currentTimeMillis();
    private static int attackCount = 0;
    private static float frozenVisualYaw = 0.0F;
    private static float frozenVisualPitch = 0.0F;
    private static boolean visualFreezeInitialized = false;

    public FunTimeRotation() {
        super(MODE_NAME);
    }

    public static void reset() {
        snapHintExpireMs = 0L;
        lastAttackTimeMs = System.currentTimeMillis();
        attackCount = 0;
        frozenVisualYaw = 0.0F;
        frozenVisualPitch = 0.0F;
        visualFreezeInitialized = false;
    }

    public static void primeSnapWindow(boolean attackPossible) {
        if (attackPossible) {
            snapHintExpireMs = System.currentTimeMillis() + 90L;
        }
    }

    public static void onAttack() {
        attackCount++;
        lastAttackTimeMs = System.currentTimeMillis();
    }

    public static Vec3d getAimPoint(Entity entity) {
        if (entity == null) {
            return Vec3d.ZERO;
        }
        return entity.getPos().add(0.0, MathHelper.clamp(entity.getEyeHeight(entity.getPose()), 0.1, entity.getHeight()), 0.0);
    }

    public static Vec3d getRandomOffsetVelocity() {
        return new Vec3d(0.041, 0.064, 0.041);
    }

    public static void updateVisualFreeze(float yaw, float pitch) {
        frozenVisualYaw = yaw;
        frozenVisualPitch = pitch;
        visualFreezeInitialized = true;
    }

    public static float getFrozenVisualYaw(float fallback) {
        return visualFreezeInitialized ? frozenVisualYaw : fallback;
    }

    public static float getFrozenVisualPitch(float fallback) {
        return visualFreezeInitialized ? frozenVisualPitch : fallback;
    }

    public static boolean shouldVisualSnap() {
        AuraModule aura = AuraModule.getInstance();
        if (aura == null || !aura.isEnabled() || !aura.getAimMode().is("Fun Time") || aura.target == null) {
            return false;
        }

        try {
            var combatManager = aura.combatExecutor.combatManager();
            return combatManager.canAttack() || combatManager.clickScheduler().willClickAt(2) || System.currentTimeMillis() <= snapHintExpireMs;
        } catch (Exception ignored) {
            return System.currentTimeMillis() <= snapHintExpireMs;
        }
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (mc.player == null) {
            return currentRotation;
        }

        AuraModule aura = AuraModule.getInstance();
        boolean auraActive = aura != null && aura.isEnabled() && aura.target != null && entity != null;

        if (auraActive) {
            smoothbackShakeStartMs = -1L;
            Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
            float yawDelta = delta.getYaw();
            float pitchDelta = delta.getPitch();
            float rotationDiff = Math.max((float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta)), 1.0E-4F);

            boolean canSnap = canAttackSoon(aura);
            float speed = canSnap ? randomRange(0.80F, 0.95F) : randomRange(0.30F, 0.52F);
            float yawLimit = Math.abs(yawDelta / rotationDiff) * (canSnap ? 138.0F : 72.0F);
            float pitchLimit = Math.abs(pitchDelta / rotationDiff) * (canSnap ? 122.0F : 62.0F);

            float moveYaw = MathHelper.clamp(yawDelta, -yawLimit, yawLimit);
            float movePitch = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit);

            float jitterScale = getJitterScale(entity, vec3d, canSnap);
            float jitterYaw = (randomRange(-2.35F, 2.35F)
                    + (float) Math.sin(System.currentTimeMillis() / 67.0) * 1.95F
                    + (float) Math.sin(System.currentTimeMillis() / 34.0) * 0.85F) * jitterScale;
            float jitterPitch = (randomRange(-1.45F, 1.45F)
                    + (float) Math.cos(System.currentTimeMillis() / 73.0) * 1.32F
                    + (float) Math.sin(System.currentTimeMillis() / 41.0) * 0.62F) * jitterScale;

            float yaw = MathHelper.lerp(speed, currentRotation.getYaw(), currentRotation.getYaw() + moveYaw) + jitterYaw;
            float pitch = MathHelper.lerp(speed, currentRotation.getPitch(), currentRotation.getPitch() + movePitch) + jitterPitch;

            return new Rotation(
                    applyGCD(yaw, currentRotation.getYaw()),
                    MathHelper.clamp(applyGCD(pitch, currentRotation.getPitch()), -90.0F, 90.0F)
            );
        }

        Rotation playerAngle = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        Rotation backDelta = RotationUtil.calculateDelta(currentRotation, playerAngle);
        float yawDelta = backDelta.getYaw();
        float pitchDelta = backDelta.getPitch();
        float rotationDiff = Math.max((float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta)), 1.0E-4F);

        if (smoothbackShakeStartMs < 0L) {
            smoothbackShakeStartMs = System.currentTimeMillis();
        }
        float fade = 1.0F - MathHelper.clamp((System.currentTimeMillis() - smoothbackShakeStartMs) / 2600.0F, 0.0F, 1.0F);

        float yawShake = randomRange(9.0F, 19.0F) * (float) Math.sin(System.currentTimeMillis() / 65.0) * fade;
        float pitchShake = randomRange(4.0F, 10.0F) * (float) Math.cos(System.currentTimeMillis() / 80.0) * fade;

        long elapsed = elapsedSinceAttack();
        float yawLimit = Math.abs(yawDelta / rotationDiff) * (elapsed >= 360L ? 30.0F : 8.0F);
        float pitchLimit = Math.abs(pitchDelta / rotationDiff) * (elapsed >= 360L ? 26.0F : 7.0F);

        float yaw = MathHelper.lerp(0.45F, currentRotation.getYaw(), currentRotation.getYaw() + MathHelper.clamp(yawDelta, -yawLimit, yawLimit) + yawShake);
        float pitch = MathHelper.lerp(0.45F, currentRotation.getPitch(), currentRotation.getPitch() + MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit) + pitchShake);

        return new Rotation(
                applyGCD(yaw, currentRotation.getYaw()),
                MathHelper.clamp(applyGCD(pitch, currentRotation.getPitch()), -90.0F, 90.0F)
        );
    }

    private boolean canAttackSoon(AuraModule aura) {
        try {
            var combatManager = aura.combatExecutor.combatManager();
            return combatManager.canAttack() || combatManager.clickScheduler().willClickAt(2);
        } catch (Exception ignored) {
            return System.currentTimeMillis() <= snapHintExpireMs;
        }
    }

    private static long elapsedSinceAttack() {
        return Math.max(0L, System.currentTimeMillis() - lastAttackTimeMs);
    }

    private float getJitterScale(Entity entity, Vec3d vec3d, boolean canSnap) {
        Vec3d point = vec3d != null ? vec3d : entity.getBoundingBox().getCenter();
        double distance = mc.player.getEyePos().distanceTo(point);
        float base = MathHelper.clamp((float) (1.12F - distance * 0.08F), 0.65F, 1.25F);
        return canSnap ? base * 1.05F : base * 1.68F;
    }

    private float randomRange(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
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
