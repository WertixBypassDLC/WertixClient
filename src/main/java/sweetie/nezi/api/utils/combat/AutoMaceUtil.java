package sweetie.nezi.api.utils.combat;

import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.player.InventoryUtil;

public final class AutoMaceUtil implements QuickImports {
    private static final float MIN_MACE_ATTACK_COOLDOWN = 0.5F;
    private static final double MIN_GROUND_DISTANCE = 4.0D;
    private static final double GROUND_CHECK_DEPTH = 12.0D;

    private AutoMaceUtil() {
    }

    public static boolean shouldUseMaceSmash() {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        if (mc.player.isOnGround()
                || mc.player.isSwimming()
                || mc.player.isGliding()
                || mc.player.isClimbing()
                || mc.player.isTouchingWater()
                || mc.player.isInLava()) {
            return false;
        }

        return mc.player.fallDistance >= (float) MIN_GROUND_DISTANCE
                || getGroundDistance() >= MIN_GROUND_DISTANCE;
    }

    public static boolean hasMaceAvailable() {
        if (mc.player == null) {
            return false;
        }

        return mc.player.getMainHandStack().isOf(Items.MACE)
                || InventoryUtil.findItem(Items.MACE, true) != -1
                || InventoryUtil.findItem(Items.MACE, false) != -1;
    }

    public static float getRequiredAttackCooldown(boolean autoMaceEnabled, float defaultCooldown) {
        if (!autoMaceEnabled) {
            return defaultCooldown;
        }

        if (!shouldUseMaceSmash() || !hasMaceAvailable()) {
            return defaultCooldown;
        }

        return Math.min(defaultCooldown, MIN_MACE_ATTACK_COOLDOWN);
    }

    public static boolean tryEquipForSmash() {
        if (!shouldUseMaceSmash() || !hasMaceAvailable() || mc.player == null) {
            return false;
        }

        if (mc.player.getMainHandStack().isOf(Items.MACE)) {
            return true;
        }

        int hotbarSlot = InventoryUtil.findItem(Items.MACE, true);
        if (hotbarSlot != -1) {
            InventoryUtil.swapToSlot(hotbarSlot);
            return mc.player.getInventory().selectedSlot == hotbarSlot;
        }

        int inventorySlot = InventoryUtil.findItem(Items.MACE, false);
        if (inventorySlot == -1) {
            return false;
        }

        int targetHotbarSlot = InventoryUtil.findBestSlotInHotBar();
        if (targetHotbarSlot == -1) {
            targetHotbarSlot = mc.player.getInventory().selectedSlot;
        }

        InventoryUtil.swapSlots(inventorySlot, targetHotbarSlot);
        InventoryUtil.swapToSlot(targetHotbarSlot);
        return mc.player.getMainHandStack().isOf(Items.MACE);
    }

    private static double getGroundDistance() {
        Vec3d start = new Vec3d(
                mc.player.getX(),
                mc.player.getBoundingBox().minY + 0.05D,
                mc.player.getZ()
        );
        Vec3d end = start.subtract(0.0D, GROUND_CHECK_DEPTH, 0.0D);

        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return GROUND_CHECK_DEPTH;
        }

        return start.y - hit.getPos().y;
    }
}
