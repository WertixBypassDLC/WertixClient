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

    // Паттерн ценника для FunTime
    private static final Pattern FUN_TIME_PRICE = Pattern.compile(
            "\\$\\s*\u0426\u0435\u043D[\u0430a]\\s*:?\\s*\\$?\\s*([\\d,. ]+)"
    );

    public int getPrice(ItemStack stack) {
        return getPrice(stack, stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC));
    }

    public int getPrice(ItemStack stack, List<Text> tooltip) {
        for (Text text : tooltip) {
            String value = sanitize(text);
            int price = tryParse(FUN_TIME_PRICE, value);
            if (price != -1) {
                return price;
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
}