package sweetie.nezi.client.features.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.other.RotationUpdateEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.event.events.player.world.AttackEvent;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.combat.AutoMaceUtil;
import sweetie.nezi.api.utils.combat.CombatExecutor;
import sweetie.nezi.api.utils.combat.TargetManager;
import sweetie.nezi.api.utils.notification.NotificationUtil;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;
import sweetie.nezi.api.utils.rotation.manager.RotationMode;
import sweetie.nezi.api.utils.rotation.manager.RotationStrategy;
import sweetie.nezi.api.utils.rotation.misc.AuraUtil;
import sweetie.nezi.api.utils.rotation.misc.PointFinder;
import sweetie.nezi.api.utils.rotation.rotations.FunTimeRotation;
import sweetie.nezi.api.utils.rotation.rotations.MatrixRotation;
import sweetie.nezi.api.utils.rotation.rotations.SnapRotation;
import sweetie.nezi.api.utils.task.TaskPriority;
import sweetie.nezi.client.features.modules.combat.elytratarget.ElytraTargetModule;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "Aura", category = Category.COMBAT)
public class AuraModule extends Module {
    @Getter private static final AuraModule instance = new AuraModule();

    private final TargetManager targetManager = new TargetManager();
    private final PointFinder pointFinder = new PointFinder();
    public final CombatExecutor combatExecutor = new CombatExecutor();

    @Getter private final ModeSetting aimMode = new ModeSetting("Режим прицела").value("Snap").values(
            "Fun Time", "Snap", "Legit Snap", "Matrix"
    );
    @Getter private final ModeSetting move = new ModeSetting("Движение").value("Free").values("Free", "Focus");

    private final SliderSetting distance    = new SliderSetting("Дистанция").value(3.3f).range(2.5f, 5f).step(0.1f);
    private final SliderSetting preDistance = new SliderSetting("Предел дистанции").value(0.3f).range(0f, 3f).step(0.1f);
    private final SliderSetting legitFov    = new SliderSetting("Легитный FOV").value(50f).range(30f, 90f).step(1f)
            .setVisible(() -> aimMode.is("Legit Snap"));
    private final BooleanSetting autoMace = new BooleanSetting("Auto Mace").value(false);
    private final BooleanSetting auraTargetPlayers = new BooleanSetting("Игроки").value(true);
    private final BooleanSetting auraTargetNaked   = new BooleanSetting("Голые").value(false).setVisible(auraTargetPlayers::getValue);
    private final MultiBooleanSetting targets = new MultiBooleanSetting("Цели").value(
            auraTargetPlayers,
            new BooleanSetting("Друзья").value(false),
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Жители").value(false),
            auraTargetNaked
    );

    public LivingEntity target;
    private int cachedAimTick = -1;
    private LivingEntity cachedAimTarget;
    private Vec3d cachedAimPoint = Vec3d.ZERO;
    private Box cachedAimBox;

    public AuraModule() {
        addSettings(
                aimMode, move, distance, preDistance, legitFov, autoMace, targets, combatExecutor.options(), combatExecutor.sprintResetTicks()
        );
    }

    public static boolean moveFixEnabled() {
        return instance.isEnabled();
    }

    public float getMovementYaw() {
        if (mc.player == null) {
            return 0.0F;
        }

        Rotation rotation = RotationManager.getInstance().getCurrentRotation();
        return rotation != null ? rotation.getYaw() : mc.player.getYaw();
    }

    public float getPreDistance() {
        return preDistance.getValue();
    }

    public float getAttackDistance() {
        return distance.getValue();
    }

    public boolean isAutoMaceEnabled() {
        return autoMace.getValue();
    }

    @Override
    public void onDisable() {
        targetManager.releaseTarget();
        target = null;
        resetAimCache();
        combatExecutor.combatManager().resetState();
        FunTimeRotation.onAuraDisabled();
    }

