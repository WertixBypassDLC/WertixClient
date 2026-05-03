package sweetie.nezi.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspMode;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspModule;

import java.awt.Color;

public class TargetEspCircle3D extends TargetEspMode {
    private static final int LONGITUDE_SEGMENTS = 44;
    private static final int LATITUDE_SEGMENTS = 16;

    private float spin;
    private float prevSpin;

    private final SmoothFloat smoothLine = new SmoothFloat(2.0f);
    private final SmoothFloat smoothSpeed = new SmoothFloat(1.0f);

    @Override
    public void onUpdate() {
        smoothLine.update(TargetEspModule.getInstance().getLineWidth(), 0.15f);
        smoothSpeed.update(TargetEspModule.getInstance().getSpeed(), 0.15f);

        prevSpin = spin;
        spin += 1.8f * smoothSpeed.get();
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) {
            return;
        }

        float partialTicks = event.partialTicks();
        float alpha = MathUtil.interpolate(prevShowAnimation, (float) showAnimation.getValue(), partialTicks);
        float sizeVal = MathUtil.interpolate(prevSizeAnimation, (float) sizeAnimation.getValue(), partialTicks);
        if (alpha <= 0.01f || sizeVal <= 0.01f) {
            return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }

        float hurtBlend = MathHelper.clamp((currentTarget.hurtTime - partialTicks) / 10f, 0f, 1f);
        Color baseColor = TargetEspModule.getInstance().getCustomColor(hurtBlend);
        float spinAngle = MathUtil.interpolate(prevSpin, spin, partialTicks);
        float time = System.currentTimeMillis() * 0.001f * smoothSpeed.get();

        float baseRadius = Math.max(0.45f, currentTarget.getWidth() * 0.84f);
        float pulse = 1.0f + (float) Math.sin(time * 2.5f) * 0.035f;
        float radiusXZ = baseRadius * (0.96f + sizeVal * 0.24f) * pulse;
        float radiusY = (currentTarget.getHeight() * 0.52f + 0.12f) * (0.97f + sizeVal * 0.10f);
        float centerY = currentTarget.getHeight() * 0.5f;

