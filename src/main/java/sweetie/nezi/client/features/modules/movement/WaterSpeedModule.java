package sweetie.nezi.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;

@ModuleRegister(name = "Water Speed", category = Category.MOVEMENT)
public class WaterSpeedModule extends Module {
    @Getter private static final WaterSpeedModule instance = new WaterSpeedModule();

    private final ModeSetting mode = new ModeSetting("Mode").value("FunTimeIce").values("FunTime", "FunTimeIce");
    private final SliderSetting speed = new SliderSetting("Speed").value(1.5f).range(0.5f, 3.0f).step(0.1f);
    private final SliderSetting packets = new SliderSetting("Packets").value(2.0f).range(1.0f, 10.0f).step(1.0f)
            .setVisible(() -> mode.is("FunTimeIce"));

    public WaterSpeedModule() {
        addSettings(mode, speed, packets);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;
            if (!mc.player.isSwimming() && !mc.player.isTouchingWater()) return;

            if (mode.is("FunTime")) {
                handleFunTime();
            } else if (mode.is("FunTimeIce")) {
                handleFunTimeIce();
            }
        }));

        addEvents(updateEvent);
    }

    private void handleFunTime() {
        Vec3d velocity = mc.player.getVelocity();
        double speedX = 0.12F;
        double speedY = 0.56F;

        if (mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F) {
            float yaw = mc.player.getYaw();
            double moveForward = mc.player.input.movementForward * 0.98;
            double moveSideways = mc.player.input.movementSideways * 0.98;

            double directionX = -Math.sin(Math.toRadians(yaw)) * moveForward + Math.cos(Math.toRadians(yaw)) * moveSideways;
            double directionZ = Math.cos(Math.toRadians(yaw)) * moveForward + Math.sin(Math.toRadians(yaw)) * moveSideways;

            double length = Math.sqrt(directionX * directionX + directionZ * directionZ);
            if (length > 1.0) {
                directionX /= length;
                directionZ /= length;
            }

            velocity = velocity.add(directionX * speedX * 0.1, 0.0, directionZ * speedX * 0.1);

            double maxBPS = 5.9;
            double maxSpeedBPT = maxBPS / 20.0;
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontalSpeed > maxSpeedBPT) {
                double scale = maxSpeedBPT / horizontalSpeed;
                velocity = new Vec3d(velocity.x * scale, velocity.y, velocity.z * scale);
            }
        }

        if (mc.options.jumpKey.isPressed()) {
            float pitch = RotationManager.getInstance().getRotation().getPitch();
            float boostMultiplier = pitch >= 0.0F ? MathHelper.clamp(pitch / 45.0F, 1.0F, 2.5F) : 0.9F;
            velocity = velocity.add(0.0, speedY * boostMultiplier * 0.08, 0.0);
        } else if (mc.options.sneakKey.isPressed()) {
            velocity = velocity.add(0.0, -speedY * 0.12, 0.0);
        }

        mc.player.setVelocity(velocity);
    }

    private void handleFunTimeIce() {
        double multiplierCap = 1.0 + packets.getValue() * 0.03;
        double multiplier = Math.min((double) speed.getValue(), multiplierCap);
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x * multiplier, vel.y, vel.z * multiplier);
    }
}