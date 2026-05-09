package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
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
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.api.utils.player.PlayerUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;

import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "TriggerBot", category = Category.COMBAT)
public class TriggerBotModule extends Module {
    @Getter private static final TriggerBotModule instance = new TriggerBotModule();

    private final SliderSetting distance       = new SliderSetting("Дистанция").value(2.9997f).range(2.0f, 5.0f).step(0.05f);
    private final BooleanSetting onlyCrits     = new BooleanSetting("Только криты").value(true);
    private final BooleanSetting smartCrits    = new BooleanSetting("Умные криты").value(true).setVisible(onlyCrits::getValue);
    private final BooleanSetting noAttackIfEat = new BooleanSetting("Не бить при еде").value(false);
    private final BooleanSetting ignoreWalls   = new BooleanSetting("Игнор стен").value(true);

    private final BooleanSetting targetPlayers = new BooleanSetting("Игроки").value(true);
    private final BooleanSetting targetNaked   = new BooleanSetting("Голые").value(true).setVisible(targetPlayers::getValue);
    private final MultiBooleanSetting targets = new MultiBooleanSetting("Цели").value(
            targetPlayers,
            targetNaked,
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Жители").value(false)
    );

    private final TimerUtil attackTimer = new TimerUtil();
    private long nextDelay = 0L;
    private final CombatExecutor combatExecutor = new CombatExecutor();

    private volatile boolean pendingWtapAttack = false;

    public TriggerBotModule() {
        addSettings(distance, onlyCrits, smartCrits, noAttackIfEat, ignoreWalls, targets);
    }

