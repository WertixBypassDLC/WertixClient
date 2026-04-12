package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Hand;
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
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.api.utils.combat.ClickScheduler;
import sweetie.nezi.api.utils.combat.TargetManager;
import sweetie.nezi.api.utils.player.PlayerUtil;
import sweetie.nezi.client.features.modules.movement.SprintModule;
import sweetie.nezi.api.utils.math.TimerUtil;

import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "TriggerBot", category = Category.COMBAT)
public class TriggerBotModule extends Module {
    @Getter private static final TriggerBotModule instance = new TriggerBotModule();

    private final SliderSetting distance = new SliderSetting("Distance").value(3.0f).range(2.0f, 5.0f).step(0.05f);
    private final BooleanSetting onlyCrits = new BooleanSetting("Only Crits").value(true);
    private final BooleanSetting smartCrits = new BooleanSetting("Smart crits").value(true).setVisible(onlyCrits::getValue);
    private final BooleanSetting shieldBreak = new BooleanSetting("Shield Break").value(true);
    private final BooleanSetting noAttackIfEat = new BooleanSetting("No attack if eat").value(false);

    private final BooleanSetting players = new BooleanSetting("Players").value(true);
    private final BooleanSetting attackInvisibles = new BooleanSetting("Attack Invisibles").value(false).setVisible(players::getValue);
    private final BooleanSetting attackNaked = new BooleanSetting("Attack Naked").value(false).setVisible(players::getValue);

    private final ClickScheduler clickScheduler = new ClickScheduler();
    private final TimerUtil attackTimer = new TimerUtil();
    private long nextDelay = 0L;

    public TriggerBotModule() {
        addSettings(distance, onlyCrits, smartCrits, shieldBreak, noAttackIfEat, players, attackInvisibles, attackNaked);
    }

    @Override
    public void onDisable() {
        AuraModule.getInstance().target = null;
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            Entity target = getTarget();

            if (target instanceof LivingEntity livingTarget) {
                AuraModule.getInstance().target = livingTarget;

                if (shouldAttack(livingTarget)) {
                    attack(livingTarget);
                }
            } else {
                AuraModule.getInstance().target = null;
            }
        }));

        addEvents(update);
    }

    private Entity getTarget() {
        if (mc.player == null || mc.world == null) return null;

        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        double range = distance.getValue();

        Vec3d cameraPos = mc.player.getCameraPosVec(tickDelta);
        Vec3d rotationVec = mc.player.getRotationVec(1.0F);
        Vec3d endPos = cameraPos.add(rotationVec.multiply(range));

        Box box = mc.player.getBoundingBox()
                .stretch(rotationVec.multiply(range))
                .expand(1.0D, 1.0D, 1.0D);

        EntityHitResult entityHit = ProjectileUtil.raycast(
                mc.player,
                cameraPos,
                endPos,
                box,
                (entity) -> !entity.isSpectator()
                        && entity.canHit()
                        && entity instanceof LivingEntity
                        && !FriendManager.getInstance().contains(entity.getName().getString()),
                range * range
        );

        if (entityHit == null || entityHit.getEntity() == null) {
            return null;
        }

        if (hasBlockingCollision(cameraPos, endPos, cameraPos.squaredDistanceTo(entityHit.getPos()))) {
            return null;
        }

        if (cameraPos.squaredDistanceTo(entityHit.getPos()) <= range * range) {
            return entityHit.getEntity();
        }

        return null;
    }

    private boolean shouldAttack(LivingEntity target) {
        if (FriendManager.getInstance().contains(target.getName().getString())) return false;
        
        if (target instanceof PlayerEntity player) {
            if (!players.getValue()) return false;
            if (TargetManager.isStationaryNumericNamePlayer(player)) return false;
            
            if (player.isInvisible() && !attackInvisibles.getValue()) {
                return false;
            }
            if (!attackNaked.getValue()) {
                boolean hasArmor = false;
                for (net.minecraft.item.ItemStack armorItem : player.getArmorItems()) {
                    if (!armorItem.isEmpty()) {
                        hasArmor = true;
                        break;
                    }
                }
                if (!hasArmor) return false;
            }
        }
        
        if (noAttackIfEat.getValue() && PlayerUtil.isEating()) return false;

        net.minecraft.item.Item mainHand = mc.player.getMainHandStack().getItem();
        boolean isWeapon = mainHand instanceof net.minecraft.item.SwordItem ||
                           mainHand instanceof net.minecraft.item.AxeItem ||
                           mainHand instanceof net.minecraft.item.MaceItem ||
                           mainHand instanceof net.minecraft.item.TridentItem;
        
        if (!isWeapon && !attackTimer.finished(nextDelay)) return false;

        float cooldownReq = 0.92f + ThreadLocalRandom.current().nextFloat() * 0.03f;
        if (mc.player.getAttackCooldownProgress(0.68f) < cooldownReq) return false;

        if (target.isBlocking() && shieldBreak.getValue()) return true;
        if (!onlyCrits.getValue()) return true;

        boolean ground = mc.player.isOnGround();
        boolean jumping = mc.options.jumpKey.isPressed();

        if (smartCrits.getValue() && ground && !jumping) return true;

        boolean falling = mc.player.fallDistance > 0.0f;
        boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava();
        boolean climbing = mc.player.isClimbing();

        return (!ground && falling) || inLiquid || climbing;
    }

    private void attack(LivingEntity target) {
        boolean sprinting = mc.player.isSprinting();

        if (SprintModule.getInstance().mode.is("Packet")) {
            mc.player.networkHandler.sendPacket(
                    new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                            mc.player,
                            net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SPRINTING
                    )
            );
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (SprintModule.getInstance().mode.is("Packet") && sprinting) {
            mc.player.networkHandler.sendPacket(
                    new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                            mc.player,
                            net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_SPRINTING
                    )
            );
        }

        attackTimer.reset();
        nextDelay = ThreadLocalRandom.current().nextLong(50L, 101L);
        clickScheduler.recalculate(500);
    }

    private boolean hasBlockingCollision(Vec3d start, Vec3d end, double targetDistanceSq) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() <= 1.0E-6D) {
            return false;
        }

        Vec3d currentStart = start;
        Vec3d stepDirection = direction.normalize().multiply(0.05D);

        for (int i = 0; i < 16; i++) {
            BlockHitResult blockHit = mc.world.raycast(new RaycastContext(
                    currentStart,
                    end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (blockHit.getType() == HitResult.Type.MISS) {
                return false;
            }

            double blockDistanceSq = start.squaredDistanceTo(blockHit.getPos());
            if (blockDistanceSq >= targetDistanceSq) {
                return false;
            }

            BlockState state = mc.world.getBlockState(blockHit.getBlockPos());
            if (canBypassTriggerBlock(state)) {
                currentStart = blockHit.getPos().add(stepDirection);
                continue;
            }

            return true;
        }

        return false;
    }

    private boolean canBypassTriggerBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof StairsBlock
                || block instanceof FenceBlock
                || block instanceof FenceGateBlock
                || block instanceof WallBlock
                || block instanceof TrapdoorBlock
                || block instanceof DoorBlock
                || block instanceof LeavesBlock
                || block instanceof PistonBlock
                || block instanceof PistonHeadBlock;
    }
}
