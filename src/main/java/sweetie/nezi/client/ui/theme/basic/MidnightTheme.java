package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class MidnightTheme extends ADefaultTheme {
    public MidnightTheme() {
        super("Midnight");
    }

    @Override
    public Color setPrimary() {
        return new Color(105, 115, 255, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(75, 80, 200, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(9, 11, 24, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(16, 20, 40, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(22, 28, 54, 255);
    }

    @Override
    public Color setText() {
        return new Color(233, 236, 255, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(149, 154, 195, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(140, 150, 255, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
