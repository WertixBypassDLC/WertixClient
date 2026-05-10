package sweetie.nezi.client.ui.theme.basic;

import java.awt.*;

public class WertixTheme extends ADefaultTheme {
    public WertixTheme() {
        super("Wertix");
    }

    @Override
    public Color setPrimary() {
        return new Color(160, 100, 255, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(105, 55, 220, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(18, 12, 31, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(31, 22, 52, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(38, 28, 63, 255);
    }

    @Override
    public Color setText() {
        return new Color(239, 232, 255, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(164, 149, 198, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(185, 140, 255, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}
