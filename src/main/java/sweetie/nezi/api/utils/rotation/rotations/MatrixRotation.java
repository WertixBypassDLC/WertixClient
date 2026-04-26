package sweetie.nezi.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationMode;
import sweetie.nezi.client.features.modules.combat.AuraModule;

import java.security.SecureRandom;

public class MatrixRotation extends RotationMode {
    private final SecureRandom random = new SecureRandom();

    public MatrixRotation() {
        super("Matrix");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        AuraModule aura = AuraModule.getInstance();
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float rotationDifference = Math.max((float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta)), 1.0E-4F);
        boolean canAttack = entity != null && aura != null && aura.isEnabled() && aura.combatExecutor.combatManager().canAttack();

        float yawJitter = canAttack ? 0.0F : (float) (randomLerp(4.0F, 6.0F) * Math.sin(System.currentTimeMillis() / 40.0D));
        float pitchJitter = canAttack ? 0.0F : (float) (randomLerp(2.0F, 3.0F) * Math.cos(System.currentTimeMillis() / 40.0D));
        float speed = canAttack
                ? 1.0F
                : mc.player.age % 2 == 0
                ? (random.nextBoolean() ? 0.5F : 0.3F)
                : 0.0F;

        float yawLimit = Math.abs(yawDelta / rotationDifference) * 180.0F;
        float pitchLimit = Math.abs(pitchDelta / rotationDifference) * 180.0F;
        float moveYaw = MathHelper.clamp(yawDelta, -yawLimit, yawLimit);
        float movePitch = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit);

        return new Rotation(
                MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentRotation.getYaw(), currentRotation.getYaw() + moveYaw) + yawJitter,
                MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentRotation.getPitch(), currentRotation.getPitch() + movePitch) + pitchJitter
        );
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }
}
