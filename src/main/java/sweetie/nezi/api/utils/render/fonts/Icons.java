package sweetie.nezi.api.utils.render.fonts;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Icons {
    // Hud widgets
    POTIONS("\ue906"),
    KEYBINDS("\ue904"),
    STAFFS("\ue905"),
    COOLDOWNS("\ue903"),

    // Watermark
    USER("\ue902"),
    SERVER("\ue900"),
    FPS("\ue901"),
    COORDS("\ue907");


    private final String letter;

    public static Icons find(String name) {
        try {
            return Icons.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
