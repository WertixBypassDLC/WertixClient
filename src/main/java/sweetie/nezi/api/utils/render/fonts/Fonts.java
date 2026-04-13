package sweetie.nezi.api.utils.render.fonts;

import lombok.experimental.UtilityClass;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Fonts {

    private static final Map<String, Font> cache = new HashMap<>();

    // Убраны "/" и используется нижний регистр для соответствия правилам Identifier
    public static final Font PS_BOLD = get("jura-bold");
    public static final Font PS_MEDIUM = PS_BOLD;

    private static Font iconsInstance;
    public static Font getICONS() {
        if (iconsInstance == null) iconsInstance = get("icons");
        return iconsInstance;
    }

    public static float getMediumThickness() { return 0.07f; }
    public static float getBoldThickness() { return 0.1f; }

    private static Font get(String input) {
        // Очистка строки от случайных слэшей и перевод в нижний регистр
        String cleanInput = input.replace("/", "").toLowerCase();
        return cache.computeIfAbsent(cleanInput, k -> Font.builder().find(k).load());
    }
}