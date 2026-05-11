package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class ToxicTheme extends ADefaultTheme {
    public ToxicTheme() {
        super("Toxic");
    }

    @Override
    public Color setPrimary() {
        return new Color(175, 255, 40, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(130, 185, 20, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(19, 24, 6, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(31, 39, 12, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(41, 52, 18, 255);
    }

    @Override
    public Color setText() {
        return new Color(245, 255, 221, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(171, 184, 127, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(200, 255, 80, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
