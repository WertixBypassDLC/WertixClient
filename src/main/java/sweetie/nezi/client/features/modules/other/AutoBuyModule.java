package sweetie.nezi.client.features.modules.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.RunSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.module.setting.StringSetting;
import sweetie.nezi.api.system.configs.ConfigManager;
import sweetie.nezi.api.utils.auction.ParseModeChoice;
import sweetie.nezi.api.utils.auction.PriceParser;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeHistoryManager;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeItem;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeMarketUtil;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeRule;
import sweetie.nezi.client.ui.autobuy.AutoBuyConfigScreen;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "Auto Buy", category = Category.OTHER)
public class AutoBuyModule extends Module {
    @Getter
    private static final AutoBuyModule instance = new AutoBuyModule();

    private enum AutoBuyState {
        IDLE,
        WAITING_REFRESH_UPDATE,
        WAITING_CONFIRM
    }

    private record BuyCandidate(Slot slot, AutoTradeItem item, AutoTradeRule rule, int totalPrice, int unitPrice, int count) { }
    private record BalanceMatch(long value, String line) { }
    private record PendingPurchaseSnapshot(ItemStack stack, String itemName, int count, int totalPrice) { }

    private static final int REFRESH_SLOT = 49;
    private static final long AUCTION_OPEN_INTERVAL_MS = 3_000L;
    private static final long ACTION_DELAY_MS = 55L;
    private static final long BUY_CONFIRM_TIMEOUT_MS = 2_800L;
    private static final long BUY_CONFIRM_SCAN_DELAY_MS = 50L;
    private static final long BUY_CONFIRM_RETRY_DELAY_MS = 140L;
    private static final long BUY_CONFIRM_OPEN_RETRY_MS = 180L;
    private static final long BUY_CONFIRM_RETURN_TO_SCAN_MS = 650L;
    private static final int BUY_CONFIRM_OPEN_RETRY_COUNT = 2;
    private static final long AUTO_SETUP_INTERVAL_MS = 20L * 60L * 1_000L;
    private static final long ANARCHY_SWITCH_DELAY_MS = 30_000L;
    private static final long ANARCHY_RETRY_DELAY_MS = 2_500L;
    private static final long STALE_AUCTION_RECOVERY_MS = 3_200L;
    private static final long REFRESH_RESPONSE_TIMEOUT_MS = 240L;
    private static final long REFRESH_SLOW_THRESHOLD_MS = 100L;
    private static final int MIN_SLOW_REFRESH_STREAK = 3;
    private static final int AUTO_SELL_RESUME_FREE_SLOTS = 6;
    private static final int BALANCE_SCOREBOARD_LINE_INDEX = 4;
    private static final Pattern ANARCHY_PATTERN = Pattern.compile("(\\d{3})");
    private static final Pattern BROAD_PRICE_PATTERN = Pattern.compile("Цен[аaAАыЫ]?:?\\s*\\$?\\s*([\\d,\\s.]+)", Pattern.CASE_INSENSITIVE);

    private static final int[] ANARCHY_CYCLE = {
            101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112,
            201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228,
            301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319,
            501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512,
            901, 902, 903, 904
    };

    private final PriceParser priceParser = new PriceParser();
    private final Map<AutoTradeItem, AutoTradeRule> rules = new EnumMap<>(AutoTradeItem.class);
    private final AutoTradeHistoryManager historyManager = new AutoTradeHistoryManager();

    private final BooleanSetting autoOpenAuction = new BooleanSetting("Авто /ah").value(true);
    private final SliderSetting refreshDelaySetting = new SliderSetting("Обновление страниц").value(80f).range(50f, 200f).step(1f);
    private final SliderSetting slowRefreshMs = new SliderSetting("Медленное обновление").value(170f).range(120f, 500f).step(1f);
    private final StringSetting auctionCommand = new StringSetting("Команда аукциона").value("ah");
    private final StringSetting nextAnarchyCommand = new StringSetting("Команда анархии").value("an%d");
    private final RunSetting openMenu = new RunSetting("Открыть меню").value(this::openConfigMenu);
    private final StringSetting rulesData = new StringSetting("AutoBuy Rules").value("").setVisible(() -> false).onAction(this::loadRulesFromSetting);

    private final TimerUtil actionTimer = new TimerUtil();
    private final TimerUtil openAuctionTimer = new TimerUtil();
    private final TimerUtil refreshTimer = new TimerUtil();
    private final TimerUtil responseTimer = new TimerUtil();
    private final TimerUtil confirmTimer = new TimerUtil();

    private AutoBuyState state = AutoBuyState.IDLE;
    private int currentAnarchyIndex;
    private boolean currentAnarchyInitialized;
    private long nextRefreshDelay = 80L;
    private int lastAuctionFingerprint;
    private long currentBalance = -1L;
    private int currentAnarchyNumber = -1;
    private long currentAnarchySince = 0L;
    private String balanceDebugSource = "NONE";
    private String balanceDebugLine = "-";
    private String balanceDebugAltLine = "-";
    private String balanceDebugLabelLine = "-";
    private long balanceDebugPrimaryValue = -1L;
    private long balanceDebugAltValue = -1L;
    private long balanceDebugLabelValue = -1L;
    private List<String> sidebarDebugLines = List.of();
    private int slowRefreshStreak;
    private long lastSlowRefreshElapsed;
    private long lastAuctionActivityAt;
    private int pendingAnarchyTarget = -1;
    private long pendingAnarchySentAt;
    private int pendingAnarchyAttempts;
    private boolean purchaseConfirmClicked;
    private int purchaseOpenRetryCount;
    private BuyCandidate pendingCandidate;
    private boolean automationEnabled;
    private long nextAutoSetupAt;
    private boolean priceProfileReady;
    private long lastPositiveBalance = -1L;
    private long lastPositiveBalanceAt;
    private int staleAuctionFingerprint;
    private long staleAuctionSince;
    private PendingPurchaseSnapshot pendingPurchaseSnapshot;
    private String actionDebugState = "Ожидание";
    private String actionDebugText = "Автоматизация выключена";
    private float auctionToggleX;
    private float auctionToggleY;
    private float auctionToggleWidth = 88f;
    private float auctionToggleHeight = 15f;

    public AutoBuyModule() {
        for (AutoTradeItem item : AutoTradeItem.values()) {
            rules.put(item, new AutoTradeRule());
        }

        priceParser.currentMode = ParseModeChoice.FUN_TIME;
        addSettings(refreshDelaySetting, openMenu, rulesData);
        saveRulesToSetting();
    }

    @Override
    public void onEnable() {
        state = AutoBuyState.IDLE;
        currentAnarchyInitialized = false;
        nextRefreshDelay = nextRefreshDelay();
        lastAuctionFingerprint = 0;
        currentBalance = -1L;
        currentAnarchyNumber = -1;
        currentAnarchySince = 0L;
        automationEnabled = false;
        balanceDebugSource = "NONE";
        balanceDebugLine = "-";
        balanceDebugAltLine = "-";
        balanceDebugLabelLine = "-";
        balanceDebugPrimaryValue = -1L;
        balanceDebugAltValue = -1L;
        balanceDebugLabelValue = -1L;
        sidebarDebugLines = List.of();
        slowRefreshStreak = 0;
        lastSlowRefreshElapsed = 0L;
        lastAuctionActivityAt = System.currentTimeMillis();
        pendingAnarchyTarget = -1;
        pendingAnarchySentAt = 0L;
        pendingAnarchyAttempts = 0;
        purchaseConfirmClicked = false;
        purchaseOpenRetryCount = 0;
        pendingCandidate = null;
        nextAutoSetupAt = 0L;
        priceProfileReady = false;
        lastPositiveBalance = -1L;
        lastPositiveBalanceAt = 0L;
        staleAuctionFingerprint = 0;
        staleAuctionSince = 0L;
        actionDebugState = "Ожидание";
        actionDebugText = "Автоматизация выключена";

        actionTimer.reset();
        refreshTimer.reset();
        responseTimer.reset();
        confirmTimer.reset();
        openAuctionTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
    }

