package sweetie.nezi.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationMode;

import java.security.SecureRandom;

public class FunTimeRotation extends RotationMode implements QuickImports {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static long smoothbackShakeStartMs = -1L;

    private static final long FORCE_MISS_DURATION_MS = 320L;
    private static boolean forceMissActive = false;
    private static long forceMissEndTime = 0L;
    private static int lastForceMissAttackCount = -1;

    // Состояния для Snap-ротаций (те самые методы reset и primeSnapWindow)
    private static boolean canSnap = false;
    private static long lastClickTime = 0L;

    public FunTimeRotation() {
        super("FunTimeSnap");
    }

    /**
     * Сбрасывает состояние ротации. Вызывается из AuraModule.
     */
    public static void reset() {
        smoothbackShakeStartMs = -1L;
        forceMissActive = false;
        canSnap = false;
        lastForceMissAttackCount = -1;
    }

    /**
     * Подготавливает окно для удара. Вызывается из AuraModule.
     */
    public static void primeSnapWindow(boolean attackPossible, long timeSinceLastClick) {
        canSnap = attackPossible;
        lastClickTime = timeSinceLastClick;
    }

    /**
     * Возвращает точку атаки. Вызывается из AuraUtil.
     */
    public static Vec3d getAimPoint(Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        // Нацеливаемся на уровень глаз с учетом текущей позы цели
        return entity.getPos().add(0, MathHelper.clamp(entity.getEyeHeight(entity.getPose()), 0.1, entity.getHeight()), 0);
    }

    @Override
    public Rotation process(Rotation currentAngle, Rotation targetAngle, Vec3d vec3d, Entity entity) {
        if (mc.player == null) return currentAngle;

        // Если аура готова бить (canSnap) — делаем резкую доводку
        if (canSnap && entity != null) {
            smoothbackShakeStartMs = -1L;
            Rotation delta = calculateDelta(currentAngle, targetAngle);

            float yawDelta = delta.getYaw();
            float pitchDelta = delta.getPitch();
            float rotationDiff = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
            if (rotationDiff < 1.0E-4F) rotationDiff = 1.0E-4F;

            // Лимиты скорости для обхода проверок на "слишком резкий поворот"
            float yawLimit = Math.abs(yawDelta / rotationDiff) * 130.0F;
            float pitchLimit = Math.abs(pitchDelta / rotationDiff) * 130.0F;

            float yaw = MathHelper.lerp(0.85F, currentAngle.getYaw(),
                    currentAngle.getYaw() + MathHelper.clamp(yawDelta, -yawLimit, yawLimit));
            float pitch = MathHelper.lerp(0.85F, currentAngle.getPitch(),
                    currentAngle.getPitch() + MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit));

            return new Rotation(
                    applyGCD(yaw, currentAngle.getYaw()),
                    MathHelper.clamp(applyGCD(pitch, currentAngle.getPitch()), -90.0F, 90.0F)
            );
        }

        // Логика SmoothBack (плавный возврат камеры, если не бьем)
        Rotation playerAngle = new Rotation(mc.player.getYaw(), mc.player.getPitch());

        if (smoothbackShakeStartMs < 0L) {
            smoothbackShakeStartMs = System.currentTimeMillis();
        }

        // Затухание тряски со временем (3 секунды)
        float fade = 1.0F - MathHelper.clamp((System.currentTimeMillis() - smoothbackShakeStartMs) / 3000.0F, 0.0F, 1.0F);

        float yawShake = randomRange(12.0F, 22.0F) * (float) Math.sin(System.currentTimeMillis() / 60.0) * fade;
        float pitchShake = randomRange(5.0F, 12.0F) * (float) Math.cos(System.currentTimeMillis() / 60.0) * fade;

        float yaw = MathHelper.lerp(0.15F, currentAngle.getYaw(), playerAngle.getYaw() + yawShake);
        float pitch = MathHelper.lerp(0.15F, currentAngle.getPitch(), playerAngle.getPitch() + pitchShake);

        return new Rotation(
                applyGCD(yaw, currentAngle.getYaw()),
                MathHelper.clamp(applyGCD(pitch, currentAngle.getPitch()), -90.0F, 90.0F)
        );
    }

    /**
     * Исправление движения под чувствительность мыши (Mouse GCD Fix)
     */
    public static float applyGCD(float targetRotation, float currentRotation) {
        if (mc.options == null) return targetRotation;

        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        float f = sensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;
        float gcd = f1 * 0.15F;

        if (gcd <= 0.0F) return targetRotation;

        float delta = targetRotation - currentRotation;
        float adjustedDelta = Math.round(delta / gcd) * gcd;
        return currentRotation + adjustedDelta;
    }

    private Rotation calculateDelta(Rotation current, Rotation target) {
        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(target.getPitch() - current.getPitch());
        return new Rotation(yawDelta, pitchDelta);
    }

    private float randomRange(float min, float max) {
        return MathHelper.lerp(RANDOM.nextFloat(), min, max);
    }
}