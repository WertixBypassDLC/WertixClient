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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TargetEspThreeDimensional extends TargetEspMode {
    private static final Identifier GLOW_TEXTURE = FileUtil.getImage("particles/glow");

    // ── Figure presets ────────────────────────────────────────────────────────
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

    // ── Skull geometry - detailed skull with proper proportions ────────────────
    // Cranium (main skull body)
    private static final SkullBox[] SKULL_CORE = {
            // Top of head
            new SkullBox(-0.24f,  0.82f, -0.14f,  0.24f,  1.00f,  0.16f),
            // Upper cranium
            new SkullBox(-0.54f,  0.60f, -0.20f,  0.54f,  0.82f,  0.26f),
            // Main cranium
            new SkullBox(-0.74f,  0.12f, -0.24f,  0.74f,  0.60f,  0.52f),
            // Cheekbones
            new SkullBox(-0.60f, -0.20f, -0.20f,  0.60f,  0.12f,  0.26f),
            // Chin area
            new SkullBox(-0.36f, -0.40f, -0.16f,  0.36f, -0.20f,  0.20f),
            // Brow ridge
            new SkullBox(-0.62f,  0.42f, -0.30f,  0.62f,  0.56f,  -0.16f),
            // Zygomatic arch (cheekbone extensions)
            new SkullBox(-0.72f,  0.00f, -0.18f, -0.56f,  0.28f,  0.12f),
            new SkullBox( 0.56f,  0.00f, -0.18f,  0.72f,  0.28f,  0.12f),
    };

    // Back of the skull
    private static final SkullBox[] SKULL_REAR = {
            new SkullBox(-0.48f,  0.22f,  0.48f,  0.48f,  0.76f,  0.76f),
            new SkullBox(-0.32f, -0.16f,  0.26f,  0.32f,  0.22f,  0.60f),
    };

    // Jaw - lower mandible
    private static final SkullBox[] SKULL_JAW = {
            // Left jaw ramus (vertical part)
            new SkullBox(-0.66f, -0.52f, -0.12f, -0.28f, -0.06f,  0.20f),
            // Right jaw ramus
            new SkullBox( 0.28f, -0.52f, -0.12f,  0.66f, -0.06f,  0.20f),
            // Jaw body (horizontal part)
            new SkullBox(-0.32f, -0.76f, -0.20f,  0.32f, -0.30f,  0.20f),
            // Chin protrusion
            new SkullBox(-0.18f, -0.80f, -0.24f,  0.18f, -0.68f, -0.14f),
    };

    // Eye sockets, nose cavity
    private static final SkullBox[] SKULL_CAVITIES = {
            // Left eye socket
            new SkullBox(-0.52f,  0.10f, -0.38f, -0.14f,  0.52f, -0.16f),
            // Right eye socket
            new SkullBox( 0.14f,  0.10f, -0.38f,  0.52f,  0.52f, -0.16f),
            // Nasal cavity (upper triangle)
            new SkullBox(-0.12f, -0.10f, -0.38f,  0.12f,  0.30f, -0.16f),
            // Nasal cavity (lower wider)
            new SkullBox(-0.16f, -0.22f, -0.32f,  0.16f, -0.10f, -0.18f),
    };

    // Teeth - individual upper and lower teeth with proper spacing
    private static final SkullBox[] SKULL_UPPER_TEETH = {
            // Upper teeth - left to right, evenly spaced with gaps
            new SkullBox(-0.26f, -0.32f, -0.24f, -0.18f, -0.22f, -0.16f),
            new SkullBox(-0.14f, -0.34f, -0.24f, -0.06f, -0.22f, -0.16f),
            new SkullBox(-0.02f, -0.34f, -0.24f,  0.06f, -0.22f, -0.16f),
            new SkullBox( 0.10f, -0.34f, -0.24f,  0.18f, -0.22f, -0.16f),
            new SkullBox( 0.22f, -0.32f, -0.24f,  0.30f, -0.22f, -0.16f),
    };

    private static final SkullBox[] SKULL_LOWER_TEETH = {
            // Lower teeth - slightly offset from upper, with gaps
            new SkullBox(-0.24f, -0.40f, -0.22f, -0.16f, -0.32f, -0.14f),
            new SkullBox(-0.12f, -0.42f, -0.22f, -0.04f, -0.32f, -0.14f),
            new SkullBox( 0.00f, -0.42f, -0.22f,  0.08f, -0.32f, -0.14f),
            new SkullBox( 0.12f, -0.42f, -0.22f,  0.20f, -0.32f, -0.14f),
            new SkullBox( 0.24f, -0.40f, -0.22f,  0.32f, -0.32f, -0.14f),
    };

    // Nasal bridge and details
    private static final SkullBox[] SKULL_DETAIL = {
            // Nasal bone
            new SkullBox(-0.06f,  0.14f, -0.36f,  0.06f,  0.34f, -0.28f),
            // Temporal region left
            new SkullBox(-0.70f,  0.24f, -0.04f, -0.60f,  0.52f,  0.28f),
            // Temporal region right
            new SkullBox( 0.60f,  0.24f, -0.04f,  0.70f,  0.52f,  0.28f),
    };

    private static final SkullBox[] SKULL_SCANLINES = { };
    private static final SkullBox[] GLITCH_SLICES = { };

    // ── 12 рёбер куба (индексы в массиве 8 вершин) ───────────────────────────
    private static final int[][] BOX_EDGES = {
            {0,1},{1,2},{2,3},{3,0},
            {4,5},{5,6},{6,7},{7,4},
            {0,4},{1,5},{2,6},{3,7}
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private float orbitAngle     = 0f;
    private float prevOrbitAngle = 0f;
    private float spinAngle      = 0f;
    private float prevSpinAngle  = 0f;
    private long  nextGlitchTime  = 0;
    private float glitchIntensity = 0f;

    @Override
    public void onUpdate() {
        updateTarget();
        prevOrbitAngle = orbitAngle;
        prevSpinAngle  = spinAngle;
        orbitAngle += 3.4f;
        spinAngle  += 2.2f;
        long now = System.currentTimeMillis();
        if (now >= nextGlitchTime) {
            glitchIntensity = 0.6f + (float)(Math.random() * 0.4f);
            nextGlitchTime  = now + 1800L + (long)(Math.random() * 2400L);
        } else {
            glitchIntensity = Math.max(0f, glitchIntensity - 0.08f);
        }
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;
        float partialTicks = event.partialTicks();
        float alpha   = MathUtil.interpolate(prevShowAnimation, (float) showAnimation.getValue(), partialTicks);
        float sizeVal = MathUtil.interpolate(prevSizeAnimation, (float) sizeAnimation.getValue(), partialTicks);
        if (alpha <= 0.01f || sizeVal <= 0.01f) return;
        if (TargetEspModule.getInstance().isCrystalsStyle()) {
            renderCrystals(event, alpha, sizeVal);
        } else if (TargetEspModule.getInstance().isSkullsStyle()) {
            renderSkulls(event, alpha, sizeVal);
        } else {
            renderFigure(event, alpha, sizeVal);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SKULLS — semi-transparent faces + wireframe edges + ghost glow
    // ─────────────────────────────────────────────────────────────────────────
    private void renderSkulls(Render3DEvent.Render3DEventData event, float alpha, float sizeAnimationValue) {
        Camera  camera    = mc.gameRenderer.getCamera();
        Vec3d   cameraPos = camera.getPos();
        SkullPreset preset = getSkullPreset();
        float skullOpacity = TargetEspModule.getInstance().getSkullOpacity();
        float appearance = alpha * alpha * (3.0f - 2.0f * alpha);
        float orbit     = MathUtil.interpolate(prevOrbitAngle, orbitAngle, event.partialTicks()) * preset.speedMultiplier;
        float baseScale = TargetEspModule.getInstance().getSkullSize() * (0.22f + appearance * 0.86f) * (0.84f + sizeAnimationValue * 0.26f);
        float time      = System.currentTimeMillis() * 0.001f;

        Vec3d center    = new Vec3d(getTargetX(), getTargetY() + currentTarget.getHeight() * preset.centerHeight, getTargetZ());
        Vec3d renderPos = center.subtract(cameraPos);

        MatrixStack matrices = event.matrixStack();
        matrices.push();
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        List<SkullNode> orbitSkulls = buildOrbitNodes(center, preset, orbit, baseScale, time, appearance);

        // ── PASS 1: линии-связи между черепами ───────────────────────────────
        drawSkullLinks(matrices, orbitSkulls, alpha * skullOpacity, preset);

        // ── PASS 2: glow billboards (additive) ────────────────────────────────
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        float haloPulse = 0.88f + (float) Math.sin(time * 3.1f) * 0.12f;
        drawGlowBillboard(matrices, camera, 0f, preset.mainLift, 0f,
                baseScale * (1.28f + preset.glowMultiplier * 0.30f) * haloPulse,
                neonColor(480, 0.04f + alpha * skullOpacity * 0.10f));

        for (SkullNode node : orbitSkulls) {
            float eyePulse = 0.85f + (float) Math.sin(time * 4.8f + node.colorSeed * 0.03f) * 0.15f;
            drawGlowBillboard(matrices, camera, node.x, node.y, node.z,
                    node.scale * (2.2f + preset.glowMultiplier * 0.22f) * eyePulse,
                    neonColor(node.colorSeed + 210, 0.03f + alpha * skullOpacity * 0.10f));
            for (int p = 0; p < 5; p++) {
                float seed   = p * 1.618f + node.colorSeed * 0.01f;
                float orbitR = node.scale * (1.4f + (float)(Math.sin(seed) * 0.5f + 0.5f) * 0.8f);
                float pAng   = time * (1.2f + (float)(Math.cos(seed * 0.7f)) * 0.6f) + seed * 2.09f;
                drawGlowBillboard(matrices, camera,
                        node.x + MathHelper.cos(pAng) * orbitR,
                        node.y + (float) Math.sin(time * 1.8f + seed) * node.scale * 0.7f,
                        node.z + MathHelper.sin(pAng) * orbitR,
                        baseScale * (0.04f + (float)(Math.sin(time * 3f + seed) * 0.5f + 0.5f) * 0.06f),
                        neonColor(node.colorSeed + p * 31, 0.06f + alpha * skullOpacity * 0.12f));
            }
        }

        // ── PASS 2.5: Ghost glow effect (additive) ───────────────────────────
        if (TargetEspModule.getInstance().isGhostGlowEnabled()) {
            float glowIntensity = TargetEspModule.getInstance().getGhostGlowIntensity();
            for (SkullNode node : orbitSkulls) {
                float ghostPulse = 0.6f + (float) Math.sin(time * 2.4f + node.colorSeed * 0.017f) * 0.4f;
                float ghostSize = node.scale * (3.2f + ghostPulse * 1.4f);
                float ghostAlpha = alpha * skullOpacity * glowIntensity * ghostPulse * 0.18f;
                // Main ghost aura
                drawGlowBillboard(matrices, camera, node.x, node.y, node.z,
                        ghostSize, neonColor(node.colorSeed + 60, ghostAlpha));
                // Secondary outer glow ring
                float outerPulse = 0.5f + (float) Math.sin(time * 1.7f + node.colorSeed * 0.023f) * 0.5f;
                drawGlowBillboard(matrices, camera, node.x, node.y, node.z,
                        ghostSize * 1.6f, neonColor(node.colorSeed + 130, ghostAlpha * 0.4f * outerPulse));
                // Trailing wisps
                for (int w = 0; w < 3; w++) {
                    float wispAngle = time * (0.8f + w * 0.3f) + node.colorSeed * 0.01f + w * 2.094f;
                    float wispDist = node.scale * (1.2f + w * 0.4f);
                    float wispX = node.x + MathHelper.cos(wispAngle) * wispDist;
                    float wispY = node.y + (float) Math.sin(time * 1.5f + w * 1.2f + node.colorSeed * 0.008f) * node.scale * 0.5f;
                    float wispZ = node.z + MathHelper.sin(wispAngle) * wispDist;
                    float wispSize = node.scale * (0.5f + (float) Math.sin(time * 3f + w * 0.8f) * 0.3f);
                    drawGlowBillboard(matrices, camera, wispX, wispY, wispZ,
                            wispSize, neonColor(node.colorSeed + w * 80 + 40, ghostAlpha * 0.6f));
                }
            }
        }

        // ── PASS 3: свечение глазниц (additive) ───────────────────────────────
        for (SkullNode node : orbitSkulls) {
            drawEyeGlows(matrices, camera, node, time, alpha * skullOpacity);
        }

        // ── PASS 4а: полупрозрачные грани ──
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder faceBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (SkullNode node : orbitSkulls) buildSkullFaces(faceBuf, matrices, node, alpha * skullOpacity, time);
        BufferRenderer.drawWithGlobalProgram(faceBuf.end());

        // ── PASS 4б: яркие wireframe рёбра ────────────────────────────────────
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(Math.max(1.0f, TargetEspModule.getInstance().getLineWidth()));

        BufferBuilder edgeBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (SkullNode node : orbitSkulls) buildSkullEdges(edgeBuf, matrices, node, alpha * skullOpacity, time);
        BufferRenderer.drawWithGlobalProgram(edgeBuf.end());

        // ── PASS 5: глитч-слайсы (additive) ──────────────────────────────────
        if (glitchIntensity > 0.05f) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder glitchBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (SkullNode node : orbitSkulls) buildGlitchSlices(glitchBuf, matrices, node, alpha * skullOpacity, time);
            BufferRenderer.drawWithGlobalProgram(glitchBuf.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private List<SkullNode> buildOrbitNodes(Vec3d center,
                                            SkullPreset preset, float orbit,
                                            float baseScale, float time, float appearance) {
        List<SkullNode> list = new ArrayList<>(preset.count);
        float orbitDistance = TargetEspModule.getInstance().getSkullOrbitDistance();
        float bodyRadius = Math.max(0.38f, currentTarget.getWidth() * 0.58f + preset.radius * orbitDistance);
        Vec3d lookTarget = center.add(0.0, currentTarget.getHeight() * 0.08, 0.0);
        for (int i = 0; i < preset.count; i++) {
            float step    = 360f / preset.count;
            float angle   = orbit + step * i;
            float radians = (float) Math.toRadians(angle);
            float wobble  = (float) Math.sin(time * 4.2f + i * 0.78f);
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
            float undergroundY = -(0.88f + currentTarget.getHeight() * (0.22f + i * 0.03f));
            float finalX = x;
            float finalY = y;
            float finalZ = z;
            x = MathHelper.lerp(riseEasing, finalX * 0.18f, finalX);
            y = MathHelper.lerp(riseEasing, undergroundY, finalY);
            z = MathHelper.lerp(riseEasing, finalZ * 0.18f, finalZ);
            float scale = baseScale * preset.orbitScale * (0.84f + ((float) Math.sin(i * 1.37f + 0.6f) * 0.5f + 0.5f) * 0.26f) * (0.58f + riseEasing * 0.42f);
            Vec3d wp    = center.add(x, y, z);
            float yaw   = getSkullYawToPoint(wp, lookTarget) + (float) Math.sin(time * 1.7f + i * 0.74f) * (2.6f + preset.speedMultiplier * 0.5f);
            float pitch = MathHelper.clamp(getSkullPitchToPoint(wp, lookTarget) * 0.55f, -18f, 18f) + (float) Math.sin(time * 2.1f + i * 0.64f) * (1.4f + preset.speedMultiplier * 0.45f);
            float roll  = (float) Math.cos(time * 1.5f + i * 0.49f) * (1.2f + preset.glowMultiplier * 0.3f);
            list.add(new SkullNode(x, y, z, scale, yaw, pitch, roll, i * 47));
        }
        return list;
    }

    // ── PASS 4а: полупрозрачные грани ─
    private void buildSkullFaces(BufferBuilder buf, MatrixStack matrices,
                                 SkullNode node, float alpha, float time) {
        matrices.push();
        matrices.translate(node.x, node.y, node.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(node.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(node.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(node.roll));
        matrices.scale(node.scale, node.scale, node.scale);
        Matrix4f m = matrices.peek().getPositionMatrix();

        float fillAlpha = alpha * 0.38f;
        float flicker   = 1.0f + (float) Math.sin(time * 2.3f + node.colorSeed * 0.02f) * 0.04f;

        // CORE - main neon color
        Color coreC = neonColor(node.colorSeed, fillAlpha * flicker);
        for (SkullBox b : SKULL_CORE) {
            drawFaces(buf, m, b,
                    faceBr(coreC, 1.00f),
                    faceBr(coreC, 0.95f),
                    faceBr(coreC, 0.70f),
                    faceBr(coreC, 0.55f),
                    faceBr(coreC, 0.40f));
        }

        // REAR - back of skull, slightly darker
        Color rearC = neonColor(node.colorSeed + 60, fillAlpha * 0.82f);
        for (SkullBox b : SKULL_REAR) {
            drawFaces(buf, m, b,
                    faceBr(rearC, 0.95f),
                    faceBr(rearC, 0.88f),
                    faceBr(rearC, 0.65f),
                    faceBr(rearC, 0.52f),
                    faceBr(rearC, 0.38f));
        }

        // JAW
        Color jawC = neonColor(node.colorSeed + 24, fillAlpha * 0.92f);
        for (SkullBox b : SKULL_JAW) {
            drawFaces(buf, m, b,
                    faceBr(jawC, 0.90f),
                    faceBr(jawC, 0.98f),
                    faceBr(jawC, 0.76f),
                    faceBr(jawC, 0.58f),
                    faceBr(jawC, 0.42f));
        }

        // DETAIL
        Color detailC = neonColor(node.colorSeed + 40, fillAlpha * 0.78f);
        for (SkullBox b : SKULL_DETAIL) {
            drawFaces(buf, m, b,
                    faceBr(detailC, 0.85f),
                    faceBr(detailC, 0.90f),
                    faceBr(detailC, 0.68f),
                    faceBr(detailC, 0.50f),
                    faceBr(detailC, 0.38f));
        }

        // CAVITIES — dark eye sockets and nose
        int cavA = MathHelper.clamp((int)(255f * Math.min(alpha + 0.3f, 1.0f) * showAnimation.getValue()), 0, 255);
        Color cavC = new Color(6, 4, 3, cavA);
        for (SkullBox b : SKULL_CAVITIES) drawFaces(buf, m, b, cavC, cavC, cavC, cavC, cavC);

        // UPPER TEETH - bright, slightly ivory colored
        int teethA = MathHelper.clamp((int)(255f * Math.min(alpha * 0.78f + 0.12f, 1.0f) * showAnimation.getValue()), 0, 255);
        Color upperTeethC = new Color(210, 255, 225, teethA);
        for (SkullBox b : SKULL_UPPER_TEETH) drawFaces(buf, m, b, upperTeethC, upperTeethC, upperTeethC, upperTeethC, upperTeethC);

        // LOWER TEETH
        Color lowerTeethC = new Color(195, 245, 210, teethA);
        for (SkullBox b : SKULL_LOWER_TEETH) drawFaces(buf, m, b, lowerTeethC, lowerTeethC, lowerTeethC, lowerTeethC, lowerTeethC);

        // SCANLINES
        float scanA = (0.14f + alpha * 0.18f) * (0.85f + (float) Math.sin(time * 6.0f + node.colorSeed) * 0.15f) * 0.8f;
        Color scanC = neonColor(node.colorSeed + 200, scanA);
        for (SkullBox b : SKULL_SCANLINES) drawFaces(buf, m, b, scanC, scanC, scanC, scanC, scanC);

        matrices.pop();
    }

    // ── PASS 4б: wireframe рёбра ───
    private void buildSkullEdges(BufferBuilder buf, MatrixStack matrices,
                                 SkullNode node, float alpha, float time) {
        matrices.push();
        matrices.translate(node.x, node.y, node.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(node.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(node.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(node.roll));
        matrices.scale(node.scale, node.scale, node.scale);
        Matrix4f m = matrices.peek().getPositionMatrix();

        float edgeAlpha = Math.min(alpha * 1.5f, 1.0f);
        float flicker   = 1.0f + (float) Math.sin(time * 2.3f + node.colorSeed * 0.02f) * 0.04f;

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

        Color upperTeethEdge = brighten(neonColor(node.colorSeed + 200, edgeAlpha * 0.88f), 86);
        for (SkullBox b : SKULL_UPPER_TEETH) drawEdges(buf, m, b, upperTeethEdge);

        Color lowerTeethEdge = brighten(neonColor(node.colorSeed + 210, edgeAlpha * 0.84f), 80);
        for (SkullBox b : SKULL_LOWER_TEETH) drawEdges(buf, m, b, lowerTeethEdge);

        Color scanEdge = neonColor(node.colorSeed + 200, edgeAlpha * 0.55f);
        for (SkullBox b : SKULL_SCANLINES) drawEdges(buf, m, b, scanEdge);

        matrices.pop();
    }

    private void buildGlitchSlices(BufferBuilder buf, MatrixStack matrices,
                                   SkullNode node, float alpha, float time) {
        float shift = (float)(Math.sin(time * 47.0f + node.colorSeed) * glitchIntensity * node.scale * 0.3f);
        matrices.push();
        matrices.translate(node.x + shift, node.y, node.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(node.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(node.pitch));
        matrices.scale(node.scale, node.scale, node.scale);
        Matrix4f m = matrices.peek().getPositionMatrix();
        Color gc = neonColor(node.colorSeed + 88, alpha * glitchIntensity * 0.35f);
        for (SkullBox b : GLITCH_SLICES) drawFaces(buf, m, b, gc, gc, gc, gc, gc);
        matrices.pop();
    }

    // ── Рисует 6 граней куба (QUADS) ─────────────────────────────────────────
    private void drawFaces(BufferBuilder buf, Matrix4f m, SkullBox b,
                           Color top, Color front, Color side, Color back, Color bottom) {
        addQuad4(buf, m, b.minX,b.maxY,b.minZ, b.maxX,b.maxY,b.minZ, b.maxX,b.maxY,b.maxZ, b.minX,b.maxY,b.maxZ, top);
        addQuad4(buf, m, b.minX,b.minY,b.minZ, b.maxX,b.minY,b.minZ, b.maxX,b.maxY,b.minZ, b.minX,b.maxY,b.minZ, front);
        addQuad4(buf, m, b.minX,b.minY,b.maxZ, b.minX,b.maxY,b.maxZ, b.maxX,b.maxY,b.maxZ, b.maxX,b.minY,b.maxZ, back);
        addQuad4(buf, m, b.minX,b.minY,b.minZ, b.minX,b.maxY,b.minZ, b.minX,b.maxY,b.maxZ, b.minX,b.minY,b.maxZ, side);
        addQuad4(buf, m, b.maxX,b.minY,b.maxZ, b.maxX,b.maxY,b.maxZ, b.maxX,b.maxY,b.minZ, b.maxX,b.minY,b.minZ, side);
        addQuad4(buf, m, b.minX,b.minY,b.maxZ, b.maxX,b.minY,b.maxZ, b.maxX,b.minY,b.minZ, b.minX,b.minY,b.minZ, bottom);
    }

    // ── Рисует 12 рёбер куба (DEBUG_LINES) ───────────────────────────────────
    private void drawEdges(BufferBuilder buf, Matrix4f m, SkullBox b, Color c) {
        float[][] v = {
                {b.minX,b.minY,b.minZ},{b.maxX,b.minY,b.minZ},
                {b.maxX,b.maxY,b.minZ},{b.minX,b.maxY,b.minZ},
                {b.minX,b.minY,b.maxZ},{b.maxX,b.minY,b.maxZ},
                {b.maxX,b.maxY,b.maxZ},{b.minX,b.maxY,b.maxZ}
        };
        for (int[] e : BOX_EDGES) {
            float[] a = v[e[0]], bb2 = v[e[1]];
            buf.vertex(m, a[0],  a[1],  a[2]).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
            buf.vertex(m, bb2[0],bb2[1],bb2[2]).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
        }
    }

    // ── Color helpers ─────────────────────────────────────────────────────────
    private Color faceBr(Color base, float br) {
        return new Color(
                MathHelper.clamp((int)(base.getRed()   * br), 0, 255),
                MathHelper.clamp((int)(base.getGreen() * br), 0, 255),
                MathHelper.clamp((int)(base.getBlue()  * br), 0, 255),
                base.getAlpha());
    }

    private Color brighten(Color base, int add) {
        return new Color(
                MathHelper.clamp(base.getRed()   + add, 0, 255),
                MathHelper.clamp(base.getGreen() + add, 0, 255),
                MathHelper.clamp(base.getBlue()  + add, 0, 255),
                base.getAlpha());
    }

    // ── Eye glows ─────────────────────────────────────────────────────────────
    private void drawEyeGlows(MatrixStack matrices, Camera camera,
                              SkullNode node, float time, float alpha) {
        float eyeY  =  0.34f * node.scale;
        float eyeOX =  0.26f * node.scale;
        float eyeZ  = -0.42f * node.scale;
        float rad   = (float) Math.toRadians(node.yaw);
        float cosY  = MathHelper.cos(rad);
        float sinY  = MathHelper.sin(rad);
        drawEyeBillboard(matrices, camera,
                node.x + (-eyeOX) * cosY, node.y + eyeY,
                node.z + (-eyeOX) * (-sinY) + eyeZ * cosY,
                node.scale, alpha, node.colorSeed, time, 0);
        drawEyeBillboard(matrices, camera,
                node.x + eyeOX * cosY, node.y + eyeY,
                node.z + eyeOX * (-sinY) + eyeZ * cosY,
                node.scale, alpha, node.colorSeed, time, 137);
    }

    private void drawEyeBillboard(MatrixStack matrices, Camera camera,
                                  float wx, float wy, float wz,
                                  float scale, float alpha, int colorSeed,
                                  float time, int seedOffset) {
        float pulse   = 0.75f + (float) Math.sin(time * 5.2f + seedOffset * 0.04f) * 0.25f;
        float eyeSize = scale * 0.24f * pulse;
        Color eyeCol  = neonColor(colorSeed + seedOffset, 0.28f + alpha * 0.24f);
        matrices.push();
        matrices.translate(wx, wy, wz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        Matrix4f m   = matrices.peek().getPositionMatrix();
        float[] rgba = ColorUtil.normalize(eyeCol);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m, -eyeSize,  eyeSize, 0f).texture(0f,1f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        buf.vertex(m,  eyeSize,  eyeSize, 0f).texture(1f,1f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        buf.vertex(m,  eyeSize, -eyeSize, 0f).texture(1f,0f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        buf.vertex(m, -eyeSize, -eyeSize, 0f).texture(0f,0f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        matrices.pop();
    }

    // ── Skull links ───────────────────────────────────────────────────────────
    private void drawSkullLinks(MatrixStack matrices, List<SkullNode> nodes, float alpha, SkullPreset preset) {
        if (nodes.size() < 2) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.lineWidth(Math.max(1.0f, TargetEspModule.getInstance().getLineWidth() * 0.9f));
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (int i = 0; i < nodes.size(); i++) {
            SkullNode cur  = nodes.get(i);
            SkullNode next = nodes.get((i + 1) % nodes.size());
            Color c1 = neonColor(cur.colorSeed  + 120, 0.10f + alpha * preset.linkAlpha);
            Color c2 = neonColor(next.colorSeed + 148, 0.08f + alpha * (preset.linkAlpha * 0.88f));
            addLine(lines, matrix, cur.x, cur.y, cur.z, next.x, next.y, next.z, c1, c2);
            if (i % 2 == 0) {
                Color hub = neonColor(cur.colorSeed + 260, 0.04f + alpha * (preset.linkAlpha * 0.62f));
                addLine(lines, matrix, cur.x, cur.y, cur.z, 0f, preset.mainLift * 0.84f, 0f, c1, hub);
            }
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COLOUR
    // ─────────────────────────────────────────────────────────────────────────
    private Color neonColor(int seed, float alpha) {
        int   fa   = MathHelper.clamp((int)(255f * alpha * showAnimation.getValue()), 0, 255);
        float wave = (float)(Math.sin(seed * 0.01745f) * 0.5f + 0.5f);
        String p   = TargetEspModule.getInstance().getSkullPreset();
        int r, g, b;
        if (isOrbitPreset(p)) {
            r = MathHelper.clamp((int)( 42 + wave * 30), 0, 255);
            g = MathHelper.clamp((int)(182 + wave * 45), 0, 255);
            b = MathHelper.clamp((int)(138 + wave * 28), 0, 255);
        } else if (isSpinPreset(p)) {
            r = MathHelper.clamp((int)( 76 + wave * 34), 0, 255);
            g = MathHelper.clamp((int)(212 + wave * 40), 0, 255);
            b = MathHelper.clamp((int)(118 + wave * 24), 0, 255);
        } else {
            r = MathHelper.clamp((int)( 58 + wave * 22), 0, 255);
            g = MathHelper.clamp((int)(196 + wave * 40), 0, 255);
            b = MathHelper.clamp((int)(128 + wave * 24), 0, 255);
        }
        return new Color(r, g, b, fa);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRYSTALS
    // ─────────────────────────────────────────────────────────────────────────
    private void renderCrystals(Render3DEvent.Render3DEventData event, float alpha, float sizeAnimationValue) {
        Camera camera   = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d center    = new Vec3d(getTargetX(), getTargetY() + currentTarget.getHeight() * 0.5, getTargetZ());
        Vec3d renderPos = center.subtract(cameraPos);
        float orbit        = MathUtil.interpolate(prevOrbitAngle, orbitAngle, event.partialTicks());
        float entityHeight = currentTarget.getHeight();
        float halfWidth    = currentTarget.getWidth() * 0.5f;
        int   crystalCount = Math.max(8, TargetEspModule.getInstance().getCrystalCount());
        float crystalScale = TargetEspModule.getInstance().getCrystalSize();
        List<CrystalNode> nodes = new ArrayList<>(crystalCount);
        for (int i = 0; i < crystalCount; i++) {
            float s1 = (float)(Math.sin(i * 1.71f + 0.35f) * 0.5f + 0.5f);
            float s2 = (float)(Math.cos(i * 2.11f + 0.90f) * 0.5f + 0.5f);
            float s3 = (float)(Math.sin(i * 2.93f + 1.40f) * 0.5f + 0.5f);
            float angle  = orbit + i * (360f / crystalCount) + s1 * 14f;
            float radius = Math.max(0.34f, halfWidth + 0.28f + s3 * 0.18f + sizeAnimationValue * 0.04f);
            nodes.add(new CrystalNode(
                    radius * MathHelper.cos((float) Math.toRadians(angle)),
                    (s2 - 0.5f) * entityHeight * 1.12f,
                    radius * MathHelper.sin((float) Math.toRadians(angle)),
                    (0.11f + sizeAnimationValue * 0.12f) * crystalScale * (0.82f + s3 * 0.48f),
                    angle, i * 37));
        }
        MatrixStack matrices = event.matrixStack();
        matrices.push();
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        drawCrystalLines(matrices, nodes, alpha);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        for (CrystalNode node : nodes)
            drawGlowBillboard(matrices, camera, node.x, node.y, node.z, node.scale * 4.1f, color(node.colorSeed, 0.24f + alpha * 0.26f));
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder tris = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (CrystalNode node : nodes) drawCrystal(tris, matrices, node, alpha);
        BufferRenderer.drawWithGlobalProgram(tris.end());
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void drawCrystalLines(MatrixStack matrices, List<CrystalNode> nodes, float alpha) {
        if (nodes.size() < 2) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.lineWidth(TargetEspModule.getInstance().getLineWidth());
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (int i = 0; i < nodes.size(); i++) {
            CrystalNode cur  = nodes.get(i);
            CrystalNode next = nodes.get((i + 1) % nodes.size());
            Color cc = color(cur.colorSeed,  0.28f + alpha * 0.28f);
            Color nc = color(next.colorSeed, 0.18f + alpha * 0.24f);
            addLine(lines, matrix, cur.x, cur.y, cur.z, next.x, next.y, next.z, cc, nc);
            if (i % 4 == 0)
                addLine(lines, matrix, cur.x, cur.y, cur.z, 0f, cur.y * 0.15f, 0f, cc, color(cur.colorSeed + 25, 0.10f + alpha * 0.16f));
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());
    }

    private void drawCrystal(BufferBuilder buffer, MatrixStack matrices, CrystalNode node, float alpha) {
        matrices.push();
        matrices.translate(node.x, node.y, node.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-node.angle + 90f));
        matrices.scale(node.scale, node.scale, node.scale);
        Matrix4f m  = matrices.peek().getPositionMatrix();
        Color base  = color(node.colorSeed, 0.36f + alpha * 0.44f);
        Color light = ColorUtil.interpolate(Color.WHITE, base, 0.22f);
        Color dark  = ColorUtil.interpolate(base, Color.BLACK, 0.42f);
        float w = 0.55f, h = 1.08f;
        addTri(buffer,m,0,0,h,-w,0,0,0,w,0,light); addTri(buffer,m,0,0,h,0,w,0,w,0,0,light);
        addTri(buffer,m,0,0,h,w,0,0,0,-w,0,base);  addTri(buffer,m,0,0,h,0,-w,0,-w,0,0,base);
        addTri(buffer,m,0,0,-h,0,w,0,-w,0,0,dark);  addTri(buffer,m,0,0,-h,w,0,0,0,w,0,dark);
        addTri(buffer,m,0,0,-h,0,-w,0,w,0,0,dark);  addTri(buffer,m,0,0,-h,-w,0,0,0,-w,0,dark);
        matrices.pop();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIGURE
    // ─────────────────────────────────────────────────────────────────────────
    private void renderFigure(Render3DEvent.Render3DEventData event, float alpha, float sizeAnimationValue) {
        Camera camera   = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d center    = new Vec3d(getTargetX(), getTargetY() + currentTarget.getHeight() * 0.58, getTargetZ());
        Vec3d renderPos = center.subtract(cameraPos);
        float figureSize  = TargetEspModule.getInstance().getFigureSize()  * (0.28f + sizeAnimationValue * 0.52f);
        float figureDepth = TargetEspModule.getInstance().getFigureDepth() * (0.35f + sizeAnimationValue * 0.65f);
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
            drawFlatGlow(matrices, pt.x * figureSize, pt.y * figureSize,  figureDepth, figureSize * 0.28f * pulse, color(i * 57,      0.18f + alpha * 0.32f));
            drawFlatGlow(matrices, pt.x * figureSize, pt.y * figureSize, -figureDepth, figureSize * 0.18f * pulse, color(i * 57 + 90, 0.10f + alpha * 0.24f));
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void drawFigureLines(MatrixStack matrices, List<FigurePoint> points,
                                 float figureSize, float figureDepth, float alpha) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.lineWidth(TargetEspModule.getInstance().getLineWidth());
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (int i = 0; i < points.size(); i++) {
            FigurePoint cur  = points.get(i);
            FigurePoint next = points.get((i + 1) % points.size());
            float x1 = cur.x  * figureSize, y1 = cur.y  * figureSize;
            float x2 = next.x * figureSize, y2 = next.y * figureSize;
            addLine(lines, matrix, x1,y1, figureDepth, x2,y2, figureDepth, color(i*61,      0.26f+alpha*0.34f), color(i*61+35, 0.22f+alpha*0.28f));
            addLine(lines, matrix, x1,y1,-figureDepth, x2,y2,-figureDepth, color(i*61+120,  0.14f+alpha*0.22f), color(i*61+155,0.12f+alpha*0.20f));
            addLine(lines, matrix, x1,y1,-figureDepth, x1,y1, figureDepth, color(i*61+120,  0.14f+alpha*0.22f), color(i*61,    0.26f+alpha*0.34f));
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHARED HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void drawGlowBillboard(MatrixStack matrices, Camera camera,
                                   float x, float y, float z, float size, Color color) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        Matrix4f m   = matrices.peek().getPositionMatrix();
        float[] rgba = ColorUtil.normalize(color);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m,-size, size,0f).texture(0f,1f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        buf.vertex(m, size, size,0f).texture(1f,1f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        buf.vertex(m, size,-size,0f).texture(1f,0f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        buf.vertex(m,-size,-size,0f).texture(0f,0f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        matrices.pop();
    }

    private void drawFlatGlow(MatrixStack matrices, float x, float y, float z, float size, Color color) {
        matrices.push();
        matrices.translate(x, y, z);
        Matrix4f m   = matrices.peek().getPositionMatrix();
        float[] rgba = ColorUtil.normalize(color);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(m,-size, size,0f).texture(0f,1f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        buf.vertex(m, size, size,0f).texture(1f,1f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        buf.vertex(m, size,-size,0f).texture(1f,0f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        buf.vertex(m,-size,-size,0f).texture(0f,0f).color(rgba[0],rgba[1],rgba[2],rgba[3]);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        matrices.pop();
    }

    private void addQuad4(BufferBuilder buf, Matrix4f m,
                          float x1,float y1,float z1, float x2,float y2,float z2,
                          float x3,float y3,float z3, float x4,float y4,float z4, Color c) {
        buf.vertex(m,x1,y1,z1).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
        buf.vertex(m,x2,y2,z2).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
        buf.vertex(m,x3,y3,z3).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
        buf.vertex(m,x4,y4,z4).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
    }

    private void addLine(BufferBuilder buf, Matrix4f m,
                         float x1,float y1,float z1, float x2,float y2,float z2, Color c1, Color c2) {
        buf.vertex(m,x1,y1,z1).color(c1.getRed(),c1.getGreen(),c1.getBlue(),c1.getAlpha());
        buf.vertex(m,x2,y2,z2).color(c2.getRed(),c2.getGreen(),c2.getBlue(),c2.getAlpha());
    }

    private void addTri(BufferBuilder buf, Matrix4f m,
                        float x1,float y1,float z1, float x2,float y2,float z2,
                        float x3,float y3,float z3, Color c) {
        buf.vertex(m,x1,y1,z1).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
        buf.vertex(m,x2,y2,z2).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
        buf.vertex(m,x3,y3,z3).color(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
    }

    private float getSkullYawToPoint(Vec3d skullPos, Vec3d lookPos) {
        return (float) Math.toDegrees(Math.atan2(lookPos.x - skullPos.x, lookPos.z - skullPos.z));
    }

    private float getSkullPitchToPoint(Vec3d skullPos, Vec3d lookPos) {
        double dx = lookPos.x - skullPos.x;
        double dy = lookPos.y - skullPos.y;
        double dz = lookPos.z - skullPos.z;
        return (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz)));
    }

    private List<FigurePoint> getFigurePoints() {
        String p = TargetEspModule.getInstance().getFigurePreset();
        if ("Diamond".equalsIgnoreCase(p))  return DIAMOND_POINTS;
        if ("Triangle".equalsIgnoreCase(p)) return TRIANGLE_POINTS;
        if ("Wave".equalsIgnoreCase(p))     return WAVE_POINTS;
        if ("Custom".equalsIgnoreCase(p))   return parseCustomFigure(TargetEspModule.getInstance().getCustomFigure());
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

    private SkullPreset getSkullPreset() {
        String p = TargetEspModule.getInstance().getSkullPreset();
        if (isOrbitPreset(p))
            return new SkullPreset(3,0.50f,0.18f,0.00f,1.06f,0.96f,1.12f,0.46f,0.02f,0.31f,0.24f,2);
        if (isSpinPreset(p))
            return new SkullPreset(3,0.56f,0.10f,-0.01f,1.46f,1.00f,1.28f,0.44f,0.01f,0.34f,0.20f,3);
        return new SkullPreset(3,0.44f,0.08f,0.01f,0.92f,0.94f,1.00f,0.45f,0.02f,0.30f,0.22f,1);
    }

    private boolean isOrbitPreset(String preset) {
        return "Орбита".equalsIgnoreCase(preset) || "Phantom Orbit".equalsIgnoreCase(preset);
    }

    private boolean isSpinPreset(String preset) {
        return "Вращение".equalsIgnoreCase(preset) || "Hell Halo".equalsIgnoreCase(preset);
    }

    // ── Records ───────────────────────────────────────────────────────────────
    private record CrystalNode(float x, float y, float z, float scale, float angle, int colorSeed) {}
    private record SkullNode  (float x, float y, float z, float scale, float yaw, float pitch, float roll, int colorSeed) {}
    private record SkullBox   (float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}
    private record FigurePoint (float x, float y) {}
    private record SkullPreset (int count, float radius, float verticalAmplitude, float verticalOffset,
                                float speedMultiplier, float orbitScale, float glowMultiplier, float centerHeight,
                                float mainLift, float mainScale, float linkAlpha, int motionMode) {}
}
