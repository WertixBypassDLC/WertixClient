package sweetie.nezi.api.utils.rotation.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.rotation.RaytracingUtil;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.rotations.FunTimeRotation;
import sweetie.nezi.api.utils.rotation.manager.Rotation;

@UtilityClass
public class AuraUtil implements QuickImports {
    private Vec3d dvdPoint = Vec3d.ZERO;
    private Vec3d dvdMotion = Vec3d.ZERO;
    // Multipoint state (Javelin port, separate from Holy World dvd)
    private Vec3d multipointPoint = Vec3d.ZERO;
    private Vec3d multipointMotion = Vec3d.ZERO;
    private float hitCount = 0;

    public void onAttack(String mode) {
        switch (mode) {
            case "Spooky Time" -> {
                float hits = 0.3f;
                hitCount += hits;
                if (hitCount >= hits * 2) hitCount = -hits;
            }
            case "Fun Time" -> FunTimeRotation.onAttack();
            default -> {
                hitCount += 1;
                if (hitCount >= 3) hitCount = 0;
            }
        }
    }

    public Vec3d getAimpoint(LivingEntity entity, String mode, float distance) {
        return switch (mode) {
            case "Fun Time" -> FunTimeRotation.getAimPoint(entity);
            case "Spooky Time" -> getSpookyTimePoint(entity);
            default -> RotationUtil.getSpot(entity);
        };
    }

    /**
     * Прямой порт логики NightDLC (Smart Multipoint).
     * Проверяет видимость точек: Центр -> Голова -> Ноги.
     * Если ничего не видно, возвращает ближайшую точку на хитбоксе.
     */
    public Vec3d getBestVector(Entity entity, float jitter) {
        if (entity == null) return new Vec3d(0, 0, 0);

        Box aabb = entity.getBoundingBox();
        double h = entity.getHeight();

        Vec3d eyes = mc.player.getEyePos();

        // 1. Центр
        Vec3d center = entity.getPos().add(0, h / 2.0, 0);
        if (isVisible(eyes, center)) return center.add(0, jitter, 0);

        // 2. Голова (чуть ниже макушки, как в UBoxPoints offsets)
        Vec3d head = entity.getPos().add(0, h - 0.2, 0);
        if (isVisible(eyes, head)) return head.add(0, jitter, 0);

        // 3. Ноги (чуть выше ступней)
        Vec3d feet = entity.getPos().add(0, 0.2, 0);
        if (isVisible(eyes, feet)) return feet.add(0, jitter, 0);

        // 4. Если за стеной - берем ближайшую точку на хитбоксе (Clamp)
        return getClampedPosition(entity, jitter);
    }

    private Vec3d getClampedPosition(Entity entity, float jitter) {
        Box box = entity.getBoundingBox();
        Vec3d eye = mc.player.getEyePos();

        // Небольшой отступ внутрь (0.1), чтобы не целиться в самый край хитбокса
        double x = MathHelper.clamp(eye.x, box.minX + 0.1, box.maxX - 0.1);
        double y = MathHelper.clamp(eye.y, box.minY + 0.1, box.maxY - 0.1);
        double z = MathHelper.clamp(eye.z, box.minZ + 0.1, box.maxZ - 0.1);

        return new Vec3d(x, y, z).add(0, jitter, 0);
    }

    private boolean isVisible(Vec3d from, Vec3d to) {
        if (mc.world == null) return false;
        // Стандартный RayCast. Если результат MISS, значит путь чист
        return mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() == HitResult.Type.MISS;
    }

    // --- Логика Spooky Time (Без изменений) ---
    public Vec3d getSpookyTimePoint(Entity entity) {
        float safe = 0.06f;
        float horValue = (entity.getWidth() / 2f) * hitCount;
        float verValue = (entity.getHeight() / 4f) * (hitCount + 1);
        Box box = entity.getBoundingBox();

        // Используем базу ClampedPosition
        Vec3d best = getClampedPosition(entity, 0f);

        return new Vec3d(
                MathHelper.clamp(best.x - horValue, box.minX + safe, box.maxX - safe),
                MathHelper.clamp(best.y - verValue, box.minY + safe, box.maxY - safe),
                MathHelper.clamp(best.z + horValue, box.minZ + safe, box.maxZ - safe)
        );
    }

