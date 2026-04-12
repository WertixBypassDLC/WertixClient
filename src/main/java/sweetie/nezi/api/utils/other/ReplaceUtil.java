package sweetie.nezi.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.Formatting;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.client.features.modules.other.StreamerModule;

@UtilityClass
public class ReplaceUtil {
    private static final String[][] SYMBOL_REPLACEMENTS = {
            {"ê”—", Formatting.BLUE + "MODER"},
            {"ê”¥", Formatting.BLUE + "ST.MODER"},
            {"ê”¡", Formatting.LIGHT_PURPLE + "MODER+"},
            {"ê”€", Formatting.GRAY + "PLAYER"},
            {"ê”‰", Formatting.YELLOW + "HELPER"},
            {"â—†", "@"},
            {"â”ƒ", "|"},
            {"ê”³", Formatting.AQUA + "ML.ADMIN"},
            {"ê”…", Formatting.RED + "Y" + Formatting.WHITE + "T"},
            {"ê”‚", Formatting.BLUE + "D.MODER"},
            {"ê• ", Formatting.YELLOW + "D.HELPER"},
            {"ê•„", Formatting.RED + "DRACULA"},
            {"ê”–", Formatting.AQUA + "OVERLORD"},
            {"ê•ˆ", Formatting.GREEN + "COBRA"},
            {"ê”¨", Formatting.LIGHT_PURPLE + "DRAGON"},
            {"ê”¤", Formatting.RED + "IMPERATOR"},
            {"ê” ", Formatting.GOLD + "MAGISTER"},
            {"ê”„", Formatting.BLUE + "HERO"},
            {"ê”’", Formatting.GREEN + "AVENGER"},
            {"ê•’", Formatting.WHITE + "RABBIT"},
            {"ê”ˆ", Formatting.YELLOW + "TITAN"},
            {"ê•€", Formatting.DARK_GREEN + "HYDRA"},
            {"ê”¶", Formatting.GOLD + "TIGER"},
            {"ê”²", Formatting.DARK_PURPLE + "BULL"},
            {"ê•–", Formatting.BLACK + "BUNNY"},
            {"ê•—ê•˜", Formatting.YELLOW + "SPONSOR"},
            {"\uD83D\uDD25", "@"},
            {"á´€", "A"},
            {"Ê™", "B"},
            {"á´„", "C"},
            {"á´…", "D"},
            {"á´‡", "E"},
            {"Ò“", "F"},
            {"É¢", "G"},
            {"Êœ", "H"},
            {"Éª", "I"},
            {"á´Š", "J"},
            {"á´‹", "K"},
            {"ÊŸ", "L"},
            {"á´", "M"},
            {"É´", "N"},
            {"êœ±", "S"},
            {"á´", "O"},
            {"á´˜", "P"},
            {"Ç«", "Q"},
            {"Ê€", "R"},
            {"á´›", "T"},
            {"á´œ", "U"},
            {"á´ ", "V"},
            {"á´¡", "W"},
            {"êœ°", "F"},
            {"Ê", "Y"},
            {"á´¢", "Z"}
    };

    public Text replace(Text input, String target, String replacement) {
        if (input == null || target == null || replacement == null) return input;
        MutableText result = Text.empty().setStyle(input.getStyle());
        appendReplaced(result, input, target, replacement);
        return result;
    }

    private void appendReplaced(MutableText result, Text current, String target, String replacement) {
        TextContent content = current.getContent();
        Style style = current.getStyle();

        if (content instanceof PlainTextContent.Literal literal) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(target), java.util.regex.Pattern.CASE_INSENSITIVE);
            String replaced = pattern.matcher(literal.string()).replaceAll(replacement);
            result.append(Text.literal(replaced).setStyle(style));
        }

        for (Text sibling : current.getSiblings()) {
            appendReplaced(result, sibling, target, replacement);
        }
    }

    public String replaceSymbols(String string) {
        String result = string;
        for (String[] replacement : SYMBOL_REPLACEMENTS) {
            result = result.replace(replacement[0], replacement[1]);
        }
        return result;
    }

    public Text replaceSymbols(Text text) {
        Text result = text;
        String raw = text.getString();
        for (String[] replacement : SYMBOL_REPLACEMENTS) {
            if (raw.contains(replacement[0])) {
                result = replace(result, replacement[0], replacement[1]);
            }
        }
        return result;
    }

    public String protectedString(String text) {
        StreamerModule streamerMode = StreamerModule.getInstance();
        String finalText = text;

        if (streamerMode.isEnabled() && streamerMode.getHideNick().getValue()) {
            finalText = finalText.replace(MinecraftClient.getInstance().getSession().getUsername(), streamerMode.getProtectedName());

            if (streamerMode.getHideFriends().getValue()) {
                for (String friendName : FriendManager.getInstance().getData()) {
                    finalText = finalText.replace(friendName, streamerMode.getProtectedFriendName(friendName));
                }
            }
        }

        return finalText;
    }
}
