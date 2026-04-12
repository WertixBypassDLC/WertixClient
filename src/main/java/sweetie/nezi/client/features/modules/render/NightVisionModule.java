package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;

@ModuleRegister(name = "Fullbright", category = Category.RENDER)
public class NightVisionModule extends Module {
    @Getter private static final NightVisionModule instance = new NightVisionModule();

    @Override
    public void onEnable() {
        addEffect();
    }

    @Override
    public void onDisable() {
        removeEffect();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> addEffect()));
        addEvents(tickEvent);
    }

    private void addEffect() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0, false, false, false));
    }

    private void removeEffect() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
