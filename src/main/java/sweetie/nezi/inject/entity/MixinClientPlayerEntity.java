package sweetie.nezi.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.nezi.api.event.events.player.move.MotionEvent;
import sweetie.nezi.api.event.events.player.other.CloseScreenEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.event.events.player.move.MoveEvent;
import sweetie.nezi.api.event.events.player.move.SprintEvent;
import sweetie.nezi.api.system.backend.SharedClass;
import sweetie.nezi.api.utils.player.DirectionalInput;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;
import sweetie.nezi.api.utils.rotation.manager.RotationPlan;
import sweetie.nezi.api.utils.rotation.rotations.FunTimeRotation;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.features.modules.combat.VelocityModule;
import sweetie.nezi.client.features.modules.movement.SprintModule;
import sweetie.nezi.client.features.modules.movement.noslow.NoSlowModule;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    public Input input;

    @Unique private double prevBodyYawX = 0.0D;
    @Unique private double prevBodyYawZ = 0.0D;
    @Unique private float prevBodyYaw = 0.0F;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHook(CallbackInfo ci) {
        UpdateEvent.getInstance().call();
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void moveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        MoveEvent.MoveEventData event = new MoveEvent.MoveEventData(movement.x, movement.y, movement.z);
        if (MoveEvent.getInstance().call(event)) {
            super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float silentRotationYawPackets(float original) {
        RotationManager rotationManager = RotationManager.getInstance();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null) {
            return original;
        }

        Rotation rotation = rotationManager.getRotation();

        if (SharedClass.player() != null) {
            float combatYaw = rotation.getYaw();
            float bodyYaw = calculateBodyYaw(combatYaw);
            float visualYaw = resolveVisualYaw(original, combatYaw);
            this.setHeadYaw(visualYaw);
            this.setBodyYaw(bodyYaw);
        }

        return rotation.getYaw();
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float silentRotationYawTick(float original) {
        RotationManager rotationManager = RotationManager.getInstance();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null) {
            return original;
        }

        Rotation rotation = rotationManager.getRotation();

        if (SharedClass.player() != null) {
            float combatYaw = rotation.getYaw();
            float bodyYaw = calculateBodyYaw(combatYaw);
            float visualYaw = resolveVisualYaw(original, combatYaw);
            this.setHeadYaw(visualYaw);
            this.setBodyYaw(bodyYaw);
        }

        return rotation.getYaw();
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float silentRotationPitchPackets(float original) {
        RotationPlan rotation = RotationManager.getInstance().getCurrentRotationPlan();
        if (rotation == null) {
            return original;
        }

        Rotation rot = RotationManager.getInstance().getRotation();

        return rot.getPitch();
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float silentRotationPitchTick(float original) {
        RotationPlan rotation = RotationManager.getInstance().getCurrentRotationPlan();
        if (rotation == null) {
            return original;
        }

        Rotation rot = RotationManager.getInstance().getRotation();
        return rot.getPitch();
    }

    @Inject(method = "sendMovementPackets", at = @At(value = "HEAD"), cancellable = true)
    private void preMotion(CallbackInfo ci) {
        MotionEvent.MotionEventData event = new MotionEvent.MotionEventData(getX(), getY(), getZ(), getYaw(1), getPitch(1), isOnGround());
        if (MotionEvent.getInstance().call(event)) ci.cancel();
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;canSprint()Z"))
    private boolean sprintEventTick(boolean original) {
        return sprintHook(original);
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean sprintEventInput(boolean original) {
        return sprintHook(original);
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void noPushByBlocksHook(double x, double d, CallbackInfo ci) {
        if (VelocityModule.getInstance().cancelPush(VelocityModule.PushingSource.BLOCK)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
    private boolean tickMovementHook(boolean original) {
        if (original) {
            SprintModule.getInstance().tickStop = 1;
        }
        if (NoSlowModule.getInstance().doUseNoSlow()) return false;
        return original;
    }

    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean sprintAffectStartHook(boolean original) {
        if (NoSlowModule.getInstance().isEnabled()) return false;

        return original;
    }

    @Inject(method = "closeHandledScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"), cancellable = true)
    private void onCloseHandledScreen(CallbackInfo ci) {
        if (CloseScreenEvent.getInstance().call()) ci.cancel();
    }

    @Unique
    private boolean sprintHook(boolean origin) {
        if (SprintModule.getInstance().mode.is("Legit")) {
            return origin;
        }
        return SprintEvent.getInstance().call(new SprintEvent.SprintEventData(new DirectionalInput(input)));
    }

    @Unique
    private float resolveVisualYaw(float originalYaw, float combatYaw) {
        if (!shouldUseFunTimeVisualOverride()) {
            return combatYaw;
        }

        Rotation visualRotation = FunTimeRotation.getVisualRotation();
        return visualRotation != null ? visualRotation.getYaw() : originalYaw;
    }

    @Unique
    private boolean shouldUseFunTimeVisualOverride() {
        AuraModule aura = AuraModule.getInstance();
        return aura != null && aura.isEnabled() && aura.getAimMode().is("Fun Time") && FunTimeRotation.hasVisualRotation();
    }

    @Unique
    private float calculateBodyYaw(float yaw) {
        double currentX = this.getX();
        double currentZ = this.getZ();
        double motionX = currentX - prevBodyYawX;
        double motionZ = currentZ - prevBodyYawZ;
        float motionSquared = (float) (motionX * motionX + motionZ * motionZ);
        float bodyYaw = prevBodyYaw;

        if (motionSquared > 0.0025000002F) {
            float movementYaw = (float) MathHelper.atan2(motionZ, motionX) * (180F / (float) Math.PI) - 90.0F;
            float yawDiff = MathHelper.abs(MathHelper.wrapDegrees(yaw) - movementYaw);
            bodyYaw = (95.0F < yawDiff && yawDiff < 265.0F) ? movementYaw - 180.0F : movementYaw;
        }

        if (SharedClass.player() != null && SharedClass.player().handSwingProgress - 0.2F > 0F) {
            bodyYaw = yaw;
        }

        float deltaYaw = MathHelper.wrapDegrees(bodyYaw - prevBodyYaw);
        bodyYaw = prevBodyYaw + deltaYaw * 0.3F;

        float yawOffsetDiff = MathHelper.wrapDegrees(yaw - bodyYaw);
        float maxHeadRotation = 52.0F;
        if (Math.abs(yawOffsetDiff) > maxHeadRotation) {
            bodyYaw += yawOffsetDiff - (float) MathHelper.sign(yawOffsetDiff) * maxHeadRotation;
        }

        prevBodyYaw = bodyYaw;
        prevBodyYawX = currentX;
        prevBodyYawZ = currentZ;
        return bodyYaw;
    }
}
