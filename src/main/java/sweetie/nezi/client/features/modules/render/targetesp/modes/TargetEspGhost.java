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

public class TargetEspGhost extends TargetEspMode {
    private static final Identifier ESP_BLOOM_TEX = FileUtil.getImage("particles/glow");

    private final SmoothFloat smoothCount = new SmoothFloat(40f);
    private final SmoothFloat smoothSize = new SmoothFloat(0.3f);
    private final SmoothFloat smoothSpeed = new SmoothFloat(1.0f);

    @Override
    public void onUpdate() {
        smoothCount.update(TargetEspModule.getInstance().getGhostCount(), 0.1f);
        smoothSize.update(TargetEspModule.getInstance().getGhostSize(), 0.15f);
        smoothSpeed.update(TargetEspModule.getInstance().getSpeed(), 0.15f);
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
        String traj = TargetEspModule.getInstance().getGhostTrajectory();

        float eased = 1.0f - (1.0f - alpha * sizeVal) * (1.0f - alpha * sizeVal) * (1.0f - alpha * sizeVal);
        float radius = Math.max(0.22f, currentTarget.getWidth() * 0.75f);
        float height = Math.max(0.35f, currentTarget.getHeight());
        double time = getStableTime() * 6.0;
        int baseColor = TargetEspModule.getInstance().getCustomColor(red).getRGB();

        float blend = getRetargetBlend();
        float dx = (float) getTransitionDx();
        float dy = (float) getTransitionDy();
        float dz = (float) getTransitionDz();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        ms.push();
        ms.translate(getTargetX() - cameraPos.x, getTargetY() - cameraPos.y, getTargetZ() - cameraPos.z);
        BufferBuilder quads = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        int count = Math.round(smoothCount.get());
        for (int j = 0; j < count; j++) {
            float k = (float) j / count;
            float fade = 1.0f - k;
            float a = eased * fade * 0.9f;

            float normX = 0, normY = 0, normZ = 0;

            if ("Двойная спираль".equals(traj)) {
                double tt = time - (double) j * (10.0 / count);
                normY = height * 0.55f + (float) Math.sin(tt) * 0.26f;
                normX = (float) Math.cos(tt) * radius;
                normZ = (float) Math.sin(tt) * radius;
            } else if ("Орбита".equals(traj)) {
                float r = radius * (0.8f + (float) Math.sin(j * 321.1) * 0.4f);
                float angle = (float) (time * (1.5 + Math.sin(j * 123.4) * 0.5) + k * Math.PI * 2);
                normY = height / 2f + (float) Math.sin(time * (1.0 + Math.cos(j) * 0.2) + k * Math.PI * 4) * 0.5f;
                normX = (float) Math.cos(angle) * r * 1.2f;
                normZ = (float) Math.sin(angle) * r * 1.2f;
            } else if ("Хаос".equals(traj)) {
                normX = (float) Math.sin(time * 1.3 + k * 10) * radius * 1.4f;
                normY = height / 2f + (float) Math.cos(time * 0.8 + k * 10) * height * 0.6f;
                normZ = (float) Math.sin(time * 1.7 + k * 10) * radius * 1.4f;
            }

            // Упругое вытягивание при смене цели
            if (blend > 0) {
                float spread = (float) Math.pow(j / (float) count, 1.5);
                normX += dx * blend * (1.0f - spread);
                normY += dy * blend * (1.0f - spread);
                normZ += dz * blend * (1.0f - spread);
            }

            float explodeRadius = radius * 3.0f;
            float explodeX = (float) Math.cos(j * Math.PI * 2 / count) * explodeRadius;
            float explodeZ = (float) Math.sin(j * Math.PI * 2 / count) * explodeRadius;
            float explodeY = height + (float) Math.sin(j) * 1.5f;

            float x1, y, z1;

            if (eased < 0.5f) {
                float t = eased * 2.0f;
                t = 1.0f - (1.0f - t) * (1.0f - t);
                x1 = MathHelper.lerp(t, 0, explodeX);
                y = MathHelper.lerp(t, height, explodeY);
                z1 = MathHelper.lerp(t, 0, explodeZ);
            } else {
                float t = (eased - 0.5f) * 2.0f;
                t = t * t;
                x1 = MathHelper.lerp(t, explodeX, normX);
                y = MathHelper.lerp(t, explodeY, normY);
                z1 = MathHelper.lerp(t, explodeZ, normZ);
            }

            float s = smoothSize.get() * (0.8f * fade + 0.2f) * eased;
            int c = setAlpha(baseColor, (int)(a * 255));

            drawBillboard(quads, ms, x1, y, z1, s, camera.getYaw(), camera.getPitch(), c);
            if ("Двойная спираль".equals(traj)) {
                drawBillboard(quads, ms, -x1, y, -z1, s, camera.getYaw(), camera.getPitch(), c);
            }
        }

        BufferRenderer.drawWithGlobalProgram(quads.end());
        ms.pop();

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
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