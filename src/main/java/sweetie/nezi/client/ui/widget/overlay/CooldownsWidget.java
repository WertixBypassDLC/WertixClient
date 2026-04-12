package sweetie.nezi.client.ui.widget.overlay;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.api.utils.render.fonts.Icons;
import sweetie.nezi.client.ui.widget.ContainerWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CooldownsWidget extends ContainerWidget {
    private record Line(String id, String name, String time) {}

    private final Map<String, Float> animMap = new HashMap<>();
    private final Map<String, Line> lineCache = new HashMap<>();
    private float heightAnim = 0f;

    public CooldownsWidget() { super(200f, 100f); }
    @Override public String getName() { return "Cooldowns"; }
    @Override protected Map<String, ContainerElement.ColoredString> getCurrentData() { return null; }

    @Override
    public boolean shouldAppearWhenInterfaceVisible() {
        if (mc.currentScreen instanceof ChatScreen) {
            return true;
        }
        if (mc.player == null) {
            return false;
        }

        ItemCooldownManager cooldownManager = mc.player.getItemCooldownManager();
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && cooldownManager.isCoolingDown(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        List<Line> currentLines = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        ItemCooldownManager cooldownManager = mc.player.getItemCooldownManager();

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !cooldownManager.isCoolingDown(stack)) {
                continue;
            }

            String id = stack.getItem().getTranslationKey();
            if (!seenIds.add(id)) {
                continue;
            }

            int remainingTicks = getCooldownRemainingTicks(cooldownManager, stack);
            if (remainingTicks <= 0) {
                continue;
            }

            currentLines.add(new Line(id, stack.getName().getString(), formatTicks(remainingTicks)));
        }

        if (currentLines.isEmpty() && mc.currentScreen instanceof ChatScreen) {
            currentLines.add(new Line("__mock_cooldown__", "Ender Pearl", "0:16"));
        }

        Set<String> activeIds = new HashSet<>();
        for (Line line : currentLines) {
            activeIds.add(line.id());
            lineCache.put(line.id(), line);
            float current = animMap.getOrDefault(line.id(), 0f);
            animMap.put(line.id(), Math.min(1f, current + (1f - current) * 0.15f));
        }

        for (Map.Entry<String, Float> entry : animMap.entrySet()) {
            if (!activeIds.contains(entry.getKey())) {
                entry.setValue(entry.getValue() + (0f - entry.getValue()) * 0.15f);
            }
        }

        animMap.entrySet().removeIf(entry -> {
            boolean stale = !activeIds.contains(entry.getKey()) && entry.getValue() < 0.01f;
            if (stale) {
                lineCache.remove(entry.getKey());
            }
            return stale;
        });

        if (lineCache.isEmpty() && !(mc.currentScreen instanceof ChatScreen)) {
            return;
        }

        List<Line> renderLines = new ArrayList<>(lineCache.values());
        renderLines.sort(Comparator.comparing(Line::name));

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float hr = hudRound();
        float hdrH = scaled(13.4f);
        float p = scaled(4f);
        float hGap = scaled(2f);
        float fTit = scaled(5.8f);
        float fRow = scaled(5.3f);
        float iS = scaled(7.9f);
        float rowH = scaled(10f);
        float rowG = scaled(1.5f);

        float maxNameW = 0f;
        float maxTimeW = 0f;
        for (Line line : renderLines) {
            maxNameW = Math.max(maxNameW, Fonts.PS_MEDIUM.getWidth(line.name(), fRow));
            maxTimeW = Math.max(maxTimeW, Fonts.PS_BOLD.getWidth(line.time(), fRow));
        }

        float headerWidth = Fonts.PS_BOLD.getWidth("Cooldowns", fTit) + scaled(34f);
        float rowWidth = maxNameW + scaled(6f) + maxTimeW;
        float cardW = Math.max(headerWidth, rowWidth) + p * 2f + scaled(2f);

        float totRows = 0f;
        for (Line line : renderLines) {
            totRows += (rowH + rowG) * animMap.getOrDefault(line.id(), 0f);
        }

        float targetH = hdrH + scaled(4f) + totRows + (renderLines.isEmpty() ? hGap * 2f : p);
        heightAnim += (targetH - heightAnim) * 0.15f;
        float cardH = heightAnim;

        ms.push();
        drawHudCard(ms, x, y, cardW, cardH, hr, 255);
        drawJoinedHeader(ms, x + hGap, y + hGap, cardW - hGap * 2f, hdrH,
                scaled(4.2f), 255, Icons.COOLDOWNS.getLetter(), "Cooldowns", iS, fTit);

        float cY = y + hGap + hdrH + scaled(4f);
        for (Line line : renderLines) {
            cY = renderCooldownRow(ms, line, cY, x, cardW, rowH, rowG, p, fRow);
        }
        ms.pop();

        getDraggable().setWidth(cardW);
        getDraggable().setHeight(cardH);
    }

    private float renderCooldownRow(MatrixStack ms, Line line, float cY, float x, float cardW,
                                    float rowH, float rowG, float p, float fRow) {
        float anim = animMap.getOrDefault(line.id(), 0f);
        if (anim < 0.01f) {
            return cY;
        }

        int alpha = Math.max(5, (int) (255f * anim));
        float midY = cY + rowH / 2f;

        Fonts.PS_MEDIUM.drawText(ms, line.name(),
                x + p, midY - fRow / 2f + scaled(0.5f),
                fRow, widgetTextColor(alpha));

        float timeWidth = Fonts.PS_BOLD.getWidth(line.time(), fRow);
        Fonts.PS_BOLD.drawText(ms, line.time(),
                x + cardW - p - timeWidth,
                midY - fRow / 2f + scaled(0.5f),
                fRow, widgetTextColor(alpha));

        return cY + (rowH + rowG) * anim;
    }

    private int getCooldownRemainingTicks(ItemCooldownManager cooldownManager, ItemStack stack) {
        Map<Identifier, ItemCooldownManager.Entry> entries = cooldownManager.entries;
        int currentTick = cooldownManager.tick;
        Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
        ItemCooldownManager.Entry entry = entries.get(itemId);
        if (entry == null) {
            return 0;
        }

        return Math.max(0, entry.endTick() - currentTick);
    }

    private String formatTicks(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
