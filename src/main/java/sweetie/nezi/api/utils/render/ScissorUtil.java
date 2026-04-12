package sweetie.nezi.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.system.interfaces.QuickImports;

import java.util.ArrayDeque;
import java.util.Deque;

@UtilityClass
public class ScissorUtil implements QuickImports {
    private final Deque<ScissorArea> areaStack = new ArrayDeque<>();

    public void start(MatrixStack matrixStack, float x, float y, float width, float height) {
        float maxWidth = mc.getWindow().getScaledWidth();
        float maxHeight = mc.getWindow().getScaledHeight();

        ScissorArea requestedArea = clampToWindow(x, y, width, height, maxWidth, maxHeight);
        if (!areaStack.isEmpty()) {
            requestedArea = intersect(areaStack.peek(), requestedArea);
        }

        matrixStack.push();
        areaStack.push(requestedArea);
        applyScissor(requestedArea, maxHeight);
    }

    public void stop(MatrixStack matrixStack) {
        if (areaStack.isEmpty()) {
            return;
        }

        areaStack.pop();
        matrixStack.pop();

        if (areaStack.isEmpty()) {
            RenderSystem.disableScissor();
            return;
        }

        applyScissor(areaStack.peek(), mc.getWindow().getScaledHeight());
    }

    private void applyScissor(ScissorArea area, float maxHeight) {
        float scale = (float) mc.getWindow().getScaleFactor();

        int scaledX = Math.round(area.x * scale);
        int scaledY = Math.round((maxHeight - area.y - area.height) * scale);
        int scaledWidth = Math.round(area.width * scale);
        int scaledHeight = Math.round(area.height * scale);

        RenderSystem.enableScissor(scaledX, scaledY, Math.max(0, scaledWidth), Math.max(0, scaledHeight));
    }

    private ScissorArea clampToWindow(float x, float y, float width, float height, float maxWidth, float maxHeight) {
        if (width <= 0f || height <= 0f) {
            return new ScissorArea(0f, 0f, 0f, 0f);
        }

        float left = Math.max(0f, x);
        float top = Math.max(0f, y);
        float right = Math.min(maxWidth, x + width);
        float bottom = Math.min(maxHeight, y + height);
        return new ScissorArea(left, top, Math.max(0f, right - left), Math.max(0f, bottom - top));
    }

    private ScissorArea intersect(ScissorArea a, ScissorArea b) {
        float x = Math.max(a.x, b.x);
        float y = Math.max(a.y, b.y);
        float right = Math.min(a.x + a.width, b.x + b.width);
        float bottom = Math.min(a.y + a.height, b.y + b.height);
        return new ScissorArea(x, y, Math.max(0f, right - x), Math.max(0f, bottom - y));
    }

    private record ScissorArea(float x, float y, float width, float height) {
    }
}
