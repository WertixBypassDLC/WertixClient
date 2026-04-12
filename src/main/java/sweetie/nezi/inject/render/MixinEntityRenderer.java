package sweetie.nezi.inject.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.nezi.client.features.modules.render.nametags.NameTagsModule;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void renderLabelIfPresent(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!NameTagsModule.getInstance().isEnabled()) {
            return;
        }

        Object renderer = this;
        if (renderer instanceof PlayerEntityRenderer) {
            ci.cancel();
            return;
        }

        String label = text.getString().trim();
        if (label.matches("\\d{1,3}:\\d{2}")) {
            return;
        }

        if (renderer instanceof ArmorStandEntityRenderer && matchesPlayerLikeArmorStandLabel(label)) {
            ci.cancel();
        }
    }

    private boolean matchesPlayerLikeArmorStandLabel(String label) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return false;
        }

        String normalizedLabel = normalizeLabel(label);
        for (PlayerEntity player : client.world.getPlayers()) {
            String baseName = normalizeLabel(player.getGameProfile().getName());
            if (normalizedLabel.equals(baseName)) {
                return true;
            }

            Team team = player.getScoreboardTeam();
            if (team != null) {
                String decorated = normalizeLabel(Team.decorateName(team, Text.literal(player.getGameProfile().getName())).getString());
                if (normalizedLabel.equals(decorated)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String normalizeLabel(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("(?i)(^|\\s)[0-9A-FK-ORX](?=\\s|$)", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }
}
