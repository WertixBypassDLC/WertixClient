package sweetie.nezi.api.utils.render.display;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BoxRender {
    private final List<OutlinedBox> outlinedBoxes = new ArrayList<>();
    private final List<FilledBox> filledBoxes = new ArrayList<>();
    private final List<StripedBox> stripedBoxes = new ArrayList<>();

    public enum Render {
        FILL, OUTLINE, STRIPED
    }

    public void setup3DRender(MatrixStack matrixStack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.gameRenderer == null || client.gameRenderer.getCamera() == null) {
            clearQueuedBoxes();
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Frustum frustum = new Frustum(matrixStack.peek().getPositionMatrix(), RenderSystem.getProjectionMatrix());
        frustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);

        renderFilledBoxes(filledBoxes, frustum);
        renderStripedBoxes(stripedBoxes, frustum);
        renderOutlinedBoxes(outlinedBoxes, frustum);

        clearQueuedBoxes();
    }

    private void clearQueuedBoxes() {
        filledBoxes.clear();
        stripedBoxes.clear();
        outlinedBoxes.clear();
    }

    private void renderFilledBoxes(List<FilledBox> boxes, Frustum frustum) {
        renderFilled(boxes, frustum);
    }

    private void renderStripedBoxes(List<StripedBox> boxes, Frustum frustum) {
        if (boxes.isEmpty()) return;

        for (StripedBox action : boxes) {
            Box box = new Box(action.pos, action.pos.add(action.params));
            if (!frustum.isVisible(box)) continue;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            RenderSystem.lineWidth(action.lineWidth);

            renderDashedOutlinedBox(action.pos, action.params, matrixFrom(action.pos.x, action.pos.y, action.pos.z), buffer, action.color, action.gapDistance);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void renderOutlinedBoxes(List<OutlinedBox> boxes, Frustum frustum) {
        renderOutlined(boxes, frustum);
    }

    private void renderFilled(List<FilledBox> boxes, Frustum frustum) {
        if (boxes.isEmpty()) return;

        for (FilledBox action : boxes) {
            Box box = new Box(action.pos, action.pos.add(action.params));
            if (!frustum.isVisible(box)) continue;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_COLOR
            );

            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            renderFilledBox(action.pos, action.params, matrixFrom(action.pos.x, action.pos.y, action.pos.z), buffer, action.color);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void renderOutlined(List<OutlinedBox> boxes, Frustum frustum) {
        if (boxes.isEmpty()) return;

        for (OutlinedBox action : boxes) {
            Box box = new Box(action.pos, action.pos.add(action.params));
            if (!frustum.isVisible(box)) continue;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(
                    VertexFormat.DrawMode.LINES,
                    VertexFormats.LINES
            );

            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
            RenderSystem.lineWidth(action.lineWidth);

            renderOutlinedBox(action.pos, action.params, matrixFrom(action.pos.x, action.pos.y, action.pos.z), buffer, action.color);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void renderDashedOutlinedBox(Vec3d pos, Vec3d params, MatrixStack matrices, BufferBuilder buffer, Color color, float gapDistance) {
        float x1 = 0.0f;
        float y1 = 0.0f;
        float z1 = 0.0f;
        float x2 = (float) params.x;
        float y2 = (float) params.y;
        float z2 = (float) params.z;

        renderDashedLine(matrices, buffer, color, x1, y1, z1, x2, y1, z1, gapDistance, gapDistance);
        renderDashedLine(matrices, buffer, color, x2, y1, z1, x2, y1, z2, gapDistance, gapDistance);
        renderDashedLine(matrices, buffer, color, x2, y1, z2, x1, y1, z2, gapDistance, gapDistance);
        renderDashedLine(matrices, buffer, color, x1, y1, z2, x1, y1, z1, gapDistance, gapDistance);

        renderDashedLine(matrices, buffer, color, x1, y2, z1, x2, y2, z1, gapDistance, gapDistance);
        renderDashedLine(matrices, buffer, color, x2, y2, z1, x2, y2, z2, gapDistance, gapDistance);
        renderDashedLine(matrices, buffer, color, x2, y2, z2, x1, y2, z2, gapDistance, gapDistance);
        renderDashedLine(matrices, buffer, color, x1, y2, z2, x1, y2, z1, gapDistance, gapDistance);

        renderDashedLine(matrices, buffer, color, x1, y1, z1, x1, y2, z1, gapDistance, gapDistance);
        renderDashedLine(matrices, buffer, color, x2, y1, z1, x2, y2, z1, gapDistance, gapDistance);
        renderDashedLine(matrices, buffer, color, x1, y1, z2, x1, y2, z2, gapDistance, gapDistance);
        renderDashedLine(matrices, buffer, color, x2, y1, z2, x2, y2, z2, gapDistance, gapDistance);
    }

    private void renderDashedLine(MatrixStack matrices, BufferBuilder buffer, Color color, float x1, float y1, float z1, float x2, float y2, float z2, float dashLength, float gapDistance) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float totalLength = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (MathHelper.approximatelyEquals(totalLength, 0.0f)) return;

        float t = 0.0f;
        while (t < 1.0f) {
            float tDashStart = t;
            float tDashEnd = Math.min(t + (dashLength / totalLength), 1.0f);
            if (tDashEnd > tDashStart) {
                float dashX1 = x1 + dx * tDashStart;
                float dashY1 = y1 + dy * tDashStart;
                float dashZ1 = z1 + dz * tDashStart;
                float dashX2 = x1 + dx * tDashEnd;
                float dashY2 = y1 + dy * tDashEnd;
                float dashZ2 = z1 + dz * tDashEnd;

                vertexLine(matrices, buffer, dashX1, dashY1, dashZ1, dashX2, dashY2, dashZ2, color);
            }
            t = tDashEnd;

            t = Math.min(t + (gapDistance / totalLength), 1.0f);
        }
    }

    private void renderFilledBox(Vec3d pos, Vec3d params, MatrixStack matrices, BufferBuilder buffer, Color color) {
        float x1 = 0.0f;
        float y1 = 0.0f;
        float z1 = 0.0f;
        float x2 = (float) params.x;
        float y2 = (float) params.y;
        float z2 = (float) params.z;

        vertexQuad(matrices, buffer, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, color);
        vertexQuad(matrices, buffer, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        vertexQuad(matrices, buffer, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        vertexQuad(matrices, buffer, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, color);
        vertexQuad(matrices, buffer, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        vertexQuad(matrices, buffer, x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, color);
    }

    private void renderOutlinedBox(Vec3d pos, Vec3d params, MatrixStack matrices, BufferBuilder buffer, Color color) {
        float x1 = 0.0f;
        float y1 = 0.0f;
        float z1 = 0.0f;
        float x2 = (float) params.x;
        float y2 = (float) params.y;
        float z2 = (float) params.z;

        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);

        vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
        vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);

        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
    }

    private void vertexLine(MatrixStack matrices, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, Color lineColor) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f model = entry.getPositionMatrix();
        Vector3f normalVec = getNormalVec(x1, y1, z1, x2, y2, z2);

        buffer.vertex(model, x1, y1, z1).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x, normalVec.y, normalVec.z);
        buffer.vertex(model, x2, y2, z2).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x, normalVec.y, normalVec.z);
    }

    private Vector3f getNormalVec(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    private MatrixStack matrixFrom(double x, double y, double z) {
        MatrixStack matrices = new MatrixStack();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.gameRenderer == null || client.gameRenderer.getCamera() == null) {
            return matrices;
        }
        Camera camera = client.gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);
        return matrices;
    }

    private void vertexQuad(MatrixStack matrices, BufferBuilder buffer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, Color color) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f model = entry.getPositionMatrix();
        Vector3f normal = new Vector3f(0, 0, 1);

        buffer.vertex(model, x1, y1, z1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(entry, normal.x, normal.y, normal.z);
        buffer.vertex(model, x2, y2, z2).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(entry, normal.x, normal.y, normal.z);
        buffer.vertex(model, x3, y3, z3).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(entry, normal.x, normal.y, normal.z);
        buffer.vertex(model, x4, y4, z4).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(entry, normal.x, normal.y, normal.z);
    }

    public void drawBox(float x1, float y1, float z1, float x2, float y2, float z2, float lineWidth, Color color, Render renderMode, float gapDistance) {
        Vec3d pos = new Vec3d(x1, y1, z1);
        Vec3d params = new Vec3d(x2 - x1, y2 - y1, z2 - z1);

        switch (renderMode) {
            case FILL -> filledBoxes.add(new FilledBox(pos, params, color));
            case OUTLINE -> outlinedBoxes.add(new OutlinedBox(pos, params, lineWidth, color));
            case STRIPED -> stripedBoxes.add(new StripedBox(pos, params, lineWidth, color, gapDistance));
        }
    }

    public record FilledBox(Vec3d pos, Vec3d params, Color color) {}
    public record OutlinedBox(Vec3d pos, Vec3d params, float lineWidth, Color color) {}
    public record StripedBox(Vec3d pos, Vec3d params, float lineWidth, Color color, float gapDistance) {}
}
