package sweetie.nezi.client.features.modules.movement.noslow.modes;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import sweetie.nezi.api.utils.player.InventoryUtil;
import sweetie.nezi.client.features.modules.movement.noslow.NoSlowMode;

public class NoSlowFuntimeNew extends NoSlowMode {
    private int swappedCrossbowSlot = -1;

    @Override
    public String getName() {
        return "Funtime New";
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) {
            restoreCrossbow();
            return;
        }

        if (slowingCancel()) {
            sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            keepSprintWhileUsing();
            handleCrossbowSwap();
            return;
        }

        restoreCrossbow();
    }

    @Override
    public void onTick() {
    }

    @Override
    public void onDisable() {
        restoreCrossbow();
    }

    @Override
    public boolean slowingCancel() {
        return mc.player != null && mc.player.isUsingItem();
    }

    private void keepSprintWhileUsing() {
        if (mc.player == null || mc.player.input == null) {
            return;
        }

        boolean canSprint = !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && mc.player.input.hasForwardMovement()
                && !mc.player.horizontalCollision
                && !mc.player.isSneaking()
                && !mc.player.isGliding()
                && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater());
        mc.player.setSprinting(canSprint);
    }

    private void handleCrossbowSwap() {
        if (swappedCrossbowSlot != -1 || mc.player == null || mc.player.currentScreenHandler == null) {
            return;
        }
        if (!mc.player.isUsingItem() || mc.player.getActiveHand() != Hand.MAIN_HAND) {
            return;
        }

        ItemStack active = mc.player.getActiveItem();
        if (!isConsumable(active)) {
            return;
        }
        if (mc.player.getOffHandStack().isOf(Items.CROSSBOW)) {
            return;
        }

        int crossbowSlot = InventoryUtil.findItem(Items.CROSSBOW);
        if (crossbowSlot == -1) {
            return;
        }

        swappedCrossbowSlot = crossbowSlot;
        InventoryUtil.swapToOffhand(crossbowSlot);
    }

    private void restoreCrossbow() {
        if (swappedCrossbowSlot == -1 || mc.player == null || mc.player.currentScreenHandler == null) {
            swappedCrossbowSlot = -1;
            return;
        }

        InventoryUtil.swapToOffhand(swappedCrossbowSlot);
        swappedCrossbowSlot = -1;
    }

    private boolean isConsumable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.getComponents().contains(DataComponentTypes.FOOD)
                || stack.isOf(Items.POTION)
                || stack.isOf(Items.SPLASH_POTION)
                || stack.isOf(Items.LINGERING_POTION)
                || stack.isOf(Items.MILK_BUCKET)
                || stack.isOf(Items.HONEY_BOTTLE);
    }
}
