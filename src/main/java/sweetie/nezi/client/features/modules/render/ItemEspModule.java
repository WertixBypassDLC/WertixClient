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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleRegister(name = "Item ESP", category = Category.RENDER)
public class ItemEspModule extends Module {
    @Getter private static final ItemEspModule instance = new ItemEspModule();

    private final SliderSetting scale = new SliderSetting("Scale").value(1.0f).range(0.5f, 2.0f).step(0.1f);
    private final SliderSetting maxDistance = new SliderSetting("Max distance").value(64f).range(16f, 128f).step(1f);
    private final SliderSetting groupRadius = new SliderSetting("Радиус группировки").value(1.0f).range(0.5f, 3.0f).step(0.1f);

    public ItemEspModule() {
        addSettings(scale, maxDistance, groupRadius);
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

        float maxDist = maxDistance.getValue();
        float partialTicks = event.partialTicks();
        float groupDist = groupRadius.getValue();
        float groupDistSq = groupDist * groupDist;

        List<ItemEntry> entries = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;
            if (mc.player.squaredDistanceTo(entity) > maxDist * maxDist) continue;

            double x = MathUtil.interpolate(entity.prevX, entity.getX(), partialTicks);
            double y = MathUtil.interpolate(entity.prevY, entity.getY(), partialTicks);
            double z = MathUtil.interpolate(entity.prevZ, entity.getZ(), partialTicks);

            String name = itemEntity.getStack().getName().getString();
            int count = itemEntity.getStack().getCount();
            entries.add(new ItemEntry(x, y, z, name, count));
        }

        List<ItemGroup> groups = new ArrayList<>();
        boolean[] used = new boolean[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            if (used[i]) continue;
            ItemEntry base = entries.get(i);
            double gx = base.x, gy = base.y, gz = base.z;
            Map<String, Integer> items = new HashMap<>();
            items.merge(base.name, base.count, Integer::sum);
            used[i] = true;
            int groupCount = 1;

            for (int j = i + 1; j < entries.size(); j++) {
                if (used[j]) continue;
                ItemEntry other = entries.get(j);
                double dx = base.x - other.x, dy = base.y - other.y, dz = base.z - other.z;
                if (dx * dx + dy * dy + dz * dz <= groupDistSq) {
                    items.merge(other.name, other.count, Integer::sum);
                    gx += other.x; gy += other.y; gz += other.z;
                    used[j] = true;
                    groupCount++;
                }
            }
            gx /= groupCount; gy /= groupCount; gz /= groupCount;
            groups.add(new ItemGroup(gx, gy + 0.5, gz, items));
        }

        DrawContext context = event.context();
        MatrixStack ms = context.getMatrices();
        float s = scale.getValue();
        float fontSize = 5.4f * s;
        float padding = 2.8f * s;
        float lineGap = 1.5f * s;
        float round = 3.5f * s;

        for (ItemGroup group : groups) {
            if (!ProjectionUtil.isInFrontOfCamera(group.x, group.y, group.z)) continue;
            Vector2f projected = ProjectionUtil.project(group.x, group.y, group.z);
            if (!ProjectionUtil.isProjectedOnScreen(projected, 50f)) continue;

            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, Integer> e : group.items.entrySet()) {
                lines.add(e.getValue() > 1 ? e.getKey() + " x" + e.getValue() : e.getKey());
            }

            float maxW = 0;
            for (String line : lines) {
                maxW = Math.max(maxW, Fonts.PS_MEDIUM.getWidth(line, fontSize));
            }
            float totalW = maxW + padding * 2f;
            float totalH = padding * 2f + lines.size() * fontSize + Math.max(0, lines.size() - 1) * lineGap;

            float cardX = projected.x - totalW / 2f;
            float cardY = projected.y;

            drawGlassDark(ms, cardX, cardY, totalW, totalH, round);

            float textY = cardY + padding;
            for (int i = 0; i < lines.size(); i++) {
                Color textCol = i == 0 ? UIColors.textColor() : UIColors.mutedText(220);
                Fonts.PS_MEDIUM.drawText(ms, lines.get(i), cardX + padding, textY, fontSize, textCol);
                textY += fontSize + lineGap;
            }
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

    private record ItemEntry(double x, double y, double z, String name, int count) {}
    private record ItemGroup(double x, double y, double z, Map<String, Integer> items) {}
}