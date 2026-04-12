package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.utils.auction.ParseModeChoice;
import sweetie.nezi.api.utils.auction.PriceParser;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.api.utils.notification.NotificationUtil;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeItem;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeMarketUtil;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "Auto Setup", category = Category.OTHER)
public class AutoSetupModule extends Module {
    @Getter
    private static final AutoSetupModule instance = new AutoSetupModule();

    private enum SetupState {
        IDLE,
        WAITING_RESULTS,
        SCANNING,
        BETWEEN_ITEMS
    }

    private record SearchEntry(AutoTradeItem item, String searchTerm) { }
    private record PricingDecision(int buyPrice, int sellPrice) { }

    private static final int AUCTION_MIN_SLOT = 0;
    private static final int AUCTION_MAX_SLOT = 44;
    private static final int REFRESH_SLOT = 49;
    private static final long SEARCH_WINDOW_MS = 4_200L;
    private static final long EARLY_FINISH_MS = 1_500L;
    private static final long BETWEEN_ITEMS_DELAY_MS = 320L;
    private static final long EARLY_REFRESH_DELAY_MS = 760L;
    private static final long NORMAL_REFRESH_DELAY_MS = 1_120L;
    private static final long LATE_REFRESH_DELAY_MS = 1_480L;
    private static final long RETRY_SEARCH_MS = 1_400L;
    private static final int LOWEST_SAMPLE_LIMIT = 8;
    private static final int MIN_SAMPLE_COUNT = 3;
    private static final int CHEAP_LIMIT = 150_000;
    private static final int MEDIUM_LIMIT = 1_000_000;
    private static final Pattern BROAD_PRICE_PATTERN = Pattern.compile("Цен[аaAАыЫ]?:?\\s*\\$?\\s*([\\d,\\s.]+)", Pattern.CASE_INSENSITIVE);

    private static final Map<AutoTradeItem, List<String>> EXTRA_SEARCH_TERMS = Map.ofEntries(
            Map.entry(AutoTradeItem.INVISIBILITY, List.of("инвиз", "зелье невидимости", "invisibility")),
            Map.entry(AutoTradeItem.GOLDEN_APPLE, List.of("гэпл", "золотое яблоко", "golden apple")),
            Map.entry(AutoTradeItem.ENCHANTED_GAPPLE, List.of("чарка", "зачарованное яблоко", "enchanted golden apple")),
            Map.entry(AutoTradeItem.TOTEM, List.of("тотем", "тотем бессмертия")),
            Map.entry(AutoTradeItem.BLOCK_DAMAGER, List.of("блок дамагер", "дамагер")),
            Map.entry(AutoTradeItem.CHUNK_LOADER_1X1, List.of("прогрузчик чанков", "блок чанкер")),
            Map.entry(AutoTradeItem.CHUNK_LOADER_3X3, List.of("прогрузчик чанков", "блок чанкер")),
            Map.entry(AutoTradeItem.CHUNK_LOADER_5X5, List.of("прогрузчик чанков", "блок чанкер")),
            Map.entry(AutoTradeItem.LOCKPICK_SPHERES, List.of("отмычка к сферам", "отмычка")),
            Map.entry(AutoTradeItem.DRAGON_SKIN, List.of("драконий скин", "скин")),
            Map.entry(AutoTradeItem.EMERALD_ORE, List.of("изумрудная руда", "изумруд")),
            Map.entry(AutoTradeItem.MACE, List.of("булава")),
            Map.entry(AutoTradeItem.CRUSHER_SWORD, List.of("меч крушителя")),
            Map.entry(AutoTradeItem.CRUSHER_CROSSBOW, List.of("арбалет крушителя")),
            Map.entry(AutoTradeItem.CRUSHER_TRIDENT, List.of("трезубец крушителя")),
            Map.entry(AutoTradeItem.CRUSHER_MACE, List.of("булава крушителя"))
    );

