package sweetie.nezi.client.ui.widget.overlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.widget.Widget;
import sweetie.nezi.client.ui.widget.WidgetManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ArmorWidget extends Widget {
    private static final float LOW_DURA_THRESHOLD = 0.07f;

    public ArmorWidget() { super(0f, 0f); }
    @Override public String getName() { return "Armor"; }

    private final List<ItemStack> ITEMS = new ArrayList<>();
    private long lastWarningMs;

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        MatrixStack ms = event.matrixStack();
        DrawContext ctx = event.context();

        updateItems();
        if (ITEMS.isEmpty() || mc.getWindow() == null) return;

        float itemSize = scaled(14f);
        float gap = scaled(3f);
        float fS = scaled(5.5f);
        float duraTextOffset = scaled(2f); // offset above icon

        float contentW = ITEMS.size() * itemSize + (ITEMS.size() - 1) * gap;
        float height = itemSize + scaled(8f) + fS + duraTextOffset; // extra space for durability text
        float x = mc.getWindow().getScaledWidth() / 2f + scaled(95f);
        float y = mc.getWindow().getScaledHeight() - scaled(20f) - fS - duraTextOffset;

        getDraggable().setX(x);
        getDraggable().setY(y);
        getDraggable().setWidth(contentW);
        getDraggable().setHeight(height);

        float curX = x;
        float curY = y + fS + duraTextOffset; // start after durability text area
        float scale = itemSize / 16f;
        boolean hasLowDura = false;

        for (ItemStack item : ITEMS) {
            // ── Durability text above icon ─────────────────────────────────────
            if (item.isDamageable()) {
                float max = item.getMaxDamage();
                float dmg = item.getDamage();
                float progress = (max - dmg) / max;

                if (progress < 1f) {
                    // Color coding: 100-60% green, 60-15% yellow, 15-1% red
                    Color duraColor;
                    if (progress >= 0.60f) {
                        duraColor = UIColors.positiveColor(255); // Green
                    } else if (progress >= 0.15f) {
                        duraColor = UIColors.middleColor(255); // Yellow
                    } else {
                        duraColor = UIColors.negativeColor(255); // Red
                    }

                    int pct = (int)(progress * 100f);
                    String duraText = pct + "%";
                    float tw = Fonts.PS_MEDIUM.getWidth(duraText, fS);
                    float textX = curX + itemSize / 2f - tw / 2f;
                    float textY = y; // at the top
                    Fonts.PS_MEDIUM.drawText(ms, duraText, textX, textY, fS, duraColor);
                }

                if (progress < LOW_DURA_THRESHOLD) hasLowDura = true;
            }

            // ── Item icon ───────────────────────────────────────────────────────
            ms.push();
            ms.translate(curX, curY, 0f);
            ms.scale(scale, scale, 1f);
            ctx.drawItem(item, 0, 0);
            ms.pop();

            curX += itemSize + gap;
        }

        if (hasLowDura && System.currentTimeMillis() - lastWarningMs > 10000) {
            lastWarningMs = System.currentTimeMillis();
            NotifWidget nw = (NotifWidget) WidgetManager.getInstance().getWidgets().stream()
                    .filter(w -> w instanceof NotifWidget).findFirst().orElse(null);
            if (nw != null) nw.addNotif("§cLow Durability");
        }
    }

    private void updateItems() {
        ITEMS.clear();
        if (mc.player == null) return;
        PlayerEntity player = mc.player;
        List<ItemStack> armor = player.getInventory().armor;

        for (int i = 0; i < armor.size(); i++) {
            if (!armor.get(i).isEmpty()) ITEMS.add(armor.get(i));
        }
        if (!player.getOffHandStack().isEmpty()) ITEMS.add(player.getOffHandStack());
        if (!player.getMainHandStack().isEmpty()) ITEMS.add(player.getMainHandStack());
    }

    @Override public void render(MatrixStack matrixStack) {}
}
