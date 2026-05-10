package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class EmeraldTheme extends ADefaultTheme {
    public EmeraldTheme() {
        super("Emerald");
    }

    @Override
    public Color setPrimary() {
        return new Color(50, 235, 160, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(25, 160, 115, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(10, 23, 20, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(17, 39, 34, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(25, 55, 47, 255);
    }

    @Override
    public Color setText() {
        return new Color(228, 248, 241, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(142, 171, 162, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(80, 245, 180, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
