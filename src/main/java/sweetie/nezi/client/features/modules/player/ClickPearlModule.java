package sweetie.nezi.client.features.modules.player;

import lombok.Getter;
import net.minecraft.item.Items;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BindSetting;
import sweetie.nezi.api.utils.player.InventoryUtil;
import sweetie.nezi.client.features.modules.movement.InventoryMoveModule;

@ModuleRegister(name = "Click Pearl", category = Category.PLAYER)
public class ClickPearlModule extends Module {
    @Getter private static final ClickPearlModule instance = new ClickPearlModule();

    private final BindSetting throwKey = new BindSetting("Кнопка перки").value(-999);

    private final InventoryUtil.ItemUsage itemUsage = new InventoryUtil.ItemUsage(Items.ENDER_PEARL, this);

    public ClickPearlModule() {
        addSettings(throwKey);
    }

    @Override
    public void onDisable() {
        itemUsage.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (shouldHandleOnTick()) {
                handle();
            }
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!shouldHandleOnTick()) {
                handle();
            }
        }));

        addEvents(tickEvent, updateEvent);
    }

    private boolean shouldHandleOnTick() {
        return InventoryMoveModule.getInstance().usesBypassFlow();
    }

    private void handle() {
        itemUsage.handleUse(throwKey.getValue(), InventoryMoveModule.getInstance().usesLegitItemBypass());
    }
}
