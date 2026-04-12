package sweetie.nezi.inject.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import sweetie.nezi.client.features.modules.render.CameraClipModule;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    protected abstract float clipToSpace(float desiredCameraDistance);

    @Shadow
    protected abstract void moveBy(float x, float y, float z);

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    private void updateThirdPerson(Args args) {
        if (!CameraClipModule.getInstance().isEnabled()) {
            return;
        }

        args.set(0, -clipToSpace(CameraClipModule.getInstance().thirdPersonDistance.getValue()));
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void updateFirstPerson(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CameraClipModule module = CameraClipModule.getInstance();
        if (!module.isEnabled() || thirdPerson || !module.firstPersonBypass.getValue() || focusedEntity == null || area == null) {
            return;
        }

        BlockPos eyePos = BlockPos.ofFloored(focusedEntity.getEyePos());
        BlockState state = area.getBlockState(eyePos);
        if (!state.isAir() && state.isSolidBlock(area, eyePos)) {
            moveBy(-module.firstPersonDistance.getValue(), 0.0F, 0.0F);
        }
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void clipToSpace(float desiredDistance, CallbackInfoReturnable<Float> returnable) {
        if (!CameraClipModule.getInstance().isEnabled()) {
            return;
        }

        returnable.setReturnValue(CameraClipModule.getInstance().thirdPersonDistance.getValue());
    }
}
