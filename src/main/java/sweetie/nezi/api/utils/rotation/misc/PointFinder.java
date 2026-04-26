package sweetie.nezi.api.utils.rotation.misc;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class PointFinder implements QuickImports {
    private final Random random = new SecureRandom();
    private Vec3d offset = Vec3d.ZERO;
    private Vec3d smoothedPoint = null;
    private int lastTargetId = Integer.MIN_VALUE;
    private int cachedCandidateTargetId = Integer.MIN_VALUE;
    private int cachedCandidateTick = Integer.MIN_VALUE;
    private float cachedCandidateDistance = Float.NaN;
    private boolean cachedCandidateIgnoreWalls = false;
    private Pair<List<Vec3d>, Box> cachedCandidateData = new Pair<>(List.of(), null);
    private long selectorSeed = 0L;

    public Pair<Vec3d, Box> computeVector(LivingEntity entity, float maxDistance, Rotation initialRotation, Vec3d velocity, boolean ignoreWalls) {
        if (entity == null || mc.player == null) {
            return new Pair<>(Vec3d.ZERO, entity == null ? null : entity.getBoundingBox());
        }

        if (entity.getId() != lastTargetId) {
            lastTargetId = entity.getId();
            offset = Vec3d.ZERO;
            smoothedPoint = null;
            selectorSeed = System.currentTimeMillis() + (long) entity.getId() * 31L;
        }

        Pair<List<Vec3d>, Box> candidatePoints = generateCandidatePoints(entity, maxDistance, ignoreWalls);
        Box entityBox = candidatePoints.getRight();
        if (candidatePoints.getLeft().isEmpty()) {
            return new Pair<>(null, entityBox);
        }

        Vec3d bestVector = findBestVector(candidatePoints.getLeft(), initialRotation);
        Vec3d fallbackPoint = clampToBox(mc.player.getEyePos(), entityBox, 0.03D);
        Vec3d base = bestVector == null ? fallbackPoint : bestVector;

        updateOffset(velocity, entityBox);
        Vec3d floated = clampToBox(base.add(offset), entityBox, 0.03D);
        smoothedPoint = smoothedPoint == null ? floated : smoothPoint(smoothedPoint, floated, entityBox);

        return new Pair<>(smoothedPoint, entityBox);
    }

    public Pair<List<Vec3d>, Box> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
        if (entity == null || mc.player == null) {
            return new Pair<>(List.of(), entity == null ? null : entity.getBoundingBox());
        }

        int currentTick = mc.player.age;
        if (entity.getId() == cachedCandidateTargetId
                && currentTick == cachedCandidateTick
                && Float.compare(maxDistance, cachedCandidateDistance) == 0
                && ignoreWalls == cachedCandidateIgnoreWalls) {
            return cachedCandidateData;
        }

        Box entityBox = entity.getBoundingBox();
        List<Vec3d> points = new ArrayList<>();
        int stepsX = 4;
        int stepsY = 7;
        int stepsZ = 4;
        double marginX = Math.min(0.07D, entityBox.getLengthX() * 0.14D);
        double marginY = Math.min(0.06D, entityBox.getLengthY() * 0.06D);
        double marginZ = Math.min(0.07D, entityBox.getLengthZ() * 0.14D);

        Vec3d eyePos = mc.player.getEyePos();
        for (int xi = 0; xi < stepsX; xi++) {
            double x = MathHelper.lerp(stepsX == 1 ? 0.5D : (double) xi / (double) (stepsX - 1), entityBox.minX + marginX, entityBox.maxX - marginX);
            for (int yi = 0; yi < stepsY; yi++) {
                double y = MathHelper.lerp(stepsY == 1 ? 0.5D : (double) yi / (double) (stepsY - 1), entityBox.minY + marginY, entityBox.maxY - marginY);
                for (int zi = 0; zi < stepsZ; zi++) {
                    double z = MathHelper.lerp(stepsZ == 1 ? 0.5D : (double) zi / (double) (stepsZ - 1), entityBox.minZ + marginZ, entityBox.maxZ - marginZ);
                    Vec3d point = new Vec3d(x, y, z);
                    if (isValidPoint(eyePos, point, maxDistance, ignoreWalls)) {
                        points.add(point);
                    }
                }
            }
        }

        cachedCandidateTargetId = entity.getId();
        cachedCandidateTick = currentTick;
        cachedCandidateDistance = maxDistance;
        cachedCandidateIgnoreWalls = ignoreWalls;
        cachedCandidateData = new Pair<>(List.copyOf(points), entityBox);
        return cachedCandidateData;
    }

    public boolean hasValidPoint(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
        if (entity == null || mc.player == null) {
            return false;
        }

        return !generateCandidatePoints(entity, maxDistance, ignoreWalls).getLeft().isEmpty();
    }

    private boolean isValidPoint(Vec3d startPoint, Vec3d endPoint, float maxDistance, boolean ignoreWalls) {
        if (startPoint.distanceTo(endPoint) > maxDistance) {
            return false;
        }
        if (ignoreWalls || mc.world == null) {
            return true;
        }
        HitResult result = mc.world.raycast(new RaycastContext(
                startPoint,
                endPoint,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return result.getType() != HitResult.Type.BLOCK;
    }

    private Vec3d findBestVector(List<Vec3d> candidatePoints, Rotation initialRotation) {
        if (candidatePoints.isEmpty()) {
            return null;
        }

        if (initialRotation == null) {
            List<Vec3d> randomized = new ArrayList<>(candidatePoints);
            Collections.shuffle(randomized, random);
            int poolSize = Math.min(12, randomized.size());
            return randomized.get(random.nextInt(poolSize));
        }

        List<Vec3d> randomized = new ArrayList<>(candidatePoints);
        Collections.shuffle(randomized, random);

        List<Vec3d> sorted = new ArrayList<>(randomized);
        sorted.sort(Comparator.comparingDouble(point ->
                calculateRotationDifference(mc.player.getEyePos(), point, initialRotation) + random.nextDouble() * 7.5D
        ));

        int poolSize = Math.min(12, sorted.size());
        if (poolSize == 1) {
            return sorted.get(0);
        }

        int index = random.nextInt(poolSize);
        return sorted.get(index);
    }

    private double calculateRotationDifference(Vec3d startPoint, Vec3d endPoint, Rotation initialRotation) {
        Rotation targetRotation = RotationUtil.fromVec3d(endPoint.subtract(startPoint));
        Rotation delta = RotationUtil.calculateDelta(initialRotation, targetRotation);
        return Math.hypot(delta.getYaw(), delta.getPitch());
    }

    private void updateOffset(Vec3d velocity, Box box) {
        Vec3d safeVelocity = velocity == null ? Vec3d.ZERO : velocity;
        offset = offset.multiply(0.72D, 0.78D, 0.72D);

        double time = (System.currentTimeMillis() + selectorSeed) / 100.0D;
        double oscillationX = Math.sin(time * 1.15D) * safeVelocity.x * 0.9D;
        double oscillationY = Math.cos(time * 0.97D) * safeVelocity.y * 0.65D;
        double oscillationZ = Math.cos(time * 1.28D) * safeVelocity.z * 0.9D;

        double noiseX = random.nextGaussian() * safeVelocity.x * 0.55D;
        double noiseY = random.nextGaussian() * safeVelocity.y * 0.45D;
        double noiseZ = random.nextGaussian() * safeVelocity.z * 0.55D;

        offset = offset.add(oscillationX + noiseX, oscillationY + noiseY, oscillationZ + noiseZ);

        double maxX = Math.max(0.012D, box.getLengthX() * 0.38D);
        double maxY = Math.max(0.020D, box.getLengthY() * 0.24D);
        double maxZ = Math.max(0.012D, box.getLengthZ() * 0.38D);

        offset = new Vec3d(
                MathHelper.clamp(offset.x, -maxX, maxX),
                MathHelper.clamp(offset.y, -maxY, maxY),
                MathHelper.clamp(offset.z, -maxZ, maxZ)
        );
    }

    private Vec3d smoothPoint(Vec3d from, Vec3d to, Box box) {
        Vec3d lerped = from.lerp(to, 0.52D);
        return clampToBox(lerped, box, 0.03D);
    }

    private Vec3d clampToBox(Vec3d point, Box box, double margin) {
        double marginX = Math.min(margin, box.getLengthX() * 0.25D);
        double marginY = Math.min(margin, box.getLengthY() * 0.25D);
        double marginZ = Math.min(margin, box.getLengthZ() * 0.25D);

        double minX = box.minX + marginX;
        double maxX = box.maxX - marginX;
        double minY = box.minY + marginY;
        double maxY = box.maxY - marginY;
        double minZ = box.minZ + marginZ;
        double maxZ = box.maxZ - marginZ;

        if (minX > maxX) {
            minX = maxX = box.getCenter().x;
        }
        if (minY > maxY) {
            minY = maxY = box.getCenter().y;
        }
        if (minZ > maxZ) {
            minZ = maxZ = box.getCenter().z;
        }

        return new Vec3d(
                MathHelper.clamp(point.x, minX, maxX),
                MathHelper.clamp(point.y, minY, maxY),
                MathHelper.clamp(point.z, minZ, maxZ)
        );
    }
}
