package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
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
import sweetie.nezi.api.module.setting.MultiBooleanSetting;

import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "Aim Bot", category = Category.COMBAT)
public class AimAssistModule extends Module {
    @Getter private static final AimAssistModule instance = new AimAssistModule();

    private final SliderSetting fov   = new SliderSetting("FOV").value(60.0f).range(10.0f, 180.0f).step(1.0f);
    private final SliderSetting range = new SliderSetting("Range").value(4.5f).range(1.0f, 10.0f).step(0.1f);
    private final SliderSetting speed = new SliderSetting("Speed").value(5.0f).range(1.0f, 10.0f).step(0.1f);

    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls").value(false);

    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Голые").value(true),
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Жители").value(false)
    );

    private final ModeSetting aimPoint = new ModeSetting("Aim Point")
            .values("Head", "Body", "Legs", "Closest")
            .value("Closest");

    private final SecureRandom secureRandom = new SecureRandom();

    @Getter private volatile LivingEntity target;
    private volatile long    lastTargetTime;
    private volatile long    lastLoopTime = System.nanoTime();
    private volatile boolean running;
    private ScheduledExecutorService scheduler;

    private volatile float smoothedYawVelocity = 0.0f;

    public AimAssistModule() {
        addSettings(fov, range, speed, throughWalls, targets, aimPoint);
    }

    @Override
    public void onEnable() {
        running = true;
        smoothedYawVelocity = 0.0f;
        startScheduler();
    }

    @Override
    public void onDisable() {
        running = false;
        stopScheduler();
        target = null;
        smoothedYawVelocity = 0.0f;
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) {
                target = null;
                return;
            }
            if (target != null && WTapModule.getInstance().isSuppressing()) return;
        }));
        addEvents(tick);
    }

    private synchronized void startScheduler() {
        stopScheduler();
        lastLoopTime = System.nanoTime();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AimBot-Thread");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::aimLoop, 0L, 4L, TimeUnit.MILLISECONDS);
    }

    private synchronized void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void aimLoop() {
        try {
            if (!running || mc.player == null || mc.world == null) {
                target = null;
                smoothedYawVelocity = 0.0f;
                return;
            }

            long  now   = System.nanoTime();
            float dtSec = Math.min((now - lastLoopTime) / 1_000_000_000.0f, 0.05f);
            lastLoopTime = now;

            LivingEntity best = findTarget();
            if (best == null) {
                target = null;
                smoothedYawVelocity *= 0.7f;
                return;
            }

            target = best;
            lastTargetTime = System.currentTimeMillis();
            applyAim(best, dtSec);
        } catch (Exception ignored) {}
    }

    private LivingEntity findTarget() {
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d lookVec   = mc.player.getRotationVec(1.0f);

        LivingEntity best      = null;
        float        bestAngle = fov.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.isAlive()) continue;
            if (!isValidTarget(living)) continue;

            Vec3d  targetPos = getAimPoint(living);
            Vec3d  delta     = targetPos.subtract(playerPos);
            double dist      = delta.length();
            if (dist > range.getValue()) continue;

            double dot   = lookVec.dotProduct(delta.normalize());
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp((float) dot, -1.0f, 1.0f)));

            if (angle < bestAngle) {
                bestAngle = (float) angle;
                best = living;
            }
        }
        return best;
    }

    private void applyAim(LivingEntity targetEntity, float dt) {
        Vec3d targetPos = getAimPoint(targetEntity);
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d delta     = targetPos.subtract(playerPos);

        float wantYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float yawDiff = MathHelper.wrapDegrees(wantYaw - mc.player.getYaw());

        float speedVal = speed.getValue();

        float maxDegreesPerSec = 20.0f + speedVal * speedVal * 1.8f;
        float springK = 8.0f + speedVal * 3.5f;

        float targetVelocity = MathHelper.clamp(yawDiff * springK, -maxDegreesPerSec, maxDegreesPerSec);

        float damping = 0.05f + (speedVal / 10.0f) * 0.4f;
        smoothedYawVelocity += (targetVelocity - smoothedYawVelocity) * Math.min(damping * 60.0f * dt, 1.0f);

        float yawStep = smoothedYawVelocity * dt;

        if (Math.abs(yawStep) > Math.abs(yawDiff)) {
            yawStep = yawDiff;
            smoothedYawVelocity = 0.0f;
        }

        float noiseMag = Math.max(0.02f, 0.15f - speedVal * 0.01f);
        float noiseYaw = (secureRandom.nextFloat() - 0.5f) * noiseMag;

        float finalYaw   = mc.player.getYaw() + yawStep + noiseYaw;
        float finalPitch = mc.player.getPitch();

        mc.execute(() -> {
            if (running && mc.player != null && mc.currentScreen == null) {
                mc.player.setYaw(finalYaw);
                mc.player.setPitch(finalPitch);
            }
        });
    }

    private Vec3d getAimPoint(LivingEntity entity) {
        Box box = entity.getBoundingBox();

        if (aimPoint.is("Closest")) {
            Vec3d  eye    = mc.player.getEyePos();
            Vec3d  look   = mc.player.getRotationVec(1.0f);
            double dist   = mc.player.distanceTo(entity);
            Box    active = box.expand(0.1);
            return new Vec3d(
                    MathHelper.clamp(eye.x + look.x * dist, active.minX, active.maxX),
                    MathHelper.clamp(eye.y + look.y * dist, active.minY, active.maxY),
                    MathHelper.clamp(eye.z + look.z * dist, active.minZ, active.maxZ)
            );
        }

        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        double h  = box.maxY - box.minY;

        return switch (aimPoint.getValue()) {
            case "Head" -> new Vec3d(cx, box.maxY - h * 0.15, cz);
            case "Legs" -> new Vec3d(cx, box.minY + h * 0.2,  cz);
            default     -> new Vec3d(cx, box.minY + h * 0.5,  cz);
        };
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || entity == mc.player || entity instanceof ArmorStandEntity) return false;
        if (!new sweetie.nezi.api.utils.combat.TargetManager.EntityFilter(targets.getList()).isValid(entity)) return false;
        if (!throughWalls.getValue() && !mc.player.canSee(entity)) return false;
        return true;
    }
}