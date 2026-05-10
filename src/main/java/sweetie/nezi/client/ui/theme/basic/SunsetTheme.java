package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class SunsetTheme extends ADefaultTheme {
    public SunsetTheme() {
        super("Sunset");
    }

    @Override
    public Color setPrimary() {
        return new Color(255, 100, 80, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(210, 70, 130, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(28, 12, 20, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(47, 22, 34, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(61, 31, 45, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 228, 228, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(193, 148, 157, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(255, 130, 115, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
