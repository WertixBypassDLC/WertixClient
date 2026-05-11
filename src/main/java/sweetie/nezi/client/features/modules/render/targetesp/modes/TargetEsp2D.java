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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspMode;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspModule;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;

public class TargetEsp2D extends TargetEspMode {
    private static final Identifier CIRCLE_TEXTURE = FileUtil.getImage("circle/lean");
    private static final Identifier GLOW_TEXTURE = FileUtil.getImage("particles/glow");
    private static final Identifier SKULL_HEALTHY = FileUtil.getImage("targetesp/skull_state_0");
    private static final Identifier SKULL_MID = FileUtil.getImage("targetesp/skull_state_1");
    private static final Identifier SKULL_LOW = FileUtil.getImage("targetesp/skull_state_2");

    private float angle = 0f;
    private float prevAngle = 0f;

    @Override
    public void onModeSelected() {
        super.onModeSelected();
        prevAngle = angle;
    }

    @Override
    public void onUpdate() {
        prevAngle = angle;
        angle += 3.2f * TargetEspModule.getInstance().getSpeed();
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) {
            return;
        }

        float partialTicks = event.partialTicks();
        float alpha = MathUtil.interpolate(prevShowAnimation, (float) showAnimation.getValue(), partialTicks);
        float sizeValue = MathUtil.interpolate(prevSizeAnimation, (float) sizeAnimation.getValue(), partialTicks);
        if (alpha <= 0.01f || sizeValue <= 0.01f) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        if ("Skull".equalsIgnoreCase(TargetEspModule.getInstance().get2DStyle())) {
            renderSkull(event, alpha, sizeValue);
        } else {
            renderCircle(event, alpha, sizeValue);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderCircle(Render3DEvent.Render3DEventData event, float alpha, float sizeValue) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }

        MatrixStack matrices = event.matrixStack();
        double renderX = getTargetX() - camera.getPos().x;
        double renderY = getTargetY() + currentTarget.getHeight() * 0.55 - camera.getPos().y;
        double renderZ = getTargetZ() - camera.getPos().z;

        float time = System.currentTimeMillis() * 0.001f;
        float spin = MathUtil.interpolate(prevAngle, angle, event.partialTicks());
        float bob = (float) Math.sin(time * 2.35f) * 0.08f;
        float size = 0.72f + sizeValue * 0.55f;

        Color ringColor = ColorUtil.setAlpha(UIColors.primary(255), (int) (alpha * 190f));
        Color glowColor = ColorUtil.setAlpha(UIColors.secondary(255), (int) (alpha * 120f));

        matrices.push();
        matrices.translate(renderX, renderY + bob, renderZ);
        faceCamera(matrices, camera);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));

        drawTexturedQuad(matrices, GLOW_TEXTURE, size * 1.28f, glowColor, true);
        drawTexturedQuad(matrices, CIRCLE_TEXTURE, size, ringColor, false);

        matrices.pop();
    }

    private void renderSkull(Render3DEvent.Render3DEventData event, float alpha, float sizeValue) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }

        float health = Math.max(0.0f, currentTarget.getHealth());
        float maxHealth = Math.max(1.0f, currentTarget.getMaxHealth());
        float hpPercent = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);
        int hurtTicks = currentTarget.hurtTime;
        boolean hurt = hurtTicks > 0;

        double renderX = getTargetX() - camera.getPos().x;
        double renderY = getTargetY() + currentTarget.getHeight() * 0.5 - camera.getPos().y;
        double renderZ = getTargetZ() - camera.getPos().z;

        float lowHpShake = (1f - hpPercent) * 0.08f;
        float hurtShake = hurt ? (hurtTicks / 8f) * 0.20f : 0f;
        float totalShake = lowHpShake + hurtShake;
        if (totalShake > 0.001f) {
            renderX += (ThreadLocalRandom.current().nextDouble() - 0.5) * totalShake;
            renderY += (ThreadLocalRandom.current().nextDouble() - 0.5) * totalShake;
            renderZ += (ThreadLocalRandom.current().nextDouble() - 0.5) * totalShake;
        }

        Color baseColor = TargetEspModule.getInstance().getCustomColor(Math.max(0f, 1f - hpPercent));
        Color redColor = new Color(255, 50, 50);
        float blend = 0f;
        if (hurt) {
            blend = MathHelper.clamp((float) Math.sin(hurtTicks * (Math.PI / 10D)), 0f, 1f);
        } else if (hpPercent < 0.3f) {
            blend = (1f - hpPercent / 0.3f) * 0.4f;
        }

        Color skullColor = blend(baseColor, redColor, blend);
        skullColor = ColorUtil.setAlpha(skullColor, (int) (alpha * 255f));

        float spin = MathUtil.interpolate(prevAngle, angle, event.partialTicks());
        float size = (0.50f + sizeValue * 0.32f) * (0.96f + (1f - hpPercent) * 0.12f);
        float glowSize = size * (1.55f + (1f - hpPercent) * 0.45f);
        Color glowColor = ColorUtil.setAlpha(skullColor, (int) (alpha * (100f + (1f - hpPercent) * 60f)));

        MatrixStack matrices = event.matrixStack();
        matrices.push();
        matrices.translate(renderX, renderY, renderZ);
        faceCamera(matrices, camera);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin * 0.15f));

        drawTexturedQuad(matrices, chooseSkullTexture(hpPercent), size, skullColor, false);
        drawTexturedQuad(matrices, GLOW_TEXTURE, glowSize, glowColor, true);

        matrices.pop();
    }

    private void faceCamera(MatrixStack matrices, Camera camera) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
    }

    private Identifier chooseSkullTexture(float hpPercent) {
        if (hpPercent > 0.6f) {
            return SKULL_HEALTHY;
        }
        if (hpPercent > 0.3f) {
            return SKULL_MID;
        }
        return SKULL_LOW;
    }

    private Color blend(Color start, Color end, float factor) {
        factor = MathHelper.clamp(factor, 0f, 1f);
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * factor);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * factor);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * factor);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * factor);
        return new Color(r, g, b, a);
    }

    private void drawTexturedQuad(MatrixStack matrices, Identifier texture, float size, Color color, boolean additive) {
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                additive ? GlStateManager.DstFactor.ONE : GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[] rgba = ColorUtil.normalize(color);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -size, size, 0f).texture(0f, 1f).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        buffer.vertex(matrix, size, size, 0f).texture(1f, 1f).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        buffer.vertex(matrix, size, -size, 0f).texture(1f, 0f).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        buffer.vertex(matrix, -size, -size, 0f).texture(0f, 0f).color(rgba[0], rgba[1], rgba[2], rgba[3]);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
