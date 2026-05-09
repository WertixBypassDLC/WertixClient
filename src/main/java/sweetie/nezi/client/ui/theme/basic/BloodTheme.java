package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

/**
 * Blood theme — deep red, very saturated, aggressive palette.
 * Replaces / sits alongside Crimson.
 */
public class BloodTheme extends ADefaultTheme {
    public BloodTheme() {
        super("Blood");
    }

    @Override
    public Color setPrimary() {
        // Intense blood-red
        return new Color(235, 12, 22, 255);
    }

    @Override
    public Color setSecondary() {
        // Deep dark crimson
        return new Color(92, 0, 8, 255);
    }

    @Override
    public Color setBlur() {
        // Almost black with a red tint
        return new Color(7, 0, 1, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(30, 0, 4, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(42, 0, 6, 255);
    }

    @Override
    public Color setText() {
        // Bright warm red-white
        return new Color(255, 215, 215, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(155, 50, 56, 255);
    }

    @Override
    public Color setKnob() {
        // Vivid red knob
        return new Color(255, 18, 28, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
