package sweetie.nezi.client.ui.theme;

import lombok.Getter;
import lombok.Setter;
import sweetie.nezi.client.ui.clickgui.module.settings.ColorComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Theme {
    @Setter private String name;
    private final List<ElementColor> elementColors = new ArrayList<>();

    public Theme(String name) {
        this.name = name;

        elementColors.add(new ElementColor("Primary", new Color(160, 100, 255)));
        elementColors.add(new ElementColor("Secondary", new Color(105, 55, 220)));
        elementColors.add(new ElementColor("Blur", new Color(30, 41, 46)));
        elementColors.add(new ElementColor("Widget blur", new Color(20, 29, 34, 255)));
        elementColors.add(new ElementColor("Background blur", new Color(25, 36, 40, 255)));
        elementColors.add(new ElementColor("Text", new Color(228, 236, 238, 255)));
        elementColors.add(new ElementColor("Inactive text", new Color(141, 154, 158)));
        elementColors.add(new ElementColor("Knob", new Color(138, 194, 201)));
        elementColors.add(new ElementColor("Inactive knob", new Color(228, 236, 238)));
        elementColors.add(new ElementColor("Positive", new Color(135, 212, 173)));
        elementColors.add(new ElementColor("Middle", new Color(222, 186, 112)));
        elementColors.add(new ElementColor("Negative", new Color(216, 122, 110)));
    }

    public Color getPrimaryColor() { return getElementColor("Primary"); }
    public Color getSecondaryColor() { return getElementColor("Secondary"); }
    public Color getBlurColor() { return getElementColor("Blur"); }
    public Color getWidgetBlurColor() { return getElementColor("Widget blur"); }
    public Color getBackgroundBlurColor() { return getElementColor("Background blur"); }
    public Color getTextColor() { return getElementColor("Text"); }
    public Color getInactiveTextColor() { return getElementColor("Inactive text"); }
    public Color getKnobColor() { return getElementColor("Knob"); }
    public Color getInactiveKnobColor() { return getElementColor("Inactive knob"); }
    public Color getPositiveColor() { return getElementColor("Positive"); }
    public Color getMiddleColor() { return getElementColor("Middle"); }
    public Color getNegativeColor() { return getElementColor("Negative"); }

    public Color getElementColor(String elementName) {
        for (ElementColor element : elementColors) {
            if (element.getName().equalsIgnoreCase(elementName)) {
                return element.getColor();
            }
        }
        return new Color(-1);
    }

    @Getter
    public static class ElementColor {
        private final String name;
        @Setter private Color color;
        private final ColorComponent colorComponent;

        public ElementColor(String name, Color color) {
            this.name = name;
            this.color = color;
            this.colorComponent = new ColorComponent(this);
        }
    }
}
