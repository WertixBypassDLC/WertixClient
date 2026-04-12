package sweetie.nezi.api.utils.render.fonts;

import lombok.experimental.UtilityClass;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Fonts {

    private static final Map<String, Font> cache = new HashMap<>();

    // Single instance for both bold and medium (same font file)
    public static final Font PS_BOLD = get("/Jura-Bold");
    public static final Font PS_MEDIUM = PS_BOLD;

    // Lazy-loaded icons font
    private static Font iconsInstance;
    public static Font getICONS() {
        if (iconsInstance == null) iconsInstance = get("/icons");
        return iconsInstance;
    }


    public static float getMediumThickness() { return 0.07f; }
    public static float getBoldThickness() { return 0.1f; }

    private static Font get(String input) {
        return cache.computeIfAbsent(input, k -> Font.builder().find(k).load());
    }
}