package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Shulker View", category = Category.RENDER)
public class ShulkerViewModule extends Module {
    @Getter
    private static final ShulkerViewModule instance = new ShulkerViewModule();

    private final BooleanSetting groundItems = new BooleanSetting("Ground items").value(true);
    private final SliderSetting range = new SliderSetting("Range").value(12f).range(4f, 32f).step(1f);

    public ShulkerViewModule() {
        addSettings(groundItems, range);
    }

    @Override
    public void onEvent() {
        EventListener render2D = Render2DEvent.getInstance().subscribe(new Listener<>(event -> renderGroundPreviews(event.context())));
        addEvents(render2D);
    }

    public void renderHoveredPreview(DrawContext context, Slot hoveredSlot, int mouseX, int mouseY) {
        if (!isEnabled() || hoveredSlot == null || !hoveredSlot.hasStack()) {
            return;
        }

        ItemStack stack = hoveredSlot.getStack();
        List<ItemStack> items = getPreviewItems(stack);
        if (items.isEmpty()) {
            return;
        }

        float scale = 0.82f;
        float width = getPreviewWidth(scale);
        float height = getPreviewHeight(scale);
        float x = mouseX + 10f;
        float y = mouseY + 18f;

        if (x + width > width()) {
            x = mouseX - width - 10f;
        }
        if (y + height > height()) {
            y = mouseY - height - 10f;
        }

        x = Math.max(8f, Math.min(x, width() - width - 8f));
        y = Math.max(8f, Math.min(y, height() - height - 8f));
        drawPreview(context, stack, items, x, y, scale, 220);
    }

    private void renderGroundPreviews(DrawContext context) {
        if (!isEnabled() || !groundItems.getValue() || mc.world == null || mc.player == null) {
            return;
        }

        double renderRange = range.getValue();
        for (ItemEntity entity : mc.world.getEntitiesByClass(ItemEntity.class, mc.player.getBoundingBox().expand(renderRange), item -> !item.isRemoved())) {
            ItemStack stack = entity.getStack();
            List<ItemStack> items = getPreviewItems(stack);
            if (items.isEmpty()) {
                continue;
            }

            Vector2f projected = ProjectionUtil.project(entity.getX(), entity.getY() + 0.65D, entity.getZ());
            if (!ProjectionUtil.isProjectedOnScreen(projected, 80f)) {
                continue;
            }

            float scale = 0.58f;
            float width = getPreviewWidth(scale);
            float height = getPreviewHeight(scale);
            drawPreview(context, stack, items, projected.x - width / 2f, projected.y - height - 10f, scale, 190);
        }
    }

    private List<ItemStack> getPreviewItems(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) {
            return List.of();
        }

        List<ItemStack> items = new ArrayList<>();
        container.stream().forEach(items::add);
        boolean hasAny = items.stream().anyMatch(item -> item != null && !item.isEmpty());
        return hasAny ? items : List.of();
    }

    private void drawPreview(DrawContext context, ItemStack sourceStack, List<ItemStack> items, float x, float y, float scale, int alpha) {
        float width = getPreviewWidth(scale);
        float height = getPreviewHeight(scale);
        float round = scaled(6f, scale);
        float headerHeight = scaled(15f, scale);
        float pad = scaled(5f, scale);
        float slotSize = scaled(18f, scale);
        float slotGap = scaled(2f, scale);

        RenderUtil.BLUR_RECT.draw(context.getMatrices(), x, y, width, height, round, UIColors.blur(Math.min(230, alpha)), 0.12f);
        RenderUtil.RECT.draw(context.getMatrices(), x, y, width, height, round, new Color(10, 12, 18, alpha));
        RenderUtil.RECT.draw(context.getMatrices(), x + 0.8f, y + 0.8f, width - 1.6f, height - 1.6f, round - 0.8f, new Color(255, 255, 255, Math.min(18, alpha / 10)));
        RenderUtil.RECT.draw(context.getMatrices(), x, y, width, headerHeight, round, new Color(18, 24, 34, Math.min(220, alpha)));

        String title = sourceStack.getName().getString();
        Fonts.PS_BOLD.drawText(context.getMatrices(), title, x + pad, y + scaled(4f, scale), scaled(5.6f, scale), UIColors.textColor(alpha));

        float gridX = x + pad;
        float gridY = y + headerHeight + scaled(4f, scale);
        for (int slot = 0; slot < 27; slot++) {
            int column = slot % 9;
            int row = slot / 9;
            float slotX = gridX + column * (slotSize + slotGap);
            float slotY = gridY + row * (slotSize + slotGap);

            RenderUtil.RECT.draw(context.getMatrices(), slotX, slotY, slotSize, slotSize, scaled(3f, scale), new Color(18, 20, 28, Math.min(190, alpha)));
            RenderUtil.RECT.draw(context.getMatrices(), slotX, slotY, slotSize, slotSize, scaled(3f, scale), UIColors.stroke(Math.min(92, alpha)));

            if (slot < items.size()) {
                ItemStack stack = items.get(slot);
                if (stack != null && !stack.isEmpty()) {
                    context.drawItem(stack, Math.round(slotX + scaled(1f, scale)), Math.round(slotY + scaled(1f, scale)));
                }
            }
        }
    }

    private float getPreviewWidth(float scale) {
        return scaled(9f * 18f + 8f * 2f + 10f, scale);
    }

    private float getPreviewHeight(float scale) {
        return scaled(15f + 3f * 18f + 2f * 2f + 10f, scale);
    }

    private float scaled(float value, float scale) {
        return value * scale;
    }

    private int width() {
        return mc.getWindow().getScaledWidth();
    }

    private int height() {
        return mc.getWindow().getScaledHeight();
    }
}
