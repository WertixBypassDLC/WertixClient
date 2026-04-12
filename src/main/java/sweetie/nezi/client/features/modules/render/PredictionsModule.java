package sweetie.nezi.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector2f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ColorSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.player.PlayerUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Font;
import sweetie.nezi.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ModuleRegister(name = "Predictions", category = Category.RENDER)
public class PredictionsModule extends Module {
    @Getter private static final PredictionsModule instance = new PredictionsModule();

    private final MultiBooleanSetting render = new MultiBooleanSetting("Render").value(
            new BooleanSetting("Ender pearl").value(true),
            new BooleanSetting("Trident").value(false),
            new BooleanSetting("Arrow").value(false),
            new BooleanSetting("Potion").value(false)
    );
    private final BooleanSetting walls = new BooleanSetting("Through walls").value(true);
    private final BooleanSetting friend = new BooleanSetting("Friendly indicator").value(false);
    private final ColorSetting friendColor = new ColorSetting("Color").value(new Color(0, 255, 0));
    private final BooleanSetting potionCircle = new BooleanSetting("Potion area").value(true).setVisible(() -> render.isEnabled("Potion"));

    private final List<PredictionPoint> points = new ArrayList<>();
    private final Map<Integer, GhostTrail> ghostTrails = new HashMap<>();
    private static final long GHOST_TRAIL_LIFETIME_MS = 3000L;

    private final String UNKNOWN = "Неизвестный";