    private static final List<SearchEntry> SEARCH_ENTRIES = List.of(
            new SearchEntry(AutoTradeItem.INVISIBILITY, "инвиз"),
            new SearchEntry(AutoTradeItem.GOLDEN_APPLE, "гэпл"),
            new SearchEntry(AutoTradeItem.ENCHANTED_GAPPLE, "чарка"),
            new SearchEntry(AutoTradeItem.ELYTRA, "элитры"),
            new SearchEntry(AutoTradeItem.NETHERITE_INGOT, "незеритовый слиток"),
            new SearchEntry(AutoTradeItem.SPAWNER, "спавнер"),
            new SearchEntry(AutoTradeItem.DIAMOND, "алмаз"),
            new SearchEntry(AutoTradeItem.BEACON, "маяк"),
            new SearchEntry(AutoTradeItem.SNIFFER_EGG, "яйцо нюхача"),
            new SearchEntry(AutoTradeItem.TRIAL_KEY, "ключ испытаний"),
            new SearchEntry(AutoTradeItem.DRAGON_HEAD, "голова дракона"),
            new SearchEntry(AutoTradeItem.VILLAGER_SPAWN_EGG, "яйцо жителя"),
            new SearchEntry(AutoTradeItem.DYNAMITE_BLACK, "блэк"),
            new SearchEntry(AutoTradeItem.DYNAMITE_WHITE, "вайт"),
            new SearchEntry(AutoTradeItem.SILVER, "серебро"),
            new SearchEntry(AutoTradeItem.TRAPKA, "трапка"),
            new SearchEntry(AutoTradeItem.TOTEM, "тотем бессмертия"),
            new SearchEntry(AutoTradeItem.BLOCK_DAMAGER, "блок дамагер"),
            new SearchEntry(AutoTradeItem.CHUNK_LOADER_1X1, "прогрузчик чанков [1x1]"),
            new SearchEntry(AutoTradeItem.CHUNK_LOADER_3X3, "прогрузчик чанков [3x3]"),
            new SearchEntry(AutoTradeItem.CHUNK_LOADER_5X5, "прогрузчик чанков [5x5]"),
            new SearchEntry(AutoTradeItem.LOCKPICK_SPHERES, "отмычка к сферам"),
            new SearchEntry(AutoTradeItem.DRAGON_SKIN, "драконий скин"),
            new SearchEntry(AutoTradeItem.EMERALD_ORE, "изумрудная руда"),
            new SearchEntry(AutoTradeItem.MACE, "булава"),
            new SearchEntry(AutoTradeItem.SPHERE_BEAST, "сфера бестии"),
            new SearchEntry(AutoTradeItem.SPHERE_SATYR, "сфера сатира"),
            new SearchEntry(AutoTradeItem.SPHERE_CHAOS, "сфера хаоса"),
            new SearchEntry(AutoTradeItem.SPHERE_ARES, "сфера ареса"),
            new SearchEntry(AutoTradeItem.SPHERE_HYDRA, "сфера гидры"),
            new SearchEntry(AutoTradeItem.SPHERE_TITAN, "сфера титана"),
            new SearchEntry(AutoTradeItem.TALISMAN_DEMON, "талисман демона"),
            new SearchEntry(AutoTradeItem.TALISMAN_DISCORD, "талисман раздора"),
            new SearchEntry(AutoTradeItem.TALISMAN_RAGE, "ярости"),
            new SearchEntry(AutoTradeItem.TALISMAN_CRUSHER, "талисман крушителя"),
            new SearchEntry(AutoTradeItem.TALISMAN_TYRANT, "тиран"),
            new SearchEntry(AutoTradeItem.POTION_ASSASSIN, "зелье ассасина"),
            new SearchEntry(AutoTradeItem.POTION_HOLY_WATER, "святая вода"),
            new SearchEntry(AutoTradeItem.POTION_PALADIN, "зелье палладина"),
            new SearchEntry(AutoTradeItem.POTION_SLEEPING, "снотворное"),
            new SearchEntry(AutoTradeItem.POTION_CLAPPER, "хлопушка"),
            new SearchEntry(AutoTradeItem.POTION_WRATH, "зелье гнева"),
            new SearchEntry(AutoTradeItem.POTION_RADIATION, "зелье радиации"),
            new SearchEntry(AutoTradeItem.CRUSHER_SWORD, "меч крушителя"),
            new SearchEntry(AutoTradeItem.CRUSHER_PICKAXE, "кирка крушителя"),
            new SearchEntry(AutoTradeItem.CRUSHER_CROSSBOW, "арбалет крушителя"),
            new SearchEntry(AutoTradeItem.CRUSHER_TRIDENT, "трезубец крушителя"),
            new SearchEntry(AutoTradeItem.CRUSHER_MACE, "булава крушителя"),
            new SearchEntry(AutoTradeItem.CRUSHER_LEGGINGS, "поножи крушителя"),
            new SearchEntry(AutoTradeItem.CRUSHER_CHESTPLATE, "нагрудник крушителя"),
            new SearchEntry(AutoTradeItem.CRUSHER_HELMET, "шлем крушителя"),
            new SearchEntry(AutoTradeItem.CRUSHER_BOOTS, "ботинки крушителя")
    );

