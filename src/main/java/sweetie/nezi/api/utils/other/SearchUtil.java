package sweetie.nezi.api.utils.other;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class SearchUtil {
    private static final String EN = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";
    private static final String RU = "\u0439\u0446\u0443\u043a\u0435\u043d\u0433\u0448\u0449\u0437\u0445\u044a\u0444\u044b\u0432\u0430\u043f\u0440\u043e\u043b\u0434\u0436\u044d\u044f\u0447\u0441\u043c\u0438\u0442\u044c\u0431\u044e\u0451";

    private static final Map<String, Set<String>> SEARCH_PRESETS = new HashMap<>();
    private static final Set<String> PRESET_QUERIES = new LinkedHashSet<>();

    static {
        registerPreset("legit",
                "triggerbot", "sprint", "inventorymove", "nightvision", "nametags",
                "predictions", "hud", "targetesp", "cheststealer", "noentitytrace",
                "tapemouse");
        registerPreset("\u043b\u0435\u0433\u0438\u0442",
                "triggerbot", "sprint", "inventorymove", "nightvision", "nametags",
                "predictions", "hud", "targetesp", "cheststealer", "noentitytrace",
                "tapemouse");

        registerPreset("rage",
                "aura", "autoswap", "elytratarget", "autototem", "velocity",
                "waterspeed", "noslow", "spider", "speed", "flight", "seeinvisibles",
                "blockesp", "cameraclip", "elytraswap", "clickpearl", "autotool",
                "funtimehelper", "nodelay", "potiontracker", "autorespawn");
        registerPreset("\u0440\u0435\u0439\u0434\u0436",
                "aura", "autoswap", "elytratarget", "autototem", "velocity",
                "waterspeed", "noslow", "spider", "speed", "flight", "seeinvisibles",
                "blockesp", "cameraclip", "elytraswap", "clickpearl", "autotool",
                "funtimehelper", "nodelay", "potiontracker", "autorespawn");

        registerPreset("funtime",
                "aura", "spider", "sprint", "inventorymove", "funtimehelper",
                "wardenhelper", "potiontracker", "auctionhelper", "streamer");
        registerPreset("ft",
                "aura", "spider", "sprint", "inventorymove", "funtimehelper",
                "wardenhelper", "potiontracker", "auctionhelper", "streamer");
        registerPreset("fantime",
                "aura", "spider", "sprint", "inventorymove", "funtimehelper",
                "wardenhelper", "potiontracker", "auctionhelper", "streamer");
        registerPreset("\u0444\u0430\u043d\u0442\u0430\u0439\u043c",
                "aura", "spider", "sprint", "inventorymove", "funtimehelper",
                "wardenhelper", "potiontracker", "auctionhelper", "streamer");
        registerPreset("\u0444\u0442",
                "aura", "spider", "sprint", "inventorymove", "funtimehelper",
                "wardenhelper", "potiontracker", "auctionhelper", "streamer");
        registerPreset("\u043f\u0438\u043e\u043d\u0435\u0440",
                "aura", "spider", "sprint", "inventorymove", "funtimehelper",
                "wardenhelper", "potiontracker", "auctionhelper", "streamer");

        registerPreset("auctionhelper", "auctionhelper");
        registerPreset("auction", "auctionhelper");
        registerPreset("ah", "auctionhelper");
        registerPreset("\u0430\u0443\u043a\u0446\u0438\u043e\u043d", "auctionhelper");
        registerPreset("\u0430\u0443\u043a", "auctionhelper");
    }

    public String fixLayout(String input) {
        return remap(input, RU, EN);
    }

    public String swapToRussian(String input) {
        return remap(input, EN, RU);
    }

    public boolean matches(String value, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedValue = normalize(value);
        String compactValue = compact(normalizedValue);
        for (String variant : queryVariants(query)) {
            if (normalizedValue.contains(variant)) {
                return true;
            }
            if (compactValue.contains(compact(variant))) {
                return true;
            }

            Set<String> preset = SEARCH_PRESETS.get(compact(variant));
            if (preset != null && preset.contains(compactValue)) {
                return true;
            }

            if (compact(variant).length() >= 4 && similarityScore(value, variant) >= 0.54) {
                return true;
            }
        }
        return false;
    }

    public String findBestMatch(Collection<String> values, String query) {
        if ((values == null || values.isEmpty()) && PRESET_QUERIES.isEmpty()) {
            return null;
        }

        String normalizedQuery = normalize(query);
        if (normalizedQuery.length() < 2) {
            return null;
        }

        String compactQuery = compact(normalizedQuery);
        String bestValue = null;
        double bestScore = 0.0;

        if (values != null) {
            for (String value : values) {
                String compactValue = compact(value);
                if (compactValue.equals(compactQuery)) {
                    return value;
                }

                double score = similarityScore(value, normalizedQuery);
                if (score > bestScore) {
                    bestScore = score;
                    bestValue = value;
                }
            }
        }

        for (String presetQuery : PRESET_QUERIES) {
            String compactPreset = compact(presetQuery);
            if (compactPreset.equals(compactQuery)) {
                return presetQuery;
            }

            double score = similarityScore(presetQuery, normalizedQuery);
            if (score > bestScore) {
                bestScore = score;
                bestValue = presetQuery;
            }
        }

        return bestScore >= 0.46 ? bestValue : null;
    }

    public Set<String> variants(String input) {
        Set<String> result = new LinkedHashSet<>();
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return result;
        }

        result.add(normalized);
        result.add(fixLayout(normalized));
        result.add(swapToRussian(normalized));
        return result;
    }

    public String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }

    private String compact(String input) {
        return normalize(input).replace(" ", "").replace("-", "").replace("_", "");
    }

    private void registerPreset(String query, String... modules) {
        PRESET_QUERIES.add(query);
        SEARCH_PRESETS.put(compact(query), Arrays.stream(modules)
                .map(SearchUtil::compact)
                .collect(LinkedHashSet::new, Set::add, Set::addAll));
    }

    private Set<String> queryVariants(String input) {
        Set<String> result = new LinkedHashSet<>(variants(input));
        for (String token : tokenize(input)) {
            result.addAll(variants(token));
        }
        return result;
    }

    private Set<String> tokenize(String input) {
        Set<String> tokens = new LinkedHashSet<>();
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return tokens;
        }

        for (String token : normalized.split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private double similarityScore(String value, String query) {
        String compactValue = compact(value);
        double best = 0.0;

        for (String variant : queryVariants(query)) {
            String compactVariant = compact(variant);
            if (compactVariant.isEmpty()) {
                continue;
            }

            if (compactValue.contains(compactVariant)) {
                return 1.0 - Math.max(0, compactValue.length() - compactVariant.length()) * 0.01;
            }

            if (compactVariant.contains(compactValue)) {
                best = Math.max(best, 0.88);
            }

            int maxLength = Math.max(compactValue.length(), compactVariant.length());
            if (maxLength == 0) {
                continue;
            }

            double prefixScore = (double) commonPrefixLength(compactValue, compactVariant) / maxLength;
            double editScore = 1.0 - (double) levenshteinDistance(compactValue, compactVariant) / maxLength;
            best = Math.max(best, Math.max(prefixScore * 0.92, editScore));
        }

        return best;
    }

    private int commonPrefixLength(String first, String second) {
        int limit = Math.min(first.length(), second.length());
        int index = 0;
        while (index < limit && first.charAt(index) == second.charAt(index)) {
            index++;
        }
        return index;
    }

    private int levenshteinDistance(String first, String second) {
        int[] costs = new int[second.length() + 1];
        for (int j = 0; j <= second.length(); j++) {
            costs[j] = j;
        }

        for (int i = 1; i <= first.length(); i++) {
            int previousDiagonal = costs[0];
            costs[0] = i;
            for (int j = 1; j <= second.length(); j++) {
                int previous = costs[j];
                int substitution = previousDiagonal + (first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1);
                int insertion = costs[j] + 1;
                int deletion = costs[j - 1] + 1;
                costs[j] = Math.min(Math.min(insertion, deletion), substitution);
                previousDiagonal = previous;
            }
        }

        return costs[second.length()];
    }

    private String remap(String input, String from, String to) {
        String normalized = normalize(input);
        StringBuilder builder = new StringBuilder(normalized.length());

        for (int i = 0; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            int index = from.indexOf(character);
            builder.append(index >= 0 && index < to.length() ? to.charAt(index) : character);
        }

        return builder.toString();
    }
}
