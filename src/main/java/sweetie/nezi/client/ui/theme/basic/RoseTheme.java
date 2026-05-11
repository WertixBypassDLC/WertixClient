package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class RoseTheme extends ADefaultTheme {
    public RoseTheme() {
        super("Rose");
    }

    @Override
    public Color setPrimary() {
        return new Color(255, 110, 175, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(215, 75, 145, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(30, 13, 23, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(49, 22, 37, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(63, 31, 48, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 233, 241, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(196, 155, 169, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(255, 140, 195, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
