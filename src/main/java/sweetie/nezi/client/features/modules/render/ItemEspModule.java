package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.joml.Vector2f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Item ESP", category = Category.RENDER)
public class ItemEspModule extends Module {
    @Getter private static final ItemEspModule instance = new ItemEspModule();

    private final SliderSetting scale = new SliderSetting("Scale").value(1.0f).range(0.5f, 2.0f).step(0.1f);
    private final SliderSetting maxDistance = new SliderSetting("Max distance").value(64f).range(16f, 128f).step(1f);

    private final List<ItemLabel> labels = new ArrayList<>();

    public ItemEspModule() {
        addSettings(scale, maxDistance);
    }

    @Override
    public void onEvent() {
        EventListener render2D = Render2DEvent.getInstance().subscribe(new Listener<>(this::onRender2D));
        addEvents(render2D);
    }

    private void onRender2D(Render2DEvent.Render2DEventData event) {
        if (mc.world == null || mc.player == null || mc.getEntityRenderDispatcher().camera == null) {
            return;
        }

        labels.clear();
        float maxDist = maxDistance.getValue();
        float partialTicks = event.partialTicks();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) {
                continue;
            }

            if (mc.player.squaredDistanceTo(entity) > maxDist * maxDist) {
                continue;
            }

            double x = MathUtil.interpolate(entity.prevX, entity.getX(), partialTicks);
            double y = MathUtil.interpolate(entity.prevY, entity.getY(), partialTicks) + entity.getHeight() + 0.25;
            double z = MathUtil.interpolate(entity.prevZ, entity.getZ(), partialTicks);

            if (!ProjectionUtil.isInFrontOfCamera(x, y, z)) {
                continue;
            }

            Vector2f projected = ProjectionUtil.project(x, y, z);
            if (!ProjectionUtil.isProjectedOnScreen(projected, 50f)) {
                continue;
            }

            String name = itemEntity.getStack().getName().getString();
            int count = itemEntity.getStack().getCount();
            String label = count > 1 ? name + " x" + count : name;
            float dist = (float) Math.sqrt(mc.player.squaredDistanceTo(entity));

            labels.add(new ItemLabel(projected.x, projected.y, label, dist));
        }

        DrawContext context = event.context();
        MatrixStack ms = context.getMatrices();
        float s = scale.getValue();
        float fontSize = 5.8f * s;
        float padding = 2.5f * s;
        float round = 3f * s;

        for (ItemLabel label : labels) {
            float nameW = Fonts.PS_MEDIUM.getWidth(label.text, fontSize);
            float totalW = nameW + padding * 2f;
            float totalH = fontSize + padding * 2f;

            float cardX = label.x - totalW / 2f;
            float cardY = label.y;

            drawGlassDark(ms, cardX, cardY, totalW, totalH, round);

            Fonts.PS_MEDIUM.drawText(ms, label.text,
                    cardX + padding, cardY + padding, fontSize,
                    Color.WHITE);
        }
    }

    private void drawGlassDark(MatrixStack ms, float x, float y, float w, float h, float round) {
        Color basePanel = UIColors.panel(220);
        Color darkPanel = new Color(
                (int)(basePanel.getRed() * 0.6f),
                (int)(basePanel.getGreen() * 0.6f),
                (int)(basePanel.getBlue() * 0.6f),
                basePanel.getAlpha()
        );

        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, round, UIColors.blur(220), 0.08f);
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, round, UIColors.backgroundBlur(220), 0.06f);
        RenderUtil.RECT.draw(ms, x, y, w, h, round, darkPanel);
        RenderUtil.RECT.draw(ms, x, y, w, h, round, UIColors.overlay(30));
        RenderUtil.RECT.draw(ms, x, y, w, h, round, UIColors.stroke(255));
    }

    private record ItemLabel(float x, float y, String text, float distance) {}
}