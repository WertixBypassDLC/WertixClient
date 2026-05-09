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
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspMode;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspModule;

public class TargetEspAtom extends TargetEspMode {
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

        float red = MathHelper.clamp((currentTarget.hurtTime - partialTicks) / 20f, 0f, 1f);
        float anim = alpha * sizeVal;
        float appearance = alpha * alpha * (3.0f - 2.0f * alpha);

        float time = getStableTime();
        int baseColor = TargetEspModule.getInstance().getCustomColor(red).getRGB();

        float blend = getRetargetBlend();

        ms.push();
        ms.translate(getTargetX() - cameraPos.x, getTargetY() + currentTarget.getHeight() / 2f - cameraPos.y, getTargetZ() - cameraPos.z);
        ms.scale(0.85f * anim, 0.85f * anim, 0.85f * anim);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();

        float radius = 1.0f;
        int segments = 160;

        // Гармоничное ускорение-замедление вращения
        float ringSpin = time * 78f + (blend * 800f);
        float ringScale = 1.0f + (1.0f - appearance) * 5.0f;

        ms.push();
        ms.scale(ringScale, ringScale, ringScale);

        for (int ring = 0; ring < 3; ring++) {
            ms.push();
            float wobble = (float) Math.sin(time * 1.35f + ring * 0.9f) * 7.5f;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ring * 60f + wobble));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ringSpin + ring * 120f));

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(Math.max(3.0f, smoothLine.get() * 2.0f));
            BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            Matrix4f matrix = ms.peek().getPositionMatrix();

            for (int i = 0; i <= segments; i++) {
                float ang = (float) (2 * Math.PI * i / segments);
                bb.vertex(matrix, (float) Math.cos(ang) * radius, 0, (float) Math.sin(ang) * radius).color(setAlpha(baseColor, (int)(255 * anim)));
            }
            BufferRenderer.drawWithGlobalProgram(bb.end());

            float speed = 2.15f + ring * 0.35f;
            for (int p = 0; p < 2; p++) {
                float base = (float) (2 * Math.PI * p / 2);
                float theta = time * speed + base + ring * 0.82f;
                float px = (float) Math.cos(theta) * radius;
                float pz = (float) Math.sin(theta) * radius;

                float pulse = 0.92f + 0.08f * (float) Math.sin(time * 6.6f + ring * 1.7f + p * 2.1f);
                float s = 0.1f * anim * pulse;

                ms.push();
                ms.translate(px, 0, pz);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 190f + ring * 77f + p * 101f));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(time * 160f + ring * 53f + p * 89f));

                drawSmallSphere(ms, s * 1.25f, setAlpha(baseColor, (int)(200 * anim)));
                drawSmallSphere(ms, s * 0.78f, setAlpha(baseColor, (int)(255 * anim)));

                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
                BufferBuilder glow = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                drawBillboard(glow, ms, 0, 0, 0, s * 3.5f, camera.getYaw(), camera.getPitch(), setAlpha(baseColor, (int)(200 * anim)));
                BufferRenderer.drawWithGlobalProgram(glow.end());
                ms.pop();
            }
            ms.pop();
        }
        ms.pop();

        float coreFade = appearance * appearance;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
        BufferBuilder core = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        drawBillboard(core, ms, 0, 0, 0, 0.55f * anim, camera.getYaw(), camera.getPitch(), setAlpha(baseColor, (int)(255 * coreFade)));
        drawBillboard(core, ms, 0, 0, 0, 0.90f * anim, camera.getYaw(), camera.getPitch(), setAlpha(baseColor, (int)(160 * coreFade)));
        drawBillboard(core, ms, 0, 0, 0, 0.25f * anim, camera.getYaw(), camera.getPitch(), setAlpha(0xFFFFFF, (int)(255 * coreFade)));
        BufferRenderer.drawWithGlobalProgram(core.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        ms.pop();
    }

    private void drawSmallSphere(MatrixStack ms, float size, int color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bb.vertex(m, -size, -size, 0).color(color);
        bb.vertex(m, -size, size, 0).color(color);
        bb.vertex(m, size, size, 0).color(color);
        bb.vertex(m, size, -size, 0).color(color);
        BufferRenderer.drawWithGlobalProgram(bb.end());
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