    @Override
    public void onDisable() {
        AuraModule.getInstance().target = null;
        pendingWtapAttack = false;
        combatExecutor.combatManager().resetState();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            Entity target = getTarget();

            if (target instanceof LivingEntity livingTarget) {
                AuraModule.getInstance().target = livingTarget;
                configureCombat(livingTarget);

                if (pendingWtapAttack) return;

                if (shouldAttack(livingTarget)) {
                    initiateAttack(livingTarget);
                }
            } else {
                AuraModule.getInstance().target = null;
            }
        }));
        addEvents(update);
    }

    private Entity getTarget() {
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        double range    = distance.getValue();

        Vec3d cameraPos   = mc.player.getCameraPosVec(tickDelta);
        Vec3d rotationVec = mc.player.getRotationVec(1.0F);
        Vec3d endPos      = cameraPos.add(rotationVec.multiply(range));

        Box box = mc.player.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0D);

        EntityHitResult entityHit = ProjectileUtil.raycast(
                mc.player, cameraPos, endPos, box,
                (entity) -> !entity.isSpectator()
                        && entity.canHit()
                        && entity instanceof LivingEntity
                        && !FriendManager.getInstance().contains(entity.getName().getString()),
                range * range
        );

        if (entityHit == null || entityHit.getEntity() == null) return null;

        if (hasBlockingCollision(cameraPos, endPos, cameraPos.squaredDistanceTo(entityHit.getPos()))) {
            return null;
        }

        return entityHit.getEntity();
    }

    private boolean shouldAttack(LivingEntity target) {
        if (!new sweetie.nezi.api.utils.combat.TargetManager.EntityFilter(targets.getList()).isValid(target)) return false;
        if (noAttackIfEat.getValue() && PlayerUtil.isEating()) return false;

        if (!attackTimer.finished(nextDelay)) return false;

        net.minecraft.item.Item mainHand = mc.player.getMainHandStack().getItem();
        boolean isWeapon = mainHand instanceof net.minecraft.item.SwordItem
                || mainHand instanceof net.minecraft.item.AxeItem
                || mainHand instanceof net.minecraft.item.MaceItem
                || mainHand instanceof net.minecraft.item.TridentItem;

        boolean falling  = mc.player.fallDistance > 0.05f && !mc.player.isOnGround();
        boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava();
        boolean inWeb    = mc.player.inPowderSnow || isInCobweb();
        boolean climbing = mc.player.isClimbing();

        if (!isWeapon) {
            // Без оружия: бьём только в крит (falling) ИЛИ с большой задержкой
            if (falling && !inLiquid && !inWeb && !climbing) return true;
            return attackTimer.finished(600L);
        }

        // Полный кулдаун, без рандомной поддавки → не «съедает» серию критов
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.92f) return false;

        if (!onlyCrits.getValue()) return combatExecutor.combatManager().canAttack();

        // Чёткий крит: только при падении
        if (falling && !inLiquid && !inWeb && !climbing) return combatExecutor.combatManager().canAttack();

        // Умные криты: разрешаем при невозможности крита (вода/паутина),
        // на земле НЕ бьём, чтобы не выдавать обычные удары между критами
        if (smartCrits.getValue() && (inLiquid || inWeb || climbing)) return combatExecutor.combatManager().canAttack();

        if (onlyCrits.getValue() && combatExecutor.combatManager().canAttackPreview(4)) return false;

        return false;
    }

    /** Check if player is inside cobweb block */
    private boolean isInCobweb() {
        return PlayerUtil.isInWeb();
    }

    private void initiateAttack(LivingEntity target) {
        if (WTapModule.getInstance().isEnabled()) {
            pendingWtapAttack = true;
            boolean accepted = WTapModule.getInstance().requestCritAttack(() -> {
                pendingWtapAttack = false;
                doAttack(target);
            });
            if (!accepted) {
                pendingWtapAttack = false;
                doAttack(target);
            }
        } else {
            doAttack(target);
        }
    }

    private void doAttack(LivingEntity target) {
        combatExecutor.performAttack();

        nextDelay = 20L + ThreadLocalRandom.current().nextLong(0, 30);
        attackTimer.reset();
    }

    private void configureCombat(LivingEntity target) {
        java.util.List<String> options = new java.util.ArrayList<>();
        if (onlyCrits.getValue()) options.add("Only crits");
        if (smartCrits.getValue()) options.add("Smart crits");
        options.add("Dynamic cooldown");
        options.add("Shield break");
        options.add("UnPress shield");
        if (ignoreWalls.getValue()) options.add("Ignore walls");
        if (noAttackIfEat.getValue()) options.add("No attack if eat");

        combatExecutor.combatManager().configurable(
                new CombatExecutor.CombatConfigurable(
                        target,
                        new Rotation(mc.player.getYaw(), mc.player.getPitch()),
                        distance.getValue(),
                        null,
                        options,
                        1.0F
                )
        );
    }

    private boolean hasBlockingCollision(Vec3d start, Vec3d end, double targetDistanceSq) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() <= 1.0E-6D) return false;

        Vec3d currentStart   = start;
        Vec3d stepDirection  = direction.normalize().multiply(0.05D);

        for (int i = 0; i < 24; i++) {
            BlockHitResult blockHit = mc.world.raycast(new RaycastContext(
                    currentStart, end, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, mc.player
            ));

            if (blockHit.getType() == HitResult.Type.MISS) return false;

            double blockDistanceSq = start.squaredDistanceTo(blockHit.getPos());
            if (blockDistanceSq >= targetDistanceSq) return false;

            if (canBypassTriggerBlock(mc.world.getBlockState(blockHit.getBlockPos()))) {
                currentStart = blockHit.getPos().add(stepDirection);
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean canBypassTriggerBlock(BlockState state) {
        if (ignoreWalls.getValue() && state.isAir()) {
            return true;
        }

        Block block = state.getBlock();
        String translationKey = block.getTranslationKey();
        return block instanceof StairsBlock || block instanceof FenceBlock
                || block instanceof FenceGateBlock || block instanceof WallBlock
                || block instanceof TrapdoorBlock  || block instanceof DoorBlock
                || block instanceof LeavesBlock    || block instanceof PistonBlock
                || block instanceof PistonHeadBlock || block instanceof SlabBlock
                || block instanceof PaneBlock || translationKey.contains("glass")
                || block instanceof TransparentBlock || block instanceof ChestBlock
                || block instanceof EnderChestBlock || block instanceof CobwebBlock;
    }
}