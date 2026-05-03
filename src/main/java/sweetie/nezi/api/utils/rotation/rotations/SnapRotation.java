package sweetie.nezi.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationMode;

public class SnapRotation extends RotationMode {
    public SnapRotation() {
        super("Snap");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float rotationDifference = Math.max((float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta)), 1.0E-4F);
        float speed = entity != null ? 1.0F : 0.4F;

        float yawLimit = Math.abs(yawDelta / rotationDifference) * 180.0F;
        float pitchLimit = Math.abs(pitchDelta / rotationDifference) * 180.0F;
        float moveYaw = MathHelper.clamp(yawDelta, -yawLimit, yawLimit);
        float movePitch = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit);

        return new Rotation(
                MathHelper.lerp(MathUtil.randomInRange(speed, speed + 0.2F), currentRotation.getYaw(), currentRotation.getYaw() + moveYaw),
                MathHelper.lerp(MathUtil.randomInRange(speed, speed + 0.2F), currentRotation.getPitch(), currentRotation.getPitch() + movePitch)
        );
    }
}
