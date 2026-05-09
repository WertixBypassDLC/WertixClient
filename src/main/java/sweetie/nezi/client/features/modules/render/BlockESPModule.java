package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Vector2f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.display.BoxRender;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.other.WardenHelperModule;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.util.shape.VoxelShape;

@ModuleRegister(name = "Block ESP", category = Category.RENDER)
public class BlockESPModule extends Module {
    @Getter private static final BlockESPModule instance = new BlockESPModule();

    private static final Color DEFAULT_OUTLINE = new Color(214, 226, 242, 196);

    private final MultiBooleanSetting blocks = new MultiBooleanSetting("Блоки").value(
            new BooleanSetting("Сундуки").value(true),
            new BooleanSetting("Бочки").value(true),
            new BooleanSetting("Спавнера").value(true),
            new BooleanSetting("Воронки").value(true),
            new BooleanSetting("Шалкера").value(true)
    );
    private final BooleanSetting useRenderDistance = new BooleanSetting("В радиусе прогрузки").value(true);
    private final SliderSetting range = new SliderSetting("Расстояние").value(32f).range(8f, 256f).step(1f).setVisible(() -> !useRenderDistance.getValue());

    private final Map<BlockPos, BlockState> renderBlocks = new HashMap<>();
    private long lastScanMs = 0L;
    private long lastPlayerChunkX = Long.MIN_VALUE;
    private long lastPlayerChunkZ = Long.MIN_VALUE;

    public BlockESPModule() {
        addSettings(blocks, useRenderDistance, range);
    }

    private float effectiveRange() {
        if (useRenderDistance.getValue()) {
            try {
                int viewChunks = mc.options.getViewDistance().getValue();
                return Math.max(16f, viewChunks * 16f);
            } catch (Throwable ignored) {
                return 96f;
            }
        }
        return range.getValue();
    }

    @Override
    public void onDisable() {
        renderBlocks.clear();
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> onRender3D()));
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> onRender2D(event.context())));
        addEvents(render3DEvent, render2DEvent);
    }

    private void onRender3D() {
        if (mc.world == null || mc.player == null) {
            renderBlocks.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long curCx = mc.player.getChunkPos().x;
        long curCz = mc.player.getChunkPos().z;
        boolean movedChunks = curCx != lastPlayerChunkX || curCz != lastPlayerChunkZ;
        // Сканируем реже (3с) и только при смене чанка или по интервалу
        if (movedChunks || now - lastScanMs >= 3000L) {
            scanBlocks();
            lastScanMs = now;
            lastPlayerChunkX = curCx;
            lastPlayerChunkZ = curCz;
        }

        Iterator<Map.Entry<BlockPos, BlockState>> iterator = renderBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BlockState> entry = iterator.next();
            BlockPos pos = entry.getKey();
            BlockState liveState = mc.world.getBlockState(pos);

            if (liveState.isAir() || !shouldRender(liveState)) {
                iterator.remove();
                continue;
            }

            Color color = resolveColor(pos, liveState);
            VoxelShape outlineShape = liveState.getOutlineShape(mc.world, pos);
            Box renderBox = outlineShape.isEmpty() ? new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : outlineShape.getBoundingBox();

            RenderUtil.BOX.drawBox(
                    (float) (pos.getX() + renderBox.minX), (float) (pos.getY() + renderBox.minY), (float) (pos.getZ() + renderBox.minZ),
                    (float) (pos.getX() + renderBox.maxX), (float) (pos.getY() + renderBox.maxY), (float) (pos.getZ() + renderBox.maxZ),
                    1.0f, color, BoxRender.Render.OUTLINE, 0f
            );
        }
    }

    private void onRender2D(net.minecraft.client.gui.DrawContext context) {
        WardenHelperModule wardenHelper = WardenHelperModule.getInstance();
        if (mc.world == null || mc.player == null || !wardenHelper.isEnabled()) {
            return;
        }

        renderBlocks.forEach((pos, state) -> {
            BlockState liveState = mc.world.getBlockState(pos);
            if (liveState.isAir() || (!isChest(liveState) && !liveState.isOf(Blocks.BARREL))) {
                return;
            }
            if (!wardenHelper.shouldRenderUnderground(pos)) {
                return;
            }

            String label = wardenHelper.getContainerLabel(pos);
            if (label == null) {
                return;
            }

            Vector2f projected = ProjectionUtil.project(pos.getX() + 0.5, pos.getY() + 1.12, pos.getZ() + 0.5);
            if (projected == null || projected.x == Float.MAX_VALUE || projected.y == Float.MAX_VALUE) {
                return;
            }

            Color timerColor = resolveColor(pos, liveState);
            float fontSize = 7.0f;
            float pad = 2.5f;
            float textWidth = Fonts.PS_BOLD.getWidth(label, fontSize);
            float boxWidth = textWidth + pad * 2f;
            float boxHeight = fontSize + pad * 2f;
            float boxX = projected.x - boxWidth / 2f;
            float boxY = projected.y - boxHeight / 2f;

            RenderUtil.BLUR_RECT.draw(context.getMatrices(), boxX, boxY, boxWidth, boxHeight, 2.5f, new Color(8, 10, 14, 112));
            Fonts.PS_BOLD.drawText(context.getMatrices(), label, boxX + pad, boxY + pad, fontSize, timerColor);
        });
    }

    private void scanBlocks() {
        renderBlocks.clear();

        float r = effectiveRange();
        float scanRangeSq = r * r;

        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;

        int chunkRadius = (int) Math.ceil(r / 16.0);

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {

                if (!mc.world.isChunkLoaded(cx, cz))
                    continue;

                var chunk = mc.world.getChunk(cx, cz);

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {

                    BlockPos pos = blockEntity.getPos();

                    if (mc.player.squaredDistanceTo(pos.toCenterPos()) > scanRangeSq)
                        continue;

                    BlockState state = blockEntity.getCachedState();

                    if (shouldRender(state)) {
                        renderBlocks.put(pos.toImmutable(), state);
                    }
                }
            }
        }
    }

    private boolean shouldRender(BlockState state) {
        Block block = state.getBlock();
        return isChest(state) && blocks.isEnabled("Сундуки")
                || block == Blocks.BARREL && blocks.isEnabled("Бочки")
                || block == Blocks.SPAWNER && blocks.isEnabled("Спавнера")
                || block == Blocks.HOPPER && blocks.isEnabled("Воронки")
                || block instanceof ShulkerBoxBlock && blocks.isEnabled("Шалкера");
    }

    private Color resolveColor(BlockPos pos, BlockState state) {
        if (isChest(state) || state.isOf(Blocks.BARREL)) {
            Color wardenColor = WardenHelperModule.getInstance().getContainerColor(pos);
            if (WardenHelperModule.getInstance().isEnabled() && wardenColor != null) {
                return wardenColor;
            }
            return new Color(255, 188, 96, 205);
        }

        return DEFAULT_OUTLINE;
    }

    private boolean isChest(BlockState state) {
        return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST);
    }

    public float getRenderRange() {
        return effectiveRange();
    }

    public boolean rendersWardenContainers() {
        return blocks.isEnabled("Сундуки") || blocks.isEnabled("Бочки");
    }

    public String getBlockId(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }
}