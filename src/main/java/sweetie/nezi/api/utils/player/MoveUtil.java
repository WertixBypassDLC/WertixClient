package sweetie.nezi.api.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;
import sweetie.nezi.api.utils.rotation.manager.RotationPlan;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.features.modules.movement.InventoryMoveModule;
import sweetie.nezi.client.features.modules.combat.AuraModule;

@UtilityClass
public class MoveUtil implements QuickImports {

    public KeyBinding[] getMovementKeys() {
        if (mc == null || mc.options == null) {
            return new KeyBinding[0];
        }

        return new KeyBinding[]{
                mc.options.sprintKey,
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey
        };
    }

    public void updateMovementKeys() {
        if (mc == null || mc.options == null || mc.getWindow() == null) {
            return;
        }

        if (isBypass()) {
            stopMovement();
            return;
        }

        for (KeyBinding movementKey : getMovementKeys()) {
            movementKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), movementKey.getDefaultKey().getCode()));
        }
    }

    public void stopMovement() {
        if (mc.options == null) return;
        for (KeyBinding key : getMovementKeys()) {
            key.setPressed(false);
        }
    }

    public boolean w() {
        return mc.options != null && mc.options.forwardKey.isPressed();
    }

    public boolean s() {
        return mc.options != null && mc.options.backKey.isPressed();
    }

    public boolean a() {
        return mc.options != null && mc.options.leftKey.isPressed();
    }

    public boolean d() {
        return mc.options != null && mc.options.rightKey.isPressed();
    }

    public boolean isMoving() {
        return mc.player != null && (mc.player.forwardSpeed != 0f || mc.player.sidewaysSpeed != 0f);
    }

    public static double lerp(double a, double b, float factor) {
        return a + (b - a) * factor;
    }

    // --- НОВОЕ (ДОБАВЛЕНО) ---
    // Вспомогательный метод для получения радианов направления (итерационный метод)
    public double getDirection(float yaw, float forward, float strafe) {
        if (forward < 0F) yaw += 180F;
        float forwardMult = 1F;
        if (forward < 0F) forwardMult = -0.5F;
        else if (forward > 0F) forwardMult = 0.5F;

        if (strafe > 0F) yaw -= 90F * forwardMult;
        if (strafe < 0F) yaw += 90F * forwardMult;
        return Math.toRadians(yaw);
    }

    // --- НОВОЕ (ДОБАВЛЕНО) ---
    // Итерационный метод MoveFix (Adequate MoveFix)
    public DirectionalInput getFixedInput(float forward, float strafe, float currentYaw, float targetYaw) {
        double intendedAngle = Math.toDegrees(getDirection(currentYaw, forward, strafe));
        intendedAngle = MathHelper.wrapDegrees(intendedAngle);

        float bestForward = 0, bestStrafe = 0;
        double minDiff = Double.MAX_VALUE;

        for (float f = -1F; f <= 1F; f += 1F) {
            for (float s = -1F; s <= 1F; s += 1F) {
                if (f == 0 && s == 0) continue;

                double potentialAngle = Math.toDegrees(getDirection(targetYaw, f, s));
                double diff = Math.abs(MathHelper.wrapDegrees(intendedAngle - potentialAngle));

                if (diff < minDiff) {
                    minDiff = diff;
                    bestForward = f;
                    bestStrafe = s;
                }
            }
        }
        return new DirectionalInput(bestForward, bestStrafe);
    }

    public static double direction(float rotationYaw, final float moveForward, final float moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;
        float forward = 1F;
        if (moveForward < 0F) forward = -0.5F;
        if (moveForward > 0F) forward = 0.5F;
        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;
        return Math.toRadians(rotationYaw);
    }

    public double[] forward(double speed) {
        if (mc.player == null || mc.player.input == null) return new double[]{0, 0};

        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        float yaw = currentRotationPlan == null ? mc.player.getYaw() : rotation.getYaw();
        if (forward != 0.0f) {
            if (strafe > 0.0f) {
                yaw += ((forward > 0.0f) ? -45 : 45);
            } else if (strafe < 0.0f) {
                yaw += ((forward > 0.0f) ? 45 : -45);
            }
            strafe = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
        double cosStrafe = Math.sin(Math.toRadians(yaw + 90.0f));
        double sinStrafe = Math.cos(Math.toRadians(yaw + 90.0f));
        double x = forward * speed * sinStrafe + strafe * speed * cosStrafe;
        double z = forward * speed * cosStrafe - strafe * speed * sinStrafe;
        return new double[]{x, z};
    }

    public void setSpeed(double speed) {
        if (mc.player == null) return;

        double[] forward = forward(speed);
        mc.player.setVelocity(forward[0], mc.player.getVelocity().y, forward[1]);
    }

    public boolean isBypass() {
        return InventoryMoveModule.getInstance().isEnabled() && InventoryMoveModule.getInstance().isProcessingPackets();
    }

    public float getFixYaw() {
        if (mc.player == null) return 0;

        if (AuraModule.moveFixEnabled()) {
            if (AuraModule.getInstance().getMove().is("Focus")) {
                LivingEntity target = AuraModule.getInstance().target;
                if (target != null) {
                    double dx = target.getX() - mc.player.getX();
                    double dz = target.getZ() - mc.player.getZ();
                    return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
                }
            }
        }
        return mc.player.getYaw();
    }
}