        Vec3d renderPos = new Vec3d(getTargetX(), getTargetY(), getTargetZ()).subtract(camera.getPos());
        MatrixStack matrices = event.matrixStack();
        matrices.push();
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        drawShell(matrices, radiusXZ, radiusY, centerY, alpha, baseColor, spinAngle, time);
        drawOutline(matrices, radiusXZ, radiusY, centerY, alpha, baseColor, spinAngle, time);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void drawShell(MatrixStack matrices, float radiusXZ, float radiusY, float centerY, float alpha, Color baseColor, float spinAngle, float time) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder shell = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int lat = 0; lat < LATITUDE_SEGMENTS; lat++) {
            float v0 = (float) lat / LATITUDE_SEGMENTS;
            float v1 = (float) (lat + 1) / LATITUDE_SEGMENTS;
            double theta0 = -Math.PI / 2.0 + v0 * Math.PI;
            double theta1 = -Math.PI / 2.0 + v1 * Math.PI;

            float ringRadius0 = (float) Math.cos(theta0);
            float ringRadius1 = (float) Math.cos(theta1);
            float y0 = centerY + (float) Math.sin(theta0) * radiusY;
            float y1 = centerY + (float) Math.sin(theta1) * radiusY;

            for (int lon = 0; lon < LONGITUDE_SEGMENTS; lon++) {
                float progress = (float) lon / LONGITUDE_SEGMENTS;
                float nextProgress = (float) (lon + 1) / LONGITUDE_SEGMENTS;
                double angle0 = Math.toRadians(progress * 360.0 + spinAngle * 18.0);
                double angle1 = Math.toRadians(nextProgress * 360.0 + spinAngle * 18.0);

                float x00 = (float) Math.cos(angle0) * radiusXZ * ringRadius0;
                float z00 = (float) Math.sin(angle0) * radiusXZ * ringRadius0;
                float x01 = (float) Math.cos(angle1) * radiusXZ * ringRadius0;
                float z01 = (float) Math.sin(angle1) * radiusXZ * ringRadius0;
                float x10 = (float) Math.cos(angle0) * radiusXZ * ringRadius1;
                float z10 = (float) Math.sin(angle0) * radiusXZ * ringRadius1;
                float x11 = (float) Math.cos(angle1) * radiusXZ * ringRadius1;
                float z11 = (float) Math.sin(angle1) * radiusXZ * ringRadius1;

                float wave0 = 0.56f + (float) Math.sin(time * 2.1f + progress * Math.PI * 4.0 + v0 * 6.0f) * 0.20f;
                float wave1 = 0.56f + (float) Math.sin(time * 2.1f + nextProgress * Math.PI * 4.0 + v1 * 6.0f) * 0.20f;
                Color c00 = color(baseColor, alpha, progress + v0 * 0.12f, wave0, 86);
                Color c01 = color(baseColor, alpha, nextProgress + v0 * 0.12f, wave0, 86);
                Color c10 = color(baseColor, alpha, progress + v1 * 0.12f, wave1 + 0.08f, 126);
                Color c11 = color(baseColor, alpha, nextProgress + v1 * 0.12f, wave1 + 0.08f, 126);

                shell.vertex(matrix, x00, y0, z00).color(c00.getRGB());
                shell.vertex(matrix, x01, y0, z01).color(c01.getRGB());
                shell.vertex(matrix, x11, y1, z11).color(c11.getRGB());
                shell.vertex(matrix, x10, y1, z10).color(c10.getRGB());
            }
        }

        BufferRenderer.drawWithGlobalProgram(shell.end());
    }

    private void drawOutline(MatrixStack matrices, float radiusXZ, float radiusY, float centerY, float alpha, Color baseColor, float spinAngle, float time) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.lineWidth(Math.max(1.0f, smoothLine.get()));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (int lat = 2; lat < LATITUDE_SEGMENTS - 1; lat += 3) {
            float v = (float) lat / LATITUDE_SEGMENTS;
            double theta = -Math.PI / 2.0 + v * Math.PI;
            float ringRadius = (float) Math.cos(theta);
            float y = centerY + (float) Math.sin(theta) * radiusY;

            for (int lon = 0; lon < LONGITUDE_SEGMENTS; lon++) {
                float progress = (float) lon / LONGITUDE_SEGMENTS;
                float nextProgress = (float) (lon + 1) / LONGITUDE_SEGMENTS;
                double angle0 = Math.toRadians(progress * 360.0 + spinAngle * 18.0);
                double angle1 = Math.toRadians(nextProgress * 360.0 + spinAngle * 18.0);

                float x1 = (float) Math.cos(angle0) * radiusXZ * ringRadius;
                float z1 = (float) Math.sin(angle0) * radiusXZ * ringRadius;
                float x2 = (float) Math.cos(angle1) * radiusXZ * ringRadius;
                float z2 = (float) Math.sin(angle1) * radiusXZ * ringRadius;

                float brightness = 0.95f + (float) Math.sin(time * 2.4f + v * 4.0f) * 0.08f;
                Color c1 = color(baseColor, alpha, progress + v * 0.2f, brightness, 210);
                Color c2 = color(baseColor, alpha, nextProgress + v * 0.2f, brightness, 210);
                addLine(lines, matrix, x1, y, z1, x2, y, z2, c1, c2);
            }
        }

        for (int lon = 0; lon < LONGITUDE_SEGMENTS; lon += 4) {
            float progress = (float) lon / LONGITUDE_SEGMENTS;
            double angle = Math.toRadians(progress * 360.0 + spinAngle * 18.0);

            for (int lat = 0; lat < LATITUDE_SEGMENTS; lat++) {
                float v0 = (float) lat / LATITUDE_SEGMENTS;
                float v1 = (float) (lat + 1) / LATITUDE_SEGMENTS;
                double theta0 = -Math.PI / 2.0 + v0 * Math.PI;
                double theta1 = -Math.PI / 2.0 + v1 * Math.PI;

                float ringRadius0 = (float) Math.cos(theta0);
                float ringRadius1 = (float) Math.cos(theta1);
                float x1 = (float) Math.cos(angle) * radiusXZ * ringRadius0;
                float z1 = (float) Math.sin(angle) * radiusXZ * ringRadius0;
                float x2 = (float) Math.cos(angle) * radiusXZ * ringRadius1;
                float z2 = (float) Math.sin(angle) * radiusXZ * ringRadius1;
                float y1 = centerY + (float) Math.sin(theta0) * radiusY;
                float y2 = centerY + (float) Math.sin(theta1) * radiusY;

                Color meridian = color(baseColor, alpha, progress + v0 * 0.18f, 1.08f, 176);
                addLine(lines, matrix, x1, y1, z1, x2, y2, z2, meridian, meridian);
            }
        }

        BufferRenderer.drawWithGlobalProgram(lines.end());
    }

    private void addLine(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, Color c1, Color c2) {
        buffer.vertex(matrix, x1, y1, z1).color(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
        buffer.vertex(matrix, x2, y2, z2).color(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
    }

    private Color color(Color baseColor, float alpha, float progress, float brightness, int alphaScale) {
        Color wave = (progress % 0.2f < 0.1f)
                ? sweetie.nezi.api.utils.color.UIColors.themeFlow((int) (progress * 1000), 255)
                : sweetie.nezi.api.utils.color.UIColors.themeFlowAlt((int) (progress * 1000), 255);
        Color mixed = ColorUtil.interpolate(baseColor, wave, 0.22f);

        int r = MathHelper.clamp((int) (mixed.getRed() * brightness), 0, 255);
        int g = MathHelper.clamp((int) (mixed.getGreen() * brightness), 0, 255);
        int b = MathHelper.clamp((int) (mixed.getBlue() * brightness), 0, 255);
        int a = MathHelper.clamp((int) (alpha * alphaScale), 0, 255);
        return new Color(r, g, b, a);
    }
}
