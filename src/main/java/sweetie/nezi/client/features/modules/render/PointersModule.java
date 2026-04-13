package sweetie.nezi.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.*;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.combat.TargetManager;
import sweetie.nezi.api.utils.math.MathUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

@ModuleRegister(name = "Pointers", category = Category.RENDER)
public class PointersModule extends Module {
    @Getter private static final PointersModule instance = new PointersModule();

    protected final MultiBooleanSetting targets = new MultiBooleanSetting("Цели").value(
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Голые").value(true),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Жители").value(false),
            new BooleanSetting("Предметы").value(false)
    );

    private final ModeSetting playerModeC = new ModeSetting("Режим игроков").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Игроки") || targets.isEnabled("Голые"));
    private final ModeSetting animalsModeC = new ModeSetting("Режим животных").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Животные"));
    private final ModeSetting mobModeC = new ModeSetting("Режим мобов").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Мобы"));
    private final ModeSetting friendModeC = new ModeSetting("Режим друзей").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Игроки") || targets.isEnabled("Голые"));
    private final ModeSetting itemModeC = new ModeSetting("Режим предметов").value("Клиент").values("Клиент", "Свой").setVisible(() -> targets.isEnabled("Предметы"));

    private final ColorSetting playerColor = new ColorSetting("Цвет игроков").value(new Color(-1)).setVisible(() -> playerModeC.is("Свой") && (targets.isEnabled("Игроки") || targets.isEnabled("Голые")));
    private final ColorSetting animalsColor = new ColorSetting("Цвет животных").value(new Color(-1)).setVisible(() -> animalsModeC.is("Свой") && targets.isEnabled("Животные"));
    private final ColorSetting mobColor = new ColorSetting("Цвет мобов").value(new Color(-1)).setVisible(() -> mobModeC.is("Свой") && targets.isEnabled("Мобы"));
    private final ColorSetting friendColor = new ColorSetting("Цвет друзей").value(new Color(94, 255, 69)).setVisible(() -> friendModeC.is("Свой") && (targets.isEnabled("Игроки") || targets.isEnabled("Голые")));
    private final ColorSetting itemColor = new ColorSetting("Цвет предметов").value(new Color(255, 72, 69)).setVisible(() -> itemModeC.is("Свой") && targets.isEnabled("Предметы"));

    private final SliderSetting pointerSize = new SliderSetting("Размер").value(1f).range(0.5f, 2.5f).step(0.1f);
    private final SliderSetting pointerRadius = new SliderSetting("Радиус").value(40f).range(20f, 100f).step(1f);

    private final ModeSetting animation = new ModeSetting("Анимация").value("Появление").values("Затухание", "Появление", "Нет");
    private final SliderSetting duration = new SliderSetting("Длительность").value(4f).range(1f, 20f).step(1f);

    private final TargetManager.EntityFilter entityFilter = new TargetManager.EntityFilter(targets.getList());

    private final HashSet<Entity> alive = new HashSet<>();
    private final HashMap<Entity, AnimationUtil> animations = new HashMap<>();
    private final AnimationUtil yawAnimation = new AnimationUtil();
    private final AnimationUtil radiusAnimation = new AnimationUtil();

    public PointersModule() {
        addSettings(targets,
                playerModeC, animalsModeC, mobModeC, friendModeC, itemModeC,
                playerColor, animalsColor, mobColor, friendColor, itemColor,
                pointerSize, pointerRadius, animation, duration
        );
    }

    @Override
    public void onDisable() {
        alive.clear();
        animations.clear();
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(2, event -> {
            if (mc.player == null || mc.world == null) {
                alive.clear();
                animations.clear();
                return;
            }

            alive.clear();
            entityFilter.targetSettings = targets.getList();
            entityFilter.needFriends = true;

            radiusAnimation.update();

            radiusAnimation.run(getContainerSize(), 300, Easing.EXPO_OUT);

            boolean playersOnly = (targets.isEnabled("Игроки") || targets.isEnabled("Голые"))
                    && !targets.isEnabled("Животные")
                    && !targets.isEnabled("Мобы")
                    && !targets.isEnabled("Предметы");

            if (playersOnly) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player != mc.player && shouldRenderEntity(player)) {
                        alive.add(player);
                    }
                }
            } else {
                for (Entity entity : mc.world.getEntities()) {
                    if (mc.player == entity) continue;

                    if ((entity instanceof ItemEntity && targets.isEnabled("Предметы")) ||
                            (entity instanceof LivingEntity le && shouldRenderEntity(le))) {
                        alive.add(entity);
                    }
                }
            }

            for (Entity entity : alive) {
                if (!animations.containsKey(entity)) {
                    animations.put(entity, new AnimationUtil());
                }
            }

            Iterator<Map.Entry<Entity, AnimationUtil>> iterator = animations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Entity, AnimationUtil> entry = iterator.next();
                Entity entity = entry.getKey();
                AnimationUtil anim = entry.getValue();

                boolean isAlive = alive.contains(entity);
                anim.update();
                anim.run(isAlive ? 1.0 : 0.0, duration.getValue().longValue() * 50, Easing.SINE_OUT);

                drawPointerToEntity(event, entity, anim);

                if (!isAlive && anim.getValue() <= 0.0) {
                    iterator.remove();
                }
            }
        }));

        addEvents(renderEvent);
    }

    private void drawPointerToEntity(Render2DEvent.Render2DEventData event, Entity entity, AnimationUtil spawn){
        DrawContext context = event.context();
        MatrixStack matrixStack = context.getMatrices();
        float centerX = getCenterX();
        float centerY = getCenterY();

        float animFactor = 1f;
        float spawnAnim = (float) spawn.getValue();

        if (animation.is("Появление")) {
            animFactor = (2f - spawnAnim);
        } else if (animation.is("Затухание")) {
            animFactor = 0.3f + 0.7f * spawnAnim;
        }

        if (spawnAnim <= 0.0) return;

        float animatedRadius = pointerRadius.getValue() * animFactor + (float) radiusAnimation.getValue();
        float playerYaw = mc.player == null ? 0f : mc.player.getYaw();
        float yaw = MathHelper.wrapDegrees((float) getEntityYaw(entity) - playerYaw);
        float renderYaw = mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? -yaw : yaw;

        matrixStack.push();
        matrixStack.translate(centerX, centerY, 0.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(renderYaw));
        matrixStack.translate(-centerX, -centerY, 0.0F);

        Color color = ColorUtil.setAlpha(getEntityColor(entity), (int) (spawnAnim * 255));
        drawPointer(context, centerX, centerY - animatedRadius, pointerSize.getValue() * 20F,
                ColorUtil.setAlpha(color, (int) (255f * (color.getAlpha() / 255f) * spawn.getValue())), false);
        matrixStack.pop();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public void drawPointer(DrawContext context, float x, float y, float size, Color color, boolean gps) {
        RenderSystem.setShaderTexture(0, FileUtil.getImage("pointers/" + (gps ? "arrow_gps" : "triangle")));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        RenderSystem.disableDepthTest();
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        float scaledSize = size + 8;
        buffer.vertex(matrix, x - (scaledSize / 2f), y + scaledSize, 0).texture(0f, 1f);
        buffer.vertex(matrix, x + scaledSize / 2f, y + scaledSize, 0).texture(1f, 1f);
        buffer.vertex(matrix, x + scaledSize / 2f, y, 0).texture(1f, 0);
        buffer.vertex(matrix, x - (scaledSize / 2f), y, 0).texture(0, 0);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private float getCenterX() {
        return mc.getWindow().getScaledWidth() / 2f;
    }

    private float getCenterY() {
        return mc.getWindow().getScaledHeight() / 2f;
    }

    private float getContainerSize() {
        if (mc.currentScreen instanceof HandledScreen<?> containerScreen) {
            return Math.max(containerScreen.width, containerScreen.height) * 0.05f + (pointerRadius.getMax() - pointerRadius.getValue());
        }

        return 0f;
    }

    private float getEntityYaw(Entity entity) {
        if (mc.player == null) return 0;
        double xA = (MathUtil.interpolate(mc.player.prevX, mc.player.getPos().x));
        double zA = (MathUtil.interpolate(mc.player.prevZ, mc.player.getPos().z));
        double x = MathUtil.interpolate(entity.prevX, entity.getPos().x) - xA;
        double z = MathUtil.interpolate(entity.prevZ, entity.getPos().z) - zA;
        return (float) -(Math.atan2(x, z) * (180 / Math.PI));
    }

    private Color getEntityColor(Entity entity) {
        int seed = entity.getId() * 13;
        Color gradient = UIColors.gradient(seed);
        return switch (entity) {
            case ItemEntity itemEntity -> itemModeC.is("Свой") ? itemColor.getValue() : gradient;
            case PlayerEntity player -> FriendManager.getInstance().contains(player.getName().getString()) ? friendModeC.is("Свой") ? friendColor.getValue() : gradient : playerModeC.is("Свой") ? playerColor.getValue() : gradient;
            case AnimalEntity animalEntity -> animalsModeC.is("Свой") ? animalsColor.getValue() : gradient;
            case MobEntity mobEntity -> mobModeC.is("Свой") ? mobColor.getValue() : gradient;
            default -> new Color(-1);
        };
    }

    private boolean shouldRenderEntity(LivingEntity entity) {
        if (entity instanceof PlayerEntity player && (targets.isEnabled("Игроки") || targets.isEnabled("Голые"))) {
            return player != mc.player && player.isAlive();
        }

        return entityFilter.isValid(entity);
    }
}