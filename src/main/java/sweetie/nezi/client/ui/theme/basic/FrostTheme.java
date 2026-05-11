package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class FrostTheme extends ADefaultTheme {
    public FrostTheme() {
        super("Frost");
    }

    @Override
    public Color setPrimary() {
        return new Color(105, 225, 255, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(60, 165, 220, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(14, 22, 31, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(22, 36, 49, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(30, 48, 64, 255);
    }

    @Override
    public Color setText() {
        return new Color(232, 247, 255, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(154, 178, 194, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(140, 235, 255, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
