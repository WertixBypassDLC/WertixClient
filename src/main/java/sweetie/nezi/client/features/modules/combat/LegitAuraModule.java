package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.combat.CombatExecutor;
import sweetie.nezi.api.utils.combat.TargetManager;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.misc.PointFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "LegitAura", category = Category.COMBAT)
public class LegitAuraModule extends Module {
    @Getter private static final LegitAuraModule instance = new LegitAuraModule();

    private final TargetManager targetManager = new TargetManager();
    private final PointFinder pointFinder = new PointFinder();
    private final CombatExecutor combatExecutor = new CombatExecutor();

    private final SliderSetting distance = new SliderSetting("Дистанция").value(3.2f).range(2.4f, 5.2f).step(0.05f);
    private final SliderSetting aimThreshold = new SliderSetting("Предел наводки").value(2.15f).range(0.5f, 8.0f).step(0.05f);
    private final SliderSetting aimFov = new SliderSetting("FOV").value(85f).range(20f, 180f).step(1f);
    private final SliderSetting rotationSpeed = new SliderSetting("Скорость ротации").value(7.8f).range(2.0f, 10.0f).step(0.1f);
    private final BooleanSetting onlyCrits = new BooleanSetting("Только криты").value(true);
    private final BooleanSetting smartCrits = new BooleanSetting("Умные криты").value(true).setVisible(onlyCrits::getValue);
    private final BooleanSetting noAttackIfEat = new BooleanSetting("Не бить при еде").value(false);
    private final BooleanSetting allowSoftWalls = new BooleanSetting("Через мягкие стены").value(true);
    private final BooleanSetting targetPlayers = new BooleanSetting("Игроки").value(true);
    private final BooleanSetting targetNaked = new BooleanSetting("Голые").value(true).setVisible(targetPlayers::getValue);
    private final MultiBooleanSetting targets = new MultiBooleanSetting("Цели").value(
            targetPlayers,
            targetNaked,
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Жители").value(false)
    );

    public LivingEntity target;
    private boolean pendingWtapAttack;
    private int cachedAimTick = -1;
    private LivingEntity cachedAimTarget;
    private Vec3d cachedAimPoint = Vec3d.ZERO;
    private Box cachedAimBox;
    private boolean cachedAimValid;
    private volatile boolean running;
    private volatile long lastLoopTime = System.nanoTime();
    private volatile float smoothedYawVelocity;
    private volatile float smoothedPitchVelocity;
    private ScheduledExecutorService scheduler;

    public LegitAuraModule() {
        addSettings(distance, aimThreshold, aimFov, rotationSpeed, onlyCrits, smartCrits, noAttackIfEat, allowSoftWalls, targets);
    }

    @Override
    public void onEnable() {
        resetState();
        running = true;
        startScheduler();
    }

