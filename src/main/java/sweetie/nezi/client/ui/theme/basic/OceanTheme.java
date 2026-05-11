package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class OceanTheme extends ADefaultTheme {
    public OceanTheme() {
        super("Ocean");
    }

    @Override
    public Color setPrimary() {
        return new Color(60, 175, 255, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(30, 120, 200, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(8, 18, 31, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(15, 31, 49, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(24, 43, 67, 255);
    }

    @Override
    public Color setText() {
        return new Color(230, 243, 255, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(147, 170, 194, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(95, 200, 255, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