    @Override
    public void onDisable() {
        resetAutomationState();
        automationEnabled = false;
        priceProfileReady = false;
        setDebugState("Ожидание", "Автоматизация выключена");
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> handleUpdate()));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::handlePacket));
        addEvents(update, packet);
    }

    public Map<AutoTradeItem, AutoTradeRule> getRules() {
        return rules;
    }

    public AutoTradeRule getRule(AutoTradeItem item) {
        return rules.get(item);
    }

    public void commitRules() {
        saveRulesToSetting();
        ConfigManager.getInstance().save();
    }

    public boolean isProcessingPurchase() {
        return state == AutoBuyState.WAITING_CONFIRM;
    }

    public boolean isManagingStorageFlow() {
        return false;
    }

    public boolean isAutomationEnabled() {
        return automationEnabled;
    }

    public long getCurrentBalance() {
        return currentBalance;
    }

    public long getFiveCheapestReserve() {
        return rules.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .filter(entry -> entry.getValue().isEnabled() && entry.getValue().getBuyUnitPrice() > 0)
                .map(entry -> (long) entry.getValue().getBuyUnitPrice() * Math.max(1, entry.getValue().getMinStackCount()))
                .filter(value -> value > 0L)
                .sorted()
                .limit(5)
                .reduce(0L, Long::sum);
    }

    public int getActiveConfiguredItemCount() {
        return (int) rules.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .filter(entry -> entry.getValue().isEnabled() && entry.getValue().getBuyUnitPrice() > 0)
                .count();
    }

    public int countAffordableConfiguredItems(long balance) {
        if (balance <= 0L) {
            return 0;
        }

        int affordable = 0;
        for (Map.Entry<AutoTradeItem, AutoTradeRule> entry : rules.entrySet()) {
            AutoTradeItem item = entry.getKey();
            AutoTradeRule rule = entry.getValue();
            if (item == null || rule == null || !rule.isEnabled() || rule.getBuyUnitPrice() <= 0) {
                continue;
            }

            long reserve = (long) rule.getBuyUnitPrice() * Math.max(1, rule.getMinStackCount());
            if (reserve > 0L && reserve <= balance) {
                affordable++;
            }
        }
        return affordable;
    }

    public boolean shouldTriggerAutoSellForBalance() {
        int activeConfigured = getActiveConfiguredItemCount();
        if (activeConfigured <= 0) {
            return false;
        }

        long balance = currentBalance > 0L ? currentBalance : lastPositiveBalance;
        if (balance <= 0L) {
            return true;
        }

        int required = Math.min(5, activeConfigured);
        return countAffordableConfiguredItems(balance) < required;
    }

    public boolean hasParsedPriceProfile() {
        return priceProfileReady;
    }

    public AutoTradeHistoryManager getHistoryManager() {
        return historyManager;
    }

    public void notifyAutoSetupProgress(String state, String detail) {
        setDebugState(state, detail);
    }

    public void markAutoSetupCompleted() {
        priceProfileReady = true;
        nextAutoSetupAt = System.currentTimeMillis() + AUTO_SETUP_INTERVAL_MS;
        setDebugState("Автопарс", "Цены обновлены, можно запускать автопродажу");
    }

    public void openConfigMenu() {
        if (mc != null) {
            mc.setScreen(AutoBuyConfigScreen.create(this));
        }
    }

    public void openAuctionForItem(AutoTradeItem item) {
        if (item == null || mc == null || mc.player == null || mc.player.networkHandler == null) {
            return;
        }

        String searchQuery = AutoTradeMarketUtil.resolveSearchQuery(item);
        if (searchQuery.isBlank()) {
            return;
        }

        if (mc.currentScreen != null) {
            mc.setScreen(null);
        }

        sendCommand("ah search " + searchQuery);
        setDebugState("Поиск", "Открываю аукцион для " + item.getTitle() + " " + item.getSubtitle());
    }

    private void handleUpdate() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        updateScoreboardState();
        handlePendingAnarchySwitch();

        if (!currentAnarchyInitialized) {
            currentAnarchyIndex = 0;
            currentAnarchyInitialized = true;
        }

        if (AutoSetupModule.getInstance().isEnabled()) {
            setDebugState("Автопарс", "Жду завершения автопарсера");
            resetAutomationState();
            return;
        }

        if (!automationEnabled) {
            setDebugState("Ожидание", "Автоматизация выключена");
            resetAutomationState();
            return;
        }

        AutoSellModule autoSell = AutoSellModule.getInstance();
        int freeSlots = countFreeInventorySlots();
        if (autoSell.isEmergencySelling()) {
            if (freeSlots < AUTO_SELL_RESUME_FREE_SLOTS) {
                if (mc.currentScreen != null && !(mc.currentScreen instanceof AutoBuyConfigScreen)) {
                    mc.player.closeHandledScreen();
                }
                resetAutomationState();
                setDebugState("Автопродажа", "Продаю предметы: свободно " + freeSlots + "/" + AUTO_SELL_RESUME_FREE_SLOTS);
                return;
            }

            autoSell.clearEmergencySell();
        }

        if (freeSlots <= 0) {
            if (mc.currentScreen != null && !(mc.currentScreen instanceof AutoBuyConfigScreen)) {
                mc.player.closeHandledScreen();
            }
            resetAutomationState();
            autoSell.requestEmergencySell(AUTO_SELL_RESUME_FREE_SLOTS);
            setDebugState("Автопродажа", "Инвентарь полон, жду " + AUTO_SELL_RESUME_FREE_SLOTS + " свободных слотов");
            return;
        }

        if (shouldTriggerAutoSellForBalance()) {
            if (!autoSell.isEnabled()) {
                autoSell.setEnabled(true);
            }
            if (autoSell.isReserveSelling()) {
                if (mc.currentScreen != null && !(mc.currentScreen instanceof AutoBuyConfigScreen)) {
                    mc.player.closeHandledScreen();
                }
                resetAutomationState();
                setDebugState("Автопродажа", "Баланс ниже резерва на 5 предметов, запускаю продажу");
                return;
            }
        }

        if (shouldRunAutoSetup()) {
            setDebugState("Автопарс", "Прошло 15 минут, запускаю обновление цен");
            startAutoSetupCycle();
            return;
        }

        if (mc.currentScreen == null) {
            handleMissingAuctionScreen();
            return;
        }

        if (mc.currentScreen instanceof AutoBuyConfigScreen) {
            setDebugState("Меню", "Открыто меню автобая");
            return;
        }

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest)) {
            setDebugState("Ожидание", "Неподдерживаемое окно");
            return;
        }

        if (state == AutoBuyState.WAITING_CONFIRM) {
            handlePurchaseConfirm(chest);
            return;
        }

        String title = normalizeTitle(mc.currentScreen.getTitle().getString());
        if (!isAuctionScreen(title, chest)) {
            handleForeignContainerScreen();
            return;
        }

        lastAuctionActivityAt = System.currentTimeMillis();

        if (state == AutoBuyState.WAITING_REFRESH_UPDATE && handleRefreshResponse(chest)) {
            return;
        }

        BuyCandidate candidate = findBestCandidate(chest);
        if (candidate == null) {
            if (currentBalance <= 0L) {
                setDebugState("Сканирование", "Баланс не распознан в скорборде");
            } else {
                setDebugState("Сканирование", "Подходящих лотов не найдено");
            }
        }

        if (candidate != null && actionTimer.finished(ACTION_DELAY_MS)) {
            mc.interactionManager.clickSlot(chest.syncId, candidate.slot().id, 0, SlotActionType.PICKUP, mc.player);
            state = AutoBuyState.WAITING_CONFIRM;
            purchaseConfirmClicked = false;
            purchaseOpenRetryCount = 0;
            pendingCandidate = candidate;
            pendingPurchaseSnapshot = createPurchaseSnapshot(candidate);
            confirmTimer.reset();
            actionTimer.reset();
            setDebugState("Покупка", "Жму лот: " + formatCandidate(candidate));
            return;
        }

        if (candidate != null) {
            setDebugState("Покупка", "Готовлю клик: " + formatCandidate(candidate));
        }

        handleRefresh(chest);
    }

    private void handlePacket(PacketEvent.PacketEventData event) {
        if (!event.isReceive() || !(event.packet() instanceof GameMessageS2CPacket packet)) {
            return;
        }

        String message = normalizeTitle(packet.content().getString());
        if (message.isBlank()) {
            return;
        }

        handleAnarchyFeedback(message);

        if (state != AutoBuyState.WAITING_CONFIRM) {
            return;
        }

        if (!isPurchaseFeedback(message)) {
            return;
        }
        if (message.contains("\u044d\u0442\u043e\u0442 \u0442\u043e\u0432\u0430\u0440 \u0443\u0436\u0435 \u043a\u0443\u043f\u0438\u043b\u0438")
                || message.contains("\u0442\u043e\u0432\u0430\u0440 \u0443\u0436\u0435 \u043a\u0443\u043f\u0438\u043b\u0438")
                || message.contains("already bought")) {
            addFailedHistory(message);
        } else if (message.contains("\u0443\u0441\u043f\u0435\u0448\u043d\u043e \u043a\u0443\u043f")
                || message.contains("\u0432\u044b \u043a\u0443\u043f\u0438\u043b\u0438")
                || message.contains("\u043a\u0443\u043f\u043b\u0435\u043d")) {
            addSuccessfulHistory();
        } else {
            addFailedHistory(message);
        }

        if (message.contains("успешно куп")
                || message.contains("вы купили")
                || message.contains("куплен")) {
            setDebugState("Покупка", "Покупка подтверждена сервером");
        } else {
            setDebugState("Покупка", "Сервер отклонил покупку: " + abbreviate(message, 44));
        }

        state = AutoBuyState.IDLE;
        purchaseConfirmClicked = false;
        purchaseOpenRetryCount = 0;
        pendingCandidate = null;
        pendingPurchaseSnapshot = null;
        actionTimer.reset();
        confirmTimer.reset();
        refreshTimer.reset();
        nextRefreshDelay = nextRefreshDelay();
        if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
            openAuctionTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
        }
    }

    private void handleMissingAuctionScreen() {
        if (!autoOpenAuction.getValue()) {
            return;
        }
        boolean recoveringPurchase = state == AutoBuyState.WAITING_CONFIRM;
        if (state == AutoBuyState.WAITING_CONFIRM && !confirmTimer.finished(BUY_CONFIRM_TIMEOUT_MS)) {
            setDebugState("Покупка", "Жду окно подтверждения");
            return;
        }
        if (state != AutoBuyState.IDLE) {
            state = AutoBuyState.IDLE;
            purchaseConfirmClicked = false;
            purchaseOpenRetryCount = 0;
            pendingCandidate = null;
            if (recoveringPurchase) {
                setDebugState("Покупка", "Окно пропало, заново открываю /ah");
                openAuctionTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
            }
        }
        if (!openAuctionTimer.finished(AUCTION_OPEN_INTERVAL_MS)) {
            setDebugState("Ожидание", "Жду таймер перед /ah");
            return;
        }

        sendCommand(auctionCommand.getValue());
        openAuctionTimer.reset();
        actionTimer.reset();
        setDebugState("Аукцион", "Открываю /ah");
    }

    private void handleForeignContainerScreen() {
        if (!autoOpenAuction.getValue()) {
            return;
        }
        boolean recoveringPurchase = state == AutoBuyState.WAITING_CONFIRM;
        if (state == AutoBuyState.WAITING_CONFIRM && !confirmTimer.finished(BUY_CONFIRM_TIMEOUT_MS)) {
            setDebugState("Покупка", "Жду подтверждение в чужом окне");
            return;
        }
        if (recoveringPurchase) {
            state = AutoBuyState.IDLE;
            purchaseConfirmClicked = false;
            purchaseOpenRetryCount = 0;
            pendingCandidate = null;
            setDebugState("Покупка", "Подтверждение зависло, возвращаюсь в /ah");
            openAuctionTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
        }
        if (mc.player != null && mc.currentScreen != null && lastAuctionActivityAt > 0L
                && System.currentTimeMillis() - lastAuctionActivityAt >= STALE_AUCTION_RECOVERY_MS) {
            mc.player.closeHandledScreen();
            setDebugState("Восстановление", "Закрываю чужое окно и возвращаюсь в /ah");
        }
        if (!openAuctionTimer.finished(AUCTION_OPEN_INTERVAL_MS)) {
            return;
        }

        sendCommand(auctionCommand.getValue());
        openAuctionTimer.reset();
        actionTimer.reset();
        setDebugState("Аукцион", "Переоткрываю /ah");
    }

    private void handlePurchaseConfirmLegacy(GenericContainerScreenHandler chest) {
        if (confirmTimer.finished(BUY_CONFIRM_TIMEOUT_MS)) {
            setDebugState("Покупка", "Не дождался подтверждения, переоткрываю /ah");
            state = AutoBuyState.IDLE;
            purchaseConfirmClicked = false;
            purchaseOpenRetryCount = 0;
            pendingCandidate = null;
            if (mc.player != null && mc.currentScreen != null) {
                mc.player.closeHandledScreen();
                openAuctionTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
            }
            return;
        }

        long requiredDelay = purchaseConfirmClicked ? BUY_CONFIRM_RETRY_DELAY_MS : ACTION_DELAY_MS;
        if (confirmTimer.getElapsedTime() < BUY_CONFIRM_SCAN_DELAY_MS || !actionTimer.finished(requiredDelay)) {
            setDebugState("Покупка", purchaseConfirmClicked ? "Повторно жду confirm-окно" : "Жду confirm-окно");
            return;
        }

        String title = mc.currentScreen == null ? "" : normalizeTitle(mc.currentScreen.getTitle().getString());
        if (!chest.slots.isEmpty() && looksLikeConfirmationWindow(title, chest)) {
            mc.interactionManager.clickSlot(chest.syncId, chest.getSlot(0).id, 0, SlotActionType.PICKUP, mc.player);
            purchaseConfirmClicked = true;
            purchaseOpenRetryCount = 0;
            actionTimer.reset();
            refreshTimer.reset();
            nextRefreshDelay = nextRefreshDelay();
            setDebugState("Покупка", "Вижу окно подтверждения, жму первый слот");
            return;
        }

        if (isAuctionScreen(title, chest)) {
            long elapsed = confirmTimer.getElapsedTime();
            if (false) {
                state = AutoBuyState.IDLE;
                purchaseConfirmClicked = false;
                purchaseOpenRetryCount = 0;
                pendingCandidate = null;
                pendingPurchaseSnapshot = null;
                actionTimer.reset();
                setDebugState("Покупка", "Лот в слоте изменился, отменяю подтверждение");
                return;
            }
            if (pendingCandidate == null) {
                state = AutoBuyState.IDLE;
                purchaseConfirmClicked = false;
                purchaseOpenRetryCount = 0;
                setDebugState("Покупка", "Потерял цель покупки, возвращаюсь к поиску");
                return;
            }

            if (!slotStillMatchesPendingCandidate(chest)) {
                state = AutoBuyState.IDLE;
                purchaseConfirmClicked = false;
                purchaseOpenRetryCount = 0;
                pendingCandidate = null;
                pendingPurchaseSnapshot = null;
                actionTimer.reset();
                setDebugState("Покупка", "Лот в слоте изменился, отменяю подтверждение");
                return;
            }

            if (!slotStillMatchesPendingCandidate(chest)) {
                state = AutoBuyState.IDLE;
                purchaseConfirmClicked = false;
                purchaseOpenRetryCount = 0;
                pendingCandidate = null;
                pendingPurchaseSnapshot = null;
                actionTimer.reset();
                setDebugState("Покупка", "Лот в слоте изменился, отменяю подтверждение");
                return;
            }

            if (purchaseOpenRetryCount < BUY_CONFIRM_OPEN_RETRY_COUNT
                    && elapsed >= BUY_CONFIRM_OPEN_RETRY_MS * (purchaseOpenRetryCount + 1L)
                    && actionTimer.finished(BUY_CONFIRM_RETRY_DELAY_MS)) {
                if (!slotStillMatchesPendingCandidate(chest)) {
                    state = AutoBuyState.IDLE;
                    purchaseConfirmClicked = false;
                    purchaseOpenRetryCount = 0;
                    pendingCandidate = null;
                    pendingPurchaseSnapshot = null;
                    actionTimer.reset();
                    setDebugState("Покупка", "Лот в слоте изменился, отменяю повторный клик");
                    return;
                }
                SlotActionType retryAction = purchaseOpenRetryCount == 0 ? SlotActionType.PICKUP : SlotActionType.QUICK_MOVE;
                mc.interactionManager.clickSlot(chest.syncId, pendingCandidate.slot().id, 0, retryAction, mc.player);
                purchaseOpenRetryCount++;
                actionTimer.reset();
                setDebugState("Покупка", "Confirm не открылся, повторно жму лот");
                return;
            }

            if (elapsed >= BUY_CONFIRM_RETURN_TO_SCAN_MS) {
                state = AutoBuyState.IDLE;
                purchaseConfirmClicked = false;
                purchaseOpenRetryCount = 0;
                pendingCandidate = null;
                pendingPurchaseSnapshot = null;
                actionTimer.reset();
                setDebugState("Покупка", "Confirm не появился, возвращаюсь к сканированию");
                return;
            }

            setDebugState("Покупка", "Лот нажат, жду переход в подтверждение");
            return;
        }

        if (isBuyConfirmationTitle(title) && !chest.slots.isEmpty()) {
            mc.interactionManager.clickSlot(chest.syncId, 0, 0, SlotActionType.PICKUP, mc.player);
            purchaseConfirmClicked = true;
            purchaseOpenRetryCount = 0;
            actionTimer.reset();
            refreshTimer.reset();
            nextRefreshDelay = nextRefreshDelay();
            setDebugState("Покупка", "Жму первый слот подтверждения");
            return;
        }

        if (containsGreenConfirmPanel(chest) && !chest.slots.isEmpty()) {
            mc.interactionManager.clickSlot(chest.syncId, chest.getSlot(0).id, 0, SlotActionType.PICKUP, mc.player);
            purchaseConfirmClicked = true;
            purchaseOpenRetryCount = 0;
            actionTimer.reset();
            refreshTimer.reset();
            nextRefreshDelay = nextRefreshDelay();
            setDebugState("Покупка", "Нашёл зелёную панель, жму slot 0");
            return;
        }

        Slot confirmSlot = findConfirmSlot(title, chest);
        if (confirmSlot == null) {
            setDebugState("Покупка", "Не вижу кнопку подтверждения");
            return;
        }

        mc.interactionManager.clickSlot(chest.syncId, confirmSlot.id, 0, SlotActionType.PICKUP, mc.player);
        purchaseConfirmClicked = true;
        purchaseOpenRetryCount = 0;
        actionTimer.reset();
        refreshTimer.reset();
        nextRefreshDelay = nextRefreshDelay();
        setDebugState("Покупка", "Подтверждаю покупку");
    }

    private void handlePurchaseConfirm(GenericContainerScreenHandler chest) {
        if (confirmTimer.finished(BUY_CONFIRM_TIMEOUT_MS)) {
            setDebugState("Покупка", "Не дождался подтверждения, переоткрываю /ah");
            state = AutoBuyState.IDLE;
            purchaseConfirmClicked = false;
            purchaseOpenRetryCount = 0;
            pendingCandidate = null;
            if (mc.player != null && mc.currentScreen != null) {
                mc.player.closeHandledScreen();
                openAuctionTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
            }
            return;
        }

        long requiredDelay = purchaseConfirmClicked ? BUY_CONFIRM_RETRY_DELAY_MS : ACTION_DELAY_MS;
        if (confirmTimer.getElapsedTime() < BUY_CONFIRM_SCAN_DELAY_MS || !actionTimer.finished(requiredDelay)) {
            setDebugState("Покупка", purchaseConfirmClicked ? "Повторно жду окно подтверждения" : "Жду окно подтверждения");
            return;
        }

        String title = mc.currentScreen == null ? "" : normalizeTitle(mc.currentScreen.getTitle().getString());
        boolean confirmationWindow = isBuyConfirmationTitle(title)
                || containsGreenConfirmPanel(chest)
                || looksLikeConfirmationWindow(title, chest);
        if (!chest.slots.isEmpty() && confirmationWindow) {
            if (!containsConfirmTargetItem(chest)) {
                if (confirmTimer.getElapsedTime() >= BUY_CONFIRM_OPEN_RETRY_MS) {
                    state = AutoBuyState.IDLE;
                    purchaseConfirmClicked = false;
                    purchaseOpenRetryCount = 0;
                    pendingCandidate = null;
                    pendingPurchaseSnapshot = null;
                    actionTimer.reset();
                    if (mc.player != null && mc.currentScreen != null) {
                        mc.player.closeHandledScreen();
                        openAuctionTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
                    }
                    setDebugState("Покупка", "В confirm-окне нет нужного лота, возвращаюсь в /ah");
                } else {
                    setDebugState("Покупка", "Вижу confirm-окно, но жду нужный предмет");
                }
                return;
            }

            mc.interactionManager.clickSlot(chest.syncId, chest.getSlot(0).id, 0, SlotActionType.PICKUP, mc.player);
            purchaseConfirmClicked = true;
            purchaseOpenRetryCount = 0;
            actionTimer.reset();
            refreshTimer.reset();
            nextRefreshDelay = nextRefreshDelay();
            setDebugState("Покупка", "Открылось подтверждение, жму slot 0");
            return;
        }

        if (isAuctionScreen(title, chest)) {
            long elapsed = confirmTimer.getElapsedTime();
            if (false) {
                state = AutoBuyState.IDLE;
                purchaseConfirmClicked = false;
                purchaseOpenRetryCount = 0;
                pendingCandidate = null;
                pendingPurchaseSnapshot = null;
                actionTimer.reset();
                setDebugState("Покупка", "Лот в слоте изменился, отменяю подтверждение");
                return;
            }
            if (pendingCandidate == null) {
                state = AutoBuyState.IDLE;
                purchaseConfirmClicked = false;
                purchaseOpenRetryCount = 0;
                setDebugState("Покупка", "Потерял цель покупки, возвращаюсь к поиску");
                return;
            }

            if (false && purchaseOpenRetryCount < BUY_CONFIRM_OPEN_RETRY_COUNT
                    && elapsed >= BUY_CONFIRM_OPEN_RETRY_MS * (purchaseOpenRetryCount + 1L)
                    && actionTimer.finished(BUY_CONFIRM_RETRY_DELAY_MS)) {
                SlotActionType retryAction = purchaseOpenRetryCount == 0 ? SlotActionType.PICKUP : SlotActionType.QUICK_MOVE;
                mc.interactionManager.clickSlot(chest.syncId, pendingCandidate.slot().id, 0, retryAction, mc.player);
                purchaseOpenRetryCount++;
                actionTimer.reset();
                setDebugState("Покупка", "Подтверждение не открылось, повторно жму лот");
                return;
            }

            if (elapsed >= BUY_CONFIRM_RETURN_TO_SCAN_MS) {
                state = AutoBuyState.IDLE;
                purchaseConfirmClicked = false;
                purchaseOpenRetryCount = 0;
                pendingCandidate = null;
                pendingPurchaseSnapshot = null;
                actionTimer.reset();
                setDebugState("Покупка", "Окно подтверждения не появилось, возвращаюсь к сканированию");
                return;
            }

            setDebugState("Покупка", "Лот нажат, жду переход в подтверждение");
            return;
        }

        if (!chest.slots.isEmpty() && looksLikeConfirmationWindow(title, chest)) {
            mc.interactionManager.clickSlot(chest.syncId, chest.getSlot(0).id, 0, SlotActionType.PICKUP, mc.player);
            purchaseConfirmClicked = true;
            purchaseOpenRetryCount = 0;
            actionTimer.reset();
            refreshTimer.reset();
            nextRefreshDelay = nextRefreshDelay();
            setDebugState("Покупка", "Окно подтверждения открыто, жму первый слот");
            return;
        }

        Slot confirmSlot = findConfirmSlot(title, chest);
        if (confirmSlot == null) {
            setDebugState("Покупка", "Не вижу кнопку подтверждения");
            return;
        }

        mc.interactionManager.clickSlot(chest.syncId, confirmSlot.id, 0, SlotActionType.PICKUP, mc.player);
        purchaseConfirmClicked = true;
        purchaseOpenRetryCount = 0;
        actionTimer.reset();
        refreshTimer.reset();
        nextRefreshDelay = nextRefreshDelay();
        setDebugState("Покупка", "Подтверждаю покупку");
    }

    private void handleRefresh(GenericContainerScreenHandler chest) {
        if (!refreshTimer.finished(nextRefreshDelay) || !actionTimer.finished(ACTION_DELAY_MS)) {
            return;
        }

        if (chest.slots.size() <= REFRESH_SLOT) {
            return;
        }

        lastAuctionFingerprint = computeAuctionFingerprint(chest);
        mc.interactionManager.clickSlot(chest.syncId, REFRESH_SLOT, 0, SlotActionType.PICKUP, mc.player);
        state = AutoBuyState.WAITING_REFRESH_UPDATE;
        responseTimer.reset();
        actionTimer.reset();
        lastAuctionActivityAt = System.currentTimeMillis();
        setDebugState("Аукцион", "Обновляю страницу через slot 49");
    }

    private boolean handleRefreshResponse(GenericContainerScreenHandler chest) {
        long elapsed = responseTimer.getElapsedTime();
        int fingerprint = computeAuctionFingerprint(chest);
        if (fingerprint != lastAuctionFingerprint) {
            staleAuctionFingerprint = 0;
            staleAuctionSince = 0L;
            updateSlowRefreshTrend(elapsed);
            if (shouldSwitchForSlowRefresh(elapsed) && canSwitchAnarchyNow()) {
                setDebugState("Анархия", "Листается слишком медленно, иду на следующую");
                switchToNextAnarchy();
                return true;
            }

            state = AutoBuyState.IDLE;
            refreshTimer.reset();
            nextRefreshDelay = nextRefreshDelay();
            actionTimer.reset();
            lastAuctionFingerprint = fingerprint;
            lastAuctionActivityAt = System.currentTimeMillis();
            setDebugState("Аукцион", "Страница обновилась за " + elapsed + "мс");
            return false;
        }

        long completeTimeout = Math.max(REFRESH_RESPONSE_TIMEOUT_MS, slowRefreshMs.getValue().longValue() + 40L);
        if (elapsed >= completeTimeout) {
            long now = System.currentTimeMillis();
            if (staleAuctionFingerprint != fingerprint) {
                staleAuctionFingerprint = fingerprint;
                staleAuctionSince = now;
            } else if (staleAuctionSince > 0L && now - staleAuctionSince >= STALE_AUCTION_RECOVERY_MS) {
                setDebugState("Восстановление", "Аукцион завис, переоткрываю /ah");
                recoverAuctionAfterStalledRefresh();
                staleAuctionFingerprint = 0;
                staleAuctionSince = 0L;
                return true;
            }

            updateSlowRefreshTrend(elapsed);
            if (shouldSwitchForSlowRefresh(elapsed) && canSwitchAnarchyNow()) {
                setDebugState("Анархия", "Серия медленных обновлений, меняю сервер");
                switchToNextAnarchy();
                return true;
            }

            state = AutoBuyState.IDLE;
            refreshTimer.reset();
            nextRefreshDelay = nextRefreshDelay();
            actionTimer.reset();
            lastAuctionActivityAt = System.currentTimeMillis();
            setDebugState("Аукцион", "Жду новое обновление, текущее заняло " + elapsed + "мс");
            return false;
        }

        setDebugState("Аукцион", "Жду ответ страницы: " + elapsed + "мс");
        return true;
    }

    private BuyCandidate findBestCandidate(GenericContainerScreenHandler chest) {
        return chest.slots.stream()
                .filter(slot -> isAuctionItemSlot(slot.id))
                .filter(slot -> slot.hasStack() && !slot.getStack().isEmpty())
                .map(this::toCandidate)
                .filter(candidate -> candidate != null)
                .min(Comparator
                        .comparingInt(BuyCandidate::unitPrice)
                        .thenComparingInt(BuyCandidate::totalPrice)
                        .thenComparingInt(candidate -> auctionSlotPriority(candidate.slot().id))
                        .thenComparingInt(candidate -> -candidate.count()))
                .orElse(null);
    }

    private BuyCandidate toCandidate(Slot slot) {
        ItemStack stack = slot.getStack();
        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
        int totalPrice = getAuctionPrice(stack, tooltip);
        int count = stack.getCount();
        if (totalPrice <= 0 || count <= 0) {
            return null;
        }

        if (currentBalance <= 0L || currentBalance < totalPrice) {
            return null;
        }

        int unitPrice = Math.max(1, (int) Math.floor(totalPrice / (double) count));
        BuyCandidate bestCandidate = null;
        int bestScore = Integer.MIN_VALUE;
        int secondScore = Integer.MIN_VALUE;
        boolean ambiguousTop = false;

        for (Map.Entry<AutoTradeItem, AutoTradeRule> entry : rules.entrySet()) {
            AutoTradeItem item = entry.getKey();
            AutoTradeRule rule = entry.getValue();
            if (item == null || rule == null || !rule.isEnabled() || rule.getBuyUnitPrice() <= 0) {
                continue;
            }

            int matchScore = item.getMatchScore(stack);
            if (matchScore < 0 || !rule.canBuy(totalPrice, count)) {
                continue;
            }

            BuyCandidate candidate = new BuyCandidate(slot, item, rule, totalPrice, unitPrice, count);
            if (matchScore > bestScore) {
                secondScore = bestScore;
                bestScore = matchScore;
                bestCandidate = candidate;
                ambiguousTop = false;
            } else if (matchScore == bestScore) {
                ambiguousTop = true;
            } else {
                secondScore = Math.max(secondScore, matchScore);
            }
        }

        if (bestCandidate == null) {
            return null;
        }

        boolean customStack = hasMeaningfulCustomName(stack) || hasMeaningfulCustomLore(stack);
        if (customStack && bestCandidate.item().isGenericBaseRule()) {
            return null;
        }

        if (ambiguousTop || secondScore >= bestScore - 2) {
            return null;
        }

        return bestCandidate;
    }

    private boolean hasMeaningfulCustomName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String currentName = normalizeAuctionText(stack.getName().getString());
        String vanillaName = normalizeAuctionText(new ItemStack(stack.getItem()).getName().getString());
        return !currentName.isBlank() && !vanillaName.isBlank() && !currentName.equals(vanillaName);
    }

    private boolean hasMeaningfulCustomLore(ItemStack stack) {
        LoreComponent lore = stack == null ? null : stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return false;
        }

        for (Text line : lore.lines()) {
            String normalized = normalizeAuctionText(line.getString());
            if (normalized.isBlank() || isGenericAuctionLoreLine(normalized)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean isGenericAuctionLoreLine(String line) {
        return line.contains("цена")
                || line.contains("price")
                || line.contains("продав")
                || line.contains("seller")
                || line.contains("нажмите")
                || line.contains("click")
                || line.contains("истекает")
                || line.contains("expires")
                || line.contains("количество")
                || line.contains("quantity")
                || line.contains("подтверд")
                || line.contains("confirm")
                || line.contains("/ah")
                || line.contains("storage")
                || line.contains("хранилищ");
    }

    private String normalizeAuctionText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceAll("(?i)\u00a7[0-9A-FK-ORX]", "")
                .replace(' ', ' ')
                .replace('?', '?')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\s{2,}", " ")
                .trim();
    }

    private boolean slotStillMatchesPendingCandidate(GenericContainerScreenHandler chest) {
        if (pendingCandidate == null) {
            return false;
        }

        int slotId = pendingCandidate.slot().id;
        if (slotId < 0 || slotId >= chest.slots.size()) {
            return false;
        }

        Slot currentSlot = chest.getSlot(slotId);
        if (currentSlot == null) {
            return false;
        }

        ItemStack currentStack = currentSlot.getStack();
        if (currentStack == null || currentStack.isEmpty() || !pendingCandidate.item().matches(currentStack)) {
            return false;
        }

        List<Text> tooltip = currentStack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
        int currentPrice = getAuctionPrice(currentStack, tooltip);
        return currentPrice > 0
                && currentPrice == pendingCandidate.totalPrice()
                && currentStack.getCount() == pendingCandidate.count();
    }

    private int computeAuctionFingerprint(GenericContainerScreenHandler chest) {
        int result = 1;
        int upper = chest.slots.size();
        for (int i = 0; i < upper; i++) {
            if (!isAuctionItemSlot(i)) {
                continue;
            }
            Slot slot = chest.getSlot(i);
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) {
                result = 31 * result + i * 17 + 7;
                continue;
            }

            result = 31 * result + System.identityHashCode(stack);
            result = 31 * result + Item.getRawId(stack.getItem());
            result = 31 * result + stack.getCount();
            result = 31 * result + stack.getComponents().hashCode();
        }
        return result;
    }

    private boolean isAuctionItemSlot(int slotId) {
        return slotId >= 0 && slotId <= 44;
    }

    private int auctionSlotPriority(int slotId) {
        if ((slotId >= 0 && slotId <= 2)
                || (slotId >= 9 && slotId <= 11)
                || (slotId >= 18 && slotId <= 20)) {
            return 0;
        }
        return 1;
    }

    private Slot findConfirmSlot(String title, GenericContainerScreenHandler chest) {
        if ((isBuyConfirmationTitle(title) || containsGreenConfirmPanel(chest)) && !chest.slots.isEmpty()) {
            return containsConfirmTargetItem(chest) ? chest.getSlot(0) : null;
        }

        Slot greenConfirmSlot = null;
        for (Slot slot : chest.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (isGreenConfirmStack(stack)) {
                if (greenConfirmSlot == null || slot.id < greenConfirmSlot.id) {
                    greenConfirmSlot = slot;
                }
            }
        }

        if (greenConfirmSlot == null || !containsConfirmTargetItem(chest)) {
            return null;
        }

        return greenConfirmSlot;
    }

    private boolean isBuyConfirmationTitle(String title) {
        return title.contains("подтверждение покупки")
                || title.contains("подтверждение")
                || title.contains("покупка предмета")
                || title.contains("confirm purchase")
                || title.contains("confirm");
    }

    private boolean containsGreenConfirmPanel(GenericContainerScreenHandler chest) {
        for (Slot slot : chest.slots) {
            ItemStack stack = slot.getStack();
            if (stack != null && !stack.isEmpty() && isGreenConfirmStack(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGreenConfirmStack(ItemStack stack) {
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        if ((itemId.contains("green") || itemId.contains("lime")) && itemId.contains("glass")) {
            return true;
        }

        String lowerName = normalizeTitle(stack.getName().getString());
        return lowerName.contains("подтверд")
                || lowerName.contains("купить")
                || lowerName.contains("confirm");
    }

    private boolean looksLikeConfirmationWindow(String title, GenericContainerScreenHandler chest) {
        if (isBuyConfirmationTitle(title) || containsGreenConfirmPanel(chest)) {
            return true;
        }

        if (pendingCandidate == null) {
            return false;
        }

        return containsConfirmTargetItem(chest);
    }

    private boolean containsConfirmTargetItem(GenericContainerScreenHandler chest) {
        if (pendingCandidate == null) {
            return false;
        }

        for (Slot slot : chest.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty() || isGreenConfirmStack(stack)) {
                continue;
            }

            if (matchesPendingCandidate(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPendingCandidate(ItemStack stack) {
        if (pendingCandidate == null) {
            return false;
        }

        AutoTradeItem expectedItem = pendingCandidate.item();
        AutoTradeRule expectedRule = pendingCandidate.rule();
        if (expectedItem == null || expectedRule == null || !expectedRule.isEnabled() || expectedRule.getBuyUnitPrice() <= 0) {
            return false;
        }

        if (expectedItem.getMatchScore(stack) < 0) {
            return false;
        }

        if (pendingPurchaseSnapshot == null) {
            return true;
        }

        ItemStack expectedStack = pendingPurchaseSnapshot.stack();
        if (expectedStack != null && !expectedStack.isEmpty() && stack.getItem() != expectedStack.getItem()) {
            return false;
        }

        if (stack.getCount() != pendingPurchaseSnapshot.count()) {
            return false;
        }

        String expectedName = normalizeAuctionText(pendingPurchaseSnapshot.itemName());
        String actualName = normalizeAuctionText(stack.getName().getString());
        if (!expectedName.isBlank()
                && !actualName.equals(expectedName)
                && !actualName.contains(expectedName)
                && !expectedName.contains(actualName)) {
            return false;
        }

        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
        int totalPrice = getAuctionPrice(stack, tooltip);
        return totalPrice <= 0 || totalPrice == pendingPurchaseSnapshot.totalPrice();
    }

    private boolean matchesAnyEnabledRule(ItemStack stack) {
        for (Map.Entry<AutoTradeItem, AutoTradeRule> entry : rules.entrySet()) {
            AutoTradeItem item = entry.getKey();
            AutoTradeRule rule = entry.getValue();
            if (item == null || rule == null || !rule.isEnabled()) {
                continue;
            }

            if (item.matches(stack)) {
                return true;
            }
        }

        return false;
    }

    private void switchToNextAnarchy() {
        currentAnarchyIndex = (currentAnarchyIndex + 1) % ANARCHY_CYCLE.length;
        int nextAnarchy = ANARCHY_CYCLE[currentAnarchyIndex];

        state = AutoBuyState.IDLE;
        nextRefreshDelay = nextRefreshDelay();
        lastAuctionFingerprint = 0;
        currentBalance = -1L;
        currentAnarchyNumber = -1;
        slowRefreshStreak = 0;
        lastSlowRefreshElapsed = 0L;
        purchaseConfirmClicked = false;
        pendingCandidate = null;
        staleAuctionFingerprint = 0;
        staleAuctionSince = 0L;

        refreshTimer.reset();
        responseTimer.reset();
        actionTimer.reset();

        if (mc.player != null) {
            mc.player.closeHandledScreen();
        }

        pendingAnarchyTarget = nextAnarchy;
        pendingAnarchySentAt = System.currentTimeMillis();
        pendingAnarchyAttempts++;

        sendCommand(formatAnarchyCommand(nextAnarchy));
        openAuctionTimer.reset();
        currentAnarchySince = System.currentTimeMillis();
        setDebugState("Анархия", "Перехожу на " + formatAnarchyCommand(nextAnarchy));
    }

    private String formatAnarchyCommand(int index) {
        String template = sanitizeCommand(nextAnarchyCommand.getValue());
        if (template.contains("%d")) {
            return String.format(Locale.ROOT, template, index);
        }
        return template + index;
    }

    private long nextRefreshDelay() {
        return Math.max(1L, refreshDelaySetting.getValue().longValue());
    }

    private void sendCommand(String rawCommand) {
        if (mc.player == null || mc.player.networkHandler == null) {
            return;
        }

        String command = sanitizeCommand(rawCommand);
        if (!command.isBlank()) {
            mc.player.networkHandler.sendChatCommand(command);
        }
    }

    private String sanitizeCommand(String value) {
        return value == null ? "" : value.trim().replaceFirst("^/", "");
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isAuctionScreen(String title, GenericContainerScreenHandler handler) {
        if (isBuyConfirmationTitle(title) || containsGreenConfirmPanel(handler)) {
            return false;
        }
        return handler.slots.size() > REFRESH_SLOT && AuctionHelperModule.isAuctionTitleLike(title);
    }

    private int countFreeInventorySlots() {
        if (mc.player == null) {
            return 0;
        }

        int freeSlots = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack == null || stack.isEmpty()) {
                freeSlots++;
            }
        }
        return freeSlots;
    }

    private boolean isAuctionTitle(String title) {
        return title.contains("аукцион")
                || title.contains("auction")
                || title.contains("поиск")
                || title.contains("search")
                || title.contains("маркет")
                || title.contains("market")
                || title.contains("[\u2603] \u0430\u0443\u043a\u0446\u0438\u043e\u043d\u044b")
                || title.contains("\u2603 \u043f\u043e")
                || title.contains("\u2603 \u043f:")
                || title.contains("0a2z1/")
                || title.matches("^[0-9a-z]{5}/.*$");
    }

    private boolean isPurchaseFeedback(String message) {
        return message.contains("\u0443\u0441\u043f\u0435\u0448\u043d\u043e \u043a\u0443\u043f")
                || message.contains("\u0432\u044b \u043a\u0443\u043f\u0438\u043b\u0438")
                || message.contains("\u043a\u0443\u043f\u043b\u0435\u043d")
                || message.contains("\u044d\u0442\u043e\u0442 \u0442\u043e\u0432\u0430\u0440 \u0443\u0436\u0435 \u043a\u0443\u043f\u0438\u043b\u0438")
                || message.contains("\u0442\u043e\u0432\u0430\u0440 \u0443\u0436\u0435 \u043a\u0443\u043f\u0438\u043b\u0438")
                || message.contains("already bought")
                || message.contains("\u043d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442")
                || message.contains("\u043d\u0435\u0434\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u043e")
                || message.contains("not enough");
    }

    private PendingPurchaseSnapshot createPurchaseSnapshot(BuyCandidate candidate) {
        if (candidate == null || candidate.slot() == null) {
            return null;
        }
        ItemStack stack = candidate.slot().getStack();
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        String itemName = copy.isEmpty()
                ? candidate.item().getTitle() + " " + candidate.item().getSubtitle()
                : copy.getName().getString();
        return new PendingPurchaseSnapshot(copy, itemName, Math.max(1, candidate.count()), Math.max(0, candidate.totalPrice()));
    }

    private void addSuccessfulHistory() {
        if (pendingPurchaseSnapshot == null) {
            return;
        }
        historyManager.addSuccess(
                pendingPurchaseSnapshot.stack(),
                pendingPurchaseSnapshot.itemName(),
                pendingPurchaseSnapshot.count(),
                pendingPurchaseSnapshot.totalPrice()
        );
    }

    private void addFailedHistory(String message) {
        if (pendingPurchaseSnapshot == null) {
            return;
        }
        historyManager.addFailure(
                pendingPurchaseSnapshot.stack(),
                pendingPurchaseSnapshot.itemName(),
                pendingPurchaseSnapshot.count(),
                pendingPurchaseSnapshot.totalPrice(),
                abbreviate(message, 46)
        );
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

    private boolean canSwitchAnarchyNow() {
        return currentAnarchySince > 0L && System.currentTimeMillis() - currentAnarchySince >= ANARCHY_SWITCH_DELAY_MS;
    }

    private void updateSlowRefreshTrend(long elapsed) {
        if (elapsed < REFRESH_SLOW_THRESHOLD_MS) {
            slowRefreshStreak = 0;
            lastSlowRefreshElapsed = 0L;
            return;
        }

        if (lastSlowRefreshElapsed <= 0L || elapsed >= lastSlowRefreshElapsed + 4L) {
            slowRefreshStreak++;
        } else {
            slowRefreshStreak = 1;
        }
        lastSlowRefreshElapsed = elapsed;
    }

    private boolean shouldSwitchForSlowRefresh(long elapsed) {
        return elapsed >= REFRESH_SLOW_THRESHOLD_MS && slowRefreshStreak >= MIN_SLOW_REFRESH_STREAK;
    }

    private void recoverAuctionAfterStalledRefresh() {
        state = AutoBuyState.IDLE;
        purchaseConfirmClicked = false;
        refreshTimer.reset();
        nextRefreshDelay = nextRefreshDelay();
        actionTimer.reset();
        lastAuctionFingerprint = 0;
        staleAuctionFingerprint = 0;
        staleAuctionSince = 0L;

        if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
            openAuctionTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
        }
    }

    private void handlePendingAnarchySwitch() {
        if (!automationEnabled || pendingAnarchyTarget == -1 || pendingAnarchySentAt <= 0L) {
            return;
        }

        if (currentAnarchyNumber == pendingAnarchyTarget) {
            pendingAnarchyTarget = -1;
            pendingAnarchySentAt = 0L;
            pendingAnarchyAttempts = 0;
            return;
        }

        if (System.currentTimeMillis() - pendingAnarchySentAt < ANARCHY_RETRY_DELAY_MS) {
            return;
        }

        if (pendingAnarchyAttempts >= ANARCHY_CYCLE.length) {
            pendingAnarchyTarget = -1;
            pendingAnarchySentAt = 0L;
            pendingAnarchyAttempts = 0;
            setDebugState("Анархия", "Не удалось сменить анархию, продолжаю работу");
            return;
        }

        setDebugState("Анархия", "Сервер не сменился, пробую следующий");
        switchToNextAnarchy();
    }

    private void handleAnarchyFeedback(String message) {
        if (pendingAnarchyTarget == -1) {
            return;
        }

        if (message.contains("\u0437\u0430\u043f\u043e\u043b\u043d\u0435\u043d")
                || message.contains("server full")
                || message.contains("\u043d\u0435\u0442 \u043c\u0435\u0441\u0442\u0430")) {
            setDebugState("Анархия", "Сервер заполнен, иду дальше");
            switchToNextAnarchy();
        }
    }

    private void updateScoreboardState() {
        if (mc.world == null) {
            currentBalance = -1L;
            balanceDebugSource = "WORLD";
            sidebarDebugLines = List.of();
            return;
        }

        try {
            Scoreboard scoreboard = mc.world.getScoreboard();
            ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (sidebar == null) {
                currentBalance = -1L;
                return;
            }

            String title = cleanScoreboardText(sidebar.getDisplayName().getString());
            int parsedAnarchy = parseAnarchyNumber(title);
            if (parsedAnarchy != -1 && parsedAnarchy != currentAnarchyNumber) {
                currentAnarchyNumber = parsedAnarchy;
                currentAnarchyIndex = findAnarchyIndex(parsedAnarchy);
                currentAnarchyInitialized = true;
                currentAnarchySince = System.currentTimeMillis();
                if (pendingAnarchyTarget == parsedAnarchy) {
                    pendingAnarchyTarget = -1;
                    pendingAnarchySentAt = 0L;
                    pendingAnarchyAttempts = 0;
                }
            }

            List<ScoreboardEntry> visibleEntries = scoreboard.getScoreboardEntries(sidebar).stream()
                    .filter(entry -> entry != null && !entry.hidden())
                    .toList();
            List<ScoreboardEntry> reversedEntries = visibleEntries.reversed();

            sidebarDebugLines = buildSidebarDebugLines(scoreboard, visibleEntries);
            balanceDebugLine = formatSidebarLine(scoreboard, visibleEntries, BALANCE_SCOREBOARD_LINE_INDEX);
            balanceDebugAltLine = formatSidebarLine(scoreboard, reversedEntries, BALANCE_SCOREBOARD_LINE_INDEX);
            balanceDebugPrimaryValue = parseBalanceFromScoreboardLine(scoreboard, visibleEntries, BALANCE_SCOREBOARD_LINE_INDEX);
            balanceDebugAltValue = parseBalanceFromScoreboardLine(scoreboard, reversedEntries, BALANCE_SCOREBOARD_LINE_INDEX);

            BalanceMatch labeledMatch = findBalanceByLabel(scoreboard, visibleEntries);
            balanceDebugLabelLine = labeledMatch.line();
            balanceDebugLabelValue = labeledMatch.value();
            long parsedBalance = labeledMatch.value();
            balanceDebugSource = "LABEL";

            if (parsedBalance <= 0L && balanceDebugPrimaryValue > 0L) {
                parsedBalance = balanceDebugPrimaryValue;
                balanceDebugSource = "L5";
            }

            if (parsedBalance <= 0L && balanceDebugAltValue > 0L) {
                parsedBalance = balanceDebugAltValue;
                balanceDebugSource = "L5R";
            }

            if (parsedBalance > 0L) {
                currentBalance = parsedBalance;
                lastPositiveBalance = parsedBalance;
                lastPositiveBalanceAt = System.currentTimeMillis();
            } else if (lastPositiveBalance > 0L && System.currentTimeMillis() - lastPositiveBalanceAt <= 8_000L) {
                currentBalance = lastPositiveBalance;
                balanceDebugSource = "HOLD";
            } else {
                currentBalance = -1L;
            }
        } catch (Exception ignored) {
            if (lastPositiveBalance > 0L && System.currentTimeMillis() - lastPositiveBalanceAt <= 8_000L) {
                currentBalance = lastPositiveBalance;
                balanceDebugSource = "HOLD";
            } else {
                currentBalance = -1L;
                balanceDebugSource = "ERR";
            }
        }
    }

    private long parseBalanceFromScoreboardLine(Scoreboard scoreboard, List<ScoreboardEntry> visibleEntries, int lineIndex) {
        if (lineIndex < 0 || lineIndex >= visibleEntries.size()) {
            return -1L;
        }

        String rawLine = resolveSidebarLineText(scoreboard, visibleEntries.get(lineIndex));
        if (!looksLikeBalanceLine(rawLine)) {
            return -1L;
        }

        long parsed = parseBalanceAmount(rawLine);
        return parsed > 0L ? parsed : -1L;
    }

    private BalanceMatch findBalanceByLabel(Scoreboard scoreboard, List<ScoreboardEntry> visibleEntries) {
        for (ScoreboardEntry entry : visibleEntries) {
            String rawLine = resolveSidebarLineText(scoreboard, entry);
            String cleanLine = cleanScoreboardText(rawLine);
            if (!containsBalanceLabel(cleanLine)) {
                continue;
            }

            long value = parseBalanceAmount(rawLine);
            if (value > 0L) {
                return new BalanceMatch(value, abbreviate(rawLine, 42));
            }
        }

        return new BalanceMatch(-1L, "-");
    }

    private boolean looksLikeBalanceLine(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return false;
        }

        String lower = rawLine.toLowerCase(Locale.ROOT);
        return containsBalanceLabel(lower)
                || rawLine.contains(",")
                || lower.matches(".*\\d+r\\s*$");
    }

    private long parseBalanceAmount(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return -1L;
        }

        String numeric = rawLine.replaceAll("[^0-9,]", "");
        if (numeric.isEmpty()) {
            return -1L;
        }

        try {
            String[] groups = numeric.split(",");
            if (groups.length == 0) {
                return -1L;
            }

            if (groups.length == 1) {
                String digits = groups[0];
                if (digits.isEmpty()) {
                    return -1L;
                }

                boolean maybeCents = rawLine.toLowerCase(Locale.ROOT).contains("r") && digits.length() > 4;
                long rawValue = Long.parseLong(digits);
                return maybeCents ? rawValue / 100L : rawValue;
            }

            StringBuilder coins = new StringBuilder();
            for (int i = 0; i < groups.length; i++) {
                String group = groups[i];
                if (group.isEmpty()) {
                    continue;
                }

                boolean last = i == groups.length - 1;
                if (last && group.length() == 5) {
                    coins.append(group, 0, 3);
                } else {
                    coins.append(group);
                }
            }

            if (coins.isEmpty()) {
                return -1L;
            }

            return Long.parseLong(coins.toString());
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private List<String> buildSidebarDebugLines(Scoreboard scoreboard, List<ScoreboardEntry> visibleEntries) {
        List<String> lines = new ArrayList<>();
        int limit = Math.min(7, visibleEntries.size());
        for (int i = 0; i < limit; i++) {
            String rawLine = resolveSidebarLineText(scoreboard, visibleEntries.get(i));
            lines.add((i + 1) + ". " + abbreviate(rawLine, 26));
        }
        return lines;
    }

    private String formatSidebarLine(Scoreboard scoreboard, List<ScoreboardEntry> visibleEntries, int lineIndex) {
        if (lineIndex < 0 || lineIndex >= visibleEntries.size()) {
            return "-";
        }
        return abbreviate(resolveSidebarLineText(scoreboard, visibleEntries.get(lineIndex)), 42);
    }

    private int findAnarchyIndex(int anarchyNumber) {
        for (int i = 0; i < ANARCHY_CYCLE.length; i++) {
            if (ANARCHY_CYCLE[i] == anarchyNumber) {
                return i;
            }
        }
        return currentAnarchyIndex;
    }

    private int parseAnarchyNumber(String title) {
        Matcher matcher = ANARCHY_PATTERN.matcher(title);
        if (!matcher.find()) {
            return -1;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private boolean containsBalanceLabel(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("монет")
                || lower.contains("монеты")
                || lower.contains("balance")
                || lower.contains("money");
    }

    private long parseDigits(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return -1L;
        }

        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private String abbreviate(String value, int maxLength) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            return "-";
        }
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String formatCompactBalance(long value) {
        if (value <= 0L) {
            return "n/a";
        }
        if (value >= 1_000_000L) {
            double compact = value / 1_000_000.0D;
            return compact >= 10.0D ? Math.round(compact) + "kk" : String.format(Locale.US, "%.1fkk", compact);
        }
        if (value >= 1_000L) {
            double compact = value / 1_000.0D;
            return compact >= 10.0D ? Math.round(compact) + "k" : String.format(Locale.US, "%.1fk", compact);
        }
        return Long.toString(value);
    }

    private String cleanScoreboardText(String value) {
        return value == null ? "" : value.replaceAll("§[0-9a-fk-or]", "").trim().toLowerCase(Locale.ROOT);
    }

    private String resolveSidebarLineText(Scoreboard scoreboard, ScoreboardEntry entry) {
        if (entry == null) {
            return "";
        }

        String displayLine = entry.display() != null ? entry.display().getString() : "";
        if (!displayLine.isBlank()) {
            return displayLine;
        }

        String ownerLine = resolveScoreOwnerLine(scoreboard, entry);
        if (!ownerLine.isBlank()) {
            return ownerLine;
        }

        return entry.name() != null ? entry.name().getString() : "";
    }

    private String resolveScoreOwnerLine(Scoreboard scoreboard, ScoreboardEntry entry) {
        String owner = entry.owner();
        if (owner == null || owner.isBlank()) {
            return "";
        }

        Team team = scoreboard.getScoreHolderTeam(owner);
        if (team == null) {
            return owner;
        }

        try {
            return Team.decorateName(team, Text.literal(owner)).getString();
        } catch (Exception ignored) {
            return owner;
        }
    }

    private void saveRulesToSetting() {
        JsonObject root = new JsonObject();
        for (Map.Entry<AutoTradeItem, AutoTradeRule> entry : rules.entrySet()) {
            AutoTradeRule rule = entry.getValue();
            JsonObject json = new JsonObject();
            json.addProperty("enabled", rule.isEnabled());
            json.addProperty("buy", rule.getBuyUnitPrice());
            json.addProperty("sell", rule.getSellPrice());
            json.addProperty("min", rule.getMinStackCount());
            root.add(entry.getKey().getId(), json);
        }
        rulesData.setValue(root.toString());
    }

    private void loadRulesFromSetting() {
        try {
            String raw = rulesData.getValue();
            if (raw == null || raw.isBlank()) {
                return;
            }

            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            for (AutoTradeItem item : AutoTradeItem.values()) {
                if (!root.has(item.getId())) {
                    continue;
                }

                JsonObject json = root.getAsJsonObject(item.getId());
                AutoTradeRule rule = rules.computeIfAbsent(item, key -> new AutoTradeRule());
                if (json.has("enabled")) {
                    rule.setEnabled(json.get("enabled").getAsBoolean());
                }
                if (json.has("buy")) {
                    rule.setBuyUnitPrice(json.get("buy").getAsInt());
                }
                if (json.has("sell")) {
                    rule.setSellPrice(json.get("sell").getAsInt());
                }
                if (json.has("min")) {
                    rule.setMinStackCount(json.get("min").getAsInt());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void renderAuctionToggle(DrawContext context, int mouseX, int mouseY, int containerX, int containerY, int backgroundWidth) {
        if (!shouldShowAuctionToggle()) {
            return;
        }

        String label = automationEnabled ? "Автобай: ВКЛ" : "Автобай: ВЫКЛ";
        float width = Math.max(102f, Fonts.PS_BOLD.getWidth(label, 6.1f) + 20f);
        float height = 17f;
        float round = 5.5f;
        float x = containerX + backgroundWidth / 2f - width / 2f;
        float y = Math.max(6f, containerY - height - 5f);
        auctionToggleX = x;
        auctionToggleY = y;
        auctionToggleWidth = width;
        auctionToggleHeight = height;

        boolean hovered = MouseUtil.isHovered(mouseX, mouseY, x, y, width, height, round);
        Color fill = automationEnabled
                ? ColorUtil.interpolate(UIColors.cardSecondary(222), UIColors.primary(118), hovered ? 0.32f : 0.24f)
                : ColorUtil.interpolate(UIColors.panel(205), UIColors.card(214), hovered ? 0.30f : 0.18f);
        Color stroke = automationEnabled
                ? UIColors.primary(hovered ? 182 : 148)
                : UIColors.stroke(hovered ? 140 : 108);

        RenderUtil.BLUR_RECT.draw(context.getMatrices(), x, y, width, height, new Vector4f(round), fill);
        RenderUtil.BLUR_RECT.draw(context.getMatrices(), x + 1f, y + 1f, width - 2f, height - 2f, new Vector4f(round - 1f),
                UIColors.overlay(hovered ? 92 : 78));
        RenderUtil.RECT.draw(context.getMatrices(), x, y, width, height, round, stroke);
        Fonts.PS_BOLD.drawCenteredText(context.getMatrices(), label, x + width / 2f, y + 5.0f, 6.1f, UIColors.textColor(255));
    }

    public boolean handleAuctionToggleClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !shouldShowAuctionToggle()) {
            return false;
        }
        if (!MouseUtil.isHovered(mouseX, mouseY, auctionToggleX, auctionToggleY, auctionToggleWidth, auctionToggleHeight, 5f)) {
            return false;
        }

        setAutomationEnabled(!automationEnabled);
        return true;
    }

    public void renderAuctionBalanceDebug(DrawContext context, int containerX, int containerY, int backgroundWidth, int backgroundHeight) {
        if (!shouldShowAuctionToggle()) {
            return;
        }

        float width = 244f;
        float height = 24f;
        float round = 4f;
        float x = containerX + backgroundWidth / 2f - width / 2f;
        float y = containerY + backgroundHeight + 6f;

        RenderUtil.BLUR_RECT.draw(context.getMatrices(), x, y, width, height, new Vector4f(round), UIColors.panel(198));
        RenderUtil.BLUR_RECT.draw(context.getMatrices(), x + 1f, y + 1f, width - 2f, height - 2f, new Vector4f(round - 1f), UIColors.overlay(72));
        RenderUtil.RECT.draw(context.getMatrices(), x, y, width, height, round, UIColors.stroke(104));

        String state = automationEnabled ? "ВКЛ" : "ВЫКЛ";
        String balance = currentBalance > 0L ? formatCompactBalance(currentBalance) : "n/a";
        String anarchy = currentAnarchyNumber > 0 ? " | an" + currentAnarchyNumber : "";
        String lineOne = "Автобай " + state + " | Баланс: " + balance + anarchy;
        String lineTwo = abbreviate(actionDebugState + ": " + actionDebugText, 74);
        Fonts.PS_MEDIUM.drawCenteredText(context.getMatrices(), lineOne, x + width / 2f, y + 3.2f, 5.0f, UIColors.textColor(255));
        Fonts.PS_MEDIUM.drawCenteredText(context.getMatrices(), lineTwo, x + width / 2f, y + 12.6f, 4.8f, UIColors.mutedText(240));
    }

    public void setAutomationEnabled(boolean enabled) {
        if (automationEnabled == enabled) {
            return;
        }

        automationEnabled = enabled;
        resetAutomationState();
        if (automationEnabled) {
            long now = System.currentTimeMillis();
            nextAutoSetupAt = now + AUTO_SETUP_INTERVAL_MS;
            openAuctionTimer.setMillis(now - AUCTION_OPEN_INTERVAL_MS);
            if (currentAnarchySince <= 0L) {
                currentAnarchySince = now;
            }
            priceProfileReady = false;
            setDebugState("Автобай", "Запущен, автопарс через 15 минут");
        } else {
            nextAutoSetupAt = 0L;
            priceProfileReady = false;
            setDebugState("Ожидание", "Автоматизация выключена");
        }
    }

    private boolean shouldRunAutoSetup() {
        return automationEnabled
                && nextAutoSetupAt > 0L
                && System.currentTimeMillis() >= nextAutoSetupAt
                && !AutoSetupModule.getInstance().isEnabled();
    }

    private void startAutoSetupCycle() {
        resetAutomationState();
        nextAutoSetupAt = System.currentTimeMillis() + AUTO_SETUP_INTERVAL_MS;
        priceProfileReady = false;
        if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }
        setDebugState("Автопарс", "Закрываю окно и запускаю поиск цен");
        AutoSetupModule.getInstance().setEnabled(true);
    }

    private boolean shouldShowAuctionToggle() {
        if (!isEnabled() || mc == null || mc.currentScreen == null || mc.player == null) {
            return false;
        }
        if (mc.currentScreen instanceof AutoBuyConfigScreen) {
            return false;
        }
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest)) {
            return false;
        }
        return isAuctionScreen(normalizeTitle(mc.currentScreen.getTitle().getString()), chest);
    }

    private void resetAutomationState() {
        state = AutoBuyState.IDLE;
        lastAuctionFingerprint = 0;
        nextRefreshDelay = nextRefreshDelay();
        actionTimer.reset();
        refreshTimer.reset();
        responseTimer.reset();
        confirmTimer.reset();
        purchaseConfirmClicked = false;
        purchaseOpenRetryCount = 0;
        pendingCandidate = null;
        pendingPurchaseSnapshot = null;
        staleAuctionFingerprint = 0;
        staleAuctionSince = 0L;
    }

    private void setDebugState(String state, String detail) {
        actionDebugState = state == null || state.isBlank() ? "Ожидание" : state;
        actionDebugText = detail == null || detail.isBlank() ? "-" : detail;
    }

    private String formatCandidate(BuyCandidate candidate) {
        if (candidate == null) {
            return "-";
        }
        return candidate.item().getTitle() + " " + candidate.item().getSubtitle()
                + " x" + candidate.count()
                + " за " + formatCompactBalance(candidate.totalPrice());
    }
}