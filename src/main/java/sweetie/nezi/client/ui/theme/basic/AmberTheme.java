package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class AmberTheme extends ADefaultTheme {
    public AmberTheme() {
        super("Amber");
    }

    @Override
    public Color setPrimary() {
        return new Color(255, 175, 45, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(200, 125, 25, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(31, 20, 9, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(50, 33, 15, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(62, 41, 20, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 238, 214, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(190, 165, 128, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(255, 195, 80, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
