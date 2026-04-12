package sweetie.nezi.api.utils.predict;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * 1:1 порт из Javelin PredictUtils.
 * Предсказание позиции сущности (особенно на элитрах).
 */
@UtilityClass
public class PredictUtils {

    public Vec3d predict(LivingEntity entity, Vec3d pos, float ticks) {
        if (Math.hypot(entity.getX() - entity.prevX, entity.getZ() - entity.prevZ) * 20.0 <= 5.0
                && (entity.getY() - entity.prevY) * 20.0 <= 5.0) {
            return pos;
        }

        float f2 = (entity.getPitch() + (entity.prevPitch - entity.getPitch())) * 0.017453292f;
        float g = -(entity.getYaw() + (entity.prevHeadYaw - entity.getYaw())) * 0.017453292f;
        float h2 = MathHelper.cos(g);
        float i2 = MathHelper.sin(g);
        float j = MathHelper.cos(f2);
        float k = MathHelper.sin(f2);

        Vec3d oldVelocity = entity.getVelocity();
        Vec3d lookVec = new Vec3d(i2 * j, -k, h2 * j);

        float f = (float) ((double) entity.getPitch() * 0.017453293005625408);
        double d = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
        double e = oldVelocity.horizontalLength();

        boolean slowFalling = entity.getVelocity().y <= 0.0;
        double gravity = slowFalling && entity.hasStatusEffect(StatusEffects.SLOW_FALLING)
                ? Math.min(entity.getFinalGravity(), 0.01) : entity.getFinalGravity();
        double h = MathHelper.square(Math.cos((double) f));

        oldVelocity = oldVelocity.add(0.0, gravity * (-1.0 + h * 0.75), 0.0);

        double i;
        if (oldVelocity.y < 0.0 && d > 0.0) {
            i = oldVelocity.y * -0.1 * h;
            oldVelocity = oldVelocity.add(lookVec.x * i / d, i, lookVec.z * i / d);
        }

        if (f < 0.0f && d > 0.0) {
            i = e * (double) (-MathHelper.sin(f)) * 0.04;
            oldVelocity = oldVelocity.add(-lookVec.x * i / d, i * 3.2, -lookVec.z * i / d);
        }

        if (d > 0.0) {
            oldVelocity = oldVelocity.add((lookVec.x / d * e - oldVelocity.x) * 0.1, 0.0, (lookVec.z / d * e - oldVelocity.z) * 0.1);
        }

        Vec3d totalMotion = oldVelocity.multiply(0.9900000095367432, 0.9800000190734863, 0.9900000095367432);
        return pos.add(totalMotion.multiply((double) ticks)).add(0.0, 0.3499999940395355, 0.0);
    }
}
