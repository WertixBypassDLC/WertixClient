package sweetie.nezi.client.features.modules.player;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BindSetting;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ColorSetting;
import sweetie.nezi.api.system.backend.Pair;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.player.InventoryUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.display.BoxRender;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.movement.InventoryMoveModule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "Funtime Helper", category = Category.PLAYER)
public class FuntimeHelperModule extends Module {
    @Getter private static final FuntimeHelperModule instance = new FuntimeHelperModule();

    private static final net.minecraft.util.Identifier CIRCLE_TEXTURE = FileUtil.getImage("circle/lean");
    private static final int FILL_ALPHA = 85;
    private static final float PREVIEW_LINE_WIDTH = 2.25f;
    private static final double DEZKA_RADIUS = 10.0d;
    private static final double YAVKA_RADIUS = 10.0d;
    private static final double FIRE_CHARGE_RADIUS = 10.0d;
    private static final double GOD_AURA_RADIUS = 2.0d;
    private static final double SNOWBALL_RADIUS = 7.0d;
    private static final double SNOWBALL_SPEED = 1.5d;
    private static final double SNOWBALL_GRAVITY = 0.03d;
    private static final double SNOWBALL_DRAG = 0.99d;
    private static final int SNOWBALL_MAX_STEPS = 160;
    private static final int SNOWBALL_SUBSTEPS = 6;
    private static final double PLAST_EXTRA_EXPAND = 0.5d;
    private static final double PLAST_SURFACE_OFFSET = 0.01d;

    private final BooleanSetting timer = new BooleanSetting("Таймер").value(true);
    private final BooleanSetting preview = new BooleanSetting("Radius Preview").value(true);
    private final BooleanSetting previewFill = new BooleanSetting("Preview Fill").value(true).setVisible(preview::getValue);
    private final BooleanSetting hitIndicator = new BooleanSetting("Hit Indicator").value(true).setVisible(preview::getValue);
    private final ColorSetting hitColor = new ColorSetting("Hit Color").value(new Color(0, 255, 136, 255)).setVisible(() -> preview.getValue() && hitIndicator.getValue());
    private final ColorSetting dezkaColor = new ColorSetting("Dezka Color").value(new Color(0x00, 0x55, 0x00, 255)).setVisible(preview::getValue);
    private final ColorSetting yavkaColor = new ColorSetting("Yavka Color").value(new Color(0x99, 0x99, 0x99, 255)).setVisible(preview::getValue);
    private final ColorSetting fireChargeColor = new ColorSetting("Fire Charge Color").value(new Color(0x55, 0x00, 0x00, 255)).setVisible(preview::getValue);
    private final ColorSetting godAuraColor = new ColorSetting("God Aura Color").value(new Color(0x00, 0x99, 0x99, 255)).setVisible(preview::getValue);
    private final ColorSetting trapkaColor = new ColorSetting("Trapka Color").value(new Color(0x8B, 0x45, 0x13, 255)).setVisible(preview::getValue);
    private final ColorSetting plastColor = new ColorSetting("Plast Color").value(new Color(0x33, 0x33, 0x33, 255)).setVisible(preview::getValue);
    private final ColorSetting snowballColor = new ColorSetting("Snowball Color").value(new Color(0xA0, 0xDC, 0xFF, 255)).setVisible(preview::getValue);

    private final Map<InventoryUtil.ItemUsage, BindSetting> keyBindings = new LinkedHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<Pair<Long, Vec3d>> consumables = new ArrayList<>();
    private final Map<Vec3d, String> consumableNames = new HashMap<>();

