package sweetie.nezi.api.utils.color;

import lombok.experimental.UtilityClass;
import sweetie.nezi.client.ui.theme.Theme;
import sweetie.nezi.client.ui.theme.ThemeEditor;

import java.awt.*;

@UtilityClass
public class UIColors {
    public Theme currentTheme() {
        return ThemeEditor.getInstance().getCurrentTheme();
    }

    private Color themeAccent() {
        return ColorUtil.interpolate(currentTheme().getPrimaryColor(), currentTheme().getSecondaryColor(), 0.45f);
    }

    private Color themedBlack(Color color, float themeAmount, int alpha) {
        Color mixed = ColorUtil.interpolate(color, Color.BLACK, themeAmount);
        return new Color(mixed.getRed(), mixed.getGreen(), mixed.getBlue(), alpha);
    }

    private Color themedSurface(Color base, float accentMix, float darkness, int alpha) {
        Color tinted = ColorUtil.interpolate(base, themeAccent(), accentMix);
        Color dark = ColorUtil.interpolate(tinted, Color.BLACK, darkness);
        return new Color(dark.getRed(), dark.getGreen(), dark.getBlue(), alpha);
    }

    private Color getColor(Color color, int alpha) {
        int finalAlpha = (int) (color.getAlpha() / 255f * alpha);
        return ColorUtil.setAlpha(color, finalAlpha);
    }

    public Color gradient(int index) { return gradient(index, 255); }
    public Color gradient(int index, int alpha) { return getColor(ColorUtil.gradient(15, index, primary(alpha), secondary(alpha)), alpha); }
    public Color themeFlow(int index) { return themeFlow(index, 255); }
    public Color themeFlow(int index, int alpha) {
        return getColor(ColorUtil.gradient(18, index, currentTheme().getPrimaryColor(), currentTheme().getSecondaryColor()), alpha);
    }

    public Color themeFlowAlt(int index) { return themeFlowAlt(index, 255); }
    public Color themeFlowAlt(int index, int alpha) {
        return getColor(ColorUtil.gradient(18, index + 90, currentTheme().getSecondaryColor(), currentTheme().getPrimaryColor()), alpha);
    }

    public Color blur() { return blur(255); }
    public Color blur(int alpha) {
        return getColor(themedSurface(currentTheme().getBlurColor(), 0.08f, 0.88f, 244), alpha);
    }

    public Color widgetBlur() { return widgetBlur(255); }
    public Color widgetBlur(int alpha) {
        return getColor(themedSurface(currentTheme().getWidgetBlurColor(), 0.08f, 0.90f, 242), alpha);
    }

    public Color backgroundBlur() { return backgroundBlur(255); }
    public Color backgroundBlur(int alpha) {
        return getColor(themedSurface(currentTheme().getBackgroundBlurColor(), 0.06f, 0.92f, 238), alpha);
    }

    public Color panel() { return panel(255); }
    public Color panel(int alpha) {
        Color base = ColorUtil.interpolate(currentTheme().getWidgetBlurColor(), currentTheme().getBlurColor(), 0.35f);
        return getColor(themedSurface(base, 0.10f, 0.95f, 218), alpha);
    }

    public Color panelSoft() { return panelSoft(255); }
    public Color panelSoft(int alpha) {
        Color base = ColorUtil.interpolate(currentTheme().getBackgroundBlurColor(), currentTheme().getWidgetBlurColor(), 0.30f);
        return getColor(themedSurface(base, 0.10f, 0.94f, 206), alpha);
    }

    public Color card() { return card(255); }
    public Color card(int alpha) {
        Color base = ColorUtil.interpolate(currentTheme().getBlurColor(), currentTheme().getBackgroundBlurColor(), 0.40f);
        return getColor(themedSurface(base, 0.12f, 0.95f, 214), alpha);
    }

    public Color cardSecondary() { return cardSecondary(255); }
    public Color cardSecondary(int alpha) {
        Color base = ColorUtil.interpolate(currentTheme().getBackgroundBlurColor(), currentTheme().getWidgetBlurColor(), 0.45f);
        return getColor(themedSurface(base, 0.18f, 0.92f, 204), alpha);
    }

    public Color overlay() { return overlay(255); }
    public Color overlay(int alpha) {
        Color darkAccent = ColorUtil.interpolate(themeAccent(), Color.BLACK, 0.93f);
        return getColor(new Color(darkAccent.getRed(), darkAccent.getGreen(), darkAccent.getBlue(), 32), alpha);
    }

    public Color stroke() { return stroke(255); }
    public Color stroke(int alpha) {
        Color base = ColorUtil.interpolate(currentTheme().getInactiveTextColor(), Color.WHITE, 0.32f);
        return getColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 82), alpha);
    }

    public Color primary() { return primary(255); }
    public Color primary(int alpha) { return getColor(currentTheme().getPrimaryColor(), alpha); }

    public Color secondary() { return secondary(255); }
    public Color secondary(int alpha) { return getColor(currentTheme().getSecondaryColor(), alpha); }

    public Color knob() { return knob(255); }
    public Color knob(int alpha) { return getColor(currentTheme().getKnobColor(), alpha); }

    public Color inactiveKnob() { return inactiveKnob(255); }
    public Color inactiveKnob(int alpha) { return getColor(currentTheme().getInactiveKnobColor(), alpha); }

    public Color textColor() { return textColor(255); }
    public Color textColor(int alpha) {
        return getColor(ColorUtil.interpolate(currentTheme().getTextColor(), Color.WHITE, 0.18f), alpha);
    }

    public Color inactiveTextColor() { return inactiveTextColor(255); }
    public Color inactiveTextColor(int alpha) {
        return getColor(ColorUtil.interpolate(currentTheme().getInactiveTextColor(), currentTheme().getTextColor(), 0.30f), alpha);
    }

    public Color positiveColor() { return positiveColor(255); }
    public Color positiveColor(int alpha) { return getColor(currentTheme().getPositiveColor(), alpha); }
    public Color middleColor() { return middleColor(255); }
    public Color middleColor(int alpha) { return getColor(currentTheme().getMiddleColor(), alpha); }
    public Color negativeColor() { return negativeColor(255); }
    public Color negativeColor(int alpha) { return getColor(currentTheme().getNegativeColor(), alpha); }

    public Color mutedText() { return mutedText(255); }
    public Color mutedText(int alpha) {
        return ColorUtil.interpolate(inactiveTextColor(alpha), textColor(alpha), 0.46f);
    }
}