    @Override
    public void onDisable() {
        running = false;
        stopScheduler();
        resetState();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> updateEventHandler()));
        addEvents(updateEvent);
    }

    public float getAttackDistance() {
        return distance.getValue();
    }

    public float getPreDistance() {
        return aimThreshold.getValue();
    }

    public float getAimThreshold() {
        return aimThreshold.getValue();
    }

    private void updateEventHandler() {
        if (mc.player == null || mc.world == null) {
            resetState();
            return;
        }

        target = updateTarget();
        AuraModule.getInstance().target = target;
        if (target == null) {
            resetAimCache();
            resetAimAssistDynamics();
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        AimData aimData = getAimData(target);
        if (aimData.point() == null || aimData.box() == null) {
            targetManager.releaseTarget();
            AuraModule.getInstance().target = null;
            target = null;
            resetAimCache();
            resetAimAssistDynamics();
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        double aimedDistance = mc.player.getEyePos().distanceTo(RotationUtil.rayCastBox(target, aimData.point()));
        if (aimedDistance > getAttackDistance()) {
            targetManager.releaseTarget();
            AuraModule.getInstance().target = null;
            target = null;
            resetAimCache();
            resetAimAssistDynamics();
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        configureCombat(target, aimData.box());
        attackTarget(aimData.point());
    }

    private LivingEntity updateTarget() {
        TargetManager.EntityFilter filter = new TargetManager.EntityFilter(targets.getList());
        float maxDistance = getAttackDistance();
        boolean wideSearch = allowSoftWalls.getValue();

        targetManager.searchTargets(mc.world.getEntities(), maxDistance, aimFov.getValue(), wideSearch);
        targetManager.validateTarget(entity -> filter.isValid(entity) && hasAllowedAttackPath(entity, maxDistance));
        return targetManager.getCurrentTarget();
    }

    private synchronized void startScheduler() {
        stopScheduler();
        lastLoopTime = System.nanoTime();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "LegitAura-Thread");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::aimLoop, 0L, 8L, TimeUnit.MILLISECONDS);
    }

    private synchronized void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void aimLoop() {
        try {
            if (!running || mc.player == null || mc.world == null || mc.currentScreen != null) {
                resetAimAssistDynamics();
                return;
            }

            LivingEntity currentTarget = target;
            if (currentTarget == null || !currentTarget.isAlive()) {
                resetAimAssistDynamics();
                return;
            }

            AimData aimData = getAimData(currentTarget);
            if (aimData.point() == null) {
                smoothedYawVelocity *= 0.58f;
                smoothedPitchVelocity *= 0.58f;
                return;
            }

            long now = System.nanoTime();
            float dt = Math.min((now - lastLoopTime) / 1_000_000_000.0f, 0.05f);
            lastLoopTime = now;
            applyAim(currentTarget, aimData.point(), dt);
        } catch (Exception ignored) {
        }
    }

    private void applyAim(LivingEntity currentTarget, Vec3d point, float dt) {
        if (mc.player == null || point == null) {
            return;
        }

        Rotation wantedRotation = RotationUtil.fromVec3d(point.subtract(mc.player.getEyePos()));
        float yawDiff = MathHelper.wrapDegrees(wantedRotation.getYaw() - mc.player.getYaw());
        float pitchDiff = MathHelper.clamp(wantedRotation.getPitch() - mc.player.getPitch(), -90.0f, 90.0f);

        float speedValue = rotationSpeed.getValue();
        float distanceFactor = MathHelper.clamp(mc.player.distanceTo(currentTarget) / Math.max(0.1f, getAttackDistance()), 0.0f, 1.0f);
        float previewBoost = combatExecutor.combatManager().canAttackPreview(4) ? 1.0f : 0.68f;
        float readyBoost = combatExecutor.combatManager().canAttack() ? 1.12f : 1.0f;
        float assistWindowYaw = Math.max(0.7f, getAimThreshold() * 0.82f);
        float assistWindowPitch = Math.max(0.45f, getAimThreshold() * 0.62f);

        float guidedYaw = reduceAssistNearCenter(yawDiff, assistWindowYaw);
        float guidedPitch = reduceAssistNearCenter(pitchDiff, assistWindowPitch);

        float springYaw = 5.2f + speedValue * 1.9f + previewBoost * 1.2f;
        float springPitch = 4.3f + speedValue * 1.55f + previewBoost;
        float maxYawPerSecond = (72.0f + speedValue * speedValue * 5.7f + distanceFactor * 40.0f) * readyBoost;
        float maxPitchPerSecond = (52.0f + speedValue * speedValue * 4.25f + distanceFactor * 26.0f) * readyBoost;

        float targetYawVelocity = MathHelper.clamp(guidedYaw * springYaw, -maxYawPerSecond, maxYawPerSecond);
        float targetPitchVelocity = MathHelper.clamp(guidedPitch * springPitch, -maxPitchPerSecond, maxPitchPerSecond);

        float smoothing = Math.min((0.22f + speedValue * 0.035f) * 60.0f * dt, 1.0f);
        smoothedYawVelocity += (targetYawVelocity - smoothedYawVelocity) * smoothing;
        smoothedPitchVelocity += (targetPitchVelocity - smoothedPitchVelocity) * smoothing;

        float yawStep = smoothedYawVelocity * dt;
        float pitchStep = smoothedPitchVelocity * dt;

        if (Math.abs(yawDiff) < assistWindowYaw) {
            yawStep *= 0.48f;
        }
        if (Math.abs(pitchDiff) < assistWindowPitch) {
            pitchStep *= 0.44f;
        }

        yawStep = MathHelper.clamp(yawStep, -Math.abs(yawDiff), Math.abs(yawDiff));
        pitchStep = MathHelper.clamp(pitchStep, -Math.abs(pitchDiff), Math.abs(pitchDiff));

        float nextYaw = mc.player.getYaw() + Math.copySign(yawStep, yawDiff);
        float nextPitch = mc.player.getPitch() + Math.copySign(pitchStep, pitchDiff);

        mc.execute(() -> {
            if (running && mc.player != null && mc.currentScreen == null && target == currentTarget) {
                mc.player.setYaw(nextYaw);
                mc.player.setPitch(MathHelper.clamp(nextPitch, -90.0f, 90.0f));
            }
        });
    }

    private float reduceAssistNearCenter(float delta, float retainWindow) {
        float abs = Math.abs(delta);
        if (abs <= retainWindow) {
            return delta * 0.22f;
        }

        float excess = abs - retainWindow;
        return Math.copySign(retainWindow * 0.22f + excess, delta);
    }

    private void attackTarget(Vec3d point) {
        if (pendingWtapAttack || target == null) {
            return;
        }

        if (!combatExecutor.combatManager().canAttackPreview(4)) {
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        if (!isWithinAimThreshold(point)) {
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        if (WTapModule.getInstance().isEnabled()) {
            pendingWtapAttack = true;
            boolean accepted = WTapModule.getInstance().requestCritAttack(() -> {
                pendingWtapAttack = false;
                combatExecutor.performAttack();
            });
            if (!accepted) {
                pendingWtapAttack = false;
                combatExecutor.performAttack();
            }
            return;
        }

        combatExecutor.performAttack();
    }

    private boolean isWithinAimThreshold(Vec3d point) {
        if (mc.player == null || point == null) {
            return false;
        }

        Rotation wantedRotation = RotationUtil.fromVec3d(point.subtract(mc.player.getEyePos()));
        float yawDiff = Math.abs(MathHelper.wrapDegrees(wantedRotation.getYaw() - mc.player.getYaw()));
        float pitchDiff = Math.abs(MathHelper.wrapDegrees(wantedRotation.getPitch() - mc.player.getPitch()));
        float threshold = getAimThreshold();
        return yawDiff <= threshold * 0.52f && pitchDiff <= Math.max(0.55f, threshold * 0.42f);
    }

    private void configureCombat(LivingEntity target, Box hitBox) {
        combatExecutor.combatManager().configurable(
                new CombatExecutor.CombatConfigurable(
                        target,
                        new Rotation(mc.player.getYaw(), mc.player.getPitch()),
                        getAttackDistance(),
                        hitBox,
                        buildCombatOptions(),
                        1.0F
                )
        );
    }

    private List<String> buildCombatOptions() {
        List<String> options = new ArrayList<>();
        if (onlyCrits.getValue()) options.add("Only crits");
        if (smartCrits.getValue()) options.add("Smart crits");
        options.add("Dynamic cooldown");
        options.add("Shield break");
        options.add("UnPress shield");
        if (allowSoftWalls.getValue()) options.add("Ignore walls");
        if (noAttackIfEat.getValue()) options.add("No attack if eat");
        return options;
    }

    private AimData getAimData(LivingEntity target) {
        if (target == null) {
            return new AimData(null, null);
        }

        if (mc.player == null) {
            return new AimData(target.getBoundingBox().getCenter(), target.getBoundingBox());
        }

        int currentTick = mc.player.age;
        if (target == cachedAimTarget && cachedAimTick == currentTick) {
            return new AimData(cachedAimValid ? cachedAimPoint : null, cachedAimBox);
        }

        Rotation baseRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());

        var pair = pointFinder.computeVector(
                target,
                getAttackDistance(),
                baseRotation,
                target.getVelocity().multiply(0.9D),
                allowSoftWalls.getValue()
        );

        Vec3d point = pair.getLeft();
        Box box = pair.getRight();
        if (point != null && allowSoftWalls.getValue() && hasHardBlockingCollision(mc.player.getEyePos(), point, mc.player.getEyePos().squaredDistanceTo(point))) {
            point = null;
        }

        cachedAimTarget = target;
        cachedAimTick = currentTick;
        cachedAimValid = point != null;
        cachedAimPoint = point == null ? Vec3d.ZERO : point;
        cachedAimBox = box;
        return new AimData(point, box);
    }

    private boolean hasAllowedAttackPath(LivingEntity entity, float maxDistance) {
        if (mc.player == null || entity == null) {
            return false;
        }

        Rotation baseRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());

        var pair = pointFinder.computeVector(entity, maxDistance, baseRotation, entity.getVelocity().multiply(0.9D), allowSoftWalls.getValue());
        Vec3d point = pair.getLeft();
        if (point == null) {
            return false;
        }

        return !hasHardBlockingCollision(mc.player.getEyePos(), point, mc.player.getEyePos().squaredDistanceTo(point));
    }

    private boolean hasHardBlockingCollision(Vec3d start, Vec3d end, double targetDistanceSq) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() <= 1.0E-6D || mc.world == null) {
            return false;
        }

        Vec3d currentStart = start;
        Vec3d stepDirection = direction.normalize().multiply(0.05D);
        for (int i = 0; i < 24; i++) {
            var blockHit = mc.world.raycast(new RaycastContext(
                    currentStart,
                    end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (blockHit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) {
                return false;
            }

            double blockDistanceSq = start.squaredDistanceTo(blockHit.getPos());
            if (blockDistanceSq >= targetDistanceSq) {
                return false;
            }

            BlockState state = mc.world.getBlockState(blockHit.getBlockPos());
            if (canBypassWallBlock(state)) {
                currentStart = blockHit.getPos().add(stepDirection);
                continue;
            }
            return true;
        }

        return false;
    }

    private boolean canBypassWallBlock(BlockState state) {
        if (!allowSoftWalls.getValue()) {
            return false;
        }

        if (state == null || state.isAir()) {
            return true;
        }

        Block block = state.getBlock();
        String translationKey = block.getTranslationKey();
        return block instanceof StairsBlock
                || block instanceof FenceBlock
                || block instanceof FenceGateBlock
                || block instanceof WallBlock
                || block instanceof TrapdoorBlock
                || block instanceof DoorBlock
                || block instanceof LeavesBlock
                || block instanceof SlabBlock
                || block instanceof PaneBlock
                || translationKey.contains("glass")
                || block instanceof TransparentBlock
                || block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof CobwebBlock;
    }

    private void resetState() {
        targetManager.releaseTarget();
        AuraModule.getInstance().target = null;
        target = null;
        pendingWtapAttack = false;
        resetAimCache();
        resetAimAssistDynamics();
        combatExecutor.combatManager().resetState();
    }

    private void resetAimCache() {
        cachedAimTick = -1;
        cachedAimTarget = null;
        cachedAimValid = false;
        cachedAimPoint = Vec3d.ZERO;
        cachedAimBox = null;
    }

    private void resetAimAssistDynamics() {
        smoothedYawVelocity = 0.0f;
        smoothedPitchVelocity = 0.0f;
        lastLoopTime = System.nanoTime();
    }

    private record AimData(Vec3d point, Box box) {
    }
}