    // --- Логика Holy World (DVD Bouncing) ---
    public Vec3d getDvDPoint(Entity entity) {
        float minMotionXZ = 0.003f;
        float maxMotionXZ = 0.04f;
        float minMotionY = 0.001f;
        float maxMotionY = 0.03f;

        double lengthX = entity.getBoundingBox().getLengthX();
        double lengthY = entity.getBoundingBox().getLengthY() * 0.8f;
        double lengthZ = entity.getBoundingBox().getLengthZ();

        if (dvdMotion.equals(Vec3d.ZERO))
            dvdMotion = new Vec3d(MathUtil.randomInRange(-0.05f, 0.05f), MathUtil.randomInRange(-0.05f, 0.05f), MathUtil.randomInRange(-0.05f, 0.05f));

        dvdPoint = dvdPoint.add(dvdMotion);

        if (dvdPoint.x >= (lengthX - 0.05) / 2.0)
            dvdMotion = new Vec3d(-MathUtil.randomInRange(minMotionXZ, maxMotionXZ), dvdMotion.getY(), dvdMotion.getZ());
        if (dvdPoint.y >= lengthY)
            dvdMotion = new Vec3d(dvdMotion.getX(), -MathUtil.randomInRange(minMotionY, maxMotionY), dvdMotion.getZ());
        if (dvdPoint.z >= (lengthZ - 0.05) / 2.0)
            dvdMotion = new Vec3d(dvdMotion.getX(), dvdMotion.getY(), -MathUtil.randomInRange(minMotionXZ, maxMotionXZ));
        if (dvdPoint.x <= -(lengthX - 0.05) / 2.0)
            dvdMotion = new Vec3d(MathUtil.randomInRange(minMotionXZ, 0.03f), dvdMotion.getY(), dvdMotion.getZ());
        if (dvdPoint.y <= 0.05)
            dvdMotion = new Vec3d(dvdMotion.getX(), MathUtil.randomInRange(minMotionY, maxMotionY), dvdMotion.getZ());
        if (dvdPoint.z <= -(lengthZ - 0.05) / 2.0)
            dvdMotion = new Vec3d(dvdMotion.getX(), dvdMotion.getY(), MathUtil.randomInRange(minMotionXZ, maxMotionXZ));

        dvdPoint.add(MathUtil.randomInRange(-0.03f, 0.03f), 0f, MathUtil.randomInRange(-0.03f, 0.03f));

        Vec3d dvdPointed = entity.getPos().add(dvdPoint);
        Box box = entity.getBoundingBox();
        return new Vec3d(
                MathHelper.clamp(dvdPointed.x, box.minX, box.maxX),
                MathHelper.clamp(dvdPointed.y, box.minY, box.maxY),
                MathHelper.clamp(dvdPointed.z, box.minZ, box.maxZ)
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    //  1:1 порт MultipointUtils из Javelin
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 1:1 порт MultipointUtils.getNearestPoint — сканирование сетки бокса.
     */
    public Vec3d getNearestPoint(Entity target, double distance) {
        Vec3d eyePos = mc.player.getCameraPosVec(1.0f);
        double maxDistSq = distance * distance;
        Box box = target.getBoundingBox();
        Vec3d boxCenter = box.getCenter();
        double stepXZ = 0.1;
        double stepY = 0.1;
        Vec3d bestPoint = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (double x = box.minX; x <= box.maxX; x += stepXZ) {
            for (double y = box.minY; y <= box.maxY; y += stepY) {
                for (double z = box.minZ; z <= box.maxZ; z += stepXZ) {
                    Vec3d point = new Vec3d(x, y, z);
                    if (eyePos.squaredDistanceTo(point) > maxDistSq) continue;

                    RaycastContext context = new RaycastContext(eyePos, point,
                            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
                    HitResult result = mc.world.raycast(context);
                    boolean visible = result.getType() == HitResult.Type.MISS || result.getType() != HitResult.Type.BLOCK;

                    if (visible) {
                        double score = boxCenter.squaredDistanceTo(point);
                        if (score < bestScore) {
                            bestScore = score;
                            bestPoint = point;
                        }
                    }
                }
            }
        }

        return bestPoint == null ? target.getBoundingBox().getCenter() : bestPoint;
    }

    /**
     * 1:1 порт MultipointUtils.getMultipoint — DVD bounce с фолбэком на сетку.
     */
    public Vec3d getMultipoint(Entity target, double distance) {
        float minMotionXZ = 0.01f;
        float maxMotionXZ = 0.03f;
        float minMotionY = 0.01f;
        float maxMotionY = 0.03f;
        double lengthX = target.getBoundingBox().getLengthX();
        double lengthY = target.getBoundingBox().getLengthY();
        double lengthZ = target.getBoundingBox().getLengthZ();

        if (multipointMotion.equals(Vec3d.ZERO)) {
            multipointMotion = new Vec3d(MathUtil.randomInRange(-0.05f, 0.05f), MathUtil.randomInRange(-0.05f, 0.05f), MathUtil.randomInRange(-0.05f, 0.05f));
        }

        multipointPoint = multipointPoint.add(multipointMotion);

        if (multipointPoint.x >= (lengthX - 0.05) / 2.0)
            multipointMotion = new Vec3d(-MathUtil.randomInRange(minMotionXZ, maxMotionXZ), multipointMotion.getY(), multipointMotion.getZ());
        if (multipointPoint.y >= lengthY / 2.0)
            multipointMotion = new Vec3d(multipointMotion.getX(), -MathUtil.randomInRange(minMotionY, maxMotionY), multipointMotion.getZ());
        if (multipointPoint.z >= (lengthZ - 0.05) / 2.0)
            multipointMotion = new Vec3d(multipointMotion.getX(), multipointMotion.getY(), -MathUtil.randomInRange(minMotionXZ, maxMotionXZ));
        if (multipointPoint.x <= -(lengthX - 0.05) / 2.0)
            multipointMotion = new Vec3d(MathUtil.randomInRange(minMotionXZ, 0.03f), multipointMotion.getY(), multipointMotion.getZ());
        if (multipointPoint.y <= 0.1)
            multipointMotion = new Vec3d(multipointMotion.getX(), MathUtil.randomInRange(minMotionY, maxMotionY), multipointMotion.getZ());
        if (multipointPoint.z <= -(lengthZ - 0.05) / 2.0)
            multipointMotion = new Vec3d(multipointMotion.getX(), multipointMotion.getY(), MathUtil.randomInRange(minMotionXZ, maxMotionXZ));

        multipointPoint.add(MathUtil.randomInRange(-0.03f, 0.03f), 0f, MathUtil.randomInRange(-0.03f, 0.03f));

        // Fallback: если текущий луч не попадает в бокс, сканируем сетку
        if (!RaytracingUtil.rayTrace(mc.player.getRotationVector(), distance, target.getBoundingBox())) {
            float halfBox = (float) (lengthX / 2.0);
            for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.05f) {
                for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.05f) {
                    for (float y1 = 0.05f; (double) y1 <= target.getBoundingBox().getLengthY(); y1 += 0.15f) {
                        Vec3d v1 = new Vec3d(target.getX() + (double) x1, target.getY() + (double) y1, target.getZ() + (double) z1);
                        Rotation rotation = RotationUtil.fromVec3d(v1.subtract(mc.player.getEyePos()));
                        if (RaytracingUtil.rayTrace(rotation.getVector(), distance, target.getBoundingBox())) {
                            multipointPoint = new Vec3d(x1, y1, z1);
                            break;
                        }
                    }
                }
            }
        }

        return target.getPos().add(multipointPoint);
    }
}
