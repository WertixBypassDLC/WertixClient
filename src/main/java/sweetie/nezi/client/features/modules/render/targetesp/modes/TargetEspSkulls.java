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
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspMode;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspModule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TargetEspSkulls extends TargetEspMode {
    private static final Identifier GLOW_TEXTURE = FileUtil.getImage("particles/glow");

    private static final SkullBox[] SKULL_CORE = {
            new SkullBox(-0.24f, 0.82f, -0.14f, 0.24f, 1.00f, 0.16f),
            new SkullBox(-0.54f, 0.60f, -0.20f, 0.54f, 0.82f, 0.26f),
            new SkullBox(-0.74f, 0.12f, -0.24f, 0.74f, 0.60f, 0.52f),
            new SkullBox(-0.60f, -0.20f, -0.20f, 0.60f, 0.12f, 0.26f),
            new SkullBox(-0.36f, -0.40f, -0.16f, 0.36f, -0.20f, 0.20f),
            new SkullBox(-0.62f, 0.42f, -0.30f, 0.62f, 0.56f, -0.16f),
            new SkullBox(-0.72f, 0.00f, -0.18f, -0.56f, 0.28f, 0.12f),
            new SkullBox( 0.56f, 0.00f, -0.18f, 0.72f, 0.28f, 0.12f),
    };
    private static final SkullBox[] SKULL_REAR = {
            new SkullBox(-0.48f, 0.22f, 0.48f, 0.48f, 0.76f, 0.76f),
            new SkullBox(-0.32f, -0.16f, 0.26f, 0.32f, 0.22f, 0.60f),
    };
    private static final SkullBox[] SKULL_JAW = {
            new SkullBox(-0.66f, -0.52f, -0.12f, -0.28f, -0.06f, 0.20f),
            new SkullBox( 0.28f, -0.52f, -0.12f, 0.66f, -0.06f, 0.20f),
            new SkullBox(-0.32f, -0.76f, -0.20f, 0.32f, -0.30f, 0.20f),
            new SkullBox(-0.18f, -0.80f, -0.24f, 0.18f, -0.68f, -0.14f),
    };
    private static final SkullBox[] SKULL_CAVITIES = {
            new SkullBox(-0.52f, 0.10f, -0.38f, -0.14f, 0.52f, -0.16f),
            new SkullBox( 0.14f, 0.10f, -0.38f, 0.52f, 0.52f, -0.16f),
            new SkullBox(-0.12f, -0.10f, -0.38f, 0.12f, 0.30f, -0.16f),
            new SkullBox(-0.16f, -0.22f, -0.32f, 0.16f, -0.10f, -0.18f),
    };
    private static final SkullBox[] SKULL_UPPER_TEETH = {
            new SkullBox(-0.26f, -0.32f, -0.24f, -0.18f, -0.22f, -0.16f),
            new SkullBox(-0.14f, -0.34f, -0.24f, -0.06f, -0.22f, -0.16f),
            new SkullBox(-0.02f, -0.34f, -0.24f, 0.06f, -0.22f, -0.16f),
            new SkullBox( 0.10f, -0.34f, -0.24f, 0.18f, -0.22f, -0.16f),
            new SkullBox( 0.22f, -0.32f, -0.24f, 0.30f, -0.22f, -0.16f),
    };
    private static final SkullBox[] SKULL_LOWER_TEETH = {
            new SkullBox(-0.24f, -0.40f, -0.22f, -0.16f, -0.32f, -0.14f),
            new SkullBox(-0.12f, -0.42f, -0.22f, -0.04f, -0.32f, -0.14f),
            new SkullBox( 0.00f, -0.42f, -0.22f, 0.08f, -0.32f, -0.14f),
            new SkullBox( 0.12f, -0.42f, -0.22f, 0.20f, -0.32f, -0.14f),
            new SkullBox( 0.24f, -0.40f, -0.22f, 0.32f, -0.32f, -0.14f),
    };
    private static final SkullBox[] SKULL_DETAIL = {
            new SkullBox(-0.06f, 0.14f, -0.36f, 0.06f, 0.34f, -0.28f),
            new SkullBox(-0.70f, 0.24f, -0.04f, -0.60f, 0.52f, 0.28f),
            new SkullBox( 0.60f, 0.24f, -0.04f, 0.70f, 0.52f, 0.28f),
    };

    private static final int[][] BOX_EDGES = {
            {0,1},{1,2},{2,3},{3,0},
            {4,5},{5,6},{6,7},{7,4},
            {0,4},{1,5},{2,6},{3,7}
    };

    private float orbitAngle = 0f;
    private float prevOrbitAngle = 0f;

    private final SmoothFloat sSize = new SmoothFloat(0.6f);
    private final SmoothFloat sDist = new SmoothFloat(1.0f);
    private final SmoothFloat sOpac = new SmoothFloat(0.35f);
    private final SmoothFloat sGlow = new SmoothFloat(0.6f);
    private final SmoothFloat sSpeed = new SmoothFloat(1.0f);

    @Override
    public void onUpdate() {
        sSize.update(TargetEspModule.getInstance().getSkullSize(), 0.15f);
        sDist.update(TargetEspModule.getInstance().getSkullOrbitDistance(), 0.15f);
        sOpac.update(TargetEspModule.getInstance().getSkullOpacity(), 0.15f);
        sGlow.update(TargetEspModule.getInstance().getGhostGlowIntensity(), 0.15f);
        sSpeed.update(TargetEspModule.getInstance().getSpeed(), 0.15f);

        prevOrbitAngle = orbitAngle;
        orbitAngle += 3.4f * sSpeed.get();
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
        SkullPreset preset = getSkullPreset();
        float appearance = alpha * alpha * (3.0f - 2.0f * alpha);
        float orbit = MathUtil.interpolate(prevOrbitAngle, orbitAngle, event.partialTicks()) * preset.speedMultiplier;
        float baseScale = sSize.get() * (0.22f + appearance * 0.86f) * (0.84f + sizeVal * 0.26f);
        float time = getStableTime();

        Vec3d center = new Vec3d(getTargetX(), getTargetY() + currentTarget.getHeight() * preset.centerHeight, getTargetZ());
        Vec3d renderPos = center.subtract(cameraPos);

        MatrixStack matrices = event.matrixStack();
        matrices.push();
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        List<SkullNode> orbitSkulls = buildOrbitNodes(center, preset, orbit, baseScale, time, appearance, partialTicks);

        drawSkullLinks(matrices, orbitSkulls, alpha * sOpac.get(), preset);

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        float haloPulse = 0.88f + (float) Math.sin(time * 3.1f) * 0.12f;
        drawGlowBillboard(matrices, camera, 0f, preset.mainLift, 0f,
                baseScale * (1.28f + preset.glowMultiplier * 0.30f) * haloPulse,
                neonColor(480, 0.04f + alpha * sOpac.get() * 0.10f));

        for (SkullNode node : orbitSkulls) {
            float eyePulse = 0.85f + (float) Math.sin(time * 4.8f + node.colorSeed * 0.03f) * 0.15f;
            drawGlowBillboard(matrices, camera, node.x, node.y, node.z,
                    node.scale * (2.2f + preset.glowMultiplier * 0.22f) * eyePulse,
                    neonColor(node.colorSeed + 210, 0.03f + alpha * sOpac.get() * 0.10f));
        }

        if (TargetEspModule.getInstance().isGhostGlowEnabled()) {
            float glowIntensity = sGlow.get();
            for (SkullNode node : orbitSkulls) {
                float ghostPulse = 0.6f + (float) Math.sin(time * 2.4f + node.colorSeed * 0.017f) * 0.4f;
                float ghostSize = node.scale * (3.2f + ghostPulse * 1.4f);
                float ghostAlpha = alpha * sOpac.get() * glowIntensity * ghostPulse * 0.18f;
                drawGlowBillboard(matrices, camera, node.x, node.y, node.z, ghostSize, neonColor(node.colorSeed + 60, ghostAlpha));
            }
        }

        for (SkullNode node : orbitSkulls) {
            drawEyeGlows(matrices, camera, node, time, alpha * sOpac.get());
        }

        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder faceBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (SkullNode node : orbitSkulls) buildSkullFaces(faceBuf, matrices, node, alpha * sOpac.get(), time);
        BufferRenderer.drawWithGlobalProgram(faceBuf.end());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(Math.max(1.0f, TargetEspModule.getInstance().getLineWidth()));

        BufferBuilder edgeBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (SkullNode node : orbitSkulls) buildSkullEdges(edgeBuf, matrices, node, alpha * sOpac.get(), time);
        BufferRenderer.drawWithGlobalProgram(edgeBuf.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private List<SkullNode> buildOrbitNodes(Vec3d center, SkullPreset preset, float orbit, float baseScale, float time, float appearance, float partialTicks) {
        List<SkullNode> list = new ArrayList<>(preset.count);
        float bodyRadius = Math.max(0.38f, currentTarget.getWidth() * 0.58f + preset.radius * sDist.get());
        Vec3d lookTarget = new Vec3d(getTargetX(), getTargetY() + currentTarget.getHeight() / 2, getTargetZ());

        float strikeProgress = currentTarget.hurtTime > 0 ? (currentTarget.hurtTime - partialTicks) / 10.0f : 0f;
        strikeProgress = MathHelper.clamp(strikeProgress, 0f, 1f);

        float blend = getRetargetBlend();
        float dx = (float) getTransitionDx();
        float dy = (float) getTransitionDy();
        float dz = (float) getTransitionDz();

        for (int i = 0; i < preset.count; i++) {
            float step = 360f / preset.count;
            float angle = orbit + step * i;
            float radians = (float) Math.toRadians(angle);
            float wobble = (float) Math.sin(time * 4.2f + i * 0.78f);
            float x, y, z;

            switch (preset.motionMode) {
                case 1 -> {
                    x = MathHelper.cos(radians) * bodyRadius * 0.94f;
                    z = MathHelper.sin(radians) * bodyRadius * 0.88f;
                    y = preset.verticalOffset + wobble * preset.verticalAmplitude;
                }
                case 2 -> {
                    x = MathHelper.cos(radians) * bodyRadius * 0.84f;
                    z = MathHelper.sin(radians) * bodyRadius * 0.84f;
                    y = preset.verticalOffset + (float) Math.sin(time * 1.9f + i * 1.1f) * preset.verticalAmplitude * 1.35f;
                }
                default -> {
                    x = MathHelper.cos(radians) * bodyRadius * 1.02f + (float) Math.sin(time * 2.1f + i) * 0.10f;
                    z = MathHelper.sin(radians) * bodyRadius * 1.02f + (float) Math.cos(time * 2.1f + i * 0.95f) * 0.10f;
                    y = preset.verticalOffset + wobble * preset.verticalAmplitude;
                }
            }

            float riseDelay = i * 0.12f;
            float riseProgress = MathHelper.clamp((appearance - riseDelay) / Math.max(0.26f, 1.0f - riseDelay), 0.0f, 1.0f);
            float riseEasing = 1.0f - (float) Math.pow(1.0f - riseProgress, 3.0f);
            float undergroundY = -(1.5f + currentTarget.getHeight() * (0.22f + i * 0.03f));

            float finalX = x, finalY = y, finalZ = z;
            x = MathHelper.lerp(riseEasing, finalX * 0.18f, finalX);
            y = MathHelper.lerp(riseEasing, undergroundY, finalY);
            z = MathHelper.lerp(riseEasing, finalZ * 0.18f, finalZ);

            if (blend > 0) {
                float spread = (float) i / preset.count;
                x += dx * blend * (1.0f - spread);
                y += dy * blend * (1.0f - spread);
                z += dz * blend * (1.0f - spread);
            }

            if (strikeProgress > 0f && i == 0) {
                float strikeEasing = (float) Math.sin(strikeProgress * Math.PI);
                x = MathHelper.lerp(strikeEasing, x, 0);
                y = MathHelper.lerp(strikeEasing, y, 0);
                z = MathHelper.lerp(strikeEasing, z, 0);
            }

            float scale = baseScale * preset.orbitScale * (0.84f + ((float) Math.sin(i * 1.37f + 0.6f) * 0.5f + 0.5f) * 0.26f) * (0.58f + riseEasing * 0.42f);
            Vec3d wp = center.add(x, y, z);

            float targetYaw, targetPitch;
            String pName = TargetEspModule.getInstance().getSkullPreset();
            if ("Орбита".equalsIgnoreCase(pName)) {
                targetYaw = time * 200f;
                targetPitch = 0f;
            } else if ("Вращение".equalsIgnoreCase(pName)) {
                targetYaw = (float) Math.sin(time * 3.0f + i) * 160f;
                targetPitch = (float) Math.cos(time * 3.0f + i) * 30f;
            } else {
                double diffX = lookTarget.x - wp.x;
                double diffY = lookTarget.y - wp.y;
                double diffZ = lookTarget.z - wp.z;
                targetYaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
                targetPitch = (float) -Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX*diffX + diffZ*diffZ)));
            }

            list.add(new SkullNode(x, y, z, scale, targetYaw, targetPitch, 0f, i * 47));
        }
        return list;
    }

    private void buildSkullFaces(BufferBuilder buf, MatrixStack matrices, SkullNode node, float alpha, float time) {
        matrices.push();
        matrices.translate(node.x, node.y, node.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(node.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(node.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(node.roll));
        matrices.scale(node.scale, node.scale, node.scale);
        Matrix4f m = matrices.peek().getPositionMatrix();

        float fillAlpha = alpha * 0.38f;
        float flicker = 1.0f + (float) Math.sin(time * 2.3f + node.colorSeed * 0.02f) * 0.04f;

        Color coreC = neonColor(node.colorSeed, fillAlpha * flicker);
        for (SkullBox b : SKULL_CORE) drawFaces(buf, m, b, faceBr(coreC, 1.00f), faceBr(coreC, 0.95f), faceBr(coreC, 0.70f), faceBr(coreC, 0.55f), faceBr(coreC, 0.40f));
        Color rearC = neonColor(node.colorSeed + 60, fillAlpha * 0.82f);
        for (SkullBox b : SKULL_REAR) drawFaces(buf, m, b, faceBr(rearC, 0.95f), faceBr(rearC, 0.88f), faceBr(rearC, 0.65f), faceBr(rearC, 0.52f), faceBr(rearC, 0.38f));
        Color jawC = neonColor(node.colorSeed + 24, fillAlpha * 0.92f);
        for (SkullBox b : SKULL_JAW) drawFaces(buf, m, b, faceBr(jawC, 0.90f), faceBr(jawC, 0.98f), faceBr(jawC, 0.76f), faceBr(jawC, 0.58f), faceBr(jawC, 0.42f));
        Color detailC = neonColor(node.colorSeed + 40, fillAlpha * 0.78f);
        for (SkullBox b : SKULL_DETAIL) drawFaces(buf, m, b, faceBr(detailC, 0.85f), faceBr(detailC, 0.90f), faceBr(detailC, 0.68f), faceBr(detailC, 0.50f), faceBr(detailC, 0.38f));

        int cavA = MathHelper.clamp((int)(255f * Math.min(alpha + 0.3f, 1.0f)), 0, 255);
        Color cavC = new Color(6, 4, 3, cavA);
        for (SkullBox b : SKULL_CAVITIES) drawFaces(buf, m, b, cavC, cavC, cavC, cavC, cavC);

        Color upperTeethC = faceBr(jawC, 1.2f);
        for (SkullBox b : SKULL_UPPER_TEETH) drawFaces(buf, m, b, upperTeethC, upperTeethC, upperTeethC, upperTeethC, upperTeethC);
        Color lowerTeethC = faceBr(jawC, 1.1f);
        for (SkullBox b : SKULL_LOWER_TEETH) drawFaces(buf, m, b, lowerTeethC, lowerTeethC, lowerTeethC, lowerTeethC, lowerTeethC);

        matrices.pop();
    }

    private void buildSkullEdges(BufferBuilder buf, MatrixStack matrices, SkullNode node, float alpha, float time) {
        matrices.push();
        matrices.translate(node.x, node.y, node.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(node.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(node.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(node.roll));
        matrices.scale(node.scale, node.scale, node.scale);
        Matrix4f m = matrices.peek().getPositionMatrix();

        float edgeAlpha = Math.min(alpha * 1.5f, 1.0f);
        float flicker = 1.0f + (float) Math.sin(time * 2.3f + node.colorSeed * 0.02f) * 0.04f;

        Color coreEdge = brighten(neonColor(node.colorSeed, edgeAlpha * flicker), 60);
        for (SkullBox b : SKULL_CORE) drawEdges(buf, m, b, coreEdge);
        Color rearEdge = brighten(neonColor(node.colorSeed + 60, edgeAlpha * 0.82f), 60);
        for (SkullBox b : SKULL_REAR) drawEdges(buf, m, b, rearEdge);
        Color jawEdge = brighten(neonColor(node.colorSeed + 24, edgeAlpha * 0.92f), 68);
        for (SkullBox b : SKULL_JAW) drawEdges(buf, m, b, jawEdge);
        Color detailEdge = brighten(neonColor(node.colorSeed + 40, edgeAlpha * 0.78f), 50);
        for (SkullBox b : SKULL_DETAIL) drawEdges(buf, m, b, detailEdge);
        Color cavEdge = brighten(neonColor(node.colorSeed + 30, edgeAlpha * 0.70f), 40);
        for (SkullBox b : SKULL_CAVITIES) drawEdges(buf, m, b, cavEdge);

        matrices.pop();
    }

    private void drawFaces(BufferBuilder buf, Matrix4f m, SkullBox b, Color top, Color front, Color side, Color back, Color bottom) {
        addQuad4(buf, m, b.minX,b.maxY,b.minZ, b.maxX,b.maxY,b.minZ, b.maxX,b.maxY,b.maxZ, b.minX,b.maxY,b.maxZ, top);
        addQuad4(buf, m, b.minX,b.minY,b.minZ, b.maxX,b.minY,b.minZ, b.maxX,b.maxY,b.minZ, b.minX,b.maxY,b.minZ, front);
        addQuad4(buf, m, b.minX,b.minY,b.maxZ, b.minX,b.maxY,b.maxZ, b.maxX,b.maxY,b.maxZ, b.maxX,b.minY,b.maxZ, back);
        addQuad4(buf, m, b.minX,b.minY,b.minZ, b.minX,b.maxY,b.minZ, b.minX,b.maxY,b.maxZ, b.minX,b.minY,b.maxZ, side);
        addQuad4(buf, m, b.maxX,b.minY,b.maxZ, b.maxX,b.maxY,b.maxZ, b.maxX,b.maxY,b.minZ, b.maxX,b.minY,b.minZ, side);
        addQuad4(buf, m, b.minX,b.minY,b.maxZ, b.maxX,b.minY,b.maxZ, b.maxX,b.minY,b.minZ, b.minX,b.minY,b.minZ, bottom);
    }

    private void drawEdges(BufferBuilder buf, Matrix4f m, SkullBox b, Color c) {
        float[][] v = {
                {b.minX,b.minY,b.minZ},{b.maxX,b.minY,b.minZ},
                {b.maxX,b.maxY,b.minZ},{b.minX,b.maxY,b.minZ},
                {b.minX,b.minY,b.maxZ},{b.maxX,b.minY,b.maxZ},
                {b.maxX,b.maxY,b.maxZ},{b.minX,b.maxY,b.maxZ}
        };
        for (int[] e : BOX_EDGES) {
            float[] a = v[e[0]], bb2 = v[e[1]];
            buf.vertex(m, a[0], a[1], a[2]).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
            buf.vertex(m, bb2[0], bb2[1], bb2[2]).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        }
    }

    private Color faceBr(Color base, float br) {
        return new Color(
                MathHelper.clamp((int)(base.getRed() * br), 0, 255),
                MathHelper.clamp((int)(base.getGreen() * br), 0, 255),
                MathHelper.clamp((int)(base.getBlue() * br), 0, 255),
                base.getAlpha());
    }

    private Color brighten(Color base, int add) {
        return new Color(
                MathHelper.clamp(base.getRed() + add, 0, 255),
                MathHelper.clamp(base.getGreen() + add, 0, 255),
                MathHelper.clamp(base.getBlue() + add, 0, 255),
                base.getAlpha());
    }

    private void drawEyeGlows(MatrixStack matrices, Camera camera, SkullNode node, float time, float alpha) {
        float eyeY = 0.34f * node.scale;
        float eyeOX = 0.26f * node.scale;
        float eyeZ = -0.42f * node.scale;
        float rad = (float) Math.toRadians(node.yaw);
        float cosY = MathHelper.cos(rad);
        float sinY = MathHelper.sin(rad);
        drawEyeBillboard(matrices, camera, node.x + (-eyeOX) * cosY, node.y + eyeY, node.z + (-eyeOX) * (-sinY) + eyeZ * cosY, node.scale, alpha, node.colorSeed, time, 0);
        drawEyeBillboard(matrices, camera, node.x + eyeOX * cosY, node.y + eyeY, node.z + eyeOX * (-sinY) + eyeZ * cosY, node.scale, alpha, node.colorSeed, time, 137);
    }

    private void drawEyeBillboard(MatrixStack matrices, Camera camera, float wx, float wy, float wz, float scale, float alpha, int colorSeed, float time, int seedOffset) {
        float pulse = 0.75f + (float) Math.sin(time * 5.2f + seedOffset * 0.04f) * 0.25f;
        float eyeSize = scale * 0.24f * pulse;
        Color eyeCol = neonColor(colorSeed + seedOffset, 0.28f + alpha * 0.24f);
        drawGlowBillboard(matrices, camera, wx, wy, wz, eyeSize, eyeCol);
    }

    private void drawGlowBillboard(MatrixStack matrices, Camera camera, float x, float y, float z, float size, Color color) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
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

    private void drawSkullLinks(MatrixStack matrices, List<SkullNode> nodes, float alpha, SkullPreset preset) {
        if (nodes.size() < 2) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.lineWidth(Math.max(1.0f, TargetEspModule.getInstance().getLineWidth() * 0.9f));
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (int i = 0; i < nodes.size(); i++) {
            SkullNode cur = nodes.get(i);
            SkullNode next = nodes.get((i + 1) % nodes.size());
            Color c1 = neonColor(cur.colorSeed + 120, 0.10f + alpha * preset.linkAlpha);
            Color c2 = neonColor(next.colorSeed + 148, 0.08f + alpha * (preset.linkAlpha * 0.88f));
            addLine(lines, matrix, cur.x, cur.y, cur.z, next.x, next.y, next.z, c1, c2);
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());
    }

    private void addQuad4(BufferBuilder buf, Matrix4f m, float x1,float y1,float z1, float x2,float y2,float z2, float x3,float y3,float z3, float x4,float y4,float z4, Color c) {
        buf.vertex(m,x1,y1,z1).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
        buf.vertex(m,x2,y2,z2).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
        buf.vertex(m,x3,y3,z3).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
        buf.vertex(m,x4,y4,z4).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
    }

    private void addLine(BufferBuilder buf, Matrix4f m, float x1,float y1,float z1, float x2,float y2,float z2, Color c1, Color c2) {
        buf.vertex(m,x1,y1,z1).color(c1.getRed(),c1.getGreen(),c1.getBlue(),c1.getAlpha());
        buf.vertex(m,x2,y2,z2).color(c2.getRed(),c2.getGreen(),c2.getBlue(),c2.getAlpha());
    }

    private Color neonColor(int seed, float alpha) {
        int fa = MathHelper.clamp((int)(255f * alpha), 0, 255);
        float wave = (float)(Math.sin(seed * 0.01745f) * 0.5f + 0.5f);
        String p = TargetEspModule.getInstance().getSkullPreset();
        int r, g, b;
        if ("Орбита".equalsIgnoreCase(p)) {
            r = MathHelper.clamp((int)(42 + wave * 30), 0, 255);
            g = MathHelper.clamp((int)(182 + wave * 45), 0, 255);
            b = MathHelper.clamp((int)(138 + wave * 28), 0, 255);
        } else if ("Вращение".equalsIgnoreCase(p)) {
            r = MathHelper.clamp((int)(76 + wave * 34), 0, 255);
            g = MathHelper.clamp((int)(212 + wave * 40), 0, 255);
            b = MathHelper.clamp((int)(118 + wave * 24), 0, 255);
        } else {
            r = MathHelper.clamp((int)(58 + wave * 22), 0, 255);
            g = MathHelper.clamp((int)(196 + wave * 40), 0, 255);
            b = MathHelper.clamp((int)(128 + wave * 24), 0, 255);
        }
        return new Color(r, g, b, fa);
    }

    private SkullPreset getSkullPreset() {
        String p = TargetEspModule.getInstance().getSkullPreset();
        if ("Орбита".equalsIgnoreCase(p))
            return new SkullPreset(3,0.50f,0.18f,0.00f,1.06f,0.96f,1.12f,0.46f,0.02f,0.31f,0.24f,2);
        if ("Вращение".equalsIgnoreCase(p))
            return new SkullPreset(3,0.56f,0.10f,-0.01f,1.46f,1.00f,1.28f,0.44f,0.01f,0.34f,0.20f,3);
        return new SkullPreset(3,0.44f,0.08f,0.01f,0.92f,0.94f,1.00f,0.45f,0.02f,0.30f,0.22f,1);
    }

    private record SkullNode(float x, float y, float z, float scale, float yaw, float pitch, float roll, int colorSeed) {}
    private record SkullBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}
    private record SkullPreset(int count, float radius, float verticalAmplitude, float verticalOffset, float speedMultiplier, float orbitScale, float glowMultiplier, float centerHeight, float mainLift, float mainScale, float linkAlpha, int motionMode) {}
}