package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.client.KeyEvent;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BindSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.utils.player.InventoryUtil;
import sweetie.nezi.api.utils.other.SlownessManager;
import sweetie.nezi.client.features.modules.movement.InventoryMoveModule;

@ModuleRegister(name = "Auto Swap", category = Category.COMBAT)
public class ItemSwapModule extends Module {
    @Getter private static final ItemSwapModule instance = new ItemSwapModule();

    private final ModeSetting firstItem = new ModeSetting("Первый предмет").value("Shield").values("Shield", "GApple", "Totem", "Ball");
    private final ModeSetting secondItem = new ModeSetting("Второй предмет").value("GApple").values("Shield", "GApple", "Totem", "Ball");
    private final BindSetting swapKey = new BindSetting("Кнопка свапа").value(-999);

    private boolean swapping = false;

    public ItemSwapModule() {
        addSettings(firstItem, secondItem, swapKey);
    }

    @Override
    public void onEvent() {
        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.key() == swapKey.getValue() && event.action() == 1 && mc.currentScreen == null) {
                swapping = true;
            }
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!shouldHandleOnTick()) return;
            performSwap();
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (shouldHandleOnTick()) return;
            performSwap();
        }));

        addEvents(keyEvent, tickEvent, updateEvent);
    }

    private boolean shouldHandleOnTick() {
        return InventoryMoveModule.getInstance().usesBypassFlow();
    }

    private boolean shouldUseLegitBypass() {
        return InventoryMoveModule.getInstance().usesLegitItemBypass();
    }

    private void performSwap() {
        if (!(mc.world != null && mc.player != null && mc.interactionManager != null && swapping)) return;

        Item item = getItem();

        if (item == null) {
            print("Предмет не найден");
            swapping = false;
            return;
        }

        int slot = InventoryUtil.findItem(item);
        if (slot == -1) {
            print("Предмет не найден в инвентаре");
            swapping = false;
            return;
        }

        if (shouldUseLegitBypass()) {
            SlownessManager.applySlowness(10, () -> swap(slot));
        } else {
            swap(slot);
        }
    }

    private Item getItem() {
        Item primary = getItemByMode(firstItem.getValue());
        Item secondary = getItemByMode(secondItem.getValue());
        return mc.player.getOffHandStack().getItem() == primary ? secondary : primary;
    }

    private void swap(int slot) {
        if (mc.interactionManager == null) {
            return;
        }

        print("Свапнула на \"" + getItem().getName().getString() + "\"");

        if (shouldUseLegitBypass()) {
            SlownessManager.applySlowness(10, () -> InventoryUtil.swapToOffhand(slot));
        } else InventoryUtil.swapToOffhand(slot);
        swapping = false;
    }

    private Item getItemByMode(String name) {
        return switch (name.toLowerCase()) {
            case "shield" -> Items.SHIELD;
            case "ball" -> Items.PLAYER_HEAD;
            case "totem" -> Items.TOTEM_OF_UNDYING;
            case "gapple" -> Items.GOLDEN_APPLE;
            default -> null;
        };
    }
}
