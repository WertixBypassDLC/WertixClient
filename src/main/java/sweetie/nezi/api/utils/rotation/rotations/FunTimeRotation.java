package sweetie.nezi.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationMode;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class FunTimeRotation extends RotationMode implements QuickImports {
    private static final SecureRandom RANDOM = new SecureRandom();

    private static long lastAttackMs = 0L;
    private static long freezeUntilMs = 0L;
    private static long nextPointShiftMs = 0L;
    private static long lastPrimeMs = 0L;

    private static int snapTicks = 0;
    private static int lastProcessedTick = Integer.MIN_VALUE;

    private static Vec3d targetPoint;
    private static Vec3d currentPoint;

    private static float jitterYaw;
    private static float jitterPitch;
    private static float targetJitterYaw;
    private static float targetJitterPitch;
    private static long nextJitterMs = 0L;

    private static float yawWave;
    private static float pitchWave;

    public FunTimeRotation() {
        super("Fun Time");
    }

    public static void onAttackTriggered() {
        long now = System.currentTimeMillis();
        lastAttackMs = now;
        freezeUntilMs = now + randomInt(150, 251);
        snapTicks = randomInt(3, 6);
        nextPointShiftMs = now;
        lastPrimeMs = now;
    }

    public static void primeSnapWindow(boolean canAttack, long timeSinceLastClick) {
        long now = System.currentTimeMillis();
        boolean recentAttack = timeSinceLastClick >= 0L && timeSinceLastClick < 260L;
        if (!canAttack && !recentAttack) {
            return;
        }

        if (now - lastPrimeMs < 45L) {
            return;
        }

        lastPrimeMs = now;
        snapTicks = Math.max(snapTicks, canAttack ? 2 : 1);
        if (canAttack) {
            nextPointShiftMs = now;
        }
    }

    public static Vec3d getAimPoint(Entity entity) {
        if (entity == null || mc.player == null || mc.world == null) {
            return Vec3d.ZERO;
        }

        long now = System.currentTimeMillis();
        Vec3d eyes = mc.player.getEyePos();
        Box box = entity.getBoundingBox();

        if (targetPoint == null || now >= nextPointShiftMs) {
            targetPoint = choosePoint(box, eyes);
            nextPointShiftMs = now + randomInt(45, 100);
        }

        if (currentPoint == null) {
            currentPoint = targetPoint;
        } else {
            float follow = randomFloat(0.20f, 0.38f);
            currentPoint = new Vec3d(
                    MathHelper.lerp(follow, currentPoint.x, targetPoint.x),
                    MathHelper.lerp(follow, currentPoint.y, targetPoint.y),
                    MathHelper.lerp(follow, currentPoint.z, targetPoint.z)
            );
        }

        return currentPoint;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (mc.player == null) {
            return targetRotation;
        }

        long now = System.currentTimeMillis();
        boolean resetPhase = entity == null || vec3d == null || targetRotation == null;
        boolean freezeActive = now < freezeUntilMs;

        updateJitter(now, freezeActive);

        if (resetPhase) {
            return processReset(currentRotation, targetRotation != null ? targetRotation : currentRotation);
        }

        if (snapTicks > 0) {
            consumeSnap();

            Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
            float yawDelta = delta.getYaw();
            float pitchDelta = delta.getPitch();

            float yawSpeed = MathHelper.clamp(Math.abs(yawDelta) * 1.08f, 44.0f, 88.8f);
            float pitchSpeed = MathHelper.clamp(Math.abs(pitchDelta) * 1.04f, 16.0f, 44.0f);

            if (now - lastAttackMs < 120L) {
                yawSpeed = MathHelper.clamp(yawSpeed * 1.12f, 52.0f, 88.8f);
                pitchSpeed = MathHelper.clamp(pitchSpeed * 1.10f, 18.0f, 46.0f);
            }

            float nextYaw = currentRotation.getYaw() + Math.copySign(yawSpeed, yawDelta);
            float nextPitch = currentRotation.getPitch() + Math.copySign(pitchSpeed, pitchDelta);

            yawWave += randomFloat(0.18f, 0.27f);
            pitchWave += randomFloat(0.16f, 0.24f);

            float attackCurveYaw = (float) Math.sin(yawWave) * randomFloat(0.75f, 2.10f);
            float attackCurvePitch = (float) Math.cos(pitchWave) * randomFloat(0.18f, 0.85f);
            float shakeYaw = mc.player.age % randomInt(5, 8) == 0 ? randomFloat(-0.48f, 0.48f) : 0f;
            float shakePitch = mc.player.age % randomInt(6, 9) == 0 ? randomFloat(-0.20f, 0.20f) : 0f;

            nextYaw += attackCurveYaw + jitterYaw + shakeYaw;
            nextPitch += attackCurvePitch + jitterPitch + shakePitch;

            return finishRotation(currentRotation, nextYaw, nextPitch);
        }

        if (freezeActive) {
            float frozenYaw = currentRotation.getYaw() + jitterYaw * 0.20f + (float) Math.sin(now / 84.0) * 0.05f;
            float frozenPitch = currentRotation.getPitch() + jitterPitch * 0.15f + (float) Math.cos(now / 96.0) * 0.03f;
            return finishRotation(currentRotation, frozenYaw, frozenPitch);
        }

        return currentRotation;
    }

    public static void reset() {
        lastAttackMs = 0L;
        freezeUntilMs = 0L;
        nextPointShiftMs = 0L;
        lastPrimeMs = 0L;
        snapTicks = 0;
        lastProcessedTick = Integer.MIN_VALUE;
        targetPoint = null;
        currentPoint = null;
        jitterYaw = 0f;
        jitterPitch = 0f;
        targetJitterYaw = 0f;
        targetJitterPitch = 0f;
        nextJitterMs = 0L;
        yawWave = 0f;
        pitchWave = 0f;
    }

    private static Vec3d choosePoint(Box box, Vec3d eyes) {
        double minX = box.minX + 0.06;
        double maxX = box.maxX - 0.06;
        double minY = box.minY + 0.04;
        double maxY = box.maxY - 0.04;
        double minZ = box.minZ + 0.06;
        double maxZ = box.maxZ - 0.06;
        double cx = (minX + maxX) * 0.5;
        double cz = (minZ + maxZ) * 0.5;

        List<Vec3d> points = new ArrayList<>(List.of(
                new Vec3d(cx, minY + (maxY - minY) * 0.76, cz),
                new Vec3d(cx, minY + (maxY - minY) * 0.60, cz),
                new Vec3d(cx, minY + (maxY - minY) * 0.45, cz),
                new Vec3d(minX + (maxX - minX) * 0.18, minY + (maxY - minY) * 0.58, cz),
                new Vec3d(minX + (maxX - minX) * 0.82, minY + (maxY - minY) * 0.58, cz),
                new Vec3d(cx, minY + (maxY - minY) * 0.28, cz),
                new Vec3d(cx, minY + (maxY - minY) * 0.62, minZ + (maxZ - minZ) * 0.22),
                new Vec3d(cx, minY + (maxY - minY) * 0.62, minZ + (maxZ - minZ) * 0.78)
        ));

        shuffle(points);

        for (Vec3d point : points) {
            Vec3d offset = point.add(
                    randomDouble(-0.028, 0.028),
                    randomDouble(-0.018, 0.018),
                    randomDouble(-0.028, 0.028)
            );
            if (isVisible(eyes, offset)) {
                return offset;
            }
        }

        return new Vec3d(
                MathHelper.clamp(eyes.x, minX, maxX),
                MathHelper.clamp(eyes.y, minY, maxY),
                MathHelper.clamp(eyes.z, minZ, maxZ)
        );
    }

    private static void updateJitter(long now, boolean freezeActive) {
        if (now >= nextJitterMs) {
            float yawScale = freezeActive ? randomFloat(0.02f, 0.09f) : randomFloat(0.18f, 0.48f);
            float pitchScale = freezeActive ? randomFloat(0.01f, 0.05f) : randomFloat(0.07f, 0.18f);
            targetJitterYaw = randomFloat(-yawScale, yawScale);
            targetJitterPitch = randomFloat(-pitchScale, pitchScale);
            nextJitterMs = now + randomInt(freezeActive ? 60 : 24, freezeActive ? 115 : 72);
        }

        float follow = freezeActive ? 0.16f : 0.46f;
        jitterYaw += (targetJitterYaw - jitterYaw) * follow;
        jitterPitch += (targetJitterPitch - jitterPitch) * follow;
    }

    private static void consumeSnap() {
        if (mc.player == null || mc.player.age == lastProcessedTick) {
            return;
        }

        lastProcessedTick = mc.player.age;
        if (snapTicks > 0) {
            snapTicks--;
        }
    }

    private static Rotation finishRotation(Rotation currentRotation, float yaw, float pitch) {
        float diff = MathHelper.wrapDegrees(yaw - currentRotation.getYaw());
        yaw = currentRotation.getYaw() + MathHelper.clamp(diff, -88.8f, 88.8f);
        pitch = MathHelper.clamp(pitch, -89.0f, 89.0f);
        return new Rotation(yaw, pitch);
    }

    private static Rotation processReset(Rotation currentRotation, Rotation targetRotation) {
        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

        if (Math.abs(yawDelta) < 0.25f && Math.abs(pitchDelta) < 0.25f) {
            jitterYaw *= 0.55f;
            jitterPitch *= 0.55f;
            return targetRotation;
        }

        jitterYaw *= 0.72f;
        jitterPitch *= 0.72f;

        float yawStep = MathHelper.clamp(Math.abs(yawDelta) * 0.38f, 2.2f, 34.0f);
        float pitchStep = MathHelper.clamp(Math.abs(pitchDelta) * 0.38f, 1.6f, 24.0f);

        float nextYaw = currentRotation.getYaw() + MathHelper.clamp(yawDelta, -yawStep, yawStep) + jitterYaw * 0.10f;
        float nextPitch = currentRotation.getPitch() + MathHelper.clamp(pitchDelta, -pitchStep, pitchStep) + jitterPitch * 0.08f;
        return finishRotation(currentRotation, nextYaw, nextPitch);
    }

    private static boolean isVisible(Vec3d from, Vec3d to) {
        HitResult hit = mc.world.raycast(new RaycastContext(
                from,
                to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static void shuffle(List<Vec3d> points) {
        for (int i = 0; i < points.size(); i++) {
            int swap = RANDOM.nextInt(points.size());
            Vec3d cached = points.get(i);
            points.set(i, points.get(swap));
            points.set(swap, cached);
        }
    }

    private static int randomInt(int minInclusive, int maxExclusive) {
        return minInclusive + RANDOM.nextInt(maxExclusive - minInclusive);
    }

    private static float randomFloat(float min, float max) {
        return min + RANDOM.nextFloat() * (max - min);
    }

    private static double randomDouble(double min, double max) {
        return min + RANDOM.nextDouble() * (max - min);
    }
}
