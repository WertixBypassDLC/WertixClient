package sweetie.nezi.api.utils.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.player.InventoryUtil;
import sweetie.nezi.api.utils.other.SlownessManager;
import sweetie.nezi.client.features.modules.movement.SprintModule;

public class ShieldBreakManager implements QuickImports {
    public boolean shouldBreakShield(PlayerEntity entity) {
        if (canBreakShield(entity)) {
            boolean slowness = SlownessManager.isEnabled();
            int invSlot = InventoryUtil.findAxeInInventory(false);
            int hotBarSlot = InventoryUtil.findAxeInInventory(true);

            if (hotBarSlot == -1 && invSlot != -1) {
                if (!slowness) {
                    return shieldBreakAction("Inventory", hotBarSlot, invSlot, entity);
                } else {
                    SlownessManager.applySlowness(10, () -> {
                        shieldBreakAction("Inventory", hotBarSlot, invSlot, entity);
                    });
                    return true;
                }
            }

            if (hotBarSlot != -1) {
                if (!slowness) {
                    shieldBreakAction("Hotbar", hotBarSlot, invSlot, entity);
                } else {
                    SlownessManager.applySlowness(150, () -> {
                        shieldBreakAction("Hotbar", hotBarSlot, invSlot, entity);
                    });
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canBreakShield(PlayerEntity entity) {
        if (entity == null) {
            return false;
        }

        if (!entity.isBlocking() || !isLookingAtMe(entity)) {
            return false;
        }

        int invSlot = InventoryUtil.findAxeInInventory(false);
        int hotBarSlot = InventoryUtil.findAxeInInventory(true);
        return hotBarSlot != -1 || invSlot != -1;
    }

    private boolean shieldBreakAction(String action, int hotBarSlot, int invSlot, PlayerEntity entity) {
        int prevSlot = mc.player.getInventory().selectedSlot;

        // 1:1 Javelin sprint reset перед атакой щита
        if (mc.player.isSprinting() && !mc.player.isOnGround() && !mc.player.isSwimming()) {
            mc.player.setSprinting(false);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            if (!SprintModule.getInstance().isEnabled()) {
                mc.options.sprintKey.setPressed(false);
            }
        }

        switch (action) {
            case "Hotbar" -> {
                InventoryUtil.swapToSlot(hotBarSlot);
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                InventoryUtil.swapToSlot(prevSlot);
                return true;
            }

            case "Inventory" -> {
                int bestSlot = InventoryUtil.findBestSlotInHotBar();
                InventoryUtil.swapSlots(invSlot, bestSlot);
                InventoryUtil.swapToSlot(bestSlot);

                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);

                InventoryUtil.swapSlots(invSlot, bestSlot);
                InventoryUtil.swapToSlot(prevSlot);
                return true;
            }
        }

        return false;
    }

    private boolean isLookingAtMe(PlayerEntity target) {
        if (mc.player == null) return false;

        Vec3d lookVec = target.getRotationVec(1.0F).normalize(); // куда смотрит цель
        Vec3d dirToMe = mc.player.getEyePos().subtract(target.getEyePos()).normalize(); // вектор от цели к нам

        double dot = lookVec.dotProduct(dirToMe);
        double angle = Math.toDegrees(Math.acos(dot));

        return angle < 100.0; // щит ломаем только если цель реально смотрит в нашу сторону
    }
}
