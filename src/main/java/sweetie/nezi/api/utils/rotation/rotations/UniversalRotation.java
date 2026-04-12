package sweetie.nezi.api.utils.rotation.rotations;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.utils.player.PlayerUtil;
import sweetie.nezi.api.utils.rotation.RaytracingUtil;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationMode;
import sweetie.nezi.client.features.modules.combat.AuraModule;

public class UniversalRotation extends RotationMode {
    private final float dermoYaw;
    private final float dermoPitch;
    private final boolean spookySkeletons;
    private final boolean huly;

    private int jopa = 0;

    public UniversalRotation(float maxYawSpeed, float maxPitchSpeed, boolean spookySkeletons, boolean huly) {
        super("Vulcan");
        this.dermoYaw = maxYawSpeed;
        this.dermoPitch = maxPitchSpeed;
        this.spookySkeletons = spookySkeletons;
        this.huly = huly;
    }

    private float lastYawDelta = 0.0f;
    private float lastPitchDelta = 0.0f;
    private int lastPitchChangeDirection = 0;
    private int ticksSinceSwitchedDirection = 0;

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        Rotation angleDelta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();

        float maxYawSpeed = dermoYaw / 3f;
        float maxPitchSpeed = dermoPitch / 3f;

        if ((pitchDelta < 0.0f && this.lastPitchDelta > 0.0f) || (pitchDelta > 0.0f && this.lastPitchDelta < 0.0f)) {
            ticksSinceSwitchedDirection = 0;
        } else {
            ++ticksSinceSwitchedDirection;
        }

        boolean invalid = ticksSinceSwitchedDirection == 0 && Math.abs(pitchDelta) > 5.0f;
        if (invalid) {
            pitchDelta -= 1f;
            pitchDelta *= 0.3f;
            maxPitchSpeed *= 0.4f;
        }

        if (Math.abs(pitchDelta) < 0.05f) {
            pitchDelta -= (float) (Math.random() * 0.05f - 0.225f);
        }

        if (Math.abs(yawDelta - lastYawDelta) < 0.08f) {
            yawDelta -= (float) (Math.random() * 0.15f - 0.125f);
        }

        if (Math.abs(pitchDelta) < 0.01f) {
            pitchDelta -= (float) (Math.random() * 0.01f - 0.005f);
        }

        if (Math.abs(yawDelta) > 180.25f) {
            maxYawSpeed *= 0.8f;
        }

        if (Math.abs(yawDelta) > 15.0f && Math.abs(pitchDelta) < 0.1f) {
            maxYawSpeed *= 0.7f;
        }

        if (Math.abs(yawDelta) < 0.05f && Math.abs(pitchDelta) < 0.05f) {
            maxYawSpeed *= 1.1f;
            maxPitchSpeed *= 1.1f;
        }

        if (yawDelta > 1.25f && lastYawDelta > 1.25f) {
            yawDelta -= lastYawDelta;
            maxYawSpeed *= 3;
        }

        if (Math.abs(yawDelta) > 2.75f && Math.abs(pitchDelta) == 0.0f) {
            maxYawSpeed *= 0.8f;
            maxPitchSpeed *= 1.1f;
        }

        if (Math.abs(yawDelta) > 0.5f && Math.abs(pitchDelta) < 0.05f) {
            maxYawSpeed *= 0.7f;
            maxPitchSpeed *= 1.05f;
        }

        if (Math.abs(yawDelta) > 1.825f && Math.abs(pitchDelta) == 0.0f) {
            maxYawSpeed *= 0.6f;
            maxPitchSpeed *= 0.9f;
        }

        if (Math.abs(yawDelta) > 20.0f && Math.abs(pitchDelta) < 0.1f) {
            maxYawSpeed *= 0.5f;
            maxPitchSpeed *= 1.1f;
        }

        if (Math.abs(yawDelta) > 0.25f && Math.abs(pitchDelta) > 0.25f && Math.abs(pitchDelta) < 20.0f && Math.abs(yawDelta) < 20.0f) {
            maxYawSpeed *= 0.95f;
            maxPitchSpeed *= 0.85f;
        }

        if (Math.abs(yawDelta) > 0.1f && Math.abs(pitchDelta) > 0.1f && Math.abs(yawDelta) < 20.0f && Math.abs(pitchDelta) < 20.0f) {
            maxYawSpeed *= 0.9f;
            maxPitchSpeed *= 0.8f;
        }

        if (Math.abs(yawDelta) > 0.05f && Math.abs(pitchDelta) == 0.0f) {
            maxYawSpeed *= 0.8f;
            maxPitchSpeed *= 0.95f;
        }

        if (Math.abs(yawDelta) > 0.05f && Math.abs(pitchDelta) < 0.05f) {
            maxYawSpeed *= 0.85f;
            maxPitchSpeed *= 1.1f;
        }

        if (Math.abs(yawDelta) > 0.75f && Math.abs(pitchDelta) > 0.75f) {
            maxYawSpeed *= 0.8f;
            maxPitchSpeed *= 0.75f;
        }

        if (Math.abs(yawDelta) > 0.03f && Math.abs(pitchDelta) > 0.03f) {
            maxYawSpeed *= 0.9f;
            maxPitchSpeed *= 0.8f;
        }

        int currentPitchChangeDirection = pitchDelta > 0 ? 1 : -1;
        if (lastPitchChangeDirection != 0 && currentPitchChangeDirection != lastPitchChangeDirection) {
            maxPitchSpeed *= 0.2f;
        }
        lastPitchChangeDirection = currentPitchChangeDirection;

        AuraModule auraModule = AuraModule.getInstance();
        EntityHitResult raytraceResult = RaytracingUtil.raytraceEntity(auraModule.getAttackDistance(), currentRotation, false);
        boolean check = raytraceResult != null && raytraceResult.getEntity() == auraModule.target;
        boolean checked = entity != null && PlayerUtil.hasCollisionWith(entity, 1f) &&
                (
                    PlayerUtil.getBlock(0, 2, 0) != Blocks.AIR &&
                    PlayerUtil.getBlock(0, -1, 0) != Blocks.AIR &&
                    PlayerUtil.getBlock(0, 2, 0) != Blocks.WATER &&
                    PlayerUtil.getBlock(0, -1, 0) != Blocks.WATER
                );

        if (spookySkeletons) {
            int maxJopa = 20;
            float deldeldel = !huly ? 30f : 13f;
            if (checked) {
                pitchDelta /= deldeldel;
                yawDelta /= deldeldel;
                jopa = maxJopa;
            }

            if (!huly && !checked && check){
                maxPitchSpeed *= 1.3f;
                maxYawSpeed *= 1.1f;
            } else if (!huly && !checked){
                maxPitchSpeed *= 1.1f;
                maxYawSpeed *= 1.25f;
            }

            if (jopa-- > 0) {
                float superJopa = Math.max(1f, (jopa / (float) maxJopa) * 15f);
                yawDelta /= superJopa;
                pitchDelta /= superJopa;
            }
        }

        lastYawDelta = yawDelta;
        lastPitchDelta = pitchDelta;

        return new Rotation(
                currentRotation.getYaw() + MathHelper.clamp(yawDelta, -maxYawSpeed, maxYawSpeed),
                currentRotation.getPitch() + MathHelper.clamp(pitchDelta, -maxPitchSpeed, maxPitchSpeed)
        );
    }
}

