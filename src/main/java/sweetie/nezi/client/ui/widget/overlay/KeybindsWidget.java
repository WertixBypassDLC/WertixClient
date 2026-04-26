package sweetie.nezi.client.ui.widget.overlay;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleManager;
import sweetie.nezi.api.system.backend.KeyStorage;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.api.utils.render.fonts.Icons;
import sweetie.nezi.client.features.modules.render.InterfaceModule;
import sweetie.nezi.client.ui.widget.ContainerWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeybindsWidget extends ContainerWidget {

    private final Map<Module, Float> animMap = new HashMap<>();
    private float heightAnim = 0f;

    public KeybindsWidget() { super(3f, 120f); }
    @Override public String getName() { return "Hotkeys"; }
    @Override protected Map<String, ContainerElement.ColoredString> getCurrentData() { return null; }

    @Override
    public boolean shouldAppearWhenInterfaceVisible() {
        if (mc.currentScreen instanceof ChatScreen) {
            return true;
        }

        for (float animation : animMap.values()) {
            if (animation > 0.01f) {
                return true;
            }
        }

        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.isEnabled() && module.hasBind()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(MatrixStack ms) {
        List<Module> visibleModules = new ArrayList<>();

        for (Module module : ModuleManager.getInstance().getModules()) {
            boolean active = module.isEnabled() && module.hasBind();
            float current = animMap.getOrDefault(module, 0f);
            animMap.put(module, current + ((active ? 1f : 0f) - current) * 0.15f);

            if (animMap.getOrDefault(module, 0f) > 0.01f) {
                visibleModules.add(module);
            }
        }

        animMap.entrySet().removeIf(entry -> !entry.getKey().isEnabled() && entry.getValue() < 0.01f);

        visibleModules.sort(Comparator.comparing(Module::getLocalizedName));

        boolean isChatPreview = mc.currentScreen instanceof ChatScreen && visibleModules.isEmpty();
        if (visibleModules.isEmpty() && !isChatPreview && getAppearProgress() <= 0.01f) {
            return;
        }

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
        float sqS = hdrH - scaled(3f);
        float nameKeyGap = scaled(12f);

        // Example entries for chat preview mode
        String exampleName = "Example";
        String exampleKey = "R";

        float maxNameW = 0f;
        float maxKeyW = 0f;
        if (isChatPreview) {
            maxNameW = Fonts.PS_MEDIUM.getWidth(exampleName, fRow);
            float keyWidth = Fonts.PS_MEDIUM.getWidth("[", fRow)
                    + Fonts.PS_BOLD.getWidth(exampleKey, fRow)
                    + Fonts.PS_MEDIUM.getWidth("]", fRow);
            maxKeyW = keyWidth;
        }
        for (Module module : visibleModules) {
            maxNameW = Math.max(maxNameW, Fonts.PS_MEDIUM.getWidth(module.getLocalizedName(), fRow));
            String key = KeyStorage.getBind(module.getBind());
            float keyWidth = Fonts.PS_MEDIUM.getWidth("[", fRow)
                    + Fonts.PS_BOLD.getWidth(key, fRow)
                    + Fonts.PS_MEDIUM.getWidth("]", fRow);
            maxKeyW = Math.max(maxKeyW, keyWidth);
        }

        float hdrContentW = Fonts.PS_BOLD.getWidth("Hotkeys", fTit) + scaled(34f);
        float rowInnW = maxNameW + nameKeyGap + maxKeyW;
        float cardW = Math.max(hdrContentW, rowInnW + scaled(20f));

        float totalRows = 0f;
        for (Module module : visibleModules) {
            totalRows += (rowH + rowG) * animMap.getOrDefault(module, 0f);
        }
        if (isChatPreview) {
            totalRows = rowH + rowG;
        }

        float targetH = hdrH + scaled(4f) + totalRows + p;
        heightAnim += (targetH - heightAnim) * 0.15f;
        float cardH = heightAnim;

        ms.push();
        drawHudCard(ms, x, y, cardW, cardH, hr, 255);
        drawJoinedHeader(ms, x + hGap, y + hGap, cardW - hGap * 2f, hdrH,
                scaled(4.2f), 255, Icons.KEYBINDS.getLetter(), "Hotkeys", iS, fTit);

        float currentY = y + hGap + hdrH + scaled(4f);
        for (Module module : visibleModules) {
            float anim = animMap.getOrDefault(module, 0f);
            if (anim < 0.01f) {
                continue;
            }

            int alpha = Math.max(5, (int) (255f * anim));
            float midY = currentY + rowH / 2f;
            String key = KeyStorage.getBind(module.getBind());
            String name = module.getLocalizedName();

            float leftBracketWidth = Fonts.PS_MEDIUM.getWidth("[", fRow);
            float rightBracketWidth = Fonts.PS_MEDIUM.getWidth("]", fRow);
            float keyWidth = Fonts.PS_BOLD.getWidth(key, fRow);
            float keyStartX = x + cardW - p - leftBracketWidth - keyWidth - rightBracketWidth;
            float availableNameWidth = Math.max(0f, keyStartX - (x + p) - nameKeyGap);

            ScissorUtil.start(ms, x + p, currentY, availableNameWidth, rowH);
            Fonts.PS_MEDIUM.drawText(ms, name,
                    x + p, midY - fRow / 2f + scaled(0.5f),
                    fRow, widgetTextColor(alpha));
            ScissorUtil.stop(ms);

            Fonts.PS_MEDIUM.drawText(ms, "[", keyStartX,
                    midY - fRow / 2f + scaled(0.5f), fRow, widgetTextColor(alpha));
            Fonts.PS_BOLD.drawText(ms, key, keyStartX + leftBracketWidth,
                    midY - fRow / 2f + scaled(0.5f), fRow, widgetTextColor(alpha));
            Fonts.PS_MEDIUM.drawText(ms, "]", keyStartX + leftBracketWidth + keyWidth,
                    midY - fRow / 2f + scaled(0.5f), fRow, widgetTextColor(alpha));

            currentY += (rowH + rowG) * anim;
        }

        // Render chat preview example entry
        if (isChatPreview) {
            int alpha = 180;
            float midY = currentY + rowH / 2f;

            float leftBracketWidth = Fonts.PS_MEDIUM.getWidth("[", fRow);
            float rightBracketWidth = Fonts.PS_MEDIUM.getWidth("]", fRow);
            float exKeyWidth = Fonts.PS_BOLD.getWidth(exampleKey, fRow);
            float keyStartX = x + cardW - p - leftBracketWidth - exKeyWidth - rightBracketWidth;

            Fonts.PS_MEDIUM.drawText(ms, exampleName,
                    x + p, midY - fRow / 2f + scaled(0.5f),
                    fRow, widgetTextColor(alpha));

            Fonts.PS_MEDIUM.drawText(ms, "[", keyStartX,
                    midY - fRow / 2f + scaled(0.5f), fRow, widgetTextColor(alpha));
            Fonts.PS_BOLD.drawText(ms, exampleKey, keyStartX + leftBracketWidth,
                    midY - fRow / 2f + scaled(0.5f), fRow, widgetTextColor(alpha));
            Fonts.PS_MEDIUM.drawText(ms, "]", keyStartX + leftBracketWidth + exKeyWidth,
                    midY - fRow / 2f + scaled(0.5f), fRow, widgetTextColor(alpha));
        }

        ms.pop();

        getDraggable().setWidth(cardW);
        getDraggable().setHeight(cardH);
    }
}
