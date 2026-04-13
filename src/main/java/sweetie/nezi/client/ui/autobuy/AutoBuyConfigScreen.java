package sweetie.nezi.client.ui.autobuy;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.other.AutoBuyModule;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeItem;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeRule;
import sweetie.nezi.client.services.RenderService;

import java.awt.Color;
import java.util.Locale;

public class AutoBuyConfigScreen extends Screen {
    private final AutoBuyModule module;
    private final AnimationUtil openAnimation = new AnimationUtil();

    private final InputField buyField = new InputField("Покупка <= за 1 шт");
    private final InputField sellField = new InputField("Продажа");
    private final InputField countField = new InputField("Мин. стак");

    private boolean closing;
    private AutoTradeItem selectedItem = AutoTradeItem.INVISIBILITY;
    private float listScroll;
    private final AnimationUtil listScrollAnimation = new AnimationUtil();
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float listX;
    private float listY;
    private float listWidth;
    private float listHeight;
    private float searchButtonX;
    private float searchButtonY;
    private float searchButtonWidth;
    private float searchButtonHeight;

    private AutoBuyConfigScreen(AutoBuyModule module) {
        super(Text.literal("Настройка автобая"));
        this.module = module;
    }

    public static AutoBuyConfigScreen create(AutoBuyModule module) {
        return new AutoBuyConfigScreen(module);
    }

    @Override
    protected void init() {
        closing = false;
        listScroll = 0f;
        syncFields();
        super.init();
    }

    @Override
    public void close() {
        closing = true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.update();
        openAnimation.run(closing ? 0.0 : 1.0, 320, Easing.EXPO_OUT);
        float anim = (float) openAnimation.getValue();
        if (closing && anim <= 0.03f) {
            if (client != null) {
                client.setScreen(null);
            }
            return;
        }

        float sw = width;
        float sh = height;
        panelWidth = scaled(414f);
        panelHeight = scaled(234f);
        panelX = sw / 2f - panelWidth / 2f;
        panelY = sh / 2f - panelHeight / 2f + (1f - anim) * scaled(22f);

        renderBackdrop(context, sw, sh, anim);
        listScrollAnimation.update();
        listScrollAnimation.run(listScroll, 220, Easing.QUINT_OUT);

        MatrixStack ms = context.getMatrices();
        ms.push();
        float pivotX = panelX + panelWidth / 2f;
        float pivotY = panelY + panelHeight / 2f;
        float scale = 0.92f + anim * 0.08f;
        ms.translate(pivotX, pivotY, 0f);
        ms.scale(scale, scale, 1f);
        ms.translate(-pivotX, -pivotY, 0f);

        drawShell(ms, anim);
        drawHeader(ms, anim);
        drawLeftGrid(context, ms, mouseX, mouseY, anim);
        drawRightEditor(ms, mouseX, mouseY, anim);
        drawFooter(ms, anim);
        ms.pop();

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBackdrop(DrawContext context, float sw, float sh, float anim) {
        int alpha = MathHelper.clamp((int) (235f * anim), 0, 235);
        RenderUtil.BLUR_RECT.draw(context.getMatrices(), 0f, 0f, sw, sh, 0f, UIColors.blur(alpha), 0.10f);
        RenderUtil.BLUR_RECT.draw(context.getMatrices(), 0f, 0f, sw, sh, 0f, UIColors.backgroundBlur(Math.min(180, alpha)), 0.08f);
    }

    private void drawShell(MatrixStack ms, float anim) {
        int alpha = MathHelper.clamp((int) (255f * anim), 0, 255);
        float round = scaled(8f);
        drawGlassSurface(ms, panelX, panelY, panelWidth, panelHeight, round,
                UIColors.card(Math.min(alpha, 120)), alpha, false);
    }

    private void drawHeader(MatrixStack ms, float anim) {
        int alpha = MathHelper.clamp((int) (255f * anim), 0, 255);
        float pad = scaled(12f);
        float titleSize = scaled(9f);
        float subSize = scaled(5.8f);
        Fonts.PS_BOLD.drawText(ms, "Автобай",
                panelX + pad, panelY + scaled(10f), titleSize, UIColors.textColor(alpha));
        Fonts.PS_MEDIUM.drawText(ms,
                "ЛКМ открывает настройку, ПКМ включает предмет. Цена покупки считается за 1 шт.",
                panelX + pad, panelY + scaled(24f), subSize, UIColors.mutedText(Math.min(220, alpha)));
    }

    private void drawLeftGrid(DrawContext context, MatrixStack ms, int mouseX, int mouseY, float anim) {
        listX = panelX + scaled(12f);
        listY = panelY + scaled(42f);
        listWidth = scaled(236f);
        listHeight = scaled(164f);
        float tileW = scaled(51f);
        float tileH = scaled(55f);
        float gap = scaled(6f);
        float round = scaled(7f);
        int alpha = MathHelper.clamp((int) (255f * anim), 0, 255);
        drawGlassSurface(ms, listX, listY, listWidth, listHeight, round,
                UIColors.panelSoft(Math.min(alpha, 112)), alpha, false);

        float innerPad = scaled(6f);
        float contentX = listX + innerPad;
        float contentY = listY + innerPad;
        float contentHeight = getListContentHeight(tileH, gap);

        ScissorUtil.start(ms, listX + scaled(1f), listY + scaled(1f), listWidth - scaled(2f), listHeight - scaled(2f));
        int index = 0;
        for (AutoTradeItem item : AutoTradeItem.values()) {
            int col = index % 4;
            int row = index / 4;
            float x = contentX + col * (tileW + gap);
            float y = contentY + row * (tileH + gap) + (float) listScrollAnimation.getValue();
            drawTile(context, ms, item, x, y, tileW, tileH, mouseX, mouseY, anim);
            index++;
        }
        ScissorUtil.stop(ms);

        if (contentHeight > listHeight - innerPad * 2f + scaled(1f)) {
            float viewport = listHeight - innerPad * 2f;
            float barH = Math.max(scaled(18f), viewport * (viewport / contentHeight));
            float scrollRange = Math.max(1f, contentHeight - viewport);
            float progress = MathHelper.clamp(-((float) listScrollAnimation.getValue()) / scrollRange, 0f, 1f);
            float barY = listY + innerPad + (viewport - barH) * progress;
            RenderUtil.RECT.draw(ms, listX + listWidth - scaled(3f), barY, scaled(1.4f), barH, scaled(0.7f), UIColors.stroke(Math.min(alpha, 92)));
        }
    }

    private void drawTile(DrawContext context, MatrixStack ms, AutoTradeItem item,
                          float x, float y, float w, float h, int mouseX, int mouseY, float anim) {
        AutoTradeRule rule = module.getRule(item);
        boolean hovered = MouseUtil.isHovered(mouseX, mouseY, x, y, w, h);
        boolean selected = item == selectedItem;
        int alpha = MathHelper.clamp((int) (255f * anim), 0, 255);
        float round = scaled(5f);

        Color bg = selected
                ? ColorUtil.interpolate(UIColors.cardSecondary(Math.min(208, alpha)), UIColors.primary(Math.min(92, alpha)), 0.18f)
                : hovered
                ? UIColors.panelSoft(Math.min(180, alpha))
                : UIColors.panel(Math.min(150, alpha));
        Color stroke = rule.isEnabled()
                ? UIColors.primary(Math.min(148, alpha))
                : selected
                ? UIColors.secondary(Math.min(96, alpha))
                : UIColors.stroke(Math.min(94, alpha));

        drawGlassSurface(ms, x, y, w, h, round, bg, alpha, true);
        RenderUtil.RECT.draw(ms, x, y, w, h, round, stroke);

        float iconScale = scaled(0.80f);
        ms.push();
        ms.translate(x + scaled(7f), y + scaled(7f), 0f);
        ms.scale(iconScale, iconScale, 1f);
        context.drawItem(item.copyDisplayStack(), 0, 0);
        ms.pop();

        float titleX = x + scaled(6f);
        Fonts.PS_BOLD.drawText(ms, item.getTitle(), titleX, y + scaled(22f), scaled(4.65f), UIColors.textColor(alpha));
        Fonts.PS_MEDIUM.drawText(ms, item.getSubtitle(), titleX, y + scaled(29f), scaled(4.05f), UIColors.mutedText(Math.min(220, alpha)));

        String status = rule.isEnabled() ? "ВКЛ" : "ВЫКЛ";
        float pillW = scaled(16f);
        float pillH = scaled(11f);
        float pillX = x + w - pillW - scaled(5f);
        float pillY = y + scaled(5f);
        Color toggleBg = rule.isEnabled()
                ? ColorUtil.interpolate(UIColors.cardSecondary(Math.min(196, alpha)), UIColors.primary(Math.min(108, alpha)), 0.26f)
                : UIColors.panel(Math.min(154, alpha));
        drawGlassSurface(ms, pillX, pillY, pillW, pillH, scaled(3f), toggleBg, alpha, true);
        Fonts.PS_BOLD.drawCenteredText(ms, status, pillX + pillW / 2f, pillY + scaled(2.2f), scaled(3.9f),
                rule.isEnabled() ? UIColors.primary(alpha) : UIColors.inactiveTextColor(alpha));

        String buyPrefix = "К";
        String sellPrefix = "П";
        String buy = rule.getBuyUnitPrice() > 0 ? buyPrefix + " " + formatCompactPrice(rule.getBuyUnitPrice()) : buyPrefix + " -";
        String sell = rule.getSellPrice() > 0 ? sellPrefix + " " + formatCompactPrice(rule.getSellPrice()) : sellPrefix + " -";
        float statY = y + h - scaled(11f);
        float statW = scaled(18f);
        float leftStatX = titleX;
        float rightStatX = x + w - scaled(24f);
        drawGlassSurface(ms, leftStatX, statY, statW, scaled(7f), scaled(2f),
                ColorUtil.interpolate(UIColors.panel(Math.min(116, alpha)), UIColors.primary(Math.min(72, alpha)), 0.14f), alpha, true);
        drawGlassSurface(ms, rightStatX, statY, statW, scaled(7f), scaled(2f),
                ColorUtil.interpolate(UIColors.panel(Math.min(116, alpha)), UIColors.secondary(Math.min(68, alpha)), 0.12f), alpha, true);
        Fonts.PS_MEDIUM.drawCenteredText(ms, buy, leftStatX + statW / 2f, statY + scaled(1.0f), scaled(3.45f), UIColors.textColor(Math.min(225, alpha)));
        Fonts.PS_MEDIUM.drawCenteredText(ms, sell, rightStatX + statW / 2f, statY + scaled(1.0f), scaled(3.45f), UIColors.textColor(Math.min(225, alpha)));
    }

    private void drawRightEditor(MatrixStack ms, int mouseX, int mouseY, float anim) {
        AutoTradeRule rule = module.getRule(selectedItem);
        int alpha = MathHelper.clamp((int) (255f * anim), 0, 255);
        float x = panelX + scaled(258f);
        float y = panelY + scaled(54f);
        float w = scaled(144f);
        float h = scaled(152f);
        float round = scaled(6f);

        drawGlassSurface(ms, x, y, w, h, round, UIColors.card(Math.min(118, alpha)), alpha, false);
        Fonts.PS_BOLD.drawText(ms, selectedItem.getTitle() + " " + selectedItem.getSubtitle(),
                x + scaled(8f), y + scaled(8f), scaled(5.7f), UIColors.textColor(alpha));

        float rowX = x + scaled(10f);
        float rowW = w - scaled(20f);
        buyField.render(ms, rowX, y + scaled(24f), rowW, scaled(24f), mouseX, mouseY, alpha);
        sellField.render(ms, rowX, y + scaled(53f), rowW, scaled(24f), mouseX, mouseY, alpha);
        countField.render(ms, rowX, y + scaled(82f), rowW, scaled(24f), mouseX, mouseY, alpha);

        searchButtonX = x + scaled(9f);
        searchButtonY = y + h - scaled(29f);
        searchButtonWidth = w - scaled(18f);
        searchButtonHeight = scaled(15f);
        boolean hoveredSearch = MouseUtil.isHovered(mouseX, mouseY, searchButtonX, searchButtonY, searchButtonWidth, searchButtonHeight);
        drawGlassSurface(ms, searchButtonX, searchButtonY, searchButtonWidth, searchButtonHeight, scaled(5f),
                hoveredSearch
                        ? ColorUtil.interpolate(UIColors.cardSecondary(Math.min(196, alpha)), UIColors.primary(Math.min(86, alpha)), 0.18f)
                        : UIColors.panel(Math.min(168, alpha)),
                alpha, true);
        Fonts.PS_MEDIUM.drawCenteredText(ms, "Открыть поиск на /ah",
                searchButtonX + searchButtonWidth / 2f,
                searchButtonY + scaled(4.0f), scaled(4.8f), UIColors.textColor(alpha));

        String enabledText = "Включено: " + (rule.isEnabled() ? "Да" : "Нет");
        Fonts.PS_MEDIUM.drawText(ms, enabledText, x + scaled(9f), y + h - scaled(10f), scaled(5.2f),
                rule.isEnabled() ? UIColors.primary(alpha) : UIColors.inactiveTextColor(alpha));
    }

    private void drawFooter(MatrixStack ms, float anim) {
        int alpha = MathHelper.clamp((int) (255f * anim), 0, 255);
        Fonts.PS_MEDIUM.drawText(ms,
                "ЛКМ по предмету -> редактировать. ПКМ -> вкл/выкл. ESC -> закрыть. Сохранение мгновенное.",
                panelX + scaled(12f), panelY + panelHeight - scaled(14f), scaled(5.2f),
                UIColors.mutedText(Math.min(215, alpha)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, searchButtonX, searchButtonY, searchButtonWidth, searchButtonHeight)) {
            module.openAuctionForItem(selectedItem);
            return true;
        }

        float tileW = scaled(51f);
        float tileH = scaled(55f);
        float gap = scaled(6f);
        float innerPad = scaled(6f);

        int index = 0;
        for (AutoTradeItem item : AutoTradeItem.values()) {
            int col = index % 4;
            int row = index / 4;
            float x = listX + innerPad + col * (tileW + gap);
            float y = listY + innerPad + row * (tileH + gap) + (float) listScrollAnimation.getValue();
            if (MouseUtil.isHovered(mouseX, mouseY, x, y, tileW, tileH)) {
                selectedItem = item;
                syncFields();
                if (button == 0) {
                    buyField.focused = true;
                    sellField.focused = false;
                    countField.focused = false;
                } else if (button == 1) {
                    AutoTradeRule rule = module.getRule(item);
                    rule.setEnabled(!rule.isEnabled());
                    module.commitRules();
                }
                return true;
            }
            index++;
        }

        buyField.mouseClicked(mouseX, mouseY, button);
        sellField.mouseClicked(mouseX, mouseY, button);
        countField.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (Character.isDigit(chr)) {
            if (buyField.append(chr) || sellField.append(chr) || countField.append(chr)) {
                applyFields();
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            close();
            return true;
        }

        if (keyCode == 259) {
            if (buyField.backspace() || sellField.backspace() || countField.backspace()) {
                applyFields();
                return true;
            }
        }

        if (keyCode == 258) {
            cycleFocus();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (MouseUtil.isHovered(mouseX, mouseY, listX, listY, listWidth, listHeight)) {
            float tileH = scaled(55f);
            float gap = scaled(6f);
            float innerPad = scaled(6f);
            float viewport = listHeight - innerPad * 2f;
            float content = getListContentHeight(tileH, gap);
            float minScroll = Math.min(0f, viewport - content);
            listScroll = MathHelper.clamp(listScroll + (float) (verticalAmount * scaled(18f)), minScroll, 0f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void syncFields() {
        AutoTradeRule rule = module.getRule(selectedItem);
        buyField.text = rule.getBuyUnitPrice() <= 0 ? "" : String.valueOf(rule.getBuyUnitPrice());
        sellField.text = rule.getSellPrice() <= 0 ? "" : String.valueOf(rule.getSellPrice());
        countField.text = String.valueOf(rule.getMinStackCount());
    }

    private void applyFields() {
        AutoTradeRule rule = module.getRule(selectedItem);
        rule.setBuyUnitPrice(parseInt(buyField.text));
        rule.setSellPrice(parseInt(sellField.text));
        rule.setMinStackCount(Math.max(1, parseInt(countField.text, 1)));
        module.commitRules();
    }

    private void cycleFocus() {
        if (buyField.focused) {
            buyField.focused = false;
            sellField.focused = true;
            return;
        }
        if (sellField.focused) {
            sellField.focused = false;
            countField.focused = true;
            return;
        }
        buyField.focused = true;
        countField.focused = false;
    }

    private int parseInt(String value) {
        return parseInt(value, 0);
    }

    private int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private float scaled(float value) {
        return RenderService.getInstance().scaled(value);
    }

    private float getListContentHeight(float tileH, float gap) {
        int rows = (int) Math.ceil(AutoTradeItem.values().length / 4f);
        return rows * tileH + Math.max(0, rows - 1) * gap;
    }

    private String formatCompactPrice(int value) {
        if (value >= 1_000_000) {
            return formatCompactUnit(value / 1_000_000f, "кк");
        }
        if (value >= 1_000) {
            return formatCompactUnit(value / 1_000f, "к");
        }
        return String.valueOf(value);
    }

    private String formatCompactUnit(float value, String suffix) {
        if (value >= 10f || Math.abs(value - Math.round(value)) < 0.05f) {
            return Math.round(value) + suffix;
        }
        return String.format(Locale.US, "%.1f%s", value, suffix);
    }

    private void drawGlassSurface(MatrixStack ms, float x, float y, float width, float height, float round,
                                  Color surface, int alpha, boolean compact) {
        int blurAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 1.04f) : (int) (alpha * 1.10f)));
        int backgroundBlurAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.96f) : (int) (alpha * 1.00f)));
        int overlayAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.10f) : (int) (alpha * 0.13f)));
        int strokeAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.22f) : (int) (alpha * 0.24f)));

        RenderUtil.BLUR_RECT.draw(ms, x, y, width, height, round, UIColors.blur(blurAlpha), 0.08f);
        RenderUtil.BLUR_RECT.draw(ms, x, y, width, height, round, UIColors.backgroundBlur(backgroundBlurAlpha), 0.06f);
        RenderUtil.RECT.draw(ms, x, y, width, height, round, surface);
        RenderUtil.RECT.draw(ms, x, y, width, height, round, UIColors.overlay(overlayAlpha));
        RenderUtil.RECT.draw(ms, x, y, width, height, round, UIColors.stroke(strokeAlpha));
    }

    private static class InputField {
        private final String label;
        private String text = "";
        private boolean focused;
        private float x;
        private float y;
        private float width;
        private float height;

        private InputField(String label) {
            this.label = label;
        }

        private void render(MatrixStack ms, float x, float y, float width, float height, int mouseX, int mouseY, int alpha) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;

            boolean hovered = MouseUtil.isHovered(mouseX, mouseY, x, y, width, height);
            float round = height / 2f;
            Color bg = focused
                    ? UIColors.cardSecondary(Math.min(204, alpha))
                    : UIColors.panel(Math.min(hovered ? 192 : 166, alpha));
            Color stroke = focused
                    ? UIColors.primary(Math.min(128, alpha))
                    : UIColors.stroke(Math.min(76, alpha));

            RenderUtil.BLUR_RECT.draw(ms, x, y, width, height, round, UIColors.blur(Math.min(255, (int) (alpha * 1.04f))), 0.08f);
            RenderUtil.BLUR_RECT.draw(ms, x, y, width, height, round, UIColors.backgroundBlur(Math.min(255, (int) (alpha * 0.96f))), 0.06f);
            RenderUtil.RECT.draw(ms, x, y, width, height, round, bg);
            RenderUtil.RECT.draw(ms, x, y, width, height, round, UIColors.overlay(Math.min(255, (int) (alpha * 0.10f))));
            RenderUtil.RECT.draw(ms, x, y, width, height, round, stroke);

            Fonts.PS_MEDIUM.drawText(ms, displayLabel(), x + height / 2f, y + scaled(3.6f), scaled(4.6f), UIColors.mutedText(alpha));
            String visible = text.isEmpty() ? "0" : text;
            if (focused && System.currentTimeMillis() % 900L > 420L) {
                visible += "_";
            }
            Fonts.PS_BOLD.drawText(ms, visible, x + height / 2f, y + height - scaled(9.1f), scaled(5.8f), UIColors.textColor(alpha));
        }

        private void mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                focused = MouseUtil.isHovered(mouseX, mouseY, x, y, width, height);
            }
        }

        private boolean append(char chr) {
            if (!focused || text.length() >= 8) {
                return false;
            }
            text += chr;
            return true;
        }

        private boolean backspace() {
            if (!focused || text.isEmpty()) {
                return false;
            }
            text = text.substring(0, text.length() - 1);
            return true;
        }

        private String displayLabel() {
            return label;
        }

        private float scaled(float value) {
            return RenderService.getInstance().scaled(value);
        }
    }
}