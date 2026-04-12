package sweetie.nezi.client.features.modules.other.autobuy;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum AutoTradeItem {
    INVISIBILITY("invisibility", "Невидимость", "Зелье", createStack(Items.POTION), null, null,
            List.of(), List.of(), "minecraft:invisibility", 1),
    GOLDEN_APPLE("golden_apple", "Золотое", "Яблоко", createStack(Items.GOLDEN_APPLE), Items.GOLDEN_APPLE, null,
            List.of(), List.of(), null, 0),
    ENCHANTED_GAPPLE("enchanted_golden_apple", "Зачар.", "Яблоко", createStack(Items.ENCHANTED_GOLDEN_APPLE), Items.ENCHANTED_GOLDEN_APPLE, null,
            List.of(), List.of(), null, 0),
    ELYTRA("elytra", "Элитры", "Крылья", createStack(Items.ELYTRA), Items.ELYTRA, null,
            List.of(), List.of(), null, 0),
    NETHERITE_INGOT("netherite_ingot", "Незерит", "Слиток", createStack(Items.NETHERITE_INGOT), Items.NETHERITE_INGOT, null,
            List.of(), List.of(), null, 0),
    SPAWNER("spawner", "Моб", "Спавнер", createStack(Items.SPAWNER), Items.SPAWNER, null,
            List.of(), List.of(), null, 0),
    DIAMOND("diamond", "Алмаз", "Ресурс", createStack(Items.DIAMOND), Items.DIAMOND, null,
            List.of(), List.of(), null, 0),
    BEACON("beacon", "Маяк", "Блок", createStack(Items.BEACON), Items.BEACON, null,
            List.of(), List.of(), null, 0),
    SNIFFER_EGG("sniffer_egg", "Нюхач", "Яйцо", createStack(Items.SNIFFER_EGG), Items.SNIFFER_EGG, null,
            List.of(), List.of(), null, 0),
    TRIAL_KEY("trial_key", "Ключ", "Испыт.", createStack(Items.TRIAL_KEY), Items.TRIAL_KEY, null,
            List.of(), List.of(), null, 0),
    DRAGON_HEAD("dragon_head", "Голова", "Дракона", createStack(Items.DRAGON_HEAD), Items.DRAGON_HEAD, null,
            List.of(), List.of(), null, 0),
    VILLAGER_SPAWN_EGG("villager_spawn_egg", "Житель", "Яйцо", createStack(Items.VILLAGER_SPAWN_EGG), Items.VILLAGER_SPAWN_EGG, null,
            List.of(), List.of(), null, 0),

    DYNAMITE_BLACK("dynamite_black", "Динамит", "BLACK", createStack(Items.TNT), Items.TNT, null,
            List.of("Этот динамит взрывается", "в 10 раз сильнее обычного", "и способен взорвать обсидиан"),
            List.of(), null, 0),
    DYNAMITE_WHITE("dynamite_white", "Динамит", "WHITE", createStack(Items.TNT), Items.TNT, null,
            List.of("Этот динамит взрывается", "в 10 раз сильнее обычного"),
            List.of("и способен взорвать обсидиан"), null, 0),
    SILVER("silver", "Серебро", "Валюта", createStack(Items.IRON_NUGGET), Items.IRON_NUGGET, null,
            List.of("Это валюта для покупки", "отмычек к тайникам", "у Знахаря (/warp stash)"),
            List.of(), null, 0),
    TRAPKA("trapka", "Трапка", "Скрап", createStack(Items.NETHERITE_SCRAP), Items.NETHERITE_SCRAP, null,
            List.of("Нерушимая клетка"), List.of(), null, 0),
    TOTEM("totem_of_undying", "Тотем", "Бессмертия", createStack(Items.TOTEM_OF_UNDYING), Items.TOTEM_OF_UNDYING, "Тотем бессмертия",
            List.of(), List.of(), null, 0),
    EMERALD_ORE("emerald_ore", "Изумруд.", "Руда", createStack(Items.EMERALD_ORE), Items.EMERALD_ORE, null,
            List.of(), List.of(), null, 0),
    MACE("mace", "Булава", "Обычная", createStack(Items.MACE), Items.MACE, "Булава",
            List.of(), List.of("Оригинальный предмет"), null, 0),
    LOCKPICK_SPHERES("lockpick_spheres", "Отмычка", "Сферы", createStack(Items.TRIPWIRE_HOOK), Items.TRIPWIRE_HOOK, "Отмычка к Сферам",
            List.of("Открыть хранилище", "С Сферами"), List.of(), null, 0),
    BLOCK_DAMAGER("block_damager", "Блок", "Дамагер", createStack(Items.JIGSAW), Items.JIGSAW, "Блок дамагер",
            List.of("Нанесение урона"), List.of(), null, 0),
    CHUNK_LOADER_1X1("chunk_loader_1x1", "Прогрузч.", "1x1", createStack(Items.STRUCTURE_BLOCK), Items.STRUCTURE_BLOCK, "Прогрузчик чанков [1x1]",
            List.of("Прогружает чанк"), List.of(), null, 0),
    CHUNK_LOADER_3X3("chunk_loader_3x3", "Прогрузч.", "3x3", createStack(Items.STRUCTURE_BLOCK), Items.STRUCTURE_BLOCK, "Прогрузчик чанков [3x3]",
            List.of("Прогружает чанк"), List.of(), null, 0),
    CHUNK_LOADER_5X5("chunk_loader_5x5", "Прогрузч.", "5x5", createStack(Items.STRUCTURE_BLOCK), Items.STRUCTURE_BLOCK, "Прогрузчик чанков [5x5]",
            List.of("Прогружает чанк"), List.of(), null, 0),
    DRAGON_SKIN("dragon_skin", "Драконий", "Скин", createStack(Items.PAPER), Items.PAPER, "Драконий скин",
            List.of("получаете драконий скин"), List.of(), null, 0),

    SPHERE_BEAST("sphere_beast", "Сфера", "Бестии", createStack(Items.PLAYER_HEAD), Items.PLAYER_HEAD, null,
            List.of("вериная дикая мощь", "Обостряет реакции", "Укрепляя ваше тело."),
            List.of(), null, 0),
    SPHERE_SATYR("sphere_satyr", "Сфера", "Сатира", createStack(Items.PLAYER_HEAD), Items.PLAYER_HEAD, null,
            List.of("Шёпот Сатира звучит", "Ускоряя расправу", "Но сковывая прыжок."),
            List.of(), null, 0),
    SPHERE_CHAOS("sphere_chaos", "Сфера", "Хаоса", createStack(Items.PLAYER_HEAD), Items.PLAYER_HEAD, null,
            List.of("Хаос искажает реальность"), List.of(), null, 0),
    SPHERE_ARES("sphere_ares", "Сфера", "Ареса", createStack(Items.PLAYER_HEAD), Items.PLAYER_HEAD, null,
            List.of("Дух Ареса пылает внутри"), List.of(), null, 0),
    SPHERE_HYDRA("sphere_hydra", "Сфера", "Гидры", createStack(Items.PLAYER_HEAD), Items.PLAYER_HEAD, null,
            List.of("Живучесть темных глубин"), List.of(), null, 0),
    SPHERE_TITAN("sphere_titan", "Сфера", "Титана", createStack(Items.PLAYER_HEAD), Items.PLAYER_HEAD, null,
            List.of("Мощь Титанов крепка"), List.of(), null, 0),

    TALISMAN_DEMON("talisman_demon", "Талисман", "Демона", createStack(Items.TOTEM_OF_UNDYING), Items.TOTEM_OF_UNDYING, null,
            List.of("Печать разжигает ярость", "Ускоряя удары сердца", "И силу каждой атаки."),
            List.of(), null, 0),
    TALISMAN_DISCORD("talisman_discord", "Талисман", "Раздора", createStack(Items.TOTEM_OF_UNDYING), Items.TOTEM_OF_UNDYING, null,
            List.of("Раздор жаждет хаоса", "Даруя безумный темп", "Но разрушая броню."),
            List.of(), null, 0),
    TALISMAN_RAGE("talisman_rage", "Талисман", "Ярости", createStack(Items.TOTEM_OF_UNDYING), Items.TOTEM_OF_UNDYING, null,
            List.of("Чистая, дикая агрессия"), List.of(), null, 0),
    TALISMAN_CRUSHER("talisman_crusher", "Талисман", "Крушителя", createStack(Items.TOTEM_OF_UNDYING), Items.TOTEM_OF_UNDYING, null,
            List.of("Легендарный символ"), List.of(), null, 0),
    TALISMAN_TYRANT("talisman_tyrant", "Талисман", "Тирана", createStack(Items.TOTEM_OF_UNDYING), Items.TOTEM_OF_UNDYING, null,
            List.of("Тиран подавляет слабых"), List.of(), null, 0),

    POTION_ASSASSIN("potion_assassin", "Зелье", "Ассасина", createStack(Items.SPLASH_POTION), Items.SPLASH_POTION, "[★] Зелье Ассасина",
            List.of(), List.of(), null, 0),
    POTION_HOLY_WATER("potion_holy_water", "Святая", "Вода", createStack(Items.SPLASH_POTION), Items.SPLASH_POTION, "[★] Святая вода",
            List.of(), List.of(), null, 0),
    POTION_PALADIN("potion_paladin", "Зелье", "Паладина", createStack(Items.SPLASH_POTION), Items.SPLASH_POTION, "[★] Зелье Палладина",
            List.of(), List.of(), null, 0),
    POTION_SLEEPING("potion_sleeping", "Зелье", "Сон", createStack(Items.SPLASH_POTION), Items.SPLASH_POTION, "[★] Снотворное",
            List.of(), List.of(), null, 0),
    POTION_CLAPPER("potion_clapper", "Зелье", "Хлопушка", createStack(Items.SPLASH_POTION), Items.SPLASH_POTION, "[★] Хлопушка",
            List.of(), List.of(), null, 0),
    POTION_WRATH("potion_wrath", "Зелье", "Гнева", createStack(Items.SPLASH_POTION), Items.SPLASH_POTION, "[★] Зелье Гнева",
            List.of(), List.of(), null, 0),
    POTION_RADIATION("potion_radiation", "Зелье", "Радиации", createStack(Items.SPLASH_POTION), Items.SPLASH_POTION, "[★] Зелье Радиации",
            List.of(), List.of(), null, 0),

    CRUSHER_SWORD("crusher_sword", "Крушит.", "Меч", createStack(Items.NETHERITE_SWORD), Items.NETHERITE_SWORD, "Меч Крушителя",
            List.of(), List.of(), null, 0),
    CRUSHER_PICKAXE("crusher_pickaxe", "Крушит.", "Кирка", createStack(Items.NETHERITE_PICKAXE), Items.NETHERITE_PICKAXE, "Кирка Крушителя",
            List.of(), List.of(), null, 0),
    CRUSHER_CROSSBOW("crusher_crossbow", "Крушит.", "Арбал.", createStack(Items.CROSSBOW), Items.CROSSBOW, "Арбалет Крушителя",
            List.of(), List.of(), null, 0),
    CRUSHER_TRIDENT("crusher_trident", "Крушит.", "Трезуб.", createStack(Items.TRIDENT), Items.TRIDENT, "Трезубец Крушителя",
            List.of(), List.of(), null, 0),
    CRUSHER_MACE("crusher_mace", "Крушит.", "Булава", createStack(Items.MACE), Items.MACE, "Булава Крушителя",
            List.of(), List.of(), null, 0),
    CRUSHER_LEGGINGS("crusher_leggings", "Крушит.", "Поножи", createStack(Items.NETHERITE_LEGGINGS), Items.NETHERITE_LEGGINGS, "Поножи Крушителя",
            List.of(), List.of(), null, 0),
    CRUSHER_CHESTPLATE("crusher_chestplate", "Крушит.", "Нагруд.", createStack(Items.NETHERITE_CHESTPLATE), Items.NETHERITE_CHESTPLATE, "Нагрудник Крушителя",
            List.of(), List.of(), null, 0),
    CRUSHER_HELMET("crusher_helmet", "Крушит.", "Шлем", createStack(Items.NETHERITE_HELMET), Items.NETHERITE_HELMET, "Шлем Крушителя",
            List.of(), List.of(), null, 0),
    CRUSHER_BOOTS("crusher_boots", "Крушит.", "Ботинки", createStack(Items.NETHERITE_BOOTS), Items.NETHERITE_BOOTS, "Ботинки Крушителя",
            List.of(), List.of(), null, 0);

    private final String id;
    private final String title;
    private final String subtitle;
    private final ItemStack displayStack;
    private final Item matchItem;
    private final String nameFragment;
    private final List<String> loreKeywords;
    private final List<String> excludedLoreKeywords;
    private final String potionEffectId;
    private final int minAmplifier;

    AutoTradeItem(String id, String title, String subtitle, ItemStack displayStack,
                  Item matchItem, String nameFragment,
                  List<String> loreKeywords, List<String> excludedLoreKeywords,
                  String potionEffectId, int minAmplifier) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.displayStack = displayStack;
        this.matchItem = matchItem;
        this.nameFragment = nameFragment;
        this.loreKeywords = loreKeywords;
        this.excludedLoreKeywords = excludedLoreKeywords;
        this.potionEffectId = potionEffectId;
        this.minAmplifier = minAmplifier;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public ItemStack copyDisplayStack() {
        return displayStack.copy();
    }

    public boolean matches(ItemStack stack) {
        return getMatchScore(stack) >= 0;
    }

    public int getMatchScore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1;
        }

        boolean hasRule = false;
        boolean needsLore = !loreKeywords.isEmpty() || !excludedLoreKeywords.isEmpty() || id.startsWith("crusher_");
        List<String> lore = needsLore ? getNormalizedLore(stack) : List.of();
        String normalizedName = normalize(stack.getName().getString());
        int score = 0;

        if (matchItem != null) {
            hasRule = true;
            if (!stack.isOf(matchItem)) {
                return -1;
            }
            score += 10;
        }

        if (nameFragment != null && !nameFragment.isBlank()) {
            hasRule = true;
            if (!normalizedName.contains(normalize(nameFragment))) {
                return -1;
            }
            score += 34;
        }

        if (!excludedLoreKeywords.isEmpty()) {
            hasRule = true;
            for (String keyword : excludedLoreKeywords) {
                String normalizedKeyword = normalize(keyword);
                for (String line : lore) {
                    if (line.contains(normalizedKeyword)) {
                        return -1;
                    }
                }
            }
            score += 6;
        }

        if (!loreKeywords.isEmpty()) {
            hasRule = true;
            for (String keyword : loreKeywords) {
                String normalizedKeyword = normalize(keyword);
                boolean found = false;
                for (String line : lore) {
                    if (line.contains(normalizedKeyword)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return -1;
                }
            }
            score += 18 + loreKeywords.size() * 4;
        }

        if (potionEffectId != null) {
            hasRule = true;
            if (!isPotionLike(stack) || !hasPotionEffect(stack, potionEffectId, minAmplifier)) {
                return -1;
            }
            if (!isPotionNameCompatible(normalizedName, lore, potionEffectId)) {
                return -1;
            }
            score += 42;
        }

        if (id.startsWith("crusher_")) {
            if (!hasCrusherOriginalMarker(lore)) {
                return -1;
            }
            if (containsLore(lore, normalize("шипы"))) {
                return -1;
            }
            score += 26;
        }

        return hasRule ? score : -1;
    }

    public boolean isGenericBaseRule() {
        return matchItem != null
                && (nameFragment == null || nameFragment.isBlank())
                && loreKeywords.isEmpty()
                && excludedLoreKeywords.isEmpty()
                && potionEffectId == null
                && !id.startsWith("crusher_");
    }

    public static AutoTradeItem byId(String id) {
        for (AutoTradeItem item : values()) {
            if (item.id.equalsIgnoreCase(id)) {
                return item;
            }
        }
        return null;
    }

    private static boolean isPotionLike(ItemStack stack) {
        return stack.isOf(Items.POTION)
                || stack.isOf(Items.SPLASH_POTION)
                || stack.isOf(Items.LINGERING_POTION);
    }

    private static boolean hasPotionEffect(ItemStack stack, String effectId, int minAmplifier) {
        Map<String, Integer> effects = parsePotionEffects(stack);
        return effects.getOrDefault(effectId, 0) >= minAmplifier;
    }

    private static boolean isPotionNameCompatible(String normalizedName, List<String> lore, String effectId) {
        if (effectId == null || !effectId.endsWith("invisibility")) {
            return true;
        }

        if (containsAny(normalizedName, "невид", "invis")) {
            return true;
        }

        for (String line : lore) {
            if (containsAny(line, "невид", "invis")) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Integer> parsePotionEffects(ItemStack stack) {
        Map<String, Integer> result = new LinkedHashMap<>();
        PotionContentsComponent potionContents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
        for (StatusEffectInstance effect : potionContents.getEffects()) {
            Identifier id = Registries.STATUS_EFFECT.getId(effect.getEffectType().value());
            if (id != null) {
                result.put(id.toString(), effect.getAmplifier() + 1);
            }
        }
        return result;
    }

    private static List<String> getNormalizedLore(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return List.of();
        }
        return lore.lines().stream()
                .map(Text::getString)
                .map(AutoTradeItem::normalize)
                .toList();
    }

    private static boolean containsLore(List<String> lore, String expected) {
        for (String line : lore) {
            if (line.contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCrusherOriginalMarker(List<String> lore) {
        return containsLore(lore, normalize("Оригинальный предмет"))
                || containsLore(lore, normalize("★"));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceAll("(?i)\u00a7[0-9A-FK-ORX]", "")
                .replace(' ', ' ')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\s{2,}", " ")
                .trim();
    }

    private static ItemStack createStack(Item item) {
        return new ItemStack(item);
    }
}
