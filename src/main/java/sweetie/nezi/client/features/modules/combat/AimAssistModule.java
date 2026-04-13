package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.system.configs.FriendManager;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "Aim Bot", category = Category.COMBAT)
public class AimAssistModule extends Module {
    @Getter private static final AimAssistModule instance = new AimAssistModule();

    private final SliderSetting fov = new SliderSetting("FOV").value(180.0f).range(30.0f, 180.0f).step(1.0f);
    private final SliderSetting range = new SliderSetting("Range").value(6.0f).range(1.0f, 20.0f).step(0.1f);
    private final SliderSetting speed = new SliderSetting("Speed").value(5.0f).range(1.0f, 10.0f).step(0.1f);

    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls").value(false);
    
    private final sweetie.nezi.api.module.setting.MultiBooleanSetting targets = new sweetie.nezi.api.module.setting.MultiBooleanSetting("Targets").value(
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Голые").value(true),
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Жители").value(false)
    );

    private final ModeSetting aimPoint = new ModeSetting("Aim Point").values("Head", "Body", "Legs").value("Head");

    private final SecureRandom secureRandom = new SecureRandom();

    private volatile LivingEntity target;
    private volatile long lastTargetTime;
    private volatile float lastAppliedYaw;
    private volatile float lastAppliedPitch;
    private volatile long lastLoopTime = System.nanoTime();

    private volatile boolean running;
    private ScheduledExecutorService scheduler;

    public AimAssistModule() {
        addSettings(
                fov,
                range,
                speed,
                throughWalls,
                targets,
                aimPoint
        );
    }

    public LivingEntity getTarget() {
        return target;
    }

    @Override
    public void onEnable() {
        running = true;
        startScheduler();
    }

