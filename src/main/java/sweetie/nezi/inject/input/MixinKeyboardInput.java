package sweetie.nezi.inject.input;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import sweetie.nezi.api.event.events.player.move.SprintEvent;
import sweetie.nezi.api.event.events.player.other.MovementInputEvent;
import sweetie.nezi.api.system.backend.SharedClass;
import sweetie.nezi.api.utils.player.DirectionalInput;
import sweetie.nezi.api.utils.player.MoveUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;
import sweetie.nezi.api.utils.rotation.manager.RotationPlan;
import sweetie.nezi.client.features.modules.combat.AuraModule;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput onTick(PlayerInput original) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return original;
        }

        Rotation currentRotation = RotationManager.getInstance().getCurrentRotation();
        RotationPlan plan = RotationManager.getInstance().getCurrentRotationPlan();
        AuraModule aura = AuraModule.getInstance();

        if (AuraModule.moveFixEnabled() && plan != null && currentRotation != null && plan.moveCorrection()) {
            float forward = original.forward() ? 1f : (original.backward() ? -1f : 0f);
            float strafe = original.left() ? 1f : (original.right() ? -1f : 0f);
            float serverYaw = currentRotation.getYaw();
            DirectionalInput fixed = null;

            switch (aura.getMove().getValue()) {
                case "Focus", "Free" -> {
                    if (forward != 0f || strafe != 0f) {
                        fixed = MoveUtil.getFixedInput(forward, strafe, mc.player.getYaw(), serverYaw);
                    }
                }

                case "Target" -> {
                    if (aura.target != null) {
                        fixed = (forward == 0f && strafe == 0f)
                                ? buildTargetApproachInput(serverYaw, aura.target.getPos(), mc.player.getPos())
                                : MoveUtil.getFixedInput(forward, strafe, mc.player.getYaw(), serverYaw);
                    }
                }

                default -> {
                }
            }

            if (fixed != null) {
                return createInput(original, fixed);
            }
        }

        MovementInputEvent.MovementInputEventData movementInputEvent = new MovementInputEvent.MovementInputEventData(
                original,
                original.jump(),
                original.sneak(),
                new DirectionalInput(original)
        );
        MovementInputEvent.getInstance().call(movementInputEvent);

        DirectionalInput untransformed = movementInputEvent.getDirectionalInput();
        DirectionalInput directionalInput = transformDirection(untransformed);

        SprintEvent.SprintEventData sprintEvent = new SprintEvent.SprintEventData(directionalInput);
        SprintEvent.getInstance().call(sprintEvent);

        this.untransformed = new PlayerInput(
                untransformed.isForwards(),
                untransformed.isBackwards(),
                untransformed.isLeft(),
                untransformed.isRight(),
                original.jump(),
                original.sneak(),
                sprintEvent.isSprint()
        );

        return new PlayerInput(
                directionalInput.isForwards(),
                directionalInput.isBackwards(),
                directionalInput.isLeft(),
                directionalInput.isRight(),
                movementInputEvent.isJump(),
                movementInputEvent.isSneak(),
                sprintEvent.isSprint()
        );
    }

    @Unique
    private DirectionalInput buildTargetApproachInput(float serverYaw, Vec3d targetPos, Vec3d playerPos) {
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.3D) {
            return new DirectionalInput(0, 0);
        }

        float angleToTarget = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float angleDiff = MathHelper.wrapDegrees(angleToTarget - serverYaw);

        float forward = 0.0F;
        float strafe = 0.0F;

        if (angleDiff >= -45.0F && angleDiff < 45.0F) {
            forward = 1.0F;
        } else if (angleDiff >= 45.0F && angleDiff < 135.0F) {
            strafe = 1.0F;
        } else if (angleDiff >= -135.0F && angleDiff < -45.0F) {
            strafe = -1.0F;
        } else {
            forward = -1.0F;
        }

        if (dist > 1.5D) {
            if (angleDiff >= -67.5F && angleDiff < -22.5F) {
                forward = 1.0F;
                strafe = -1.0F;
            } else if (angleDiff >= 22.5F && angleDiff < 67.5F) {
                forward = 1.0F;
                strafe = 1.0F;
            }
        }

        return new DirectionalInput(forward, strafe);
    }

    @Unique
    private PlayerInput createInput(PlayerInput old, DirectionalInput fixed) {
        return new PlayerInput(
                fixed.isForwards(),
                fixed.isBackwards(),
                fixed.isLeft(),
                fixed.isRight(),
                old.jump(),
                old.sneak(),
                old.sprint()
        );
    }

    @Unique
    private DirectionalInput transformDirection(DirectionalInput input) {
        if (AuraModule.moveFixEnabled()) {
            return input;
        }

        ClientPlayerEntity player = SharedClass.player();
        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rot = rotationManager.getCurrentRotation();
        RotationPlan rotationPlan = rotationManager.getCurrentRotationPlan();

        if (rotationPlan == null || rot == null || player == null || !rotationPlan.moveCorrection()) {
            return input;
        }

        float z = KeyboardInput.getMovementMultiplier(input.isForwards(), input.isBackwards());
        float x = KeyboardInput.getMovementMultiplier(input.isLeft(), input.isRight());

        if (rotationPlan.freeMoveCorrection()) {
            float deltaYaw = player.getYaw() - rot.getYaw();
            float radians = deltaYaw * 0.017453292f;
            float newX = x * MathHelper.cos(radians) - z * MathHelper.sin(radians);
            float newZ = z * MathHelper.cos(radians) + x * MathHelper.sin(radians);
            return new DirectionalInput(Math.round(newZ), Math.round(newX));
        }

        return input;
    }
}
