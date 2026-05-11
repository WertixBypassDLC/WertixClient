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
    public static final AnimationUtil transitionAnimation = new AnimationUtil();

    public static LivingEntity currentTarget = null;
    private static int currentTargetId = Integer.MIN_VALUE;

    public float prevShowAnimation = 0f;
    public float prevSizeAnimation = 0f;

    @Getter private static double targetX = -1;
    @Getter private static double targetY = -1;
    @Getter private static double targetZ = -1;

    private static double lastTargetX = -1;
    private static double lastTargetY = -1;
    private static double lastTargetZ = -1;
    private static double smoothedTargetX = -1;
    private static double smoothedTargetY = -1;
    private static double smoothedTargetZ = -1;

    // Точки для расчета вектора отлета/прилета (эффект растяжения)
    private static double transitionStartX = 0;
    private static double transitionStartY = 0;
    private static double transitionStartZ = 0;

    public AuraModule aura() {
        return AuraModule.getInstance();
    }

    public static float getRetargetBlend() {
        return (float) transitionAnimation.getValue();
    }

    public static double getTransitionDx() {
        return transitionStartX - lastTargetX;
    }

    public static double getTransitionDy() {
        return transitionStartY - lastTargetY;
    }

    public static double getTransitionDz() {
        return transitionStartZ - lastTargetZ;
    }

    public static void snapToTarget() {
        transitionAnimation.setValue(0.0);
        transitionAnimation.run(0.0, 1, Easing.LINEAR);

        if (currentTarget == null) {
            return;
        }

        double x = MathUtil.interpolate((float) currentTarget.prevX, (float) currentTarget.getX());
        double y = MathUtil.interpolate((float) currentTarget.prevY, (float) currentTarget.getY());
        double z = MathUtil.interpolate((float) currentTarget.prevZ, (float) currentTarget.getZ());

        lastTargetX = x;
        lastTargetY = y;
        lastTargetZ = z;
        smoothedTargetX = x;
        smoothedTargetY = y;
        smoothedTargetZ = z;
        transitionStartX = x;
        transitionStartY = y;
        transitionStartZ = z;
        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public void syncAnimationSnapshot() {
        prevShowAnimation = (float) showAnimation.getValue();
        prevSizeAnimation = (float) sizeAnimation.getValue();
    }

    public void onModeSelected() {
        syncAnimationSnapshot();
    }

    public void onModeDeselected() {
    }

    public void updateTarget() {
        LivingEntity aimTarget = AimAssistModule.getInstance().isEnabled() ? AimAssistModule.getInstance().getTarget() : null;
        LivingEntity auraTarget = aura().target;
        LivingEntity triggerTarget = TriggerBotModule.getInstance().isEnabled() ? aura().target : null;

        LivingEntity previousTarget = currentTarget;

        if (auraTarget != null) {
            currentTarget = auraTarget;
        } else if (triggerTarget != null) {
            currentTarget = triggerTarget;
        } else if (aimTarget != null) {
            currentTarget = aimTarget;
        } else if (!reason() && showAnimation.getValue() <= 0.01 && showAnimation.getToValue() == 0.0) {
            currentTarget = null;
        }

        if (currentTarget != null) {
            if (previousTarget == null || currentTarget.getId() != currentTargetId) {
                currentTargetId = currentTarget.getId();

                // Захватываем текущую позицию ESP для плавного, гармоничного старта перелета
                if (smoothedTargetX != -1 && previousTarget != null) {
                    transitionStartX = smoothedTargetX;
                    transitionStartY = smoothedTargetY;
                    transitionStartZ = smoothedTargetZ;

                    transitionAnimation.setValue(1.0);
                    transitionAnimation.run(0.0, 750, Easing.QUART_OUT);
                }
            }
        } else if (currentTarget == null && showAnimation.getToValue() == 0.0) {
            currentTargetId = Integer.MIN_VALUE;
        }
    }

    public void updateAnimation(long duration, String mode, float size, float in, float out) {
        prevShowAnimation = (float) showAnimation.getValue();
        prevSizeAnimation = (float) sizeAnimation.getValue();

        sizeAnimation.update();
        double dyingSize = switch (mode) {
            case "Появление" -> in;
            case "Затухание" -> out;
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
        transitionAnimation.update();

        float animationValue = (float) showAnimation.getValue();
        float animationTarget = (float) showAnimation.getToValue();
        float blend = getRetargetBlend();

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
            // Если идет анимация переключения (blend > 0), ESP плавно "доезжает" по идеальной кривой Easing
            if (blend > 0.001f) {
                smoothedTargetX = transitionStartX + (lastTargetX - transitionStartX) * (1.0f - blend);
                smoothedTargetY = transitionStartY + (lastTargetY - transitionStartY) * (1.0f - blend);
                smoothedTargetZ = transitionStartZ + (lastTargetZ - transitionStartZ) * (1.0f - blend);
            } else {
                // Обычное мягкое следование, когда ESP уже прикрепился к игроку
                float s = Math.max(0.05f, Math.min(0.55f, TargetEspModule.getInstance().getSmoothness()));
                smoothedTargetX += (lastTargetX - smoothedTargetX) * s;
                smoothedTargetY += (lastTargetY - smoothedTargetY) * Math.max(0.035f, s * 0.86f);
                smoothedTargetZ += (lastTargetZ - smoothedTargetZ) * s;
            }
        }

        targetX = smoothedTargetX;
        targetY = smoothedTargetY;
        targetZ = smoothedTargetZ;
    }

    public int setAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }

    public static class SmoothFloat {
        private float value;
        public SmoothFloat(float initial) { this.value = initial; }
        public void update(float target, float speed) { this.value += (target - this.value) * speed; }
        public float get() { return this.value; }
        public void set(float val) { this.value = val; }
    }

    public abstract void onUpdate();
    public abstract void onRender3D(Render3DEvent.Render3DEventData event);
}