    @Override
    public void onDisable() {
        running = false;
        stopScheduler();
        target = null;
        lastAppliedYaw = 0.0f;
        lastAppliedPitch = 0.0f;
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) {
                target = null;
            }
        }));
        addEvents(tick);
    }

    private synchronized void startScheduler() {
        stopScheduler();
        lastLoopTime = System.nanoTime();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "AimBot-Thread");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::aimLoop, 0L, 2L, TimeUnit.MILLISECONDS);
    }

    private synchronized void stopScheduler() {
        if (scheduler == null) {
            return;
        }
        scheduler.shutdownNow();
        scheduler = null;
    }

    private void aimLoop() {
        try {
            if (!running || mc == null || mc.player == null || mc.world == null) {
                target = null;
                return;
            }

            long now = System.nanoTime();
            float dtSec = Math.min((now - lastLoopTime) / 1_000_000_000.0f, 0.05f);
            lastLoopTime = now;

            LivingEntity bestTarget = findTarget();
            if (bestTarget == null) {
                target = null;
                return;
            }

            target = bestTarget;
            lastTargetTime = System.currentTimeMillis();
            applyAim(bestTarget, dtSec);
        } catch (Throwable ignored) {
        }
    }

    private LivingEntity findTarget() {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        if (target != null && isValidTarget(target) && System.currentTimeMillis() - lastTargetTime < 200L) {
            Vec3d stickyPos = getAimPoint(target);
            double stickyDist = stickyPos.subtract(playerPos).length();
            if (stickyDist <= range.getValue() && isInsideFov(lookVec, playerPos, stickyPos)) {
                return target;
            }
        }

        LivingEntity best = null;
        float bestAngle = fov.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || !isValidTarget(living)) {
                continue;
            }

            Vec3d targetPos = getAimPoint(living);
            Vec3d delta = targetPos.subtract(playerPos);
            double dist = delta.length();

            if (dist > range.getValue()) {
                continue;
            }
            if (!throughWalls.getValue() && !mc.player.canSee(living)) {
                continue;
            }

            double dot = lookVec.dotProduct(delta.normalize());
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp((float) dot, -1.0f, 1.0f)));

            if (angle < bestAngle) {
                bestAngle = (float) angle;
                best = living;
            }
        }
        return best;
    }

    private boolean isInsideFov(Vec3d lookVec, Vec3d playerPos, Vec3d targetPos) {
        Vec3d delta = targetPos.subtract(playerPos);
        if (delta.lengthSquared() <= 1.0E-6) {
            return true;
        }
        double dot = lookVec.dotProduct(delta.normalize());
        double angle = Math.toDegrees(Math.acos(MathHelper.clamp((float) dot, -1.0f, 1.0f)));
        return angle <= fov.getValue();
    }

    private void applyAim(LivingEntity target, float dt) {
        if (mc.player == null) {
            return;
        }

        Vec3d targetPos = getAimPoint(target);
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d delta = targetPos.subtract(playerPos);

        double horizontalDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float wantYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float wantPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontalDist));
        wantPitch = MathHelper.clamp(wantPitch, -89.0f, 89.0f);

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        float yawDiff = MathHelper.wrapDegrees(wantYaw - currentYaw);
        float pitchDiff = wantPitch - currentPitch;

        if (Math.abs(yawDiff) > fov.getValue()) {
            return;
        }

        float lambda = (3.0f + (speed.getValue() - 1.0f) / 9.0f * 27.0f) * 1.2f;
        float totalDiff = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        // Progressive speed-up: the further the crosshair, the faster we rotate
        float adaptiveMultiplier;
        if (totalDiff > 60.0f) {
            adaptiveMultiplier = 3.5f;
        } else if (totalDiff > 45.0f) {
            adaptiveMultiplier = 2.5f;
        } else if (totalDiff > 30.0f) {
            adaptiveMultiplier = 1.8f;
        } else if (totalDiff > 15.0f) {
            adaptiveMultiplier = 1.2f;
        } else if (totalDiff < 5.0f) {
            adaptiveMultiplier = 0.6f;
        } else {
            adaptiveMultiplier = 1.0f;
        }

        float alpha = 1.0f - (float) Math.exp(-lambda * adaptiveMultiplier * dt);
        alpha = MathHelper.clamp(alpha, 0.0f, 1.0f);

        float newYaw = currentYaw + yawDiff * alpha;
        float newPitch = currentPitch + pitchDiff * alpha;

        // Bypass Grim
        float maxStep = 1.8f + secureRandom.nextFloat() * 0.4f;
        newYaw = currentYaw + clampAbs(MathHelper.wrapDegrees(newYaw - currentYaw), maxStep);
        newPitch = currentPitch + clampAbs(newPitch - currentPitch, maxStep * 0.65f);

        // Bypass Vulkan
        newYaw += (secureRandom.nextFloat() - 0.5f) * 0.07f;
        newPitch += (secureRandom.nextFloat() - 0.5f) * 0.045f;

        // Bypass Matrix
        if (Math.abs(yawDiff) < 3.0f) {
            float curve = (float) Math.sin(System.nanoTime() / 300_000_000.0) * 0.05f;
            newYaw += curve;
        }

        // Bypass Polar
        float yawRate = MathHelper.wrapDegrees(newYaw - lastAppliedYaw);
        if (Math.abs(yawRate) > 7.5f) {
            newYaw = lastAppliedYaw + Math.signum(yawRate) * 6.5f;
        }

        // Bypass Sloth
        if (secureRandom.nextInt(500) == 0) {
            return;
        }
        if (secureRandom.nextInt(80) == 0) {
            newYaw += (secureRandom.nextFloat() - 0.5f) * 0.2f;
            newPitch += (secureRandom.nextFloat() - 0.5f) * 0.12f;
        }

        float maxChangePerLoop = 18.0f;
        newYaw = currentYaw + clampAbs(MathHelper.wrapDegrees(newYaw - currentYaw), maxChangePerLoop);
        newPitch = currentPitch + clampAbs(newPitch - currentPitch, maxChangePerLoop * 0.7f);
        newPitch = MathHelper.clamp(newPitch, -89.0f, 89.0f);

        lastAppliedYaw = newYaw;
        lastAppliedPitch = newPitch;

        float finalYaw = newYaw;
        float finalPitch = newPitch;
        mc.execute(() -> {
            if (running && mc.player != null) {
                mc.player.setYaw(finalYaw);
                mc.player.setPitch(finalPitch);
            }
        });
    }

    private static float clampAbs(float value, float maxAbs) {
        return MathHelper.clamp(value, -maxAbs, maxAbs);
    }

    private Vec3d getAimPoint(LivingEntity entity) {
        Box box = entity.getBoundingBox();
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        double height = box.maxY - box.minY;

        return switch (aimPoint.getValue()) {
            case "Head" -> new Vec3d(centerX, box.maxY - height * 0.1, centerZ);
            case "Legs" -> new Vec3d(centerX, box.minY + height * 0.2, centerZ);
            default -> new Vec3d(centerX, box.minY + height * 0.5, centerZ);
        };
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (mc.player == null || entity == null || entity == mc.player || !entity.isAlive() || entity.isInvulnerable()) {
            return false;
        }
        if (entity instanceof ArmorStandEntity) {
            return false;
        }
        if (mc.player.distanceTo(entity) > range.getValue()) {
            return false;
        }

        if (!new sweetie.nezi.api.utils.combat.TargetManager.EntityFilter(targets.getList()).isValid(entity)) {
            return false;
        }

        if (!throughWalls.getValue() && !mc.player.canSee(entity)) {
            return false;
        }

        return true;
    }
}
