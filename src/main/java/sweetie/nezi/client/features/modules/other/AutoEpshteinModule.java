package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
import sweetie.nezi.api.utils.math.TimerUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@ModuleRegister(name = "AutoEpshtein", category = Category.OTHER)
public class AutoEpshteinModule extends Module {
    @Getter private static final AutoEpshteinModule instance = new AutoEpshteinModule();

    private static final long RECENT_OPEN_COOLDOWN_MS = 15_000L;

    private final SliderSetting range = new SliderSetting("Range").value(4.5f).range(2.5f, 6.0f).step(0.1f);
    private final SliderSetting delay = new SliderSetting("Delay").value(80f).range(0f, 500f).step(5f);
    private final BooleanSetting autoClose = new BooleanSetting("Auto close").value(true);

    private final TimerUtil actionTimer = new TimerUtil();
    private final Map<BlockPos, Long> recentOpened = new HashMap<>();

    private BlockPos pendingChestPos;
    private BlockPos activeChestPos;
    private long pendingChestAt;

    public AutoEpshteinModule() {
        addSettings(range, delay, autoClose);
    }

    @Override
    public void onDisable() {
        pendingChestPos = null;
        activeChestPos = null;
        pendingChestAt = 0L;
        recentOpened.clear();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> handleUpdate()));
        addEvents(updateEvent);
    }

    private void handleUpdate() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        cleanupRecentOpened();

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            if (activeChestPos == null && pendingChestPos != null) {
                activeChestPos = pendingChestPos.toImmutable();
                pendingChestPos = null;
            }

            if (activeChestPos != null) {
                lootContainer(handler);
            }
            return;
        }

        activeChestPos = null;
        if (pendingChestPos != null && System.currentTimeMillis() - pendingChestAt > 1200L) {
            pendingChestPos = null;
        }

        if (mc.currentScreen != null || isInventoryFull() || !actionTimer.finished(delay.getValue().longValue())) {
            return;
        }

        ChestCandidate candidate = findBestChestCandidate();
        if (candidate == null) {
            return;
        }

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, candidate.hitResult());
        mc.player.swingHand(Hand.MAIN_HAND);
        pendingChestPos = candidate.pos().toImmutable();
        pendingChestAt = System.currentTimeMillis();
        recentOpened.put(pendingChestPos, pendingChestAt);
        actionTimer.reset();
    }

    private void lootContainer(GenericContainerScreenHandler handler) {
        if (isInventoryFull()) {
            closeActiveChest();
            return;
        }

        int chestSize = handler.getRows() * 9;
        if (chestSize <= 0) {
            closeActiveChest();
            return;
        }

        if (!actionTimer.finished(delay.getValue().longValue())) {
            return;
        }

        for (int i = 0; i < chestSize; i++) {
            Slot slot = handler.getSlot(i);
            if (!slot.hasStack()) {
                continue;
            }

            mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            actionTimer.reset();
            return;
        }

        if (autoClose.getValue()) {
            closeActiveChest();
        }
    }

    private void closeActiveChest() {
        if (mc.player == null) {
            return;
        }

        if (activeChestPos != null) {
            recentOpened.put(activeChestPos.toImmutable(), System.currentTimeMillis());
        }

        mc.player.closeHandledScreen();
        activeChestPos = null;
        pendingChestPos = null;
        pendingChestAt = 0L;
        actionTimer.reset();
    }

    private ChestCandidate findBestChestCandidate() {
        int radius = (int) Math.ceil(range.getValue());
        BlockPos playerPos = mc.player.getBlockPos();

        return BlockPos.streamOutwards(playerPos, radius, radius, radius)
                .map(BlockPos::toImmutable)
                .filter(this::isValidChest)
                .filter(pos -> mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= range.getValue() * range.getValue())
                .filter(pos -> !isRecentlyOpened(pos))
                .map(pos -> {
                    BlockHitResult hitResult = findReachableHit(pos);
                    if (hitResult == null) {
                        return null;
                    }
                    return new ChestCandidate(pos, hitResult, mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)));
                })
                .filter(candidate -> candidate != null)
                .min(Comparator.comparingDouble(ChestCandidate::distanceSq))
                .orElse(null);
    }

    private boolean isValidChest(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST);
    }

    private boolean isRecentlyOpened(BlockPos pos) {
        Long openedAt = recentOpened.get(pos);
        return openedAt != null && System.currentTimeMillis() - openedAt < RECENT_OPEN_COOLDOWN_MS;
    }

    private void cleanupRecentOpened() {
        long now = System.currentTimeMillis();
        recentOpened.entrySet().removeIf(entry -> now - entry.getValue() >= RECENT_OPEN_COOLDOWN_MS);
    }

    private BlockHitResult findReachableHit(BlockPos chestPos) {
        Vec3d eyePos = mc.player.getEyePos();

        Direction[] directions = {
                Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN
        };

        for (Direction direction : directions) {
            Vec3d faceCenter = Vec3d.ofCenter(chestPos).add(
                    direction.getOffsetX() * 0.5,
                    direction.getOffsetY() * 0.5,
                    direction.getOffsetZ() * 0.5
            );

            if (eyePos.squaredDistanceTo(faceCenter) > range.getValue() * range.getValue()) {
                continue;
            }

            BlockHitResult ray = mc.world.raycast(new RaycastContext(
                    eyePos,
                    faceCenter,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (ray.getType() == HitResult.Type.BLOCK && ray.getBlockPos().equals(chestPos)) {
                return ray;
            }
        }

        return null;
    }

    private boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().main.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private record ChestCandidate(BlockPos pos, BlockHitResult hitResult, double distanceSq) { }
}
