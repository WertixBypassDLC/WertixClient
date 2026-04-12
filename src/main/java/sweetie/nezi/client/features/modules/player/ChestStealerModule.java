package sweetie.nezi.client.features.modules.player;

import lombok.Getter;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.math.TimerUtil;

@ModuleRegister(name = "Chest Stealer", category = Category.PLAYER)
public class ChestStealerModule extends Module {
    @Getter private static final ChestStealerModule instance = new ChestStealerModule();

    // Настройка задержки (по дефолту 10 мс)
    private final SliderSetting delay = new SliderSetting("Delay").value(10f).range(0f, 500f).step(10f);
    // Автоматическое закрытие, когда сундук пуст
    private final BooleanSetting autoClose = new BooleanSetting("Auto close").value(true);

    private final TimerUtil timer = new TimerUtil();

    public ChestStealerModule() {
        addSettings(delay, autoClose);
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            // Проверяем, открыт ли контейнер (сундук, шалкер, бочка)
            if (mc.player == null || mc.player.currentScreenHandler == null) return;

            if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
                return;
            }

            // Проверяем, полон ли наш инвентарь (кроме брони и оффхенда)
            if (isInventoryFull()) {
                if (autoClose.getValue()) {
                    mc.player.closeHandledScreen();
                }
                return;
            }

            // Количество слотов в самом контейнере (исключая инвентарь игрока)
            // Обычно это rows * 9
            int rows = handler.getRows();
            int chestSize = rows * 9;

            // Если таймер прошел
            if (timer.finished(delay.getValue().longValue())) {
                for (int i = 0; i < chestSize; i++) {
                    Slot slot = handler.getSlot(i);

                    // Если в слоте есть предмет
                    if (slot.hasStack()) {
                        // Quick Move (Shift + Click)
                        mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                        timer.reset();
                        return; // Берем по 1 предмету за тик (или задержку), чтобы не кикало
                    }
                }

                // Если мы дошли до сюда, значит цикл не нашел предметов -> сундук пуст
                if (autoClose.getValue()) {
                    mc.player.closeHandledScreen();
                }
            }
        }));

        addEvents(update);
    }

    private boolean isInventoryFull() {
        for (int i = 9; i < 36; i++) { // Слоты инвентаря (без хотбара, брони и т.д., можно расширить 0-36)
            if (mc.player.getInventory().main.get(i).isEmpty()) {
                return false;
            }
        }
        // Проверяем хотбар тоже
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().main.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}