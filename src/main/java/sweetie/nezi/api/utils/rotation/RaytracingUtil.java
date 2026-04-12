package sweetie.nezi.api.utils.rotation;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sweetie.nezi.api.utils.rotation.manager.Rotation;

import java.util.function.Predicate;

@UtilityClass
public class RaytracingUtil {
    private boolean shouldBlockHitCancel(Vec3d start, Vec3d entityPos, BlockHitResult blockHit) {
        if (blockHit == null || blockHit.getType() != HitResult.Type.BLOCK) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return false;

        var blockState = client.world.getBlockState(blockHit.getBlockPos());
        if (blockState == null) return false;

        if (!blockState.getCollisionShape(client.world, blockHit.getBlockPos()).isEmpty()) {
            Vec3d blockPos = blockHit.getPos();
            return blockPos.squaredDistanceTo(start) < entityPos.squaredDistanceTo(start);
        }

        return false;
    }

    public BlockHitResult raycast(
            Rotation rotation,
            double range,
            boolean includeFluids,
            float tickDelta
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return null;
        return raycast(range, includeFluids, client.player.getCameraPosVec(tickDelta), rotation.getVector(), client.cameraEntity);
    }

    public BlockHitResult raycast(double range, boolean includeFluids, Vec3d start, Vec3d direction, Entity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return null;
        Vec3d end = start.add(direction.x * range, direction.y * range, direction.z * range);

        return client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, entity));
    }

    public EntityHitResult raytraceEntity(double range, Rotation rotation, boolean ignoreWalls) {
        return raytraceEntity(range, rotation, ignoreWalls, entity -> true, 0f);
    }

    public EntityHitResult raytraceEntity(double range, Rotation rotation, boolean ignoreWalls, float expand) {
        return raytraceEntity(range, rotation, ignoreWalls, entity -> true, expand);
    }

    public EntityHitResult raytraceEntity(double range, Rotation rotation, boolean ignoreWalls, Predicate<Entity> filter, float expand) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) return null;
        Entity entity = client.getCameraEntity();
        if (entity == null) return null;

        Vec3d cameraVec = entity.getEyePos();
        Vec3d rotationVec = rotation.getVector();

        Vec3d vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
        Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1f, 1f, 1f);

        EntityHitResult entityHit = ProjectileUtil.raycast(
                entity, cameraVec, vec3d3, box,
                e -> e.canHit() && e.isAlive() && !e.isSpectator() && filter.test(e),
                range * range
        );
        if (entityHit == null) return null;

        if (ignoreWalls) return entityHit;

        Vec3d entityPos = entityHit.getEntity().getBoundingBox().raycast(cameraVec, vec3d3).orElse(entityHit.getEntity().getPos());
        BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                cameraVec, entityPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.ANY,
                client.player
        ));

        return shouldBlockHitCancel(cameraVec, entityPos, blockHit) ? null : entityHit;
    }

    /**
     * Проверяет, попадает ли луч взгляда (clientVec) на расстоянии range в указанный бокс.
     * 1:1 порт из Javelin RaytracingUtil.
     */
    public boolean rayTrace(Vec3d clientVec, double range, Box box) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return false;
        Vec3d cameraVec = client.player.getEyePos();
        return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
    }
}
