package sweetie.nezi.client.features.modules.movement.speed.modes;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.player.MoveUtil;
import sweetie.nezi.api.utils.speed.SpeedBlockUtil;
import sweetie.nezi.client.features.modules.movement.speed.SpeedMode;
import sweetie.nezi.inject.entity.LivingEntityMovementAccessor;

import java.util.function.Supplier;

public class SpeedShulker extends SpeedMode {

    private final SliderSetting radiusXZ = new SliderSetting("Radius XZ").value(2f).range(1f, 6f).step(1f);
    private final SliderSetting radiusY = new SliderSetting("Radius Y").value(2f).range(1f, 6f).step(1f);
    private final SliderSetting factor = new SliderSetting("Factor").value(0.2f).range(0.01f, 1.0f).step(0.01f);

    private Float baseMovementSpeed = null; // запоминаем 1 раз

    public SpeedShulker(Supplier<Boolean> condition) {
        radiusXZ.setVisible(condition);
        radiusY.setVisible(condition);
        factor.setVisible(condition);
        addSettings(radiusXZ, radiusY, factor);
    }

    @Override
    public String getName() {
        return "Shulker";
    }

    @Override
    public void onTravel() {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.input == null) return;

        // Условие как в оригинале
        if (mc.player.input.movementForward <= 0) return;
        if (!mc.player.isSprinting()) return;
        if (!MoveUtil.isMoving()) return;

        // ВАЖНО: эффект именно в воздухе
        if (mc.player.isOnGround()) return;

        BlockPos center = mc.player.getBlockPos();

        int rxz = ((Number) radiusXZ.getValue()).intValue();
        int ry = ((Number) radiusY.getValue()).intValue();

        boolean nearShulker = false;
        for (BlockPos pos : SpeedBlockUtil.getCube(center, rxz, ry)) {
            if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                nearShulker = true;
                break;
            }
        }
        if (!nearShulker) return;

        double factorVal = ((Number) factor.getValue()).doubleValue();

        double accel = factorVal * 0.02;

        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;

        // нормализация ввода
        double len = Math.hypot(forward, strafe);
        if (len < 1e-6) return;
        double f = forward / len;
        double s = strafe / len;

        // поворот по yaw в мир
        double yaw = Math.toRadians(mc.player.getYaw());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);

        double addX = (f * cos - s * sin) * accel;
        double addZ = (f * sin + s * cos) * accel;

        // применяем
        mc.player.addVelocity(addX, 0.0, addZ);

         Vec3d v = mc.player.getVelocity();
         double h = Math.hypot(v.x, v.z);
         double cap = 1.2; // под себя
         if (h > cap) {
             double k = cap / h;
             mc.player.setVelocity(v.x * k, v.y, v.z * k);
        }
    }
}