    public PredictionsModule() {
        addSettings(render, walls, friend, friendColor, potionCircle);
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            handleRender3D(event);
        }));

        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            handleRender2D(event);
        }));

        addEvents(render3DEvent, render2DEvent);
    }

    private void handleRender2D(Render2DEvent.Render2DEventData event) {
        Font font = Fonts.PS_MEDIUM;
        MatrixStack matrixStack = event.matrixStack();

        for (PredictionPoint point : points) {
            Vector2f project = ProjectionUtil.project((float)point.position.x, (float)point.position.y, (float)point.position.z);
            if (project.x == Float.MAX_VALUE && project.y == Float.MAX_VALUE) continue;

            String text = String.format("%s (%.1f сек)", point.itemName, point.ticks * 50 / 1000.0);
            String ownerText = "От " + point.ownerName;
            float offset = 3f, fontSize = 7f;
            float textWidth = font.getWidth(text, fontSize);
            float ownerWidth = font.getWidth(ownerText, fontSize);
            float textHeight = fontSize + offset * 2f;
            float addRectWidth = ownerWidth + offset * 2f;
            float posX = project.x - textWidth / 2f - offset;
            float posY = project.y;

            Color bgColor = point.isFriend && friend.getValue() ? friendColor.getValue() : UIColors.backgroundBlur();
            Color textColor = UIColors.textColor();

            RenderUtil.BLUR_RECT.draw(matrixStack, posX, posY, textWidth + offset * 2f, textHeight, 2f, bgColor);
            font.drawText(matrixStack, text, posX + offset, posY + offset, fontSize, textColor);

            if (!ownerText.contains(UNKNOWN)) {
                RenderUtil.BLUR_RECT.draw(matrixStack, project.x - addRectWidth / 2f, posY + textHeight + 2f, addRectWidth, textHeight, 2f, bgColor);
                font.drawText(matrixStack, ownerText, project.x - ownerWidth / 2f, posY + textHeight + 2f + offset, fontSize, textColor);
            }
        }
    }

    private void handleRender3D(Render3DEvent.Render3DEventData event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client == null ? null : client.world;
        if (client == null || world == null || client.getEntityRenderDispatcher() == null || client.getEntityRenderDispatcher().camera == null) {
            points.clear();
            return;
        }
        MinecraftClient mc = client;
        MatrixStack matrixStack = event.matrixStack();
        Vec3d renderOffset = client.getEntityRenderDispatcher().camera.getPos();
        points.clear();

        matrixStack.push();
        matrixStack.translate(-renderOffset.x, -renderOffset.y, -renderOffset.z);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (var entity : world.getEntities()) {
            String name = entity instanceof ThrownItemEntity thrownItemEntity
                    ? thrownItemEntity.getStack().getName().getString()
                    : entity.getName().getString();

            boolean isPearl = entity instanceof EnderPearlEntity;
            boolean isTrident = entity instanceof TridentEntity trident && !trident.isNoClip() && !trident.groundCollision;
            boolean isArrow = entity instanceof ArrowEntity;
            boolean isPotion = entity instanceof PotionEntity;

            if ((isPearl && render.isEnabled("Ender pearl"))
                    || (isTrident && render.isEnabled("Trident"))
                    || (isArrow && render.isEnabled("Arrow"))
                    || (isPotion && render.isEnabled("Potion"))) {

                if ((isArrow || isTrident) && (entity.getVelocity().lengthSquared() < 0.001)) continue;

                UUID ownerUuid = ((ProjectileEntity) entity).ownerUuid != null ? ((ProjectileEntity) entity).ownerUuid : null;
                var owner = ownerUuid != null ? world.getPlayerByUuid(ownerUuid) : null;
                boolean isFriend = owner != null && (FriendManager.getInstance().contains(owner.getName().getString()) || owner.getName().getString().equals(client.getSession().getUsername()));
                String ownerName = owner != null ? mc.getSession().getUsername().equals(owner.getName().getString()) ? "Вас" : owner.getName().getString() : UNKNOWN;

                predictTrajectory(entity, isFriend, name, ownerName, matrixStack, world, isPotion);
            }

        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        // Render ghost trails for projectiles that left render distance
        renderGhostTrails(matrixStack, renderOffset);

        matrixStack.pop();
    }


    private void predictTrajectory(Entity entity, boolean isFriend, String itemName, String ownerName, MatrixStack matrixStack, ClientWorld world, boolean isPotion) {
        Color color = isFriend && friend.getValue() ? Color.GREEN : UIColors.gradient(entity.age % 360);

        Vec3d motion = entity.getVelocity();
        Vec3d pos = entity.getPos();
        Vec3d prevPos;
        int ticks = 0;
        List<Vec3d> trailPoints = new ArrayList<>();
        trailPoints.add(pos);

        for (int i = 0; i <= 149; i++) {
            prevPos = pos;
            pos = pos.add(motion);
            motion = getMotion(entity, motion);

            boolean canSee = walls.getValue() || PlayerUtil.canSee(pos);

            var matrix = matrixStack.peek().getPositionMatrix();
            var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            if (canSee) buffer.vertex(matrix, (float)prevPos.x, (float)prevPos.y, (float)prevPos.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

            var hit = world.raycast(new RaycastContext(prevPos, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
            if (hit.getType() == HitResult.Type.BLOCK) pos = hit.getPos();
            trailPoints.add(pos);

            if (canSee) buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

            if (canSee) BufferRenderer.drawWithGlobalProgram(buffer.end());

            if (hit.getType() == HitResult.Type.BLOCK || pos.y < -128) {
                if (isPotion && potionCircle.getValue() && hit.getType() == HitResult.Type.BLOCK) {
                    renderPotionImpactCircle(matrixStack, pos, color, entity.age, ticks);
                }
                points.add(new PredictionPoint(pos, ticks, isFriend, itemName, ownerName));
                break;
            }
            ticks++;
        }

        // Cache trajectory for ghost trail rendering
        ghostTrails.put(entity.getId(), new GhostTrail(trailPoints, System.currentTimeMillis(), isFriend, itemName, ownerName));
    }

    private void renderPotionImpactCircle(MatrixStack matrixStack, Vec3d pos, Color color, int age, int ticks) {
        float time = (System.currentTimeMillis() % 1600L) / 1600f;
        float pulse = 0.86f + (float) Math.sin((time + ticks * 0.015f) * Math.PI * 2.0) * 0.12f;
        float radius = 4.0f * pulse;
        float y = (float) pos.y + 0.03f + (float) Math.sin((time + age * 0.02f) * Math.PI * 2.0) * 0.025f;
        int segments = 42;
        int outerAlpha = Math.max(54, (int) (165 * (0.75f + pulse * 0.2f)));
        int innerAlpha = Math.max(22, (int) (95 * (0.65f + pulse * 0.2f)));

        var matrix = matrixStack.peek().getPositionMatrix();
        var lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (Math.PI * 2.0 * i / segments);
            float angle2 = (float) (Math.PI * 2.0 * (i + 1) / segments);
            float x1 = (float) pos.x + MathHelper.cos(angle1) * radius;
            float z1 = (float) pos.z + MathHelper.sin(angle1) * radius;
            float x2 = (float) pos.x + MathHelper.cos(angle2) * radius;
            float z2 = (float) pos.z + MathHelper.sin(angle2) * radius;

            lines.vertex(matrix, x1, y, z1).color(color.getRed(), color.getGreen(), color.getBlue(), outerAlpha);
            lines.vertex(matrix, x2, y, z2).color(color.getRed(), color.getGreen(), color.getBlue(), outerAlpha);

            float innerRadius = radius * 0.82f;
            float ix1 = (float) pos.x + MathHelper.cos(angle1) * innerRadius;
            float iz1 = (float) pos.z + MathHelper.sin(angle1) * innerRadius;
            float ix2 = (float) pos.x + MathHelper.cos(angle2) * innerRadius;
            float iz2 = (float) pos.z + MathHelper.sin(angle2) * innerRadius;

            lines.vertex(matrix, ix1, y + 0.02f, iz1).color(color.getRed(), color.getGreen(), color.getBlue(), innerAlpha);
            lines.vertex(matrix, ix2, y + 0.02f, iz2).color(color.getRed(), color.getGreen(), color.getBlue(), innerAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());
    }


    private Vec3d getMotion(Entity entity, Vec3d motion) {
        Vec3d motion2 = motion;
        motion2 = entity.isTouchingWater() ? motion2.multiply(0.8) : motion2.multiply(0.99);

        if (!entity.hasNoGravity()) {
            double gravity = entity instanceof PotionEntity ? 0.05D : 0.03D;
            motion2 = motion2.subtract(0, gravity, 0);
        }

        return motion2;
    }

    private void renderGhostTrails(MatrixStack matrixStack, Vec3d renderOffset) {
        long now = System.currentTimeMillis();

        // Clean up expired ghost trails
        ghostTrails.values().removeIf(trail -> now - trail.createdAt > GHOST_TRAIL_LIFETIME_MS);

        // Check which entity IDs are still alive
        java.util.Set<Integer> aliveIds = new java.util.HashSet<>();
        if (mc.world != null) {
            for (Entity e : mc.world.getEntities()) {
                aliveIds.add(e.getId());
            }
        }

        // Only draw ghost trails for entities that are no longer in the world
        for (Map.Entry<Integer, GhostTrail> entry : ghostTrails.entrySet()) {
            if (aliveIds.contains(entry.getKey())) {
                continue; // Entity is still alive, skip — it's drawn normally
            }

            GhostTrail trail = entry.getValue();
            float age = (now - trail.createdAt) / (float) GHOST_TRAIL_LIFETIME_MS;
            int alpha = Math.max(20, (int) (200 * (1f - age)));
            Color color = trail.isFriend && friend.getValue()
                    ? new Color(0, 255, 0, alpha)
                    : new Color(UIColors.gradient(0).getRed(), UIColors.gradient(0).getGreen(), UIColors.gradient(0).getBlue(), alpha);

            List<Vec3d> pts = trail.points;
            if (pts.size() < 2) continue;

            matrixStack.push();
            matrixStack.translate(-renderOffset.x, -renderOffset.y, -renderOffset.z);

            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            for (int i = 0; i < pts.size() - 1; i++) {
                Vec3d a = pts.get(i);
                Vec3d b = pts.get(i + 1);
                var matrix = matrixStack.peek().getPositionMatrix();
                var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                buffer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                buffer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }

            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            matrixStack.pop();

            // Also render the label for the ghost trail endpoint
            if (!pts.isEmpty()) {
                Vec3d lastPoint = pts.getLast();
                points.add(new PredictionPoint(lastPoint, pts.size(), trail.isFriend, trail.itemName, trail.ownerName));
            }
        }
    }

    private record GhostTrail(List<Vec3d> points, long createdAt, boolean isFriend, String itemName, String ownerName) {}

}

final class PredictionPoint {
    final Vec3d position;
    final int ticks;
    final boolean isFriend;
    final String itemName;
    final String ownerName;

    PredictionPoint(Vec3d position, int ticks, boolean isFriend, String itemName, String ownerName) {
        this.position = position;
        this.ticks = ticks;
        this.isFriend = isFriend;
        this.itemName = itemName;
        this.ownerName = ownerName;
    }
}
