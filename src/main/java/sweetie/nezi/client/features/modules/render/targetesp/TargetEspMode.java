package sweetie.nezi.client.features.modules.render.targetesp;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.client.features.modules.combat.AimAssistModule;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.features.modules.combat.TriggerBotModule;

public abstract class TargetEspMode implements QuickImports {
    public static final AnimationUtil showAnimation = new AnimationUtil();
    public static final AnimationUtil sizeAnimation = new AnimationUtil();
    public static LivingEntity currentTarget = null;
    private static int currentTargetId = Integer.MIN_VALUE;
    private static float retargetBlend = 0f;
    public float prevShowAnimation = 0f;
    public float prevSizeAnimation = 0f;

    public AuraModule aura() {
        return AuraModule.getInstance();
    }

    public void updateTarget() {
        LivingEntity aimTarget = AimAssistModule.getInstance().isEnabled() ? AimAssistModule.getInstance().getTarget() : null;
        LivingEntity auraTarget = aura().target;
        LivingEntity triggerTarget = TriggerBotModule.getInstance().isEnabled() ? aura().target : null;
        LivingEntity previousTarget = currentTarget;

        if (aimTarget != null) {
            currentTarget = aimTarget;
        } else if (auraTarget != null) {
            currentTarget = auraTarget;
        } else if (triggerTarget != null) {
            currentTarget = triggerTarget;
        } else if (!reason()) {
            currentTarget = null;
        }

        if (currentTarget != null) {
            if (previousTarget == null || currentTarget.getId() != currentTargetId) {
                currentTargetId = currentTarget.getId();
                retargetBlend = 1.0f;
            }
        } else if (showAnimation.getToValue() == 0.0) {
            currentTargetId = Integer.MIN_VALUE;
        }
    }

    public void updateAnimation(long duration, String mode, float size, float in, float out) {
        prevShowAnimation = (float) showAnimation.getValue();
        prevSizeAnimation = (float) sizeAnimation.getValue();

        sizeAnimation.update();
        double dyingSize = switch (mode) {
            case "In" -> in;
            case "Out" -> out;
            default -> size;
        };
        sizeAnimation.run(reason() ? size : dyingSize, duration, Easing.CUBIC_BOTH);

        showAnimation.update();
        showAnimation.run(reason() ? 1.0 : 0.0, duration, Easing.CUBIC_BOTH);
    }

    public boolean reason() {
        boolean auraActive = aura().target != null && (aura().isEnabled() || TriggerBotModule.getInstance().isEnabled());
        boolean aimAssistActive = AimAssistModule.getInstance().isEnabled() && AimAssistModule.getInstance().getTarget() != null;
        return auraActive || aimAssistActive;
    }


    public boolean canDraw() {
        if (mc.player == null || mc.world == null) return false;
        return showAnimation.getValue() > 0.0;
    }

    public static void updatePositions() {
        float animationValue = (float) showAnimation.getValue();
        float animationTarget = (float) showAnimation.getToValue();

        boolean useLastPosition = TargetEspModule.getInstance().lastPosition.getValue();
        boolean preventUpdate = useLastPosition && animationTarget == 0.0 && animationValue <= 0.9f;

        if (currentTarget != null && !preventUpdate) {
            lastTargetX = MathUtil.interpolate((float) currentTarget.prevX, (float) currentTarget.getX());
            lastTargetY = MathUtil.interpolate((float) currentTarget.prevY, (float) currentTarget.getY());
            lastTargetZ = MathUtil.interpolate((float) currentTarget.prevZ, (float) currentTarget.getZ());
        }

        if (smoothedTargetX == -1 || smoothedTargetY == -1 || smoothedTargetZ == -1) {
            smoothedTargetX = lastTargetX;
            smoothedTargetY = lastTargetY;
            smoothedTargetZ = lastTargetZ;
        } else {
            retargetBlend = Math.max(0.0f, retargetBlend - 0.055f);
            float smoothness = Math.max(0.05f, Math.min(0.55f, TargetEspModule.getInstance().getSmoothness()));
            float baseFollow = smoothness * (0.80f + animationValue * 0.30f);
            float switchFollow = MathHelper.lerp(retargetBlend, baseFollow * 0.42f, baseFollow);
            float verticalFollow = Math.max(0.035f, switchFollow * 0.86f);
            smoothedTargetX += (lastTargetX - smoothedTargetX) * switchFollow;
            smoothedTargetY += (lastTargetY - smoothedTargetY) * verticalFollow;
            smoothedTargetZ += (lastTargetZ - smoothedTargetZ) * switchFollow;
        }

        targetX = smoothedTargetX;
        targetY = smoothedTargetY;
        targetZ = smoothedTargetZ;
    }

    @Getter private static double targetX = -1;
    @Getter private static double targetY = -1;
    @Getter private static double targetZ = -1;

    private static double lastTargetX = -1;
    private static double lastTargetY = -1;
    private static double lastTargetZ = -1;
    private static double smoothedTargetX = -1;
    private static double smoothedTargetY = -1;
    private static double smoothedTargetZ = -1;

    public abstract void onUpdate();
    public abstract void onRender3D(Render3DEvent.Render3DEventData event);
}
