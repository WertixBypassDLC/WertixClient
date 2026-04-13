package sweetie.nezi.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspMode;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspModule;

public class TargetEspGarland extends TargetEspMode {
    private static final Identifier ESP_BLOOM_TEX = FileUtil.getImage("particles/glow");

    private final SmoothFloat smoothSpeed = new SmoothFloat(1.0f);
    private final SmoothFloat smoothLine = new SmoothFloat(2.0f);

    @Override
    public void onUpdate() {
        smoothSpeed.update(TargetEspModule.getInstance().getSpeed(), 0.15f);
        smoothLine.update(TargetEspModule.getInstance().getLineWidth(), 0.15f);
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;
        float partialTicks = event.partialTicks();
        float alpha = MathUtil.interpolate(prevShowAnimation, (float) showAnimation.getValue(), partialTicks);
        float sizeVal = MathUtil.interpolate(prevSizeAnimation, (float) sizeAnimation.getValue(), partialTicks);
        if (alpha <= 0.001f || sizeVal <= 0.001f) return;

        MatrixStack ms = event.matrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        float sp = smoothSpeed.get();
        float anim = alpha * sizeVal;

        float height = currentTarget.getHeight();
        float radius = currentTarget.getWidth() * 1.2f;
        float period = 4000f / Math.max(0.05f, sp);
        float time = (System.currentTimeMillis() % (long) period) / period;
        float offset = time * 360f;

        ms.push();
        ms.translate(getTargetX() - cameraPos.x, getTargetY() - cameraPos.y, getTargetZ() - cameraPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(Math.max(1.0f, smoothLine.get()));
        BufferBuilder wire = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        int wireColor = setAlpha(0x00B3B0B, (int)(anim * 255));
        Matrix4f pm = ms.peek().getPositionMatrix();

        int lightsCount = 30;
        int spirals = 3;
        for (int i = 0; i <= lightsCount; i++) {
            float progress = (float) i / (float) lightsCount;
            float angle = (float) Math.toRadians(offset + (progress * 360f * spirals));
            float currentRadius = radius * (1.0f - (progress * 0.6f));
            float px = (float) Math.cos(angle) * currentRadius;
            float pz = (float) Math.sin(angle) * currentRadius;
            float py = progress * height;
            wire.vertex(pm, px, py, pz).color(wireColor);
        }
        BufferRenderer.drawWithGlobalProgram(wire.end());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        BufferBuilder bulbs = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int i = 0; i <= lightsCount; i++) {
            float progress = (float) i / (float) lightsCount;
            float angle = (float) Math.toRadians(offset + (progress * 360f * spirals));
            float currentRadius = radius * (1.0f - (progress * 0.6f));
            float px = (float) Math.cos(angle) * currentRadius;
            float pz = (float) Math.sin(angle) * currentRadius;
            float py = progress * height;

            float size = 0.15f * anim;
            float twinkle = (float) Math.sin(((System.currentTimeMillis() / 100.0) * sp) + i) * 0.2f + 0.8f;
            int festive = (i % 4 == 0) ? 0xFFFF0000 : (i % 4 == 1) ? 0xFFFFD700 : (i % 4 == 2) ? 0xFF00FF00 : 0xFF00BFFF;
            int color = setAlpha(festive, (int)(anim * twinkle * 255));
            drawBillboard(bulbs, ms, px, py, pz, size, camera.getYaw(), camera.getPitch(), color);
        }
        BufferRenderer.drawWithGlobalProgram(bulbs.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        ms.pop();
    }

    private void drawBillboard(BufferBuilder buffer, MatrixStack ms, float x, float y, float z, float scale, float yaw, float pitch, int color) {
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        Matrix4f m = ms.peek().getPositionMatrix();
        buffer.vertex(m, -scale, -scale, 0).texture(0, 0).color(color);
        buffer.vertex(m, -scale, scale, 0).texture(0, 1).color(color);
        buffer.vertex(m, scale, scale, 0).texture(1, 1).color(color);
        buffer.vertex(m, scale, -scale, 0).texture(1, 0).color(color);
        ms.pop();
    }
}