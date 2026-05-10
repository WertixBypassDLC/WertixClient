package sweetie.nezi.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.module.setting.ColorSetting;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.clickgui.module.ExpandableComponent;
import sweetie.nezi.client.ui.theme.Theme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static sweetie.nezi.api.system.interfaces.QuickImports.mc;

public class ColorComponent extends ExpandableComponent.ExpandableSettingComponent {
    private static final int MAX_RECENT = 6;
    private static final List<Color> recentColors = new ArrayList<>();

    private final Theme.ElementColor elementColor;
    private final ColorSetting setting;

    private boolean draggingHue = false;
    private boolean draggingSatBright = false;
    private boolean draggingAlpha = false;
    private boolean wasDragging = false;

    private float hueCache = 0f;
    private boolean inited;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.setting = setting;
        this.elementColor = null;
        updateHeight(getDefaultHeight());
        initHueCache();
    }

    public ColorComponent(Theme.ElementColor elementColor) {
        super(null);
        this.elementColor = elementColor;
        this.setting = null;
        updateHeight(getDefaultHeight());
    }

    private void initHueCache() {
        if (inited) {
            return;
        }

        Color color = getCurrentColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hueCache = hsb[0];

        inited = true;
    }

    private Color getCurrentColor() {
        return setting != null ? setting.getValue() : elementColor.getColor();
    }

    private void setCurrentColor(Color color) {
        if (setting != null) setting.setValue(color);
        else elementColor.setColor(color);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack ms = context.getMatrices();
        updateOpen();
        initHueCache();

        if (draggingHue) updateHue(mouseX);
        if (draggingSatBright) updateSatBright(mouseX, mouseY);
        if (draggingAlpha) updateAlpha(mouseX);

        float baseHeight = scaled(getDefaultHeight());
        float fontSize = baseHeight * 0.45f;

        int fullAlpha = (int) (getAlpha() * 255f);

        if (setting != null) {
            Fonts.PS_MEDIUM.drawText(ms, setting.getName(), getX() + scaled(2f), getY() + baseHeight / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));

            float previewSize = baseHeight * 0.55f;
            float previewX = getX() + getWidth() - previewSize - scaled(3f);
            float previewY = getY() + baseHeight / 2f - previewSize / 2f;
            float previewRound = previewSize * 0.22f;
            RenderUtil.RECT.draw(ms, previewX - scaled(0.6f), previewY - scaled(0.6f), previewSize + scaled(1.2f), previewSize + scaled(1.2f), previewRound + scaled(0.4f), UIColors.stroke(Math.min(fullAlpha, 140)));
            RenderUtil.RECT.draw(ms, previewX, previewY, previewSize, previewSize, previewRound, ColorUtil.setAlpha(getCurrentColor(), (int) (getCurrentColor().getAlpha() / 255f * fullAlpha)));
            updateHeight(getDefaultHeight());
        }

        float animValue = getAnimValue();
        if (animValue > 0.0) {
            float pickerY = getColorPickerY() + getAnimY();
            float pickerHeight = getColorPickerHeight();
            float hueY = getHueY() + getAnimY();
            float alphaY = getAlphaY() + getAnimY();
            float totalPickerHeight = (pickerHeight + getHueHeight() + getAlphaHeight() + gap() * 2f) * animValue;
            RenderUtil.BLUR_RECT.draw(ms, getPickerX(), pickerY, getPickerWidth(), totalPickerHeight, scaled(3.0f), UIColors.panelSoft(Math.min(fullAlpha, 192)));
            RenderUtil.RECT.draw(ms, getPickerX(), pickerY, getPickerWidth(), totalPickerHeight, scaled(3.0f), UIColors.stroke(Math.min(fullAlpha, 120)));
            Color[] colors = getGradientColors(animValue);
            float colorPickerRound = getWidth() * 0.02f;
            RenderUtil.GRADIENT_RECT.draw(ms, getPickerX() + scaled(2f), pickerY + scaled(2f), getPickerWidth() - scaled(4f), pickerHeight - scaled(2f), colorPickerRound, colors[0], colors[1], colors[2], colors[3]);

            drawHueBar(ms, animValue);
            drawAlphaBar(ms, animValue);
            drawRecentColors(ms, animValue);
            drawSelectors(ms);

            float alphaHeight = (getAlphaHeight() + gap());
            float recentH = getRecentRowHeight() > 0 ? (getRecentRowHeight() + gap()) : 0f;
            float extraHeight = (getHueHeight() + getColorPickerHeight() + alphaHeight + recentH + gap()) * animValue;
            float baseHeightFinal = setting != null ? baseHeight : 0f;
            setHeight(baseHeightFinal + extraHeight);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (setting != null && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()))) {
            toggleOpen();
            return;
        }

        if (isNotOver()) return;

        if (MouseUtil.isHovered(mouseX, mouseY, getPickerX(), getColorPickerY(), getPickerWidth(), getColorPickerHeight())) {
            draggingSatBright = true;
            wasDragging = true;
            updateSatBright(mouseX, mouseY);
        } else if (MouseUtil.isHovered(mouseX, mouseY, getPickerX(), getHueY(), getPickerWidth(), getHueHeight())) {
            draggingHue = true;
            wasDragging = true;
            updateHue(mouseX);
        } else if (MouseUtil.isHovered(mouseX, mouseY, getPickerX(), getAlphaY(), getPickerWidth(), getAlphaHeight())) {
            draggingAlpha = true;
            wasDragging = true;
            updateAlpha(mouseX);
        } else if (!recentColors.isEmpty() && getAnimValue() > 0.8f) {
            float ry = getRecentY();
            float size = getRecentRowHeight();
            float slotGap = scaled(2f);
            float sx = getPickerX() + scaled(2f);
            for (int i = 0; i < recentColors.size() && i < MAX_RECENT; i++) {
                float slotX = sx + i * (size + slotGap);
                if (MouseUtil.isHovered(mouseX, mouseY, slotX, ry, size, size)) {
                    Color picked = recentColors.get(i);
                    setCurrentColor(picked);
                    float[] hsb = Color.RGBtoHSB(picked.getRed(), picked.getGreen(), picked.getBlue(), null);
                    hueCache = hsb[0];
                    return;
                }
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (wasDragging) {
            addRecentColor(getCurrentColor());
            wasDragging = false;
        }
        draggingHue = false;
        draggingSatBright = false;
        draggingAlpha = false;
    }

    private void updateHue(double mouseX) {
        float rel = (float) ((mouseX - getPickerX()) / getPickerWidth());
        rel = Math.max(0f, Math.min(1f, rel));
        hueCache = rel;

        Color color = getCurrentColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        setCurrentColor(new Color(Color.HSBtoRGB(rel, hsb[1], hsb[2]), true));
    }

    private void updateSatBright(double mouseX, double mouseY) {
        float sat = (float) ((mouseX - getPickerX()) / getPickerWidth());
        float bri = 1f - (float) ((mouseY - getColorPickerY()) / getColorPickerHeight());
        sat = Math.max(0f, Math.min(1f, sat));
        bri = Math.max(0f, Math.min(1f, bri));

        Color color = getCurrentColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        float hue = (hsb[1] == 0 || hsb[2] == 0) ? hueCache : hsb[0];
        Color newColor = new Color(Color.HSBtoRGB(hue, sat, bri));
        setCurrentColor(new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), color.getAlpha()));
    }

    private void updateAlpha(double mouseX) {
        float rel = (float) ((mouseX - getPickerX()) / getPickerWidth());
        rel = Math.max(0f, Math.min(1f, rel));
        int alpha = (int) (rel * 255);

        Color c = getCurrentColor();
        setCurrentColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
    }

    private void drawAlphaBar(MatrixStack ms, float animValue) {
        float y = getAlphaY() + getAnimY();
        float h = getAlphaHeight();

        Color c = getCurrentColor();
        Color left = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
        Color right = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (getAnimValue() * getAlpha() * 255f));

        RenderUtil.GRADIENT_RECT.draw(ms, getPickerX() + scaled(2f), y, getPickerWidth() - scaled(4f), h, h * 0.3f, left, right, left, right);
    }

    private void drawHueBar(MatrixStack ms, float animValue) {
        float y = getHueY() + getAnimY();
        float h = getAlphaHeight();

        RenderUtil.TEXTURE_RECT.draw(
                ms, getPickerX() + scaled(2f), y,
                getPickerWidth() - scaled(4f), h, h * 0.3f,
                new Color(255, 255, 255, (int) (getAnimValue() * getAlpha() * 255f)),
                0f, 0f, 1f, 1f,
                mc.getTextureManager().getTexture(FileUtil.getImage("interface/hue")).getGlId()
        );
    }
    private void drawSelectors(MatrixStack ms) {
        int alpha = (int) (getAnimValue() * getAlpha() * 255f);
        Color currentColor = getCurrentColor();
        Color cursorColor = ColorUtil.setAlpha(Color.WHITE, alpha);
        float[] hsb = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

        float lineOffset = scaled(4f);
        float lineWidth = lineOffset;
        float lineHeight = lineWidth;
        float lineRound = lineOffset * 0.5f;
        float lineYOffset = getHueHeight() / 2f - lineHeight / 2f;

        float circleOffset = scaled(2f);
        float circleSize = circleOffset * 2f;

        float usableWidth = getPickerWidth() - scaled(4f);
        float satX = getPickerX() + scaled(2f) + hsb[1] * usableWidth;
        float briY = getColorPickerY() + (1 - hsb[2]) * getColorPickerHeight();
        RenderUtil.RECT.draw(ms, satX - circleOffset, briY + getAnimY() - circleOffset, circleSize, circleSize, circleSize * 0.5f, cursorColor);

        float hueX = getPickerX() + scaled(2f) + hueCache * usableWidth;
        RenderUtil.RECT.draw(ms, hueX - lineOffset, getHueY() + getAnimY() + lineYOffset, lineWidth, lineHeight, lineRound, cursorColor);

        float alphaRel = currentColor.getAlpha() / 255f;
        float alphaX = getPickerX() + scaled(2f) + alphaRel * usableWidth;
        RenderUtil.RECT.draw(ms, alphaX - lineOffset, getAlphaY() + getAnimY() + lineYOffset, lineWidth, lineHeight, lineRound, cursorColor);
    }

    private void drawRecentColors(MatrixStack ms, float animValue) {
        if (recentColors.isEmpty()) return;
        int alpha = (int) (animValue * getAlpha() * 255f);
        float y = getRecentY() + getAnimY();
        float size = getRecentRowHeight();
        float slotGap = scaled(2f);
        float startX = getPickerX() + scaled(2f);

        for (int i = 0; i < recentColors.size() && i < MAX_RECENT; i++) {
            float sx = startX + i * (size + slotGap);
            Color c = recentColors.get(i);
            RenderUtil.RECT.draw(ms, sx - scaled(0.4f), y - scaled(0.4f), size + scaled(0.8f), size + scaled(0.8f), size * 0.22f + scaled(0.3f), UIColors.stroke(Math.min(alpha, 120)));
            RenderUtil.RECT.draw(ms, sx, y, size, size, size * 0.22f, ColorUtil.setAlpha(c, (int) (c.getAlpha() / 255f * alpha)));
        }
    }

    private float getRecentY() { return getAlphaY() + getAlphaHeight() + gap(); }
    private float getRecentRowHeight() { return recentColors.isEmpty() ? 0f : scaled(5f) * getAnimValue(); }

    private static void addRecentColor(Color color) {
        recentColors.removeIf(c -> c.getRGB() == color.getRGB() && c.getAlpha() == color.getAlpha());
        recentColors.add(0, color);
        while (recentColors.size() > MAX_RECENT) {
            recentColors.remove(recentColors.size() - 1);
        }
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) { }

    private float getAnimY() { return (-gap() * (1f - getAnimValue())); }
    private float getColorPickerY() { return getY() + (setting != null ? scaled(getDefaultHeight()) : 0f); }
    private float getColorPickerHeight() { return getWidth() * getAnimValue() * 0.36f; }
    private float getHueY() { return getColorPickerY() + getColorPickerHeight() + gap(); }
    private float getHueHeight() { return scaled(5f) * getAnimValue(); }
    private float getAlphaY() { return getHueY() + getHueHeight() + gap(); }
    private float getAlphaHeight() { return getHueHeight(); }
    private float getPickerX() { return getX(); }
    private float getPickerWidth() { return getWidth(); }
    private float getDefaultHeight() { return 15f; }
    private float getAnimValue() { return getValue(); }

    private Color[] getGradientColors(float anim) {
        int alpha = (int) (anim * getAlpha() * 255f);
        Color topLeft = ColorUtil.setAlpha(Color.WHITE, alpha);
        Color bottom = ColorUtil.setAlpha(Color.BLACK, alpha);

        float hue = hueCache;
        Color hueColor = new Color(Color.HSBtoRGB(hue, 1f, 1f));
        Color topRight = ColorUtil.setAlpha(hueColor, alpha);

        return new Color[]{topLeft, topRight, bottom, bottom};
    }
}