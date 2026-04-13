package sweetie.nezi.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspMode;

import java.util.ArrayList;
import java.util.List;

public class TargetEspComets extends TargetEspMode {
    private final List<double[]> previousPositions = new ArrayList<>();

    private float ghostRotationAngle = 0f;
    private float ghostYRotationAngle = 0f;
    private float prevGhostRotationAngle = 0f;
    private float prevGhostYRotationAngle = 0f;

    @Override
    public void onUpdate() {
        prevGhostRotationAngle = ghostRotationAngle;
        prevGhostYRotationAngle = ghostYRotationAngle;

        float blend = getRetargetBlend();
        ghostRotationAngle += 8f + (blend * 18f);
        ghostYRotationAngle += 8f;
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        MatrixStack matrixStack = event.matrixStack();

        RenderUtil.WORLD.startRender(matrixStack);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        float size = 0.24f;
        float dony = 0.92f * MathUtil.interpolate(prevSizeAnimation, (float) sizeAnimation.getValue(), event.partialTicks());
        float rem = (float) (0.28 * MathUtil.interpolate(prevSizeAnimation, (float) sizeAnimation.getValue(), event.partialTicks()));
        int trailLength = 28;

        List<double[]> currentGhostPositions = new ArrayList<>();

        double centerX = getTargetX();
        double centerY = getTargetY() + currentTarget.getHeight() / 2;
        double centerZ = getTargetZ();

        float blend = getRetargetBlend();

        for (int i = 0; i < 3; i++) {
            float angle = MathUtil.interpolate(prevGhostRotationAngle, ghostRotationAngle, event.partialTicks()) + (i * 180);

            double radius = Math.max(currentTarget.getWidth() * dony, 0.28) + (blend * 1.5);

            double offsetX = Math.cos(Math.toRadians(angle)) * radius;
            double offsetZ = Math.sin(Math.toRadians(angle)) * radius;
            double offsetY = Math.cos(Math.toRadians(angle)) / 3f * radius;

            double ghostYI = MathUtil.interpolate(prevGhostYRotationAngle, ghostYRotationAngle, event.partialTicks());

            if (i == 0) {
                offsetY = Math.sin(Math.toRadians(ghostYI)) * currentTarget.getHeight() * rem;
            } else if (i == 2) {
                offsetY = -Math.sin(Math.toRadians(ghostYI)) * currentTarget.getHeight() * rem;
                offsetX = -Math.cos(Math.toRadians(angle)) * radius;
            }

            double ghostX = centerX + offsetX;
            double ghostY = centerY + offsetY;
            double ghostZ = centerZ + offsetZ;

            currentGhostPositions.add(new double[]{ghostX, ghostY, ghostZ, angle});
        }

        previousPositions.addAll(currentGhostPositions);
        if (previousPositions.size() > trailLength * 3) {
            previousPositions.subList(0, previousPositions.size() - trailLength * 3).clear();
        }

        for (int i = 0; i < 3; i++) {
            double[] currentPos = currentGhostPositions.get(i);
            float angle = (float) currentPos[3];

            double renderX = currentPos[0] - mc.getEntityRenderDispatcher().camera.getPos().getX();
            double renderY = currentPos[1] - mc.getEntityRenderDispatcher().camera.getPos().getY();
            double renderZ = currentPos[2] - mc.getEntityRenderDispatcher().camera.getPos().getZ();
            renderGhost(matrixStack, renderX, renderY, renderZ, angle, size, 1);

            for (int t = 0; t < previousPositions.size() / 3; t++) {
                int index = t * 3 + i;
                if (index >= previousPositions.size()) continue;

                double[] trailPos = previousPositions.get(index);
                double trailRenderX = trailPos[0] - mc.getEntityRenderDispatcher().camera.getPos().getX();
                double trailRenderY = trailPos[1] - mc.getEntityRenderDispatcher().camera.getPos().getY();
                double trailRenderZ = trailPos[2] - mc.getEntityRenderDispatcher().camera.getPos().getZ();
                float trailAngle = (float) trailPos[3];

                float trailAlpha = (float) (t + 1) / (previousPositions.size() / 3f + 1);
                float trailSize = Math.max(size * 0.46f, size * trailAlpha);
                renderGhost(matrixStack, trailRenderX, trailRenderY, trailRenderZ, trailAngle, trailSize, trailAlpha);
            }
        }

        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderUtil.WORLD.endRender(matrixStack);
    }

    public void renderGhost(MatrixStack stack, double x, double y, double z, float angle, float size, float alphaMultiplier) {
        stack.push();
        stack.translate(x, y, z);

        Camera camera = mc.gameRenderer.getCamera();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw() + 180.0F));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-camera.getPitch() + 180.0F));

        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        Matrix4f matrix = stack.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float[] c = ColorUtil.normalize(ColorUtil.setAlpha(UIColors.gradient((int) angle), (int) (255 * showAnimation.getValue())));
        float alpha = Math.min(1f, c[3] * (0.52f + alphaMultiplier * 0.44f));

        buffer.vertex(matrix, -size, size, 0).texture(0f, 1f).color(c[0], c[1], c[2], alpha);
        buffer.vertex(matrix, size, size, 0).texture(1f, 1f).color(c[0], c[1], c[2], alpha);
        buffer.vertex(matrix, size, -size, 0).texture(1f, 0f).color(c[0], c[1], c[2], alpha);
        buffer.vertex(matrix, -size, -size, 0).texture(0f, 0f).color(c[0], c[1], c[2], alpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        stack.pop();
    }
}