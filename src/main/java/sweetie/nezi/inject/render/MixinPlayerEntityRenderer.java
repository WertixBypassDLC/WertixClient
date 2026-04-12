package sweetie.nezi.inject.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.scoreboard.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {
    public MixinPlayerEntityRenderer(EntityRendererFactory.Context ctx, PlayerEntityModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("HEAD"))
    private void rwHealthFix(AbstractClientPlayerEntity abstractClientPlayerEntity, PlayerEntityRenderState playerEntityRenderState, float f, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null || client.world == null) return;

        // Не трогаем здоровье локального игрока — у него и так актуальные данные с сервера
        if (abstractClientPlayerEntity == client.player) return;

        // Берем скорборд сущности
        Scoreboard scoreboard = abstractClientPlayerEntity.getScoreboard();
        ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);

        if (scoreboardObjective != null) {
            try {
                // Получаем значение из скорборда для конкретного игрока
                ReadableScoreboardScore score = scoreboard.getScore(abstractClientPlayerEntity, scoreboardObjective);
                if (score != null) {
                    float hp = (float) score.getScore();
                    // Устанавливаем здоровье сущности, если оно в разумных пределах (hp > 0, чтобы не "убить" игрока клиентсайд)
                    if (hp > 0 && hp <= 1000f) { // 1000 - запас на случай кастомных атрибутов
                        abstractClientPlayerEntity.setHealth(hp);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