    @Override
    public void onEnable() {
        targetManager.releaseTarget();
        target = null;
        resetAimCache();
        combatExecutor.combatManager().resetState();
        FunTimeRotation.reset();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> updateEventHandler()));
        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> rotationUpdateHandler()));
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> AuraUtil.onAttack(aimMode.getValue())));
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> handleRender2D(event.context())));
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(this::handlePacket));
        addEvents(updateEvent, rotationUpdateEvent, attackEvent, render2DEvent, packetEvent);
    }

    private void rotationUpdateHandler() {
        if (mc.player == null || target == null) {
            return;
        }

        AimData aimData = getAimData(target);
        Vec3d attackVector = aimData.point();
        Rotation rotation = RotationUtil.fromVec3d(attackVector.subtract(mc.player.getEyePos()));
        rotateToTarget(target, attackVector, rotation);
    }

    private void updateEventHandler() {
        if (mc.player == null || mc.world == null) {
            target = null;
            resetAimCache();
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        target = updateTarget();
        if (target == null) {
            resetAimCache();
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        if (autoMace.getValue()) {
            AutoMaceUtil.tryEquipForSmash();
        }

        AimData aimData = getAimData(target);
        if (aimData.point().distanceTo(mc.player.getEyePos()) > getAttackDistance() + getPreDistance()) {
            targetManager.releaseTarget();
            target = null;
            resetAimCache();
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        attackTarget(target);
    }

    private LivingEntity updateTarget() {
        TargetManager.EntityFilter filter = new TargetManager.EntityFilter(targets.getList());
        filter.needFriends = targets.isEnabled("Друзья");

        float maxDistance = getAttackDistance() + getPreDistance();
        boolean ignoreWalls = combatExecutor.options().isEnabled("Ignore walls");
        targetManager.searchTargets(mc.world.getEntities(), maxDistance, 360.0F, ignoreWalls);

        if (aimMode.is("Legit Snap")) {
            targetManager.validateTarget(entity -> filter.isValid(entity) && isTargetInsideLegitCircle(entity));
        } else {
            targetManager.validateTarget(filter::isValid);
        }

        return targetManager.getCurrentTarget();
    }

    private void attackTarget(LivingEntity target) {
        AimData aimData = getAimData(target);
        combatExecutor.combatManager().configurable(
                new CombatExecutor.CombatConfigurable(
                        target,
                        RotationManager.getInstance().getRotation(),
                        getAttackDistance(),
                        aimData.box(),
                        combatExecutor.options().getList(),
                        combatExecutor.sprintResetTicks().getValue()
                )
        );

        if (mc.player.getEyePos().distanceTo(RotationUtil.rayCastBox(target, aimData.point())) > getAttackDistance()) {
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        if (aimMode.is("Legit Snap") && !isTargetInsideLegitCircle(target)) {
            combatExecutor.combatManager().releaseSprintReset();
            return;
        }

        if (autoMace.getValue()) {
            AutoMaceUtil.tryEquipForSmash();
        }

        combatExecutor.performAttack();
    }

    private void rotateToTarget(LivingEntity target, Vec3d targetVec, Rotation rotation) {
        if (usingElytraTarget() && ElytraTargetModule.getInstance().elytraRotationProcessor.customRotations.getValue()) {
            return;
        }

        RotationStrategy strategy = new RotationStrategy(getRotationMode(), true, move.is("Free"));

        boolean canAttack = combatExecutor.combatManager().canAttackPreview();
        long sinceLastClick = combatExecutor.combatManager().clickScheduler().lastClickPassed();

        // --- Distance-based urgency ---
        // The farther the target, the longer we've been clicking without hitting → rotate faster
        double distToTarget = mc.player.getEyePos().distanceTo(targetVec);
        double maxDist = getAttackDistance() + getPreDistance();
        // urgency 0..1 (0 = close, 1 = at edge of range)
        double urgency = Math.min(1.0, distToTarget / maxDist);

        // --- Crit acceleration ---
        boolean critPossible = canDoCrit();

        if (aimMode.is("Snap")) {
            // Reduce the "give up" window when far or when crit is ready
            long threshold = critPossible ? 60L : (urgency > 0.7 ? 70L : 100L);
            if (!canAttack && sinceLastClick >= threshold) {
                return;
            }
        } else if (aimMode.is("Legit Snap")) {
            long threshold = critPossible ? 60L : (urgency > 0.7 ? 70L : 100L);
            if (!canAttack && sinceLastClick >= threshold) {
                return;
            }
            RotationManager.getInstance().clear();
        } else if (aimMode.is("Fun Time")) {
            if (!canAttack) {
                return;
            }
            strategy.ticksUntilReset(60);
            RotationManager.getInstance().clear();
        }

        Vec3d rotationVec = aimMode.is("Fun Time") ? rotation.getVector() : targetVec;
        RotationManager.getInstance().addRotation(new Rotation.VecRotation(rotation, rotationVec), target, strategy, TaskPriority.HIGH, this);
    }

    /**
     * Returns true if the player can land a critical hit right now.
     * Crit = falling + not in liquid + not climbing + not in cobweb.
     */
    private boolean canDoCrit() {
        if (mc.player == null) return false;
        boolean falling  = mc.player.fallDistance > 0.05f && !mc.player.isOnGround();
        boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava();
        boolean climbing = mc.player.isClimbing();
        return falling && !inLiquid && !climbing;
    }

    private RotationMode getRotationMode() {
        return switch (aimMode.getValue()) {
            case "Fun Time" -> new FunTimeRotation();
            case "Matrix" -> new MatrixRotation();
            case "Legit Snap", "Snap" -> new SnapRotation();
            default -> new SnapRotation();
        };
    }

    private AimData getAimData(LivingEntity target) {
        if (target == null) {
            return new AimData(Vec3d.ZERO, null);
        }

        if (mc.player == null) {
            return new AimData(target.getBoundingBox().getCenter(), target.getBoundingBox());
        }

        int currentTick = mc.player.age;
        if (target == cachedAimTarget && cachedAimTick == currentTick) {
            return new AimData(cachedAimPoint, cachedAimBox);
        }

        AimData computed = computeAimData(target);
        cachedAimTarget = target;
        cachedAimTick = currentTick;
        cachedAimPoint = computed.point();
        cachedAimBox = computed.box();
        return computed;
    }

    private AimData computeAimData(LivingEntity target) {
        if (usingElytraTarget()) {
            return new AimData(ElytraTargetModule.getInstance().elytraRotationProcessor.getPredictedPos(target), target.getBoundingBox());
        }

        Rotation initialRotation = RotationManager.getInstance().getRotation();
        Pair<Vec3d, Box> pair = pointFinder.computeVector(
                target,
                getAttackDistance() + getPreDistance(),
                initialRotation,
                getOffsetVelocity(),
                combatExecutor.options().isEnabled("Ignore walls")
        );

        Vec3d point = pair.getLeft();
        if (point == null || point.equals(Vec3d.ZERO)) {
            point = getRandomBodyToHeadPoint(target);
        }
        return new AimData(point, pair.getRight());
    }

    private Vec3d getOffsetVelocity() {
        return switch (aimMode.getValue()) {
            case "Fun Time" -> FunTimeRotation.getRandomOffsetVelocity();
            case "Matrix" -> new Vec3d(0.1D, 0.1D, 0.1D);
            case "Snap" -> new Vec3d(0.12D, 0.12D, 0.12D);
            case "Legit Snap" -> Vec3d.ZERO;
            default -> Vec3d.ZERO;
        };
    }

    private Vec3d getRandomBodyToHeadPoint(LivingEntity target) {
        Box box = target.getBoundingBox();
        double bodyCenter = box.minY + target.getHeight() * 0.45D;
        double headTop = box.maxY - 0.1D;
        double randomY = bodyCenter + ThreadLocalRandom.current().nextDouble() * (headTop - bodyCenter);
        double randomX = MathHelper.lerp(ThreadLocalRandom.current().nextDouble() * 0.3D - 0.15D, box.getCenter().x, box.getCenter().x);
        double randomZ = MathHelper.lerp(ThreadLocalRandom.current().nextDouble() * 0.3D - 0.15D, box.getCenter().z, box.getCenter().z);
        return new Vec3d(
                MathHelper.clamp(randomX, box.minX + 0.05D, box.maxX - 0.05D),
                MathHelper.clamp(randomY, box.minY + 0.1D, box.maxY - 0.1D),
                MathHelper.clamp(randomZ, box.minZ + 0.05D, box.maxZ - 0.05D)
        );
    }

    private boolean isTargetInsideLegitCircle(LivingEntity entity) {
        if (mc.player == null || entity == null) {
            return false;
        }

        boolean ignoreWalls = combatExecutor.options().isEnabled("Ignore walls");
        List<Vec3d> points = pointFinder.generateCandidatePoints(entity, getAttackDistance(), ignoreWalls).getLeft();
        if (points.isEmpty()) {
            points = List.of(entity.getBoundingBox().getCenter());
        }

        for (Vec3d point : points) {
            if (isPointInsideLegitCircle(point)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPointInsideLegitCircle(Vec3d targetPoint) {
        var camera = mc.getEntityRenderDispatcher().camera;
        if (camera == null) {
            return false;
        }

        Vec3d cameraPos = camera.getPos();
        Vec3d direction = targetPoint.subtract(cameraPos);
        double flat = Math.hypot(direction.x, direction.z);
        if (flat < 1.0E-6D) {
            return true;
        }

        float yawToTarget = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F);
        float pitchToTarget = MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(direction.y, flat)));
        float yawDelta = MathHelper.wrapDegrees(yawToTarget - camera.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - camera.getPitch());
        double angularDelta = Math.hypot(yawDelta, pitchDelta);

        return angularDelta <= legitFov.getValue();
    }

    private void handleRender2D(DrawContext context) {
        if (!isEnabled() || !aimMode.is("Legit Snap") || mc.options.hudHidden) {
            return;
        }

        float radius = MathHelper.clamp(legitFov.getValue(), 30.0F, 90.0F) * 1.8F;
        float centerX = mc.getWindow().getScaledWidth() / 2.0F;
        float centerY = mc.getWindow().getScaledHeight() / 2.0F;
        int color = withAlpha(0x00FFFFFF, 214);
        int segments = Math.max(140, Math.round(radius * 4.2F));
        drawSmoothLegitRing(context, centerX, centerY, radius, 1.05F, color, segments);
    }

    private void drawSmoothLegitRing(DrawContext context, float centerX, float centerY, float radius, float thickness, int color, int segments) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float halfThickness = thickness / 2.0F;
        float innerRadius = Math.max(1.0F, radius - halfThickness);
        float outerRadius = radius + halfThickness;

        int solidColor = withAlpha(color, 235);
        int fadeColor = withAlpha(color, 0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder coreRing = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            coreRing.vertex(matrix, centerX + cos * outerRadius, centerY + sin * outerRadius, 0.0F).color(solidColor);
            coreRing.vertex(matrix, centerX + cos * innerRadius, centerY + sin * innerRadius, 0.0F).color(solidColor);
        }
        BufferRenderer.drawWithGlobalProgram(coreRing.end());

        float outerFadeRadius = outerRadius + 0.75F;
        BufferBuilder outerFade = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            outerFade.vertex(matrix, centerX + cos * outerFadeRadius, centerY + sin * outerFadeRadius, 0.0F).color(fadeColor);
            outerFade.vertex(matrix, centerX + cos * outerRadius, centerY + sin * outerRadius, 0.0F).color(withAlpha(color, 158));
        }
        BufferRenderer.drawWithGlobalProgram(outerFade.end());

        float innerFadeRadius = Math.max(0.8F, innerRadius - 0.65F);
        BufferBuilder innerFade = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            innerFade.vertex(matrix, centerX + cos * innerRadius, centerY + sin * innerRadius, 0.0F).color(withAlpha(color, 158));
            innerFade.vertex(matrix, centerX + cos * innerFadeRadius, centerY + sin * innerFadeRadius, 0.0F).color(fadeColor);
        }
        BufferRenderer.drawWithGlobalProgram(innerFade.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }

    private void handlePacket(PacketEvent.PacketEventData event) {
        if (!event.isReceive() || !(event.packet() instanceof EntityStatusS2CPacket statusPacket) || statusPacket.getStatus() != 30) {
            return;
        }

        if (!combatExecutor.options().isEnabled("Shield break")) {
            return;
        }

        Entity entity = statusPacket.getEntity(mc.world);
        if (entity != null && entity.equals(target)) {
            NotificationUtil.add("Shield broken: " + entity.getDisplayName().getString());
        }
    }

    private boolean usingElytraTarget() {
        return target != null && ElytraTargetModule.getInstance().elytraRotationProcessor.using();
    }

    private void resetAimCache() {
        cachedAimTick = -1;
        cachedAimTarget = null;
        cachedAimPoint = Vec3d.ZERO;
        cachedAimBox = null;
    }

    private record AimData(Vec3d point, Box box) {
    }
}
