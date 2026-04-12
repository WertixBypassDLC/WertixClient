package sweetie.nezi.client.features.modules.movement.spider.modes;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import sweetie.nezi.api.event.events.player.move.MotionEvent;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.client.features.modules.movement.spider.SpiderMode;

public class SpiderFunTime extends SpiderMode {
    private final TimerUtil timerUtil = new TimerUtil();

    @Override
    public String getName() {
        return "Fun Time";
    }

    @Override
    public void onMotion(MotionEvent.MotionEventData event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (mc.options.jumpKey.isPressed() || !hozColl() || !timerUtil.finished(210)) {
            return;
        }

        Box playerBox = mc.player.getBoundingBox().expand(-1.0E-3);
        Box feetBox = new Box(
                playerBox.minX,
                playerBox.minY,
                playerBox.minZ,
                playerBox.maxX,
                playerBox.minY + 0.55,
                playerBox.maxZ
        );

        if (!hasSurfaceCollision(feetBox)) {
            return;
        }

        Box headCheckBox = new Box(
                playerBox.minX - 0.30,
                playerBox.minY + 1.0,
                playerBox.minZ - 0.30,
                playerBox.maxX + 0.12,
                playerBox.maxY + 0.20,
                playerBox.maxZ + 0.12
        );

        Box lipCheckBox = new Box(
                playerBox.minX - 0.08,
                playerBox.maxY - 0.15,
                playerBox.minZ - 0.08,
                playerBox.maxX + 0.08,
                playerBox.maxY + 0.55,
                playerBox.maxZ + 0.08
        );

        event.ground(true);
        mc.player.setOnGround(true);
        mc.player.fallDistance = 0f;

        if (hasSurfaceCollision(headCheckBox)) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.69, mc.player.getVelocity().z);
        } else if (hasSurfaceCollision(lipCheckBox) || mc.player.horizontalCollision) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.50, mc.player.getVelocity().z);
        } else {
            mc.player.jump();
        }

        timerUtil.reset();
    }

    private boolean hasSurfaceCollision(Box box) {
        return mc.world.getBlockCollisions(mc.player, box).iterator().hasNext() || hasFunTimeSurface(box.expand(0.05));
    }

    private boolean hasFunTimeSurface(Box box) {
        BlockPos min = BlockPos.ofFloored(box.minX - 0.1, box.minY - 0.1, box.minZ - 0.1);
        BlockPos max = BlockPos.ofFloored(box.maxX + 0.1, box.maxY + 0.1, box.maxZ + 0.1);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (!isFunTimeSurface(state)) {
                        continue;
                    }

                    VoxelShape shape = state.getCollisionShape(mc.world, pos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    for (Box shapeBox : shape.getBoundingBoxes()) {
                        if (shapeBox.offset(pos).intersects(box)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean isFunTimeSurface(BlockState state) {
        return state.isOf(Blocks.CHEST)
                || state.isOf(Blocks.TRAPPED_CHEST)
                || state.isOf(Blocks.BAMBOO)
                || state.getBlock() instanceof net.minecraft.block.DoorBlock
                || state.getBlock() instanceof net.minecraft.block.TrapdoorBlock;
    }
}
