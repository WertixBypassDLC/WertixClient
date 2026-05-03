package sweetie.nezi.client.features.modules.movement.spider.modes;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import sweetie.nezi.api.event.events.player.move.MotionEvent;
import sweetie.nezi.api.module.setting.StringSetting;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.api.utils.player.InventoryUtil;
import sweetie.nezi.client.features.modules.movement.spider.SpiderMode;
import sweetie.nezi.client.features.modules.movement.spider.SpiderModule;

import java.util.Locale;
import java.util.function.Supplier;

public class SpiderCustom extends SpiderMode {
    private static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private final TimerUtil climbStopWatch = new TimerUtil();
    private final StringSetting itemId = new StringSetting("Item").value("nether_bricks");

    public SpiderCustom(Supplier<Boolean> condition) {
        itemId.setVisible(condition);
        addSettings(itemId);
    }

    @Override
    public String getName() {
        return "Custom";
    }

    @Override
    public void onEnable() {
        climbStopWatch.reset();
    }

    @Override
    public void onDisable() {
        if (mc != null) {
            mc.options.sneakKey.setPressed(false);
        }
    }

    @Override
    public void onMotion(MotionEvent.MotionEventData event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        mc.options.sneakKey.setPressed(mc.options.attackKey.isPressed());

        if (!mc.player.horizontalCollision || !climbStopWatch.finished(115L)) {
            return;
        }

        Item item = resolveConfiguredItem();
        if (item == null) {
            print("Custom Spider: неверный item id. Используй формат nether_bricks");
            SpiderModule.getInstance().setEnabled(false);
            return;
        }

        if (!(item instanceof BlockItem)) {
            print("Custom Spider: нужен ставящийся блок, а не " + formatItemId(item));
            SpiderModule.getInstance().setEnabled(false);
            return;
        }

        int itemSlot = InventoryUtil.findItem(item, true);
        if (itemSlot == -1) {
            print("Custom Spider: предмет должен быть в хотбаре - " + formatItemId(item));
            SpiderModule.getInstance().setEnabled(false);
            return;
        }

        Direction collisionDirection = resolveCollisionDirection();
        float targetYaw = (float) yawFromFacing(collisionDirection);
        float targetPitch = 75.0f;

        event.ground(true);
        event.yaw(targetYaw);
        event.pitch(targetPitch);

        mc.player.setOnGround(true);
        mc.player.setYaw(targetYaw);
        mc.player.setPitch(targetPitch);
        mc.player.setSprinting(true);
        mc.player.jump();

        if (placeBlockFacingWall(itemSlot, targetYaw, targetPitch)) {
            mc.player.fallDistance = 0.0f;
            climbStopWatch.reset();
        }
    }

    private Item resolveConfiguredItem() {
        String rawId = normalizeItemId(itemId.getText());
        if (rawId.isEmpty()) {
            return null;
        }

        String namespace = "minecraft";
        String path = rawId;

        int separator = rawId.indexOf(':');
        if (separator >= 0) {
            namespace = rawId.substring(0, separator);
            path = rawId.substring(separator + 1);
        }

        if (namespace.isBlank() || path.isBlank()) {
            return null;
        }

        Item item;
        try {
            item = Registries.ITEM.get(Identifier.of(namespace, path));
        } catch (RuntimeException ignored) {
            return null;
        }

        if (item == null || item == Items.AIR) {
            return null;
        }

        return Registries.ITEM.getId(item).getNamespace().equals(namespace)
                && Registries.ITEM.getId(item).getPath().equals(path) ? item : null;
    }

    private Direction resolveCollisionDirection() {
        Box playerBox = mc.player.getBoundingBox().expand(-1.0E-3);
        Direction bestDirection = mc.player.getHorizontalFacing();
        double bestScore = 0.0;

        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            Box probeBox = createProbeBox(playerBox, direction);
            double score = getCollisionScore(probeBox);
            if (score > bestScore) {
                bestScore = score;
                bestDirection = direction;
            }
        }

        return bestDirection;
    }

    private Box createProbeBox(Box playerBox, Direction direction) {
        double reach = 0.24;
        double inset = 0.03;
        double yPadding = 0.05;

        double minX = playerBox.minX + inset;
        double maxX = playerBox.maxX - inset;
        double minY = playerBox.minY + yPadding;
        double maxY = playerBox.maxY - yPadding;
        double minZ = playerBox.minZ + inset;
        double maxZ = playerBox.maxZ - inset;

        return switch (direction) {
            case NORTH -> new Box(minX, minY, playerBox.minZ - reach, maxX, maxY, playerBox.minZ + inset);
            case SOUTH -> new Box(minX, minY, playerBox.maxZ - inset, maxX, maxY, playerBox.maxZ + reach);
            case WEST -> new Box(playerBox.minX - reach, minY, minZ, playerBox.minX + inset, maxY, maxZ);
            case EAST -> new Box(playerBox.maxX - inset, minY, minZ, playerBox.maxX + reach, maxY, maxZ);
            default -> playerBox;
        };
    }

    private double getCollisionScore(Box probeBox) {
        BlockPos min = BlockPos.ofFloored(probeBox.minX - 0.1, probeBox.minY - 0.1, probeBox.minZ - 0.1);
        BlockPos max = BlockPos.ofFloored(probeBox.maxX + 0.1, probeBox.maxY + 0.1, probeBox.maxZ + 0.1);

        double score = 0.0;

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    VoxelShape shape = mc.world.getBlockState(pos).getCollisionShape(mc.world, pos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    for (Box shapeBox : shape.getBoundingBoxes()) {
                        score += getIntersectionVolume(probeBox, shapeBox.offset(pos));
                    }
                }
            }
        }

        return score;
    }

    private double getIntersectionVolume(Box first, Box second) {
        double overlapX = Math.max(0.0, Math.min(first.maxX, second.maxX) - Math.max(first.minX, second.minX));
        double overlapY = Math.max(0.0, Math.min(first.maxY, second.maxY) - Math.max(first.minY, second.minY));
        double overlapZ = Math.max(0.0, Math.min(first.maxZ, second.maxZ) - Math.max(first.minZ, second.minZ));
        return overlapX * overlapY * overlapZ;
    }

    private boolean placeBlockFacingWall(int slot, float yaw, float pitch) {
        int lastSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        BlockHitResult hitResult = raycast(yaw, pitch, 4.5);
        boolean placed = false;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            mc.player.swingHand(Hand.MAIN_HAND);
            placed = true;
        }

        mc.player.getInventory().selectedSlot = lastSlot;
        return placed;
    }

    private BlockHitResult raycast(float yaw, float pitch, double distance) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = Vec3d.fromPolar(pitch, yaw).normalize();
        Vec3d end = eyePos.add(direction.multiply(distance));
        return mc.world.raycast(new RaycastContext(
                eyePos,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
    }

    private String normalizeItemId(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('\\', '/');

        if (normalized.startsWith("minecraft/")) {
            normalized = normalized.substring("minecraft/".length());
        }

        return normalized;
    }

    private String formatItemId(Item item) {
        Identifier identifier = Registries.ITEM.getId(item);
        return identifier == null ? itemId.getText() : identifier.getPath();
    }

    private static double yawFromFacing(Direction direction) {
        return switch (direction) {
            case SOUTH -> 0.0;
            case WEST -> 90.0;
            case NORTH -> 180.0;
            case EAST -> -90.0;
            default -> 0.0;
        };
    }
}
