package sweetie.nezi.client.features.modules.other.autobuy;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoTradeMarketUtil {
    private static final Pattern[] PRICE_PATTERNS = {
            Pattern.compile("(?iu)(?:\\$\\s*)?цен[аaы]?[^\\d]{0,24}([\\d][\\d\\s,._]*)"),
            Pattern.compile("(?iu)price[^\\d]{0,24}([\\d][\\d\\s,._]*)")
    };

    private AutoTradeMarketUtil() {
    }

    public static int parseAuctionPrice(ItemStack stack, List<Text> tooltip) {
        if (stack == null || stack.isEmpty() || tooltip == null || tooltip.isEmpty()) {
            return -1;
        }

        int best = -1;
        int count = Math.max(1, stack.getCount());
        for (Text line : tooltip) {
            String clean = stripFormatting(line.getString());
            if (clean.isBlank()) {
                continue;
            }

            String normalized = clean.toLowerCase(Locale.ROOT);
            for (Pattern pattern : PRICE_PATTERNS) {
                Matcher matcher = pattern.matcher(normalized);
                if (!matcher.find()) {
                    continue;
                }

                String digits = matcher.group(1).replaceAll("[^\\d]", "");
                if (digits.isEmpty()) {
                    continue;
                }

                try {
                    long raw = Long.parseLong(digits);
                    if (raw <= 0L) {
                        continue;
                    }

                    if (isUnitPriceLine(normalized)) {
                        raw *= count;
                    }

                    if (raw > Integer.MAX_VALUE) {
                        raw = Integer.MAX_VALUE;
                    }

                    best = Math.max(best, (int) raw);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return best;
    }

    public static String resolveSearchQuery(AutoTradeItem item) {
        if (item == null) {
            return "";
        }

        return switch (item) {
            case INVISIBILITY -> "инвиз";
            case GOLDEN_APPLE -> "гэпл";
            case ENCHANTED_GAPPLE -> "чарка";
            case ELYTRA -> "элитры";
            case NETHERITE_INGOT -> "незеритовый слиток";
            case SPAWNER -> "спавнер";
            case DIAMOND -> "алмаз";
            case BEACON -> "маяк";
            case SNIFFER_EGG -> "яйцо нюхача";
            case TRIAL_KEY -> "ключ испытаний";
            case DRAGON_HEAD -> "голова дракона";
            case VILLAGER_SPAWN_EGG -> "яйцо жителя";
            case DYNAMITE_BLACK -> "блэк";
            case DYNAMITE_WHITE -> "вайт";
            case SILVER -> "серебро";
            case TRAPKA -> "трапка";
            case TOTEM -> "тотем бессмертия";
            case EMERALD_ORE -> "изумрудная руда";
            case MACE -> "булава";
            case LOCKPICK_SPHERES -> "отмычка к сферам";
            case BLOCK_DAMAGER -> "блок дамагер";
            case CHUNK_LOADER_1X1 -> "прогрузчик чанков [1x1]";
            case CHUNK_LOADER_3X3 -> "прогрузчик чанков [3x3]";
            case CHUNK_LOADER_5X5 -> "прогрузчик чанков [5x5]";
            case DRAGON_SKIN -> "драконий скин";
            case SPHERE_BEAST -> "сфера бестии";
            case SPHERE_SATYR -> "сфера сатира";
            case SPHERE_CHAOS -> "сфера хаоса";
            case SPHERE_ARES -> "сфера ареса";
            case SPHERE_HYDRA -> "сфера гидры";
            case SPHERE_TITAN -> "сфера титана";
            case TALISMAN_DEMON -> "талисман демона";
            case TALISMAN_DISCORD -> "талисман раздора";
            case TALISMAN_RAGE -> "ярости";
            case TALISMAN_CRUSHER -> "талисман крушителя";
            case TALISMAN_TYRANT -> "тиран";
            case POTION_ASSASSIN -> "зелье ассасина";
            case POTION_HOLY_WATER -> "святая вода";
            case POTION_PALADIN -> "зелье палладина";
            case POTION_SLEEPING -> "снотворное";
            case POTION_CLAPPER -> "хлопушка";
            case POTION_WRATH -> "зелье гнева";
            case POTION_RADIATION -> "зелье радиации";
            case CRUSHER_SWORD -> "меч крушителя";
            case CRUSHER_PICKAXE -> "кирка крушителя";
            case CRUSHER_CROSSBOW -> "арбалет крушителя";
            case CRUSHER_TRIDENT -> "трезубец крушителя";
            case CRUSHER_MACE -> "булава крушителя";
            case CRUSHER_LEGGINGS -> "поножи крушителя";
            case CRUSHER_CHESTPLATE -> "нагрудник крушителя";
            case CRUSHER_HELMET -> "шлем крушителя";
            case CRUSHER_BOOTS -> "ботинки крушителя";
        };
    }

    private static boolean isUnitPriceLine(String line) {
        return line.contains("за 1")
                || line.contains("за 1 ед")
                || line.contains("за 1 шт")
                || line.contains("за шту")
                || line.contains("/1");
    }

    private static String stripFormatting(String value) {
        return value == null
                ? ""
                : value.replaceAll("§[0-9A-FK-ORa-fk-or]", "")
                .replace('\u00A0', ' ')
                .trim();
    }
}
