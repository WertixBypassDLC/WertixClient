package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.other.RotationUpdateEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.event.events.player.world.AttackEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.combat.CombatExecutor;
import sweetie.nezi.api.utils.combat.TargetManager;
import sweetie.nezi.api.utils.predict.PredictUtils;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;
import sweetie.nezi.api.utils.rotation.manager.RotationMode;
import sweetie.nezi.api.utils.rotation.manager.RotationStrategy;
import sweetie.nezi.api.utils.rotation.misc.AuraUtil;
import sweetie.nezi.api.utils.rotation.rotations.FunTimeRotation;
import sweetie.nezi.api.utils.rotation.rotations.MatrixRotation;
import sweetie.nezi.api.utils.rotation.rotations.SmoothRotation;
import sweetie.nezi.api.utils.rotation.rotations.UniversalRotation;
import sweetie.nezi.api.utils.task.TaskPriority;
import sweetie.nezi.client.features.modules.combat.elytratarget.ElytraTargetModule;

import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "Aura", category = Category.COMBAT)
public class AuraModule extends Module {
    @Getter private static final AuraModule instance = new AuraModule();

    private final TargetManager targetManager = new TargetManager();
    public final CombatExecutor combatExecutor = new CombatExecutor();

    @Getter private final ModeSetting aimMode = new ModeSetting("Aim mode").value("Smooth").values(
            "Smooth", "Snap",
            "Matrix", "Vulcan",
            "Fun Time"
    );
    @Getter private final ModeSetting move = new ModeSetting("Move").value("Focus").values("Focus", "Free", "Target", "None");

