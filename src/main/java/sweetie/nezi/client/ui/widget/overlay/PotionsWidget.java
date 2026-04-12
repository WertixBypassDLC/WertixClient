package sweetie.nezi.client.ui.widget.overlay;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Language;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.other.TextUtil;
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

public class PotionsWidget extends ContainerWidget {
    private record PotionLine(String id, String name, String time) {}

    private final Map<String, Float> animMap = new HashMap<>();
    private final Map<String, PotionLine> lineCache = new HashMap<>();
    private float heightAnim = 0f;

    public PotionsWidget() { super(3f, 180f); }
    @Override public String getName() { return "Potions"; }
    @Override protected Map<String, ContainerElement.ColoredString> getCurrentData() { return null; }

    @Override
    public boolean shouldAppearWhenInterfaceVisible() {
        return mc.currentScreen instanceof ChatScreen || (mc.player != null && !mc.player.getActiveStatusEffects().isEmpty());
    }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        List<PotionLine> currentLines = new ArrayList<>();
        for (StatusEffectInstance effect : mc.player.getActiveStatusEffects().values()) {
            if (effect.getDuration() <= 0) {
                continue;
            }

            currentLines.add(new PotionLine(
                    effect.getTranslationKey(),
                    effectName(effect),
                    TextUtil.getDurationText(effect.getDuration())
            ));
        }

        if (currentLines.isEmpty() && mc.currentScreen instanceof ChatScreen) {
            currentLines.add(new PotionLine("__mock_blindness__", "Slowness", "42:28"));
        }

        Set<String> activeIds = new HashSet<>();
        for (PotionLine line : currentLines) {
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

        List<PotionLine> renderLines = new ArrayList<>(lineCache.values());
        renderLines.sort(Comparator.comparing(PotionLine::name));

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float hr = hudRound();
        float hdrH = scaled(13.4f);
        float p = scaled(4f);
        float hGap = scaled(2f);
        float fTit = scaled(5.8f);
        float fRow = scaled(5.3f);
        float iS = scaled(7.9f);

        float maxNameW = 0f;
        for (PotionLine line : renderLines) {
            maxNameW = Math.max(maxNameW, Fonts.PS_MEDIUM.getWidth(line.name(), fRow));
        }
        float cardW = Math.max(maxNameW + scaled(45f), Fonts.PS_BOLD.getWidth("Potions", fTit) + scaled(34f));

        float rowHeightFull = scaled(10f) + scaled(1.5f);
        float totRows = 0f;
        for (PotionLine line : renderLines) {
            totRows += rowHeightFull * animMap.getOrDefault(line.id(), 0f);
        }

        float targetH = hdrH + scaled(4f) + totRows + (renderLines.isEmpty() ? hGap * 2f : p);
        heightAnim += (targetH - heightAnim) * 0.15f;
        float cardH = heightAnim;

        ms.push();
        drawHudCard(ms, x, y, cardW, cardH, hr, 255);
        drawJoinedHeader(ms, x + hGap, y + hGap, cardW - hGap * 2f, hdrH,
                scaled(4.2f), 255, Icons.POTIONS.getLetter(), "Potions", iS, fTit);

        float cY = y + hGap + hdrH + scaled(4f);
        for (PotionLine line : renderLines) {
            cY = renderEffectRow(ms, line, cY, x, cardW, scaled(10f), scaled(1.5f), p, fRow);
        }
        ms.pop();

        getDraggable().setWidth(cardW);
        getDraggable().setHeight(cardH);
    }

    private float renderEffectRow(MatrixStack ms, PotionLine line, float cY, float x, float cardW,
                                  float rowH, float rowG, float p, float fRow) {
        float anim = animMap.getOrDefault(line.id(), 0f);
        if (anim < 0.01f) {
            return cY;
        }

        int alpha = (int) (255f * anim);
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

    private String effectName(StatusEffectInstance effect) {
        String base = Language.getInstance().get(effect.getTranslationKey());
        if (effect.getAmplifier() <= 0) {
            return base;
        }

        int level = effect.getAmplifier() + 1;
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        String roman = (level >= 1 && level <= 10) ? numerals[level] : String.valueOf(level);
        return base + " " + roman;
    }
}