    private final PriceParser priceParser = new PriceParser();
    private final TimerUtil phaseTimer = new TimerUtil();
    private final TimerUtil refreshTimer = new TimerUtil();
    private final TimerUtil retryTimer = new TimerUtil();
    private final List<SearchEntry> activeEntries = new ArrayList<>();
    private final List<Integer> sampledUnitPrices = new ArrayList<>();

    private SetupState state = SetupState.IDLE;
    private int currentIndex;
    private int bestUnitPrice = Integer.MAX_VALUE;
    private SearchEntry currentEntry;
    private List<String> currentSearchTerms = List.of();
    private int currentSearchTermIndex;

    public AutoSetupModule() {
        priceParser.currentMode = ParseModeChoice.FUN_TIME;
    }

    @Override
    public void onEnable() {
        startSetupProcess();
    }

    @Override
    public void onDisable() {
        state = SetupState.IDLE;
        currentIndex = 0;
        bestUnitPrice = Integer.MAX_VALUE;
        currentEntry = null;
        currentSearchTerms = List.of();
        currentSearchTermIndex = 0;
        activeEntries.clear();
        sampledUnitPrices.clear();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> handleUpdate()));
        addEvents(update);
    }

    private void startSetupProcess() {
        state = SetupState.IDLE;
        currentIndex = 0;
        bestUnitPrice = Integer.MAX_VALUE;
        currentEntry = null;
        currentSearchTerms = List.of();
        currentSearchTermIndex = 0;
        activeEntries.clear();
        sampledUnitPrices.clear();
        activeEntries.addAll(buildActiveEntries());
        AutoBuyModule.getInstance().notifyAutoSetupProgress("Автопарс", "Готовлю список включённых предметов");
        notifyUser("Автосетап запущен");
        beginSearch();
    }

    private void handleUpdate() {
        if (state == SetupState.IDLE || mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        switch (state) {
            case WAITING_RESULTS -> handleWaitingResults();
            case SCANNING -> handleScanning();
            case BETWEEN_ITEMS -> handleBetweenItems();
            default -> {
            }
        }
    }

    private void handleWaitingResults() {
        if (isAuctionScreenOpen()) {
            state = SetupState.SCANNING;
            phaseTimer.reset();
            refreshTimer.reset();
            bestUnitPrice = Integer.MAX_VALUE;
            sampledUnitPrices.clear();
            AutoBuyModule.getInstance().notifyAutoSetupProgress("Автопарс", "Сканирую: " + formatEntryName(currentEntry));
            return;
        }

        if (retryTimer.finished(RETRY_SEARCH_MS)) {
            sendCurrentSearchCommand();
            retryTimer.reset();
            AutoBuyModule.getInstance().notifyAutoSetupProgress("Автопарс", "Повторяю поиск: " + formatEntryName(currentEntry));
        }
    }

    private void handleScanning() {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest) || !isAuctionScreenOpen()) {
            state = SetupState.WAITING_RESULTS;
            retryTimer.reset();
            return;
        }

        scanPage(chest);

        if (shouldFinishCurrentEntry()) {
            finishCurrentEntry();
            return;
        }

        if (refreshTimer.finished(currentRefreshDelay())) {
            refreshPage(chest);
            refreshTimer.reset();
            AutoBuyModule.getInstance().notifyAutoSetupProgress("Автопарс", "Обновляю страницу для: " + formatEntryName(currentEntry));
        }
    }

    private void handleBetweenItems() {
        if (!phaseTimer.finished(BETWEEN_ITEMS_DELAY_MS)) {
            return;
        }

        currentIndex++;
        beginSearch();
    }

    private void beginSearch() {
        if (currentIndex >= activeEntries.size()) {
            finishSetup();
            return;
        }

        currentEntry = activeEntries.get(currentIndex);
        bestUnitPrice = Integer.MAX_VALUE;
        sampledUnitPrices.clear();
        currentSearchTerms = resolveSearchTerms(currentEntry);
        currentSearchTermIndex = 0;
        state = SetupState.WAITING_RESULTS;

        if (mc.currentScreen != null && mc.player != null) {
            mc.player.closeHandledScreen();
        }

        sendCurrentSearchCommand();
        retryTimer.reset();
        AutoBuyModule.getInstance().notifyAutoSetupProgress("Автопарс", "Ищу: " + formatEntryName(currentEntry));
        notifyUser("Поиск: " + formatEntryName(currentEntry));
    }

    private void finishCurrentEntry() {
        if (currentEntry != null && sampledUnitPrices.isEmpty() && currentSearchTermIndex + 1 < currentSearchTerms.size()) {
            currentSearchTermIndex++;
            state = SetupState.WAITING_RESULTS;
            if (mc.currentScreen != null && mc.player != null) {
                mc.player.closeHandledScreen();
            }
            sendCurrentSearchCommand();
            retryTimer.reset();
            AutoBuyModule.getInstance().notifyAutoSetupProgress("Автопарс", "Повторный поиск: " + formatEntryName(currentEntry));
            notifyUser("Повтор: " + currentEntry.item().getTitle());
            return;
        }

        if (currentEntry != null && !sampledUnitPrices.isEmpty()) {
            PricingDecision pricing = resolvePricingDecision();
            AutoTradeRule rule = AutoBuyModule.getInstance().getRule(currentEntry.item());
            if (rule != null && pricing != null) {
                rule.setEnabled(true);
                rule.setBuyUnitPrice(pricing.buyPrice());
                rule.setSellPrice(pricing.sellPrice());
                AutoBuyModule.getInstance().commitRules();
                notifyUser(currentEntry.item().getTitle() + ": купить " + formatPrice(pricing.buyPrice()) + " / продать " + formatPrice(pricing.sellPrice()));
            }
        } else if (currentEntry != null) {
            notifyUser("Цена не найдена: " + currentEntry.item().getTitle());
        }

        if (mc.currentScreen != null && mc.player != null) {
            mc.player.closeHandledScreen();
        }

        state = SetupState.BETWEEN_ITEMS;
        phaseTimer.reset();
    }

    private void finishSetup() {
        notifyUser("Автосетап завершён");
        AutoBuyModule.getInstance().markAutoSetupCompleted();
        AutoSellModule.getInstance().resumeAfterParsing();
        if (!AutoSellModule.getInstance().isEnabled()) {
            AutoSellModule.getInstance().setEnabled(true);
        }
        setEnabled(false);
    }

    private boolean shouldFinishCurrentEntry() {
        return phaseTimer.finished(SEARCH_WINDOW_MS)
                || phaseTimer.finished(EARLY_FINISH_MS) && sampledUnitPrices.size() >= MIN_SAMPLE_COUNT;
    }

    private long currentRefreshDelay() {
        long elapsed = phaseTimer.getElapsedTime();

        if (elapsed < 900L && sampledUnitPrices.size() < 2) {
            return EARLY_REFRESH_DELAY_MS;
        }

        if (elapsed < 2_600L && sampledUnitPrices.size() < 4) {
            return NORMAL_REFRESH_DELAY_MS;
        }

        return LATE_REFRESH_DELAY_MS;
    }

    private List<String> resolveSearchTerms(SearchEntry entry) {
        List<String> terms = new ArrayList<>();
        if (entry == null) {
            return terms;
        }

        if (entry.searchTerm() != null && !entry.searchTerm().isBlank()) {
            terms.add(entry.searchTerm());
        }

        for (String term : EXTRA_SEARCH_TERMS.getOrDefault(entry.item(), List.of())) {
            if (term != null && !term.isBlank() && !terms.contains(term)) {
                terms.add(term);
            }
        }

        return terms;
    }

    private void sendCurrentSearchCommand() {
        if (currentSearchTerms.isEmpty()) {
            return;
        }

        int index = Math.max(0, Math.min(currentSearchTermIndex, currentSearchTerms.size() - 1));
        sendSearchCommand(currentSearchTerms.get(index));
    }

    private PricingDecision resolvePricingDecision() {
        if (sampledUnitPrices.isEmpty()) {
            return null;
        }

        List<Integer> prices = new ArrayList<>(sampledUnitPrices);
        prices.sort(Integer::compareTo);

        int first = prices.get(0);
        int second = prices.size() > 1 ? prices.get(1) : first;
        int third = prices.size() > 2 ? prices.get(2) : second;
        int firstGap = Math.max(0, second - first);
        int secondGap = Math.max(0, third - second);

        boolean isolatedLowest = prices.size() > 1
                && firstGap >= Math.max(priceStep(first) * 2, Math.round(first * 0.18f))
                && firstGap >= secondGap * 2;

        float sellMarkup = isVolatileItem(currentEntry == null ? null : currentEntry.item()) ? 1.07f : 1.06f;
        int tentativeSell = roundPriceNearest(Math.round(first * sellMarkup));
        if (isolatedLowest) {
            tentativeSell = Math.max(tentativeSell, roundPriceNearest(first + Math.round(firstGap * 0.34f)));
        }

        int sellCap = Integer.MAX_VALUE;
        if (prices.size() > 1) {
            sellCap = roundPriceDown(Math.max(priceStep(second), second - priceStep(second)));
        }

        int sellPrice = Math.max(roundPriceNearest(first), Math.min(tentativeSell, sellCap));

        float buyMultiplier = resolveBuyMultiplier(currentEntry == null ? null : currentEntry.item(), sellPrice);
        int buyPrice = roundPriceDown(Math.round(sellPrice * buyMultiplier));
        if (buyPrice >= sellPrice) {
            buyPrice = roundPriceDown(Math.max(priceStep(sellPrice), sellPrice - priceStep(sellPrice)));
        }

        return new PricingDecision(Math.max(1, buyPrice), Math.max(1, sellPrice));
    }

    private float resolveBuyMultiplier(AutoTradeItem item, int sourcePrice) {
        float multiplier;
        if (sourcePrice <= CHEAP_LIMIT) {
            multiplier = 0.84f;
        } else if (sourcePrice <= MEDIUM_LIMIT) {
            multiplier = 0.81f;
        } else {
            multiplier = 0.78f;
        }

        if (isVolatileItem(item)) {
            multiplier -= 0.10f;
        }

        return MathHelper.clamp(multiplier, 0.55f, 0.95f);
    }

    private void scanPage(GenericContainerScreenHandler chest) {
        int upper = Math.min(AUCTION_MAX_SLOT + 1, chest.slots.size());
        for (int slotId = AUCTION_MIN_SLOT; slotId < upper; slotId++) {
            if (!isAuctionItemSlot(slotId)) {
                continue;
            }

            Slot slot = chest.getSlot(slotId);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || currentEntry == null || !currentEntry.item().matches(stack)) {
                continue;
            }

            List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
            int totalPrice = getAuctionPrice(stack, tooltip);
            if (totalPrice <= 0 || stack.getCount() <= 0) {
                continue;
            }

            int unitPrice = Math.max(1, (int) Math.floor(totalPrice / (double) stack.getCount()));
            rememberUnitPrice(unitPrice);
        }
    }

    private void rememberUnitPrice(int unitPrice) {
        if (unitPrice <= 0) {
            return;
        }

        bestUnitPrice = Math.min(bestUnitPrice, unitPrice);
        if (!sampledUnitPrices.contains(unitPrice)) {
            sampledUnitPrices.add(unitPrice);
            sampledUnitPrices.sort(Integer::compareTo);
            while (sampledUnitPrices.size() > LOWEST_SAMPLE_LIMIT) {
                sampledUnitPrices.remove(sampledUnitPrices.size() - 1);
            }
        }
    }

    private void refreshPage(GenericContainerScreenHandler chest) {
        if (chest.slots.size() <= REFRESH_SLOT) {
            return;
        }

        mc.interactionManager.clickSlot(chest.syncId, REFRESH_SLOT, 0, SlotActionType.PICKUP, mc.player);
    }

    private List<SearchEntry> buildActiveEntries() {
        List<SearchEntry> entries = new ArrayList<>();
        AutoBuyModule autoBuyModule = AutoBuyModule.getInstance();
        for (SearchEntry entry : SEARCH_ENTRIES) {
            AutoTradeRule rule = autoBuyModule.getRule(entry.item());
            if (rule != null && rule.isEnabled()) {
                entries.add(entry);
            }
        }

        if (entries.isEmpty()) {
            entries.addAll(SEARCH_ENTRIES);
        }
        return entries;
    }

    private boolean isAuctionItemSlot(int slotId) {
        return slotId >= AUCTION_MIN_SLOT && slotId <= AUCTION_MAX_SLOT;
    }

    private int getAuctionPrice(ItemStack stack, List<Text> tooltip) {
        int parsed = AutoTradeMarketUtil.parseAuctionPrice(stack, tooltip);
        if (parsed > 0) {
            return parsed;
        }

        parsed = priceParser.getPrice(stack, tooltip);
        if (parsed > 0) {
            return parsed;
        }

        for (Text line : tooltip) {
            String clean = line.getString().replaceAll("§[0-9a-fk-or]", "").trim();
            Matcher matcher = BROAD_PRICE_PATTERN.matcher(clean);
            if (!matcher.find()) {
                continue;
            }

            String digits = matcher.group(1).replaceAll("[,\\s.]", "");
            if (digits.isEmpty()) {
                continue;
            }

            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
            }
        }

        return -1;
    }

    private boolean isVolatileItem(AutoTradeItem item) {
        return item == AutoTradeItem.INVISIBILITY
                || item == AutoTradeItem.TRIAL_KEY
                || item == AutoTradeItem.SNIFFER_EGG;
    }

    private boolean isAuctionScreenOpen() {
        if (mc.currentScreen == null || !(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest)) {
            return false;
        }

        String title = normalizeTitle(mc.currentScreen.getTitle().getString());
        return chest.slots.size() > REFRESH_SLOT && AuctionHelperModule.isAuctionTitleLike(title);
    }

    private void sendSearchCommand(String searchTerm) {
        if (mc.player == null || mc.player.networkHandler == null || currentEntry == null) {
            return;
        }

        mc.player.networkHandler.sendChatCommand("ah search " + searchTerm);
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
    }

    private int roundPriceNearest(int value) {
        int step = priceStep(value);
        return Math.max(step, Math.round(value / (float) step) * step);
    }

    private int roundPriceDown(int value) {
        int step = priceStep(value);
        return Math.max(step, (value / step) * step);
    }

    private int priceStep(int value) {
        if (value >= 10_000_000) {
            return 100_000;
        }
        if (value >= 1_000_000) {
            return 50_000;
        }
        if (value >= 250_000) {
            return 10_000;
        }
        if (value >= 50_000) {
            return 5_000;
        }
        if (value >= 10_000) {
            return 1_000;
        }
        return 100;
    }

    private String formatPrice(int value) {
        if (value >= 1_000_000) {
            return String.format(Locale.US, "%.1fкк", value / 1_000_000f);
        }
        if (value >= 1_000) {
            return String.format(Locale.US, "%.1fк", value / 1_000f);
        }
        return String.valueOf(value);
    }

    private String formatEntryName(SearchEntry entry) {
        if (entry == null) {
            return "неизвестно";
        }
        return entry.item().getTitle() + " " + entry.item().getSubtitle();
    }

    private void notifyUser(String text) {
        NotificationUtil.add(text);
    }
}
