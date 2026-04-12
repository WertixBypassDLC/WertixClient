package sweetie.nezi.api.utils.auction;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import sweetie.nezi.api.system.interfaces.QuickImports;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceParser implements QuickImports {
    public ParseModeChoice currentMode = ParseModeChoice.FUN_TIME;

    private static final Pattern DOLLAR_PRICE = Pattern.compile(
            "\\$\\s*\u0426\u0435\u043D[\u0430a]\\s*:?\\s*\\$?\\s*([\\d,. ]+)"
    );
    private static final Pattern HOLY_WORLD_PRICE = Pattern.compile(
            "\u258D\\s*\u0426\u0435\u043D[\u0430a]\\s+\u0437\u0430\\s+1\\s+\u0435\u0434\\.\\s*:?\\s*([\\d,. ]+)"
    );
    private static final Pattern GENERIC_PRICE = Pattern.compile(
            "\u0426\u0435\u043D[\u0430a]\\s*:\\s*([\\d,. ]+)"
    );
    private static final Pattern BROAD_PRICE = Pattern.compile(
            "\u0426\u0435\u043D[\u0430a]\\s*:?\\s*\\$?\\s*([\\d,. ]+)"
    );

    public int getPrice(ItemStack stack) {
        return getPrice(stack, stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC));
    }

    public int getPrice(ItemStack stack, List<Text> tooltip) {
        Pattern primary = getPattern();

        for (Text text : tooltip) {
            String value = sanitize(text);
            int price = tryParse(primary, value);
            if (price != -1) {
                return price;
            }
        }

        if (primary != GENERIC_PRICE && primary != BROAD_PRICE) {
            for (Text text : tooltip) {
                String value = sanitize(text);
                int price = tryParse(GENERIC_PRICE, value);
                if (price != -1) {
                    return price;
                }

                price = tryParse(BROAD_PRICE, value);
                if (price != -1) {
                    return price;
                }
            }
        }

        return -1;
    }

    private String sanitize(Text text) {
        return text.getString().replaceAll("Â§[0-9a-fk-or]", "").replace("Â¤", "").trim();
    }

    private int tryParse(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            return -1;
        }

        String number = matcher.group(1).replace(",", "").replace(".", "").replace(" ", "").trim();
        if (number.isEmpty()) {
            return -1;
        }

        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private Pattern getPattern() {
        return switch (currentMode) {
            case FUN_TIME, SPOOKY_TIME -> DOLLAR_PRICE;
            case HOLY_WORLD -> HOLY_WORLD_PRICE;
            case REALLY_WORLD -> GENERIC_PRICE;
        };
    }
}
