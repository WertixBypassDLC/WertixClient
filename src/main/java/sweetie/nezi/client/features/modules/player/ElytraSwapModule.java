package sweetie.nezi.client.features.modules.player;

import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BindSetting;
import sweetie.nezi.api.system.backend.KeyStorage;
import sweetie.nezi.api.utils.player.InventoryUtil;
import sweetie.nezi.api.utils.other.SlownessManager;
import sweetie.nezi.client.features.modules.movement.InventoryMoveModule;

@ModuleRegister(name = "Elytra Swap", category = Category.PLAYER)
public class ElytraSwapModule extends Module {
    @Getter private static final ElytraSwapModule instance = new ElytraSwapModule();

    private final BindSetting swapKey = new BindSetting("Кнопка свапа").value(-999);
    private final BindSetting launchKey = new BindSetting("Кнопка фейерверка").value(-999);

    private final InventoryUtil.ItemUsage itemUsage = new InventoryUtil.ItemUsage(Items.FIREWORK_ROCKET, this);
    private boolean swapUsed = false;

    public ElytraSwapModule() {
        addSettings(swapKey, launchKey);
        itemUsage.setUseRotation(false);
    }

    @Override
    public void onDisable() {
        itemUsage.onDisable();
        swapUsed = false;
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            handleMainLogic(!SlownessManager.isEnabled());
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            handleMainLogic(SlownessManager.isEnabled());
        }));

        addEvents(tickEvent, updateEvent);
    }

    private void handleMainLogic(boolean slow) {
        handleFireworkLaunch(slow);
        handleChestplateSwap(slow);
    }

    public void handleFireworkLaunch(boolean tick) {
        if (tick || !mc.player.isGliding()) return;

        itemUsage.handleUse(launchKey.getValue(), false);
    }

    public void handleChestplateSwap(boolean tick) {
        if (tick) return;

        if (KeyStorage.isPressed(swapKey.getValue())) {
            boolean canWorkWithScreen = mc.currentScreen == null || InventoryMoveModule.getInstance().isEnabled();
            if (!swapUsed && canWorkWithScreen) {
                if (slots() == -1) return;

                if (SlownessManager.isEnabled()) {
                    SlownessManager.applySlowness(10, () -> {
                        swapChestplate();
                        swapUsed = true;
                    });
                } else {
                    swapChestplate();
                    swapUsed = true;
                }
            }
        } else {
            swapUsed = false;
        }
    }

    public void swapChestplate() {
        if (mc.player == null || mc.interactionManager == null) return;

        if (InventoryUtil.hasElytraEquipped()) {
            int slot = slots();

            if (slot != -1) {
                if (slot >= 0 && slot <= 8) {
                    InventoryUtil.swapSlotsFull(6, slot);
                } else if (slot >= 36 && slot <= 44) {
                    int hotbarSlot = slot - 36;
                    InventoryUtil.swapSlotsFull(6, hotbarSlot);
                } else {
                    int emptySlot = InventoryUtil.findEmptySlot();
                    if (emptySlot == -1) {
                        emptySlot = InventoryUtil.findBestSlotInHotBar();
                    }

                    if (emptySlot != -1) {
                        InventoryUtil.swapSlots(slot, emptySlot);
                        InventoryUtil.swapSlotsFull(6, emptySlot);
                        InventoryUtil.swapSlots(slot, emptySlot);
                    }
                }
            }
        } else {
            int slot = slots();

            if (slot != -1) {
                if (slot >= 0 && slot <= 8) {
                    InventoryUtil.swapSlotsFull(6, slot);
                } else if (slot >= 36 && slot <= 44) {
                    int hotbarSlot = slot - 36;
                    InventoryUtil.swapSlotsFull(6, hotbarSlot);
                } else {
                    int emptySlot = InventoryUtil.findEmptySlot();
                    if (emptySlot == -1) {
                        emptySlot = InventoryUtil.findBestSlotInHotBar();
                    }

                    if (emptySlot != -1) {
                        InventoryUtil.swapSlots(slot, emptySlot);
                        InventoryUtil.swapSlotsFull(6, emptySlot);
                        InventoryUtil.swapSlots(slot, emptySlot);
                    }
                }
            }
        }
    }

    private int findBestSlotFor(Item... items) {
        for (Item item : items) {
            int slot = InventoryUtil.findItem(item);
            if (slot != -1) return slot;
        }
        return -1;
    }

    public int slots() {
        return InventoryUtil.hasElytraEquipped() ? findChestplateSlot() : findElytraSlot();
    }

    public int findElytraSlot() {
        return findBestSlotFor(Items.ELYTRA);
    }

    public int findChestplateSlot() {
        return findBestSlotFor(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
                Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE);
    }
}
