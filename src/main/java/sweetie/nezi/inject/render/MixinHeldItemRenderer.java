package sweetie.nezi.inject.render;

import com.google.common.base.MoreObjects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.nezi.client.features.modules.render.SwingAnimationModule;
import sweetie.nezi.client.features.modules.render.ViewModelModule;

@Mixin(value = HeldItemRenderer.class, priority = 900)
public abstract class MixinHeldItemRenderer {

    @Unique
    private static final boolean holdMyItemsLoaded = FabricLoader.getInstance().isModLoaded("hold-my-items");

    @Shadow private ItemStack mainHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float prevEquipProgressOffHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private ItemStack offHand;

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Shadow
    private static HeldItemRenderer.HandRenderType getHandRenderType(ClientPlayerEntity player) {
        throw new AssertionError();
    }

    /**
     * Inject custom item rendering (SwingAnimation + ViewModel).
     * Skipped when HoldMyItems mod is loaded to avoid conflicts.
     */
    @Inject(method = "renderFirstPersonItem", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (holdMyItemsLoaded) return;

        if (!(item.isEmpty()) && !(item.getItem() instanceof FilledMapItem)) {
            try {
                SwingAnimationModule.getInstance().handleRenderItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
                ci.cancel();
            } catch (Throwable t) {
                // fallback to vanilla rendering
            }
        }
    }

    /**
     * Apply ViewModel hand position offsets after matrix push.
     * Skipped when HoldMyItems mod is loaded to avoid conflicts.
     */
    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void injectViewModelPosition(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (holdMyItemsLoaded) return;

        ViewModelModule viewModel = ViewModelModule.getInstance();
        if (viewModel.isEnabled() && !item.isEmpty() && !(item.getItem() instanceof FilledMapItem)) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean isRight = arm == Arm.RIGHT;

            if (isRight) {
                matrices.translate(
                        viewModel.rightX.getValue().doubleValue(),
                        viewModel.rightY.getValue().doubleValue(),
                        viewModel.rightZ.getValue().doubleValue()
                );
            } else {
                matrices.translate(
                        -viewModel.leftX.getValue().doubleValue(),
                        viewModel.leftY.getValue().doubleValue(),
                        viewModel.leftZ.getValue().doubleValue()
                );
            }
        }
    }

    /**
     * Overwrite renderItem to provide vanilla behavior when HoldMyItems is loaded,
     * preventing double-rendering conflicts with the HoldMyItems mod's own arm rendering.
     *
     * @author WertixClient
     * @reason HoldMyItems compatibility
     */
    @Overwrite
    public void renderItem(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light) {
        float f = player.getHandSwingProgress(tickDelta);
        Hand hand = (Hand) MoreObjects.firstNonNull(player.preferredHand, Hand.MAIN_HAND);
        float g = player.getLerpedPitch(tickDelta);
        HeldItemRenderer.HandRenderType handRenderType = getHandRenderType(player);

        float j;
        float k;
        if (handRenderType.renderMainHand) {
            j = hand == Hand.MAIN_HAND ? f : 0.0F;
            k = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressMainHand, this.equipProgressMainHand);
            this.renderFirstPersonItem(player, tickDelta, g, Hand.MAIN_HAND, j, this.mainHand, k, matrices, vertexConsumers, light);
        }

        if (handRenderType.renderOffHand) {
            j = hand == Hand.OFF_HAND ? f : 0.0F;
            k = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressOffHand, this.equipProgressOffHand);
            this.renderFirstPersonItem(player, tickDelta, g, Hand.OFF_HAND, j, this.offHand, k, matrices, vertexConsumers, light);
        }

        vertexConsumers.draw();
    }
}
