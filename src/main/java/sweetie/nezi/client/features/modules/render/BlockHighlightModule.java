package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.ColorSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.display.BoxRender;

import java.awt.*;

@Getter
@ModuleRegister(name = "Block Highlight", category = Category.RENDER)
public class BlockHighlightModule extends Module {
    @Getter private static final BlockHighlightModule instance = new BlockHighlightModule();

    private final ColorSetting color = new ColorSetting("Color").value(new Color(236, 242, 252, 214));
    private final SliderSetting lineWidth = new SliderSetting("Line width").value(1.4f).range(0.5f, 4f).step(0.1f);

    public BlockHighlightModule() {
        addSettings(color, lineWidth);
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> onRender3D()));
        addEvents(render3DEvent);
    }

    private void onRender3D() {
        if (mc.world == null || mc.player == null || mc.crosshairTarget == null) {
            return;
        }

        if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK || !(mc.crosshairTarget instanceof BlockHitResult blockHitResult)) {
            return;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        VoxelShape outlineShape = state.getOutlineShape(mc.world, pos);
        Box outlineBox = outlineShape.isEmpty() ? new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : outlineShape.getBoundingBox();
        Color outlineColor = color.getValue();

        RenderUtil.BOX.drawBox(
                (float) (pos.getX() + outlineBox.minX), (float) (pos.getY() + outlineBox.minY), (float) (pos.getZ() + outlineBox.minZ),
                (float) (pos.getX() + outlineBox.maxX), (float) (pos.getY() + outlineBox.maxY), (float) (pos.getZ() + outlineBox.maxZ),
                lineWidth.getValue(), outlineColor, BoxRender.Render.OUTLINE, 0f
        );
    }
}
