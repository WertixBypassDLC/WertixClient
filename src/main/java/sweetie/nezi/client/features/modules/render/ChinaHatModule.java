package sweetie.nezi.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ColorSetting;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.system.configs.FriendManager;

import java.awt.Color;

@ModuleRegister(name = "China Hat", category = Category.RENDER)
public class ChinaHatModule extends Module {
    @Getter
    private static final ChinaHatModule instance = new ChinaHatModule();

    private static final int SEGMENTS = 48;

    private final BooleanSetting renderOnOthers = new BooleanSetting("Others").value(false);
    private final ColorSetting customColor = new ColorSetting("Color").value(new Color(140, 255, 210, 220));

    public ChinaHatModule() {
        addSettings(renderOnOthers, customColor);
    }

    @Override
    public void onEvent() {
        EventListener render3D = Render3DEvent.getInstance().subscribe(new Listener<>(this::handleRender3D));
        addEvents(render3D);
    }

    private void handleRender3D(Render3DEvent.Render3DEventData event) {
        if (mc.world == null || mc.player == null || mc.getEntityRenderDispatcher().camera == null) {
            return;
        }

        MatrixStack matrices = event.matrixStack();
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldRender(player)) {
                continue;
            }

            renderHat(player, event.partialTicks(), matrices);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private boolean shouldRender(PlayerEntity player) {
        if (player == null || !player.isAlive() || player.isSpectator()) {
            return false;
        }

        if (player == mc.player) {
            return true;
        }

        return renderOnOthers.getValue() || FriendManager.getInstance().contains(player.getName().getString());
    }

    private void renderHat(PlayerEntity player, float partialTicks, MatrixStack matrices) {
        float renderYaw = MathHelper.lerpAngleDegrees(partialTicks, player.prevBodyYaw, player.bodyYaw);
        double x = MathHelper.lerp(partialTicks, player.prevX, player.getX());
        double y = MathHelper.lerp(partialTicks, player.prevY, player.getY());
        double z = MathHelper.lerp(partialTicks, player.prevZ, player.getZ());
        float radius = Math.max(0.34f, player.getWidth() * 0.92f);
        float topY = player.isSneaking() ? 0.28f : 0.34f;
        float baseY = player.isSneaking() ? player.getHeight() + 0.02f : player.getHeight() + 0.08f;

        Color baseColor = FriendManager.getInstance().contains(player.getName().getString())
                ? new Color(132, 229, 121, 220)
                : UIColors.primary(220);
        Color accent = ColorUtil.interpolate(baseColor, customColor.getValue(), 0.35f);
        int fillAlpha = Math.max(32, accent.getAlpha() / 3);

        matrices.push();
        matrices.translate(x, y + baseY, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-renderYaw));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        var fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SEGMENTS; i++) {
            float angle1 = (float) (Math.PI * 2.0 * i / SEGMENTS);
            float angle2 = (float) (Math.PI * 2.0 * (i + 1) / SEGMENTS);
            addVertex(fill, matrix, 0f, topY, 0f, accent.getRed(), accent.getGreen(), accent.getBlue(), fillAlpha);
            addVertex(fill, matrix, MathHelper.sin(angle1) * radius, 0f, MathHelper.cos(angle1) * radius, accent.getRed(), accent.getGreen(), accent.getBlue(), fillAlpha);
            addVertex(fill, matrix, MathHelper.sin(angle2) * radius, 0f, MathHelper.cos(angle2) * radius, accent.getRed(), accent.getGreen(), accent.getBlue(), fillAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(fill.end());

        var lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SEGMENTS; i++) {
            float angle1 = (float) (Math.PI * 2.0 * i / SEGMENTS);
            float angle2 = (float) (Math.PI * 2.0 * (i + 1) / SEGMENTS);
            float x1 = MathHelper.sin(angle1) * radius;
            float z1 = MathHelper.cos(angle1) * radius;
            float x2 = MathHelper.sin(angle2) * radius;
            float z2 = MathHelper.cos(angle2) * radius;

            addVertex(lines, matrix, x1, 0f, z1, baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 255);
            addVertex(lines, matrix, x2, 0f, z2, baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 255);

            if (i % 2 == 0) {
                addVertex(lines, matrix, 0f, topY, 0f, accent.getRed(), accent.getGreen(), accent.getBlue(), 210);
                addVertex(lines, matrix, x1, 0f, z1, accent.getRed(), accent.getGreen(), accent.getBlue(), 170);
            }
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());
        matrices.pop();
    }

    private void addVertex(net.minecraft.client.render.BufferBuilder buffer, Matrix4f matrix,
                           float x, float y, float z, int r, int g, int b, int a) {
        buffer.vertex(matrix, x, y, z).color(r, g, b, a);
    }
}
