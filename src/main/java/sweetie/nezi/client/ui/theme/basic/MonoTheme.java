package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class MonoTheme extends ADefaultTheme {
    public MonoTheme() {
        super("Monochrome");
    }

    @Override
    public Color setPrimary() {
        return new Color(208, 212, 224, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(127, 135, 154, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(16, 18, 24, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(25, 28, 36, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(34, 38, 48, 255);
    }

    @Override
    public Color setText() {
        return new Color(238, 241, 248, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(152, 158, 172, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(221, 226, 236, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
