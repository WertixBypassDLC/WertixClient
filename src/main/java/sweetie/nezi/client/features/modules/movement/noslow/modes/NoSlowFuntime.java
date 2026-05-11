package sweetie.nezi.client.features.modules.movement.noslow.modes;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import sweetie.nezi.client.features.modules.movement.noslow.NoSlowMode;

public class NoSlowFuntime extends NoSlowMode {
    @Override
    public String getName() {
        return "Funtime";
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || !slowingCancel()) {
            return;
        }

        sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
    }

    @Override
    public void onTick() {
    }

    @Override
    public boolean slowingCancel() {
        if (mc.player == null || !mc.player.isUsingItem()) {
            return false;
        }

        ItemStack activeItem = mc.player.getActiveItem();
        return !activeItem.isEmpty() && activeItem.isOf(Items.CROSSBOW);
    }
}
