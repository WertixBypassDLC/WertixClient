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

import java.util.ArrayList;
import java.util.List;

public class TargetEspCrystals extends TargetEspMode {
    private static final Identifier ESP_BLOOM_TEX = FileUtil.getImage("particles/glow");
    private final List<Crystal> crystalList = new ArrayList<>();
    private float rotationAngle = 0;

    private final SmoothFloat smoothSize = new SmoothFloat(0.8f);
    private final SmoothFloat smoothCount = new SmoothFloat(7f);
    private final SmoothFloat smoothSpeed = new SmoothFloat(1.0f);

    @Override
    public void onUpdate() {
        smoothSize.update(TargetEspModule.getInstance().getCrystalSize(), 0.15f);
        smoothCount.update(TargetEspModule.getInstance().getCrystalCount(), 0.1f);
        smoothSpeed.update(TargetEspModule.getInstance().getSpeed(), 0.15f);

        int targetCount = Math.round(smoothCount.get());
        if (crystalList.size() != targetCount) {
            createCrystals(targetCount);
        }
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;
        float partialTicks = event.partialTicks();
        float alpha = MathUtil.interpolate(prevShowAnimation, (float) showAnimation.getValue(), partialTicks);
        float sizeVal = MathUtil.interpolate(prevSizeAnimation, (float) sizeAnimation.getValue(), partialTicks);
        if (alpha <= 0.001f || sizeVal <= 0.001f || crystalList.isEmpty()) return;

        MatrixStack ms = event.matrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        float red = MathHelper.clamp((currentTarget.hurtTime - partialTicks) / 20f, 0f, 1f);
        int baseColor = TargetEspModule.getInstance().getCustomColor(red).getRGB();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        rotationAngle = (rotationAngle + 0.5f * smoothSpeed.get()) % 360.0f;

        ms.push();
        ms.translate(getTargetX() - cameraPos.x, getTargetY() - cameraPos.y, getTargetZ() - cameraPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));

        float blend = getRetargetBlend();

        for (Crystal crystal : crystalList) {
            crystal.render(ms, alpha * sizeVal, baseColor, camera, smoothSpeed.get(), smoothSize.get(), blend);
        }
        ms.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    private void createCrystals(int count) {
        crystalList.clear();
        for (int i = 0; i < count; i++) {
            float angle = (float) (i * Math.PI * 2 / count);
            float radius = 0.55f + (i % 2 == 0 ? 0.15f : 0.0f);
            float y = 0.6f + (i % 3 == 0 ? 0.3f : 0.0f);
            Vec3d pos = new Vec3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Vec3d rot = new Vec3d(Math.random() * 360, Math.random() * 360, Math.random() * 360);
            crystalList.add(new Crystal(pos, rot));
        }
    }

    private class Crystal {
        private final Vec3d position;
        private final Vec3d rotation;
        private final float rotationSpeed = 0.8f + (float) (Math.random() * 1.5f);
        private final float bobbingOffset = (float) (Math.random() * Math.PI * 2);

        public Crystal(Vec3d position, Vec3d rotation) {
            this.position = position;
            this.rotation = rotation;
        }

        public void render(MatrixStack ms, float anim, int baseColor, Camera camera, float speedMul, float dynamicSize, float blend) {
            ms.push();
            float t = getStableTime() * 2.0f;
            float bobY = (float) Math.sin(t * 1.5f + bobbingOffset) * 0.15f;

            float scatterX = (float) Math.cos(t * 2.0f + rotation.x) * blend * 2.5f;
            float scatterY = (float) Math.sin(t * 2.5f + rotation.y) * blend * 2.5f;
            float scatterZ = (float) Math.sin(t * 3.0f + rotation.z) * blend * 2.5f;

            ms.translate(position.x + scatterX, position.y + bobY + scatterY, position.z + scatterZ);

            float size = dynamicSize * 0.05f;
            float pulsation = 1.0f + (float) Math.sin(t * 2.0f + bobbingOffset) * 0.15f;
            ms.scale(pulsation, pulsation, pulsation);

            float selfRotation = getStableTime() * 3.6f * rotationSpeed;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y + selfRotation));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(ms, baseColor, 0.2f, true, anim, size);

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(ms, baseColor, 0.3f, true, anim, size);
            drawCrystal(ms, baseColor, 0.8f, false, anim, size);

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawBloomSphere(ms, baseColor, anim, camera, size);

            ms.pop();
        }

        private void drawBloomSphere(MatrixStack ms, int baseColor, float anim, Camera camera, float size) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
            int bloomColor = setAlpha(baseColor, (int) (0.4f * 255 * anim));
            float bloomSize = size * 13.0f;

            for (int i = 0; i < 3; i++) {
                ms.push();
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 120f));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                Matrix4f matrix = ms.peek().getPositionMatrix();
                bb.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bb.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bb.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bb.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bb.end());
                ms.pop();
            }
        }

        private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled, float anim, float size) {
            BufferBuilder bb = Tessellator.getInstance().begin(
                    filled ? VertexFormat.DrawMode.QUADS : VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR
            );
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            float h = size * 1.8f;
            float w = size * 0.8f;
            int col = setAlpha(baseColor, (int)(alpha * 255 * anim));

            Vec3d[] eq = new Vec3d[4];
            for (int i = 0; i < 4; i++) {
                float angle = (float) (i * Math.PI / 2);
                eq[i] = new Vec3d(Math.cos(angle) * w, 0, Math.sin(angle) * w);
            }
            Vec3d top = new Vec3d(0, h, 0);
            Vec3d bot = new Vec3d(0, -h, 0);

            for (int i = 0; i < 4; i++) {
                Vec3d p1 = eq[i];
                Vec3d p2 = eq[(i + 1) % 4];
                if (filled) {
                    drawQuad(ms, bb, top, p1, p2, p2, col);
                    drawQuad(ms, bb, bot, p2, p1, p1, col);
                } else {
                    drawQuad(ms, bb, top, p1, p1, top, col);
                    drawQuad(ms, bb, p1, p2, p2, p1, col);
                    drawQuad(ms, bb, bot, p1, p1, bot, col);
                }
            }
            BufferRenderer.drawWithGlobalProgram(bb.end());
        }

        private void drawQuad(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, int color) {
            Matrix4f m = ms.peek().getPositionMatrix();
            bb.vertex(m, (float) v1.x, (float) v1.y, (float) v1.z).color(color);
            bb.vertex(m, (float) v2.x, (float) v2.y, (float) v2.z).color(color);
            bb.vertex(m, (float) v3.x, (float) v3.y, (float) v3.z).color(color);
            bb.vertex(m, (float) v4.x, (float) v4.y, (float) v4.z).color(color);
        }
    }
}