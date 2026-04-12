package sweetie.nezi.client.features.modules.player;

import lombok.Getter;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.inject.client.MinecraftClientAccessor;

@ModuleRegister(name = "No Delay", category = Category.PLAYER)
public class NoDelayModule extends Module {
    @Getter private static final NoDelayModule instance = new NoDelayModule();

    public final BooleanSetting fastPlace = new BooleanSetting("Быстрая установка").value(false);
    public final SliderSetting placeDelay = new SliderSetting("Задержка установки").value(0f).range(0f, 5f).step(1f).setVisible(fastPlace::getValue);

    public final BooleanSetting fastExp = new BooleanSetting("Быстрые опытки").value(false);
    public final SliderSetting expDelay = new SliderSetting("Задержка опыток").value(0f).range(0f, 5f).step(1f).setVisible(fastExp::getValue);

    public final BooleanSetting noJumpDelay = new BooleanSetting("Без задержки прыжка").value(true);

    public NoDelayModule() {
        addSettings(fastPlace, placeDelay, fastExp, expDelay, noJumpDelay);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            // Логика FastPlace
            if (fastPlace.getValue() && mc.player.getMainHandStack().getItem() instanceof BlockItem) {
                if (((MinecraftClientAccessor) mc).getItemUseCooldown() > placeDelay.getValue().intValue()) {
                    ((MinecraftClientAccessor) mc).setItemUseCooldown(placeDelay.getValue().intValue());
                }
            }

            // Логика FastExp
            if (fastExp.getValue() && mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                if (((MinecraftClientAccessor) mc).getItemUseCooldown() > expDelay.getValue().intValue()) {
                    ((MinecraftClientAccessor) mc).setItemUseCooldown(expDelay.getValue().intValue());
                }
            }

            // Старая логика прыжка
            if (noJumpDelay.getValue()) {
                mc.player.jumpingCooldown = 0;
            }
        }));

        addEvents(updateEvent);
    }
}