    private final SliderSetting distance = new SliderSetting("Distance").value(3f).range(2.5f, 5f).step(0.1f);
    private final SliderSetting preDistance = new SliderSetting("Pre distance").value(0.3f).range(0f, 3f).step(0.1f);
    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Mobs").value(false),
            new BooleanSetting("Animals").value(false)
    );

    // Bypasses are always enabled, no UI setting exposed

    private final BooleanSetting clientLook = new BooleanSetting("Client look").value(false);
    private final BooleanSetting elytraOverride = new BooleanSetting("Elytra override").value(false);
    private final SliderSetting elytraDistance = new SliderSetting("Elytra distance").value(4f).range(2.5f, 5f).step(0.1f).setVisible(elytraOverride::getValue);
    private final SliderSetting elytraPreDistance = new SliderSetting("Elytra pre distance").value(16f).range(0f, 32f).step(0.1f).setVisible(elytraOverride::getValue);
    private final BooleanSetting predictOnElytra = new BooleanSetting("Predict on elytra").value(true);
    private final SliderSetting predictTicks = new SliderSetting("Predict ticks").value(2f).range(1f, 4f).step(0.1f).setVisible(predictOnElytra::getValue);

    public LivingEntity target;

    public AuraModule() {
        // Enable all bypass options by default (they stay hidden from UI)
        combatExecutor.options().forceEnable("Only crits");
        combatExecutor.options().forceEnable("Smart crits");
        combatExecutor.options().forceEnable("Raytrace");
        combatExecutor.options().forceEnable("Shield break");
        combatExecutor.options().forceEnable("Always shield");
        combatExecutor.options().forceEnable("Ignore walls");

        addSettings(
                aimMode, move, distance, preDistance, targets, clientLook,
                elytraOverride, elytraDistance, elytraPreDistance,
                predictOnElytra, predictTicks
        );
    }

    public static boolean moveFixEnabled() {
        return instance.isEnabled() && !instance.move.is("None");
    }

    public float getMovementYaw() {
        if (mc.player == null) {
            return 0;
        }

        Rotation rotation = RotationManager.getInstance().getCurrentRotation();
        if (rotation != null) {
            return rotation.getYaw();
        }

        return mc.player.getYaw();
    }

    public float getPredictTicks() {
        return predictTicks.getValue();
    }

    public float getPreDistance() {
        return (mc.player.isGliding() && elytraOverride.getValue()) ? elytraPreDistance.getValue() : preDistance.getValue();
    }

    public float getAttackDistance() {
        return (mc.player.isGliding() && elytraOverride.getValue()) ? elytraDistance.getValue() : distance.getValue();
    }

    @Override
    public void onDisable() {
        targetManager.releaseTarget();
        target = null;
        FunTimeRotation.reset();
    }

    @Override
    public void onEnable() {
        targetManager.releaseTarget();
        target = null;
        FunTimeRotation.reset();
    }

    @Override
    public void onEvent() {
        EventListener eventUpdate = UpdateEvent.getInstance().subscribe(new Listener<>(event -> updateEventHandler()));
        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> postRotMoveEventHandler()));
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> AuraUtil.onAttack(aimMode.getValue())));
        addEvents(eventUpdate, rotationUpdateEvent, attackEvent);
    }

    private void postRotMoveEventHandler() {
        if (target == null) {
            return;
        }

        Vec3d attackVector = getTargetVector(target);
        Rotation rotation = RotationUtil.fromVec3d(attackVector.subtract(mc.player.getEyePos()));
        rotateToTarget(target, attackVector, rotation);
    }

    private void updateEventHandler() {
        target = updateTarget();
        if (target == null) {
            return;
        }

        if (RotationUtil.getSpot(target).distanceTo(mc.player.getEyePos()) > getAttackDistance() + getPreDistance()) {
            targetManager.releaseTarget();
            return;
        }

        attackTarget(target);
    }

    private LivingEntity updateTarget() {
        TargetManager.EntityFilter filter = new TargetManager.EntityFilter(targets.getList());
        targetManager.searchTargets(mc.world.getEntities(), getAttackDistance() + getPreDistance());
        targetManager.validateTarget(filter::isValid);
        return targetManager.getCurrentTarget();
    }

    private void attackTarget(LivingEntity target) {
        combatExecutor.combatManager().configurable(
                new CombatExecutor.CombatConfigurable(
                        target,
                        RotationManager.getInstance().getRotation(),
                        distance.getValue(),
                        true, true, true, true, true, true, false
                )
        );

        if (mc.player.getEyePos().distanceTo(
                RotationUtil.rayCastBox(target, getTargetVector(target))
        ) > getAttackDistance()) {
            return;
        }

        combatExecutor.performAttack();
    }

    private void rotateToTarget(LivingEntity target, Vec3d targetVec, Rotation rotation) {
        RotationStrategy configurable = new RotationStrategy(getRotationMode(),
                !move.is("None"), move.is("Free")).clientLook(clientLook.getValue());

        boolean canAttack = combatExecutor.combatManager().canAttack();
        boolean noHitRule = !canAttack;
        long timeSinceLastClick = combatExecutor.combatManager().clickScheduler().lastClickPassed();

        if (usingElytraTarget() && ElytraTargetModule.getInstance().elytraRotationProcessor.customRotations.getValue()) {
            return;
        }

        if (noHitRule && aimMode.is("Snap")) {
            return;
        }

        if (aimMode.is("Fun Time")) {
            boolean recentAttack = timeSinceLastClick < 380L;
            if (!canAttack && !recentAttack) {
                return;
            }

            FunTimeRotation.primeSnapWindow(canAttack, timeSinceLastClick);
            configurable.ticksUntilReset(4);
        }

        RotationManager.getInstance().addRotation(new Rotation.VecRotation(rotation, targetVec), target, configurable, TaskPriority.HIGH, this);
    }

    private RotationMode getRotationMode() {
        return switch (aimMode.getValue()) {
            case "Vulcan" -> new UniversalRotation(160, 80, false, false);
            case "Fun Time" -> new FunTimeRotation();
            case "Matrix" -> new MatrixRotation();
            default -> new SmoothRotation();
        };
    }

    private Vec3d getTargetVector(LivingEntity target) {
        if (target == null) {
            return Vec3d.ZERO;
        }

        if (usingElytraTarget()) {
            return ElytraTargetModule.getInstance().elytraRotationProcessor.getPredictedPos(target);
        }

        // Random aim point between body center and head
        Vec3d aimpoint = getRandomBodyToHeadPoint(target);

        if (predictOnElytra.getValue() && target instanceof PlayerEntity
                && mc.player.isGliding() && target.isGliding()) {
            float ticks = mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter()) > 8.0
                    ? 8.0f : predictTicks.getValue();
            aimpoint = PredictUtils.predict(target, target.getPos(), ticks);
        }

        return aimpoint;
    }

    /**
     * Returns a random aim point between the entity's body center and head top.
     * This provides natural-looking aim variation while keeping accuracy.
     */
    private Vec3d getRandomBodyToHeadPoint(LivingEntity target) {
        Box box = target.getBoundingBox();
        double bodyCenter = box.minY + target.getHeight() * 0.45;
        double headTop = box.maxY - 0.1;
        double randomY = bodyCenter + ThreadLocalRandom.current().nextDouble() * (headTop - bodyCenter);
        double randomX = MathHelper.lerp(ThreadLocalRandom.current().nextDouble() * 0.3 - 0.15, box.getCenter().x, box.getCenter().x);
        double randomZ = MathHelper.lerp(ThreadLocalRandom.current().nextDouble() * 0.3 - 0.15, box.getCenter().z, box.getCenter().z);
        return new Vec3d(
                MathHelper.clamp(randomX, box.minX + 0.05, box.maxX - 0.05),
                MathHelper.clamp(randomY, box.minY + 0.1, box.maxY - 0.1),
                MathHelper.clamp(randomZ, box.minZ + 0.05, box.maxZ - 0.05)
        );
    }

    private boolean usingElytraTarget() {
        return target != null && ElytraTargetModule.getInstance().elytraRotationProcessor.using();
    }
}
