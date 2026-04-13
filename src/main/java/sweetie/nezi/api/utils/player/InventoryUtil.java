package sweetie.nezi.api.utils.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.system.backend.KeyStorage;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.other.NetworkUtil;
import sweetie.nezi.api.utils.other.SlownessManager;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;
import sweetie.nezi.api.utils.rotation.manager.RotationStrategy;
import sweetie.nezi.api.utils.rotation.rotations.SmoothRotation;
import sweetie.nezi.api.utils.task.TaskPriority;
import sweetie.nezi.client.features.modules.movement.InventoryMoveModule;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.ui.clickgui.ScreenClickGUI;

@UtilityClass
public class InventoryUtil implements QuickImports {
    public void dropSlot(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 1, SlotActionType.THROW, mc.player);
        if (!InventoryMoveModule.getInstance().isBasic())
            NetworkUtil.sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }

    public void swapToOffhand(int slot) {
        if (slot == -1) return;
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }

    public void swapSlots(int from, int to) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        int swapSlot = (to >= 0 && to <= 8) ? to : (from >= 0 && from <= 8) ? from : -1;
        if (swapSlot == -1) return;

        mc.interactionManager.clickSlot(syncId, (swapSlot == to) ? from : to, swapSlot, SlotActionType.SWAP, mc.player);
        if (!InventoryMoveModule.getInstance().isBasic()) mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }
    public void swapSlotsFull(int from, int to) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;

        mc.interactionManager.clickSlot(syncId, from, to, SlotActionType.SWAP, mc.player);
        if (!InventoryMoveModule.getInstance().isBasic()) mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }

    public void useItem(Hand hand) {
        if (mc.player == null) return;

        RotationManager rotationManager = RotationManager.getInstance();

        float currentYaw = rotationManager.getCurrentRotationPlan() != null ? rotationManager.getRotation().getYaw() : mc.player.getYaw();
        float currentPitch = rotationManager.getCurrentRotationPlan() != null ? rotationManager.getRotation().getPitch() : mc.player.getPitch();

        NetworkUtil.sendPacket(s -> new PlayerInteractItemC2SPacket(hand, s, currentYaw, currentPitch));
        mc.player.swingHand(hand);
    }

    public void swapToSlot(int slot) {
        swapToSlot(slot, true);
    }

    public void swapToSlot(int slot, boolean client) {
        if (mc.player == null) return;

        if (mc.player.getInventory().selectedSlot == slot) return;

        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        if (client) mc.player.getInventory().selectedSlot = slot;
    }

    public int findItem(Item item, boolean inHotbar) {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int firstSlot = inHotbar ? 0 : 9;
        int lastSlot = inHotbar ? 9 : 36;
        int finalSlot = -1;

        for (int i = firstSlot; i < lastSlot; i++) {
            if (main.get(i).getItem() == item) {
                finalSlot = i;
            }
        }
        return finalSlot;
    }

    public int findItem(Item input) {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int slot = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == input) {
                slot = i;
                break;
            }
        }

        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }

    public boolean hasElytraEquipped() {
        return mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    public int findAxeInInventory(boolean inHotbar) {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int firstSlot = inHotbar ? 0 : 9;
        int lastSlot = inHotbar ? 9 : 36;

        for (int i = firstSlot; i < lastSlot; i++) {
            if (main.get(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    public int findBestSlotInHotBar() {
        int emptySlot = findEmptySlot();
        return emptySlot != -1 ? emptySlot : findNonSwordSlot();
    }

    public int findEmptySlot() {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int currentItem = mc.player.getInventory().selectedSlot;

        for (int i = 0; i < 9; i++) {
            if (main.get(i).isEmpty() && currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    private static int findNonSwordSlot() {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int currentItem = mc.player.getInventory().selectedSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = main.get(i);
            if (!(stack.getItem() instanceof SwordItem) && currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    public int countNonEnchantedTotems() {
        if (mc.player == null) return -1;
        int count = 0;
        DefaultedList<ItemStack> main = mc.player.getInventory().main;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING) && !stack.hasEnchantments()) {
                count++;
            }
        }
        return count;
    }

    public int findBestTotemSlot(boolean saveEnchanted) {
        DefaultedList<ItemStack> main = mc.player.getInventory().main;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                if (!saveEnchanted || !stack.hasEnchantments()) {
                    return i < 9 ? i + 36 : i;
                }
            }
        }
        return -1;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ItemUsage {
        private final Item item;
        private final Module provider;

        private boolean forceUse = false;
        @Setter private boolean useRotation = true;
        private Rotation customRotation = new Rotation(Float.MIN_VALUE, Float.MIN_VALUE);

        private int targetSlot = -1;
        private int previousSlot = -1;
        private int originalInvSlot = -1;
        private int stateDelay = 0;

        private UsageState usageState = UsageState.IDLE;

        private boolean canUseWithCurrentScreen() {
            if (mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ScreenClickGUI) {
                return false;
            }
            return mc.currentScreen == null || InventoryMoveModule.getInstance().isEnabled();
        }

        public void onDisable() {
            usageState = UsageState.IDLE;
            stateDelay = 0;
        }

        public void updateCustomRotation(Rotation rotation) {
            customRotation = rotation;
        }

        public void applyRotation() {
            if (!useRotation) return;
            float yaw = customRotation.getYaw() != Float.MIN_VALUE ? customRotation.getYaw() : mc.player.getYaw();
            float pitch = customRotation.getPitch() != Float.MIN_VALUE ? customRotation.getPitch() : mc.player.getPitch();

            RotationStrategy configurable = new RotationStrategy(new SmoothRotation(), AuraModule.moveFixEnabled());
            RotationManager.getInstance().addRotation(new Rotation(yaw, pitch), configurable, TaskPriority.REQUIRED, provider);
        }

        public void handleUse(boolean isLegit) {
            if (!canUseWithCurrentScreen() || mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) return;

            if (isLegit) {
                executeLegitMode(-1);
            } else {
                executePacketMode();
            }
            forceUse = false;
        }


        public void handleUse(int bind, boolean isLegit) {
            boolean pressed = bind != -999 && bind != -1 && KeyStorage.isPressed(bind);

            // Если кнопка не нажата и стейт-машина закончила работу
            if (!pressed && usageState == UsageState.IDLE) {
                forceUse = false;
                return;
            }

            if (!canUseWithCurrentScreen() || mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) return;

            if (isLegit) {
                executeLegitMode(bind);
            } else {
                executePacketMode();
            }
        }

        public void executePacketMode() {
            if (mc.player.getOffHandStack().isOf(item)) {
                applyRotation();
                InventoryUtil.useItem(Hand.OFF_HAND);
                forceUse = true;
                return;
            }

            if (mc.player.getMainHandStack().isOf(item)) {
                applyRotation();
                InventoryUtil.useItem(Hand.MAIN_HAND);
                forceUse = true;
                return;
            }

            int hbSlot = InventoryUtil.findItem(item, true);
            int invSlot = InventoryUtil.findItem(item, false);
            int oldSlot = mc.player.getInventory().selectedSlot;

            if (hbSlot != -1) {
                InventoryUtil.swapToSlot(hbSlot);
                applyRotation();
                InventoryUtil.useItem(Hand.MAIN_HAND);
                InventoryUtil.swapToSlot(oldSlot);
                forceUse = true;
            } else if (invSlot != -1) {
                int bestSlot = InventoryUtil.findBestSlotInHotBar();
                if (bestSlot == -1) return;

                Runnable runnable = () -> {
                    InventoryUtil.swapSlots(invSlot, bestSlot);
                    InventoryUtil.swapToSlot(bestSlot);
                    applyRotation();
                    InventoryUtil.useItem(Hand.MAIN_HAND);
                    InventoryUtil.swapToSlot(oldSlot);
                    InventoryUtil.swapSlots(invSlot, bestSlot);
                    forceUse = true;
                };

                if (SlownessManager.isEnabled()) {
                    SlownessManager.applySlowness(10, runnable);
                } else {
                    runnable.run();
                }
            }
        }

        public void executeLegitMode(int bind) {
            switch (usageState) {
                case IDLE:
                    if (bind != -1 && bind != -999 && !KeyStorage.isPressed(bind)) return;

                    if (mc.player.getOffHandStack().isOf(item)) {
                        applyRotation();
                        InventoryUtil.useItem(Hand.OFF_HAND);
                        forceUse = true;
                        return;
                    }
                    if (mc.player.getMainHandStack().isOf(item)) {
                        applyRotation();
                        InventoryUtil.useItem(Hand.MAIN_HAND);
                        forceUse = true;
                        return;
                    }

                    int hbSlot = InventoryUtil.findItem(item, true);
                    int invSlot = InventoryUtil.findItem(item, false);

                    if (hbSlot != -1) {
                        targetSlot = hbSlot;
                        previousSlot = mc.player.getInventory().selectedSlot;
                        originalInvSlot = -1;

                        InventoryUtil.swapToSlot(targetSlot);
                        usageState = UsageState.USE_ITEM;
                        stateDelay = 1;
                    } else if (invSlot != -1) {
                        originalInvSlot = invSlot;
                        previousSlot = mc.player.getInventory().selectedSlot;
                        targetSlot = InventoryUtil.findBestSlotInHotBar();

                        if (targetSlot == -1) return;

                        InventoryUtil.swapSlots(originalInvSlot, targetSlot);
                        InventoryUtil.swapToSlot(targetSlot);

                        usageState = UsageState.USE_ITEM;
                        stateDelay = 1;
                    }
                    break;

                case USE_ITEM:
                    if (stateDelay-- > 0) return;

                    applyRotation();
                    InventoryUtil.useItem(Hand.MAIN_HAND);

                    usageState = UsageState.RESTORE_SLOT;
                    stateDelay = 1;
                    break;

                case RESTORE_SLOT:
                    if (stateDelay-- > 0) return;

                    InventoryUtil.swapToSlot(previousSlot);

                    if (originalInvSlot != -1) {
                        InventoryUtil.swapSlots(originalInvSlot, targetSlot);
                    }

                    usageState = UsageState.IDLE;
                    forceUse = true;
                    break;
            }
        }

        public enum UsageState {
            IDLE,
            USE_ITEM,
            RESTORE_SLOT
        }
    }
}