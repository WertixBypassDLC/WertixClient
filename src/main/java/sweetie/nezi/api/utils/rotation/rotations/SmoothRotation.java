package sweetie.nezi.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationMode;

public class SmoothRotation extends RotationMode {
    public SmoothRotation() {
        super("Smooth");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        float straightLineYaw = Math.abs(yawDelta / rotationDifference) * (180.0f / 7f);
        float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * (90.0f / 8f);

        return new Rotation(
                currentRotation.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
                currentRotation.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
        );
    }
}