    public FuntimeHelperModule() {
        keyBindings.put(new InventoryUtil.ItemUsage(Items.PHANTOM_MEMBRANE, this), new BindSetting("Божья аура").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.ENDER_EYE, this), new BindSetting("Дезорентация").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SUGAR, this), new BindSetting("Явная пыль").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.FIRE_CHARGE, this), new BindSetting("Огненный смерч").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.NETHERITE_SCRAP, this), new BindSetting("Трапка").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.DRIED_KELP, this), new BindSetting("Пласт").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SNOWBALL, this), new BindSetting("Snowball").value(-999));

        addSettings(
                timer,
                preview, previewFill, hitIndicator, hitColor,
                dezkaColor, yavkaColor, fireChargeColor, godAuraColor, trapkaColor, plastColor, snowballColor
        );
        keyBindings.values().forEach(this::addSettings);
    }

    @Override
    public void onDisable() {
        keyBindings.keySet().forEach(InventoryUtil.ItemUsage::onDisable);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (shouldHandleOnTick()) {
                handleItemUsage();
            }
        }));
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!shouldHandleOnTick()) {
                handleItemUsage();
            }
        }));
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(this::handleRender2DEvent));
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(this::handleRender3DEvent));
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(this::handlePacketEvent));
        addEvents(tickEvent, updateEvent, render2DEvent, render3DEvent, packetEvent);
    }

    private void handleRender2DEvent(Render2DEvent.Render2DEventData event) {
        if (!timer.getValue()) return;

        MatrixStack matrixStack = event.matrixStack();
        consumables.removeIf(consumable -> (consumable.left() - System.currentTimeMillis()) <= 0L);

        for (Pair<Long, Vec3d> consumable : consumables) {
            Vec3d position = consumable.right();
            Vector2f screenPos = ProjectionUtil.project(position);
            if (screenPos.x == Float.MAX_VALUE || screenPos.y == Float.MAX_VALUE) {
                continue;
            }

            double time = MathUtil.round((double) (consumable.left() - System.currentTimeMillis()) / 1000.0D, 1);
            String name = consumableNames.getOrDefault(position, "Таймер");
            String text = name + ": " + time + "s";
            float size = 7f;
            float gap = 3f;
            float textWidth = Fonts.PS_BOLD.getWidth(text, size);
            float posX = screenPos.x - textWidth / 2f;
            float posY = screenPos.y;

            RenderUtil.BLUR_RECT.draw(matrixStack, posX, posY, textWidth + gap * 2f, size + gap * 2f, 2f, UIColors.blur());
            Fonts.PS_BOLD.drawText(matrixStack, text, posX + gap, posY + gap, size, UIColors.textColor());
        }
    }

    private void handleRender3DEvent(Render3DEvent.Render3DEventData event) {
        if (!preview.getValue() || mc.player == null || mc.world == null) {
            return;
        }

        PreviewItem previewItem = resolveHeldPreviewItem(mc.player.getMainHandStack(), mc.player.getOffHandStack());
        if (previewItem == null) {
            return;
        }

        switch (previewItem) {
            case DEZKA -> renderRadiusPreview(event, DEZKA_RADIUS, dezkaColor.getValue());
            case YAVKA -> renderRadiusPreview(event, YAVKA_RADIUS, yavkaColor.getValue());
            case FIRE_CHARGE -> renderRadiusPreview(event, FIRE_CHARGE_RADIUS, fireChargeColor.getValue());
            case GOD_AURA -> renderRadiusPreview(event, GOD_AURA_RADIUS, godAuraColor.getValue());
            case TRAPKA -> renderTrapkaPreview(event);
            case PLAST -> renderPlastPreview(event);
            case SNOWBALL -> renderSnowballPreview(event);
        }
    }

    private void renderRadiusPreview(Render3DEvent.Render3DEventData event, double radius, Color baseColor) {
        Vec3d center = getInterpolatedPlayerPos(event.partialTicks()).add(0.0, -1.4, 0.0);
        boolean highlight = hitIndicator.getValue() && hasPlayersInRadius(center, radius);
        Color color = highlight ? hitColor.getValue() : baseColor;
        drawGroundCircle(event.matrixStack(), center, (float) radius, color);
    }

    private void renderTrapkaPreview(Render3DEvent.Render3DEventData event) {
        Box box = getTrapkaBox(event.partialTicks());
        boolean highlight = hitIndicator.getValue() && hasPlayersInBox(box);
        Color color = highlight ? hitColor.getValue() : trapkaColor.getValue();
        drawBoxPreview(box, color);
    }

    private void renderPlastPreview(Render3DEvent.Render3DEventData event) {
        Box box = getPlastBox(event.partialTicks());
        boolean highlight = hitIndicator.getValue() && hasPlayersInBox(box);
        Color color = highlight ? hitColor.getValue() : plastColor.getValue();
        drawBoxPreview(box, color);
    }

    private void renderSnowballPreview(Render3DEvent.Render3DEventData event) {
        SnowballPrediction prediction = predictSnowball(event.partialTicks());
        if (prediction == null || prediction.points.size() < 2) {
            return;
        }

        boolean highlight = hitIndicator.getValue() && hasPlayersInRadius(prediction.landingPos, SNOWBALL_RADIUS);
        Color color = highlight ? hitColor.getValue() : snowballColor.getValue();
        drawTrajectory(event.matrixStack(), prediction.points, color);
        drawGroundCircle(event.matrixStack(), prediction.landingPos.add(0.0, 0.03, 0.0), (float) SNOWBALL_RADIUS, color);
    }

    private void drawGroundCircle(MatrixStack matrices, Vec3d center, float radius, Color color) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }

        Color fillColor = withAlpha(color, previewFill.getValue() ? FILL_ALPHA : 0);
        Color ringColor = withAlpha(color, Math.max(120, color.getAlpha()));

        RenderUtil.WORLD.startRender(matrices);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderTexture(0, CIRCLE_TEXTURE);

        matrices.push();
        matrices.translate(center.x - camera.getPos().x, center.y - camera.getPos().y + 0.02, center.z - camera.getPos().z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float[] fill = ColorUtil.normalize(fillColor);
        float[] outline = ColorUtil.normalize(ringColor);
        buffer.vertex(matrix, radius, -radius, 0f).texture(0f, 1f).color(outline[0], outline[1], outline[2], outline[3]);
        buffer.vertex(matrix, -radius, -radius, 0f).texture(1f, 1f).color(outline[0], outline[1], outline[2], outline[3]);
        buffer.vertex(matrix, -radius, radius, 0f).texture(1f, 0f).color(fill[0], fill[1], fill[2], Math.max(fill[3], outline[3] * 0.65f));
        buffer.vertex(matrix, radius, radius, 0f).texture(0f, 0f).color(fill[0], fill[1], fill[2], Math.max(fill[3], outline[3] * 0.65f));
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
        RenderUtil.WORLD.endRender(matrices);
    }

    private void drawBoxPreview(Box box, Color color) {
        if (previewFill.getValue()) {
            RenderUtil.BOX.drawBox(
                    (float) box.minX, (float) box.minY, (float) box.minZ,
                    (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                    PREVIEW_LINE_WIDTH, withAlpha(color, FILL_ALPHA), BoxRender.Render.FILL, 0f
            );
        }

        RenderUtil.BOX.drawBox(
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                PREVIEW_LINE_WIDTH, withAlpha(color, 255), BoxRender.Render.OUTLINE, 0f
        );
    }

    private void drawTrajectory(MatrixStack matrices, List<Vec3d> points, Color color) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null || points.size() < 2) {
            return;
        }

        matrices.push();
        matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(PREVIEW_LINE_WIDTH);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 1; i < points.size(); i++) {
            Vec3d prev = points.get(i - 1);
            Vec3d next = points.get(i);
            buffer.vertex(matrix, (float) prev.x, (float) prev.y, (float) prev.z).color(color.getRGB());
            buffer.vertex(matrix, (float) next.x, (float) next.y, (float) next.z).color(color.getRGB());
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private SnowballPrediction predictSnowball(float partialTicks) {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        List<Vec3d> points = new ArrayList<>();
        Vec3d position = getInterpolatedEyePos(partialTicks);
        Vec3d velocity = mc.player.getRotationVec(partialTicks).normalize().multiply(SNOWBALL_SPEED);
        points.add(position);

        Vec3d landing = null;
        outer:
        for (int i = 0; i < SNOWBALL_MAX_STEPS; i++) {
            for (int sub = 0; sub < SNOWBALL_SUBSTEPS; sub++) {
                Vec3d prev = position;
                Vec3d step = velocity.multiply(1.0 / SNOWBALL_SUBSTEPS);
                position = position.add(step);

                HitResult hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                        prev,
                        position,
                        net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                        net.minecraft.world.RaycastContext.FluidHandling.ANY,
                        mc.player
                ));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    landing = blockHit.getPos();
                    points.add(landing);
                    break outer;
                }

                points.add(position);
                if (position.y < mc.world.getBottomY() - 8.0) {
                    landing = position;
                    break outer;
                }

                double drag = Math.pow(SNOWBALL_DRAG, 1.0 / SNOWBALL_SUBSTEPS);
                velocity = velocity.subtract(0.0, SNOWBALL_GRAVITY / SNOWBALL_SUBSTEPS, 0.0).multiply(drag);
            }
        }

        if (points.size() < 2) {
            return null;
        }
        if (landing == null) {
            landing = points.get(points.size() - 1);
        }

        return new SnowballPrediction(points, landing);
    }

    private PreviewItem resolveHeldPreviewItem(ItemStack mainHand, ItemStack offHand) {
        PreviewItem main = getPreviewItem(mainHand);
        return main != null ? main : getPreviewItem(offHand);
    }

    private PreviewItem getPreviewItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        Item item = stack.getItem();
        if (item == Items.ENDER_EYE) return PreviewItem.DEZKA;
        if (item == Items.SUGAR) return PreviewItem.YAVKA;
        if (item == Items.FIRE_CHARGE) return PreviewItem.FIRE_CHARGE;
        if (item == Items.PHANTOM_MEMBRANE) return PreviewItem.GOD_AURA;
        if (item == Items.NETHERITE_SCRAP) return PreviewItem.TRAPKA;
        if (item == Items.DRIED_KELP) return PreviewItem.PLAST;
        if (item == Items.SNOWBALL) return PreviewItem.SNOWBALL;
        return null;
    }

    private boolean hasPlayersInRadius(Vec3d center, double radius) {
        double radiusSq = radius * radius;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (shouldIgnorePlayer(player) || !canSeePlayer(player)) {
                continue;
            }
            if (player.getPos().squaredDistanceTo(center) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPlayersInBox(Box box) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (shouldIgnorePlayer(player) || !canSeePlayer(player)) {
                continue;
            }
            if (player.getBoundingBox().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    private boolean canSeePlayer(PlayerEntity target) {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        Vec3d from = mc.player.getEyePos();
        Box box = target.getBoundingBox();
        Vec3d[] checks = new Vec3d[]{
                target.getEyePos(),
                box.getCenter(),
                new Vec3d(box.minX, box.getCenter().y, box.minZ),
                new Vec3d(box.maxX, box.getCenter().y, box.maxZ)
        };

        for (Vec3d to : checks) {
            HitResult hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                    from,
                    to,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE,
                    mc.player
            ));
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldIgnorePlayer(PlayerEntity player) {
        return mc.player == null
                || player == mc.player
                || !player.isAlive()
                || player.isSpectator()
                || player.isInvisible()
                || player.isInvisibleTo(mc.player);
    }

    private Vec3d getInterpolatedPlayerPos(float partialTicks) {
        if (mc.player == null) {
            return Vec3d.ZERO;
        }
        double x = mc.player.prevX + (mc.player.getX() - mc.player.prevX) * partialTicks;
        double y = mc.player.prevY + (mc.player.getY() - mc.player.prevY) * partialTicks;
        double z = mc.player.prevZ + (mc.player.getZ() - mc.player.prevZ) * partialTicks;
        return new Vec3d(x, y, z);
    }

    private Vec3d getInterpolatedEyePos(float partialTicks) {
        Vec3d pos = getInterpolatedPlayerPos(partialTicks);
        return pos.add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
    }

    private Box getTrapkaBox(float partialTicks) {
        Vec3d position = getInterpolatedPlayerPos(partialTicks);
        double centerX = Math.floor(position.x) + 0.5;
        double centerY = Math.floor(position.y) + 0.5 + 1.625;
        double centerZ = Math.floor(position.z) + 0.5;
        float halfSize = 2.0f;
        return new Box(
                centerX - halfSize, centerY - halfSize, centerZ - halfSize,
                centerX + halfSize, centerY + halfSize, centerZ + halfSize
        );
    }

    private Box getPlastBox(float partialTicks) {
        if (mc.player == null) {
            return Box.of(Vec3d.ZERO, 0.0, 0.0, 0.0);
        }

        float width = 4.0f;
        float height = 4.0f;
        float thickness = 1.5f;
        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        float halfThickness = thickness / 2.0f;

        Vec3d lookVec = mc.player.getRotationVec(partialTicks);
        Vec3d eyePos = getInterpolatedEyePos(partialTicks);
        Direction normal = getDominantLookDirection(lookVec);
        double shift = halfThickness + PLAST_SURFACE_OFFSET;
        Vec3d center = eyePos.add(lookVec.multiply(4.0)).add(
                normal.getOffsetX() * shift,
                normal.getOffsetY() * shift,
                normal.getOffsetZ() * shift
        );

        Box plane = switch (normal.getAxis()) {
            case X -> new Box(
                    center.x - halfThickness, center.y - halfHeight, center.z - halfWidth,
                    center.x + halfThickness, center.y + halfHeight, center.z + halfWidth
            );
            case Y -> new Box(
                    center.x - halfWidth, center.y - halfThickness, center.z - halfHeight,
                    center.x + halfWidth, center.y + halfThickness, center.z + halfHeight
            );
            case Z -> new Box(
                    center.x - halfWidth, center.y - halfHeight, center.z - halfThickness,
                    center.x + halfWidth, center.y + halfHeight, center.z + halfThickness
            );
        };

        return switch (normal.getAxis()) {
            case X -> new Box(
                    plane.minX, plane.minY - PLAST_EXTRA_EXPAND, plane.minZ - PLAST_EXTRA_EXPAND,
                    plane.maxX, plane.maxY + PLAST_EXTRA_EXPAND, plane.maxZ + PLAST_EXTRA_EXPAND
            );
            case Y -> new Box(
                    plane.minX - PLAST_EXTRA_EXPAND, plane.minY, plane.minZ - PLAST_EXTRA_EXPAND,
                    plane.maxX + PLAST_EXTRA_EXPAND, plane.maxY, plane.maxZ + PLAST_EXTRA_EXPAND
            );
            case Z -> new Box(
                    plane.minX - PLAST_EXTRA_EXPAND, plane.minY - PLAST_EXTRA_EXPAND, plane.minZ,
                    plane.maxX + PLAST_EXTRA_EXPAND, plane.maxY + PLAST_EXTRA_EXPAND, plane.maxZ
            );
        };
    }

    private Direction getDominantLookDirection(Vec3d lookVec) {
        double ax = Math.abs(lookVec.x);
        double ay = Math.abs(lookVec.y);
        double az = Math.abs(lookVec.z);

        if (ay >= ax && ay >= az) {
            return lookVec.y >= 0.0 ? Direction.UP : Direction.DOWN;
        }
        if (ax >= az) {
            return lookVec.x >= 0.0 ? Direction.EAST : Direction.WEST;
        }
        return lookVec.z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
    }

    private void handlePacketEvent(PacketEvent.PacketEventData event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client == null ? null : client.player;
        ClientWorld world = client == null ? null : client.world;
        if (!timer.getValue() || event.isSend() || player == null || world == null) {
            return;
        }

        if (!(event.packet() instanceof PlaySoundS2CPacket soundPacket)) {
            return;
        }

        String soundPath = soundPacket.getSound().getIdAsString();
        if (soundPath.equals("minecraft:block.piston.contract")) {
            Vec3d pos = Vec3d.ofCenter(new BlockPos((int) soundPacket.getX(), (int) soundPacket.getY(), (int) soundPacket.getZ()));
            consumables.add(new Pair<>(System.currentTimeMillis() + 15_000L, pos));
            consumableNames.put(pos, "Трапка");
            return;
        }

        if (!soundPath.equals("minecraft:block.anvil.place")) {
            return;
        }

        BlockPos soundPos = new BlockPos((int) soundPacket.getX(), (int) soundPacket.getY(), (int) soundPacket.getZ());
        long delay = 250L;
        float playerPitch = player.getPitch();

        scheduler.schedule(() -> getCube(soundPos, 4, 4).stream()
                .filter(pos -> getDistance(soundPos, pos) > 2 && world.getBlockState(pos).getBlock() == Blocks.COBBLESTONE)
                .min(Comparator.comparing(pos -> getDistance(soundPos, pos)))
                .ifPresent(pos -> {
                    if (getCube(pos, 1, 1).stream().anyMatch(p -> world.getBlockState(p).getBlock() == Blocks.ANVIL)) {
                        return;
                    }

                    long solidCount = getCube(pos, 1, 1).stream().filter(p -> {
                        BlockState state = world.getBlockState(p);
                        return !state.isAir() && state.isSolidBlock(world, p);
                    }).count();

                    if (solidCount >= 14) {
                        Vec3d addPos = Vec3d.ofCenter(pos);
                        long duration = Math.abs(playerPitch) >= 45.0f ? 60_000L - delay : 20_000L - delay;
                        consumables.add(new Pair<>(System.currentTimeMillis() + duration, addPos));
                        consumableNames.put(addPos, "Пласт");
                    } else if (solidCount >= 5) {
                        Vec3d addPos = Vec3d.ofCenter(pos).add(0, -1.5, 0);
                        consumables.add(new Pair<>(System.currentTimeMillis() + 15_000L - delay, addPos));
                        consumableNames.put(addPos, "Трапка");
                    }
                }), delay, TimeUnit.MILLISECONDS);
    }

    private boolean shouldHandleOnTick() {
        return InventoryMoveModule.getInstance().usesBypassFlow();
    }

    private void handleItemUsage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        if (client.currentScreen != null && !InventoryMoveModule.getInstance().isEnabled()) return;
        boolean useLegitBypass = InventoryMoveModule.getInstance().usesLegitItemBypass();
        keyBindings.forEach((usage, bind) -> usage.handleUse(bind.getValue(), useLegitBypass));
    }

    private double getDistance(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<BlockPos> getCube(BlockPos center, int xRadius, int yRadius) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = -xRadius; x <= xRadius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -xRadius; z <= xRadius; z++) {
                    positions.add(center.add(x, y, z));
                }
            }
        }
        return positions;
    }

    private enum PreviewItem {
        DEZKA,
        YAVKA,
        FIRE_CHARGE,
        GOD_AURA,
        TRAPKA,
        PLAST,
        SNOWBALL
    }

    private static final class SnowballPrediction {
        private final List<Vec3d> points;
        private final Vec3d landingPos;

        private SnowballPrediction(List<Vec3d> points, Vec3d landingPos) {
            this.points = points;
            this.landingPos = landingPos;
        }
    }
}
