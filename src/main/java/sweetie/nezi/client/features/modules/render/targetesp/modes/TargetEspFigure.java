package sweetie.nezi.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspMode;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspModule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TargetEspFigure extends TargetEspMode {
    private static final Identifier GLOW_TEXTURE = FileUtil.getImage("particles/glow");

    private static final List<FigurePoint> STAR_POINTS = List.of(
            new FigurePoint(0.0f, 1.0f), new FigurePoint(0.26f, 0.32f),
            new FigurePoint(1.0f, 0.0f), new FigurePoint(0.36f, -0.18f),
            new FigurePoint(0.0f, -1.0f), new FigurePoint(-0.36f, -0.18f),
            new FigurePoint(-1.0f, 0.0f), new FigurePoint(-0.26f, 0.32f)
    );
    private static final List<FigurePoint> DIAMOND_POINTS = List.of(
            new FigurePoint(0.0f, 1.0f), new FigurePoint(0.68f, 0.0f),
            new FigurePoint(0.0f, -1.0f), new FigurePoint(-0.68f, 0.0f)
    );
    private static final List<FigurePoint> TRIANGLE_POINTS = List.of(
            new FigurePoint(0.0f, 1.0f), new FigurePoint(0.88f, -0.68f),
            new FigurePoint(-0.88f, -0.68f)
    );
    private static final List<FigurePoint> WAVE_POINTS = List.of(
            new FigurePoint(-1.0f, -0.18f), new FigurePoint(-0.68f, 0.44f),
            new FigurePoint(-0.34f, -0.44f), new FigurePoint(0.0f, 0.18f),
            new FigurePoint(0.34f, 0.92f), new FigurePoint(0.68f, -0.14f),
            new FigurePoint(1.0f, 0.30f)
    );

    private float spinAngle = 0f;
    private float prevSpinAngle = 0f;

    private final SmoothFloat smoothSize = new SmoothFloat(0.9f);
    private final SmoothFloat smoothDepth = new SmoothFloat(0.18f);
    private final SmoothFloat smoothLine = new SmoothFloat(2.0f);
    private final SmoothFloat smoothSpeed = new SmoothFloat(1.0f);

    @Override
    public void onUpdate() {
        smoothSize.update(TargetEspModule.getInstance().getFigureSize(), 0.15f);
        smoothDepth.update(TargetEspModule.getInstance().getFigureDepth(), 0.15f);
        smoothLine.update(TargetEspModule.getInstance().getLineWidth(), 0.15f);
        smoothSpeed.update(TargetEspModule.getInstance().getSpeed(), 0.15f);

        prevSpinAngle = spinAngle;
        spinAngle += 2.2f * smoothSpeed.get();
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;
        float partialTicks = event.partialTicks();
        float alpha = MathUtil.interpolate(prevShowAnimation, (float) showAnimation.getValue(), partialTicks);
        float sizeVal = MathUtil.interpolate(prevSizeAnimation, (float) sizeAnimation.getValue(), partialTicks);
        if (alpha <= 0.01f || sizeVal <= 0.01f) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d center = new Vec3d(getTargetX(), getTargetY() + currentTarget.getHeight() * 0.58, getTargetZ());
        Vec3d renderPos = center.subtract(cameraPos);

        float figureSize = smoothSize.get() * (0.28f + sizeVal * 0.52f);
        float figureDepth = smoothDepth.get() * (0.35f + sizeVal * 0.65f);
        float spin = MathUtil.interpolate(prevSpinAngle, spinAngle, event.partialTicks());

        List<FigurePoint> points = getFigurePoints();
        if (points.size() < 3) return;

        MatrixStack matrices = event.matrixStack();
        matrices.push();
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        drawFigureLines(matrices, points, figureSize, figureDepth, alpha);

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        for (int i = 0; i < points.size(); i++) {
            FigurePoint pt = points.get(i);
            float pulse = 0.92f + (float) Math.sin(System.currentTimeMillis() * 0.01 + i * 0.55f) * 0.08f;
            drawFlatGlow(matrices, pt.x * figureSize, pt.y * figureSize, figureDepth, figureSize * 0.28f * pulse, color(i * 57, 0.18f + alpha * 0.32f));
            drawFlatGlow(matrices, pt.x * figureSize, pt.y * figureSize, -figureDepth, figureSize * 0.18f * pulse, color(i * 57 + 90, 0.10f + alpha * 0.24f));
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void drawFigureLines(MatrixStack matrices, List<FigurePoint> points, float figureSize, float figureDepth, float alpha) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.lineWidth(Math.max(1.0f, smoothLine.get()));
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i < points.size(); i++) {
            FigurePoint cur = points.get(i);
            FigurePoint next = points.get((i + 1) % points.size());
            float x1 = cur.x * figureSize, y1 = cur.y * figureSize;
            float x2 = next.x * figureSize, y2 = next.y * figureSize;
            addLine(lines, matrix, x1, y1, figureDepth, x2, y2, figureDepth, color(i * 61, 0.26f + alpha * 0.34f), color(i * 61 + 35, 0.22f + alpha * 0.28f));
            addLine(lines, matrix, x1, y1, -figureDepth, x2, y2, -figureDepth, color(i * 61 + 120, 0.14f + alpha * 0.22f), color(i * 61 + 155, 0.12f + alpha * 0.20f));
            addLine(lines, matrix, x1, y1, -figureDepth, x1, y1, figureDepth, color(i * 61 + 120, 0.14f + alpha * 0.22f), color(i * 61, 0.26f + alpha * 0.34f));
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());
    }

    private void drawFlatGlow(MatrixStack matrices, float x, float y, float z, float size, Color color) {
        matrices.push();
        matrices.translate(x, y, z);
        Matrix4f m = matrices.peek().getPositionMatrix();
        float[] rgba = ColorUtil.normalize(color);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m, -size, size, 0f).texture(0f, 1f).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        buf.vertex(m, size, size, 0f).texture(1f, 1f).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        buf.vertex(m, size, -size, 0f).texture(1f, 0f).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        buf.vertex(m, -size, -size, 0f).texture(0f, 0f).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        matrices.pop();
    }

    private void addLine(BufferBuilder buf, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, Color c1, Color c2) {
        buf.vertex(m, x1, y1, z1).color(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
        buf.vertex(m, x2, y2, z2).color(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
    }

    private List<FigurePoint> getFigurePoints() {
        String p = TargetEspModule.getInstance().getFigurePreset();
        if ("Ромб".equalsIgnoreCase(p)) return DIAMOND_POINTS;
        if ("Треугольник".equalsIgnoreCase(p)) return TRIANGLE_POINTS;
        if ("Волна".equalsIgnoreCase(p)) return WAVE_POINTS;
        if ("Свой".equalsIgnoreCase(p)) return parseCustomFigure(TargetEspModule.getInstance().getCustomFigure());
        return STAR_POINTS;
    }

    private List<FigurePoint> parseCustomFigure(String raw) {
        List<FigurePoint> pts = new ArrayList<>();
        if (raw == null || raw.isBlank()) return STAR_POINTS;
        for (String chunk : raw.split(";")) {
            String[] pair = chunk.trim().split(",");
            if (pair.length != 2) continue;
            try {
                pts.add(new FigurePoint(
                        MathHelper.clamp(Float.parseFloat(pair[0].trim()), -2f, 2f),
                        MathHelper.clamp(Float.parseFloat(pair[1].trim()), -2f, 2f)));
            } catch (NumberFormatException ignored) {}
        }
        return pts.size() >= 3 ? pts : STAR_POINTS;
    }

    private Color color(int index, float alpha) {
        int fa = MathHelper.clamp((int)(255f * alpha * showAnimation.getValue()), 0, 255);
        Color base = index % 2 == 0 ? UIColors.themeFlow(index, fa) : UIColors.themeFlowAlt(index, fa);
        return ColorUtil.interpolate(base, Color.WHITE, 0.12f);
    }

    private record FigurePoint(float x, float y) {}
}