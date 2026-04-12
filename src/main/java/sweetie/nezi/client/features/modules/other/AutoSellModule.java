package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.api.utils.player.InventoryUtil;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeItem;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeRule;

import java.util.Locale;

@ModuleRegister(name = "Auto Sell", category = Category.OTHER)
public class AutoSellModule extends Module {
    @Getter
    private static final AutoSellModule instance = new AutoSellModule();

    private enum SellState {
        IDLE,
        PREPARING_COMMAND,
        WAITING_COMMAND_RESULT,
        WAITING_FOR_FREE_SLOT,
        OPENING_AUCTION,
        OPENING_STORAGE,
        LOOTING_STORAGE,
        CLOSING_STORAGE
    }

    private record SellCandidate(int inventorySlot, int handlerSlot, int hotbarSlot, AutoTradeItem item, AutoTradeRule rule, int count) { }

    private static final int MAX_ACTIVE_LISTINGS = 5;
    private static final int STORAGE_BUTTON_SLOT = 46;
    private static final int STORAGE_MIN_SLOT = 0;
    private static final int STORAGE_MAX_SLOT = 29;
    private static final long WAIT_FOR_PURCHASE_MESSAGE_MS = 20_000L;
    private static final long SELL_COMMAND_SETTLE_MS = 1_300L;
    private static final long AUCTION_OPEN_INTERVAL_MS = 1_500L;
    private static final long STORAGE_OPEN_DELAY_MS = 50L;
    private static final long STORAGE_LOOT_DELAY_MS = 35L;
    private static final long STORAGE_CLOSE_DELAY_MS = 90L;

    private final BooleanSetting relistStorage = new BooleanSetting("Забирать из хранилища").value(true);
    private final SliderSetting sellDelay = new SliderSetting("Задержка продажи").value(700f).range(250f, 3000f).step(25f);

    private final TimerUtil sellTimer = new TimerUtil();
    private final TimerUtil responseTimer = new TimerUtil();
    private final TimerUtil waitTimer = new TimerUtil();
    private final TimerUtil actionTimer = new TimerUtil();
    private final TimerUtil auctionOpenTimer = new TimerUtil();

    private SellState state = SellState.IDLE;
    private SellCandidate pendingCandidate;
    private long pendingSellPrice;
    private int availableAuctionSlots = MAX_ACTIVE_LISTINGS;
    private int soldMessagesWhileWaiting;
    private int recoveredStorageSlots;
    private boolean emergencySellRequested;
    private int emergencyResumeFreeSlots = 6;

    public AutoSellModule() {
        addSettings(relistStorage, sellDelay);
    }

    @Override
    public void onEnable() {
        resetState();
        emergencySellRequested = false;
        emergencyResumeFreeSlots = 6;
        // Start selling immediately by marking the timer as already elapsed
        sellTimer.setMillis(System.currentTimeMillis() - (long) sellDelay.getValue().floatValue());
    }

    @Override
    public void onDisable() {
        resetState();
        emergencySellRequested = false;
        emergencyResumeFreeSlots = 6;
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> handleUpdate()));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::handlePacket));
        addEvents(update, packet);
    }

    public void resumeAfterParsing() {
        resetState();
        availableAuctionSlots = MAX_ACTIVE_LISTINGS;
        auctionOpenTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
    }

    public void requestEmergencySell(int resumeFreeSlots) {
        if (!isEnabled()) {
            setEnabled(true, true);
        }

        emergencySellRequested = true;
        emergencyResumeFreeSlots = Math.max(1, resumeFreeSlots);
        if (mc != null && mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }
        sellTimer.setMillis(System.currentTimeMillis() - sellDelay.getValue().longValue());
    }

    public void clearEmergencySell() {
        emergencySellRequested = false;
        emergencyResumeFreeSlots = 6;
    }

    public boolean isEmergencySelling() {
        return emergencySellRequested;
    }

    public boolean isReserveSelling() {
        return !emergencySellRequested && shouldSellForReserve();
    }

    private void resetState() {
        state = SellState.IDLE;
        pendingCandidate = null;
        pendingSellPrice = 0L;
        availableAuctionSlots = MAX_ACTIVE_LISTINGS;
        soldMessagesWhileWaiting = 0;
        recoveredStorageSlots = 0;
        sellTimer.reset();
        responseTimer.reset();
        waitTimer.reset();
        actionTimer.reset();
        auctionOpenTimer.reset();
    }

    private void handleUpdate() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        if (AutoSetupModule.getInstance().isEnabled()) {
            return;
        }

        if (AutoBuyModule.getInstance().isEnabled() && AutoBuyModule.getInstance().isManagingStorageFlow()) {
            return;
        }

        if (emergencySellRequested && countFreeInventorySlots() >= emergencyResumeFreeSlots && state == SellState.IDLE) {
            clearEmergencySell();
            return;
        }

        switch (state) {
            case PREPARING_COMMAND -> handlePreparingCommand();
            case WAITING_COMMAND_RESULT -> handleWaitingCommandResult();
            case WAITING_FOR_FREE_SLOT -> handleWaitingForFreeSlot();
            case OPENING_AUCTION -> handleOpenAuctionForStorage();
            case OPENING_STORAGE -> handleOpenStorageButton();
            case LOOTING_STORAGE -> handleLootStorage();
            case CLOSING_STORAGE -> handleCloseStorageScreen();
            case IDLE -> handleSellingCycle();
        }
    }

    private void handleSellingCycle() {
        if (emergencySellRequested && countFreeInventorySlots() >= emergencyResumeFreeSlots) {
            clearEmergencySell();
            return;
        }

        if (!emergencySellRequested && !shouldSellForReserve()) {
            return;
        }

        // Don't sell if there are fewer than 5 sellable items in inventory
        if (countSellableItems() < 5) {
            return;
        }

        if (availableAuctionSlots <= 0) {
            state = SellState.WAITING_FOR_FREE_SLOT;
            waitTimer.reset();
            return;
        }

        if (!sellTimer.finished(sellDelay.getValue().longValue())) {
            return;
        }

        SellCandidate candidate = findSellCandidate();
        if (candidate == null) {
            return;
        }

        prepareCandidateInHand(candidate);
        pendingCandidate = candidate;
        pendingSellPrice = Math.max(1L, (long) candidate.rule().getSellPrice() * candidate.count());
        actionTimer.reset();
        sellTimer.reset();
        state = SellState.PREPARING_COMMAND;
    }

    private void handlePreparingCommand() {
        if (pendingCandidate == null || pendingSellPrice <= 0L || mc.player == null || mc.player.networkHandler == null) {
            pendingCandidate = null;
            pendingSellPrice = 0L;
            state = SellState.IDLE;
            return;
        }

        if (!isCandidateInMainHand(pendingCandidate)) {
            prepareCandidateInHand(pendingCandidate);
            actionTimer.reset();
            return;
        }

        if (!actionTimer.finished(140L)) {
            return;
        }

        mc.player.networkHandler.sendChatCommand("ah sell " + pendingSellPrice);
        availableAuctionSlots = Math.max(0, availableAuctionSlots - 1);
        responseTimer.reset();
        state = SellState.WAITING_COMMAND_RESULT;
    }

    private void handleWaitingCommandResult() {
        if (responseTimer.finished(SELL_COMMAND_SETTLE_MS)) {
            pendingCandidate = null;
            pendingSellPrice = 0L;
            state = SellState.IDLE;
        }
    }

    private void handleWaitingForFreeSlot() {
        if (soldMessagesWhileWaiting > 0) {
            availableAuctionSlots = Math.min(MAX_ACTIVE_LISTINGS, soldMessagesWhileWaiting);
            soldMessagesWhileWaiting = 0;
            pendingCandidate = null;
            pendingSellPrice = 0L;
            state = SellState.IDLE;
            sellTimer.reset();
            return;
        }

        if (!relistStorage.getValue() || !waitTimer.finished(WAIT_FOR_PURCHASE_MESSAGE_MS)) {
            return;
        }

        recoveredStorageSlots = 0;
        state = SellState.OPENING_AUCTION;
        auctionOpenTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
        actionTimer.reset();
    }

    private void handleOpenAuctionForStorage() {
        if (mc.currentScreen != null && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest) {
            String title = normalize(mc.currentScreen.getTitle().getString());
            if (AuctionHelperModule.isAuctionTitleLike(title) && chest.slots.size() > STORAGE_BUTTON_SLOT) {
                state = SellState.OPENING_STORAGE;
                actionTimer.reset();
                return;
            }
        }

        if (mc.currentScreen != null && mc.player != null) {
            mc.player.closeHandledScreen();
        }

        if (!auctionOpenTimer.finished(AUCTION_OPEN_INTERVAL_MS)) {
            return;
        }

        mc.player.networkHandler.sendChatCommand("ah");
        auctionOpenTimer.reset();
    }

    private void handleOpenStorageButton() {
        if (mc.currentScreen == null || !(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest)) {
            state = SellState.OPENING_AUCTION;
            auctionOpenTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
            return;
        }

        String title = normalize(mc.currentScreen.getTitle().getString());
        if (!AuctionHelperModule.isAuctionTitleLike(title) || chest.slots.size() <= STORAGE_BUTTON_SLOT) {
            state = SellState.OPENING_AUCTION;
            auctionOpenTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
            return;
        }

        if (!actionTimer.finished(STORAGE_OPEN_DELAY_MS)) {
            return;
        }

        mc.interactionManager.clickSlot(chest.syncId, STORAGE_BUTTON_SLOT, 0, SlotActionType.PICKUP, mc.player);
        state = SellState.LOOTING_STORAGE;
        actionTimer.reset();
    }

    private void handleLootStorage() {
        if (mc.currentScreen == null || !(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest)) {
            state = SellState.OPENING_AUCTION;
            auctionOpenTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
            return;
        }

        String title = normalize(mc.currentScreen.getTitle().getString());
        if (AuctionHelperModule.isAuctionTitleLike(title)) {
            if (!actionTimer.finished(STORAGE_OPEN_DELAY_MS + 80L)) {
                return;
            }
            state = SellState.OPENING_STORAGE;
            actionTimer.reset();
            return;
        }

        if (!actionTimer.finished(STORAGE_LOOT_DELAY_MS)) {
            return;
        }

        int lootSlot = findStorageLootSlot(chest);
        if (lootSlot == -1) {
            state = SellState.CLOSING_STORAGE;
            actionTimer.reset();
            return;
        }

        mc.interactionManager.clickSlot(chest.syncId, lootSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
        recoveredStorageSlots++;
        actionTimer.reset();
    }

    private void handleCloseStorageScreen() {
        if (!actionTimer.finished(STORAGE_CLOSE_DELAY_MS)) {
            return;
        }

        if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }

        pendingCandidate = null;
        pendingSellPrice = 0L;
        soldMessagesWhileWaiting = 0;
        if (recoveredStorageSlots > 0) {
            availableAuctionSlots = Math.min(MAX_ACTIVE_LISTINGS, recoveredStorageSlots);
            state = SellState.IDLE;
            sellTimer.reset();
        } else {
            availableAuctionSlots = 0;
            state = SellState.WAITING_FOR_FREE_SLOT;
            waitTimer.reset();
        }

        recoveredStorageSlots = 0;
        auctionOpenTimer.setMillis(System.currentTimeMillis() - AUCTION_OPEN_INTERVAL_MS);
    }

    private void handlePacket(PacketEvent.PacketEventData event) {
        if (!event.isReceive() || !(event.packet() instanceof GameMessageS2CPacket packet)) {
            return;
        }

        String message = normalize(packet.content().getString());
        if (message.isBlank()) {
            return;
        }

        if (isStorageFullMessage(message)) {
            pendingCandidate = null;
            pendingSellPrice = 0L;
            availableAuctionSlots = 0;
            soldMessagesWhileWaiting = 0;
            state = SellState.WAITING_FOR_FREE_SLOT;
            waitTimer.reset();
            return;
        }

        if (isSoldMessage(message)) {
            availableAuctionSlots = Math.min(MAX_ACTIVE_LISTINGS, availableAuctionSlots + 1);
            if (state == SellState.WAITING_FOR_FREE_SLOT) {
                soldMessagesWhileWaiting++;
                waitTimer.reset();
            }
        }
    }

    private boolean shouldSellForReserve() {
        if (emergencySellRequested) {
            return true;
        }

        AutoBuyModule autoBuy = AutoBuyModule.getInstance();
        if (autoBuy == null) {
            return true;
        }

        if (autoBuy.isAutomationEnabled() && !autoBuy.hasParsedPriceProfile()) {
            return false;
        }

        if (!autoBuy.isEnabled() && !autoBuy.isAutomationEnabled()) {
            return true;
        }

        return autoBuy.shouldTriggerAutoSellForBalance();
    }

    private void prepareCandidateInHand(SellCandidate candidate) {
        if (candidate == null) {
            return;
        }

        int selectedSlot = mc.player.getInventory().selectedSlot;
        if (candidate.inventorySlot() < 9) {
            InventoryUtil.swapToSlot(candidate.inventorySlot());
            return;
        }

        int targetHotbarSlot = candidate.hotbarSlot();
        if (targetHotbarSlot == -1) {
            targetHotbarSlot = selectedSlot;
        }

        InventoryUtil.swapSlots(candidate.handlerSlot(), targetHotbarSlot);
        InventoryUtil.swapToSlot(targetHotbarSlot);
    }

    private boolean isCandidateInMainHand(SellCandidate candidate) {
        if (candidate == null || mc.player == null) {
            return false;
        }

        ItemStack mainHand = mc.player.getMainHandStack();
        return mainHand != null
                && !mainHand.isEmpty()
                && candidate.item() != null
                && candidate.item().matches(mainHand);
    }

    private SellCandidate findSellCandidate() {
        int currentHotbar = mc.player.getInventory().selectedSlot;
        for (int inventoryIndex = 0; inventoryIndex < 36; inventoryIndex++) {
            ItemStack stack = mc.player.getInventory().getStack(inventoryIndex);
            if (stack.isEmpty()) {
                continue;
            }

            for (AutoTradeItem item : AutoTradeItem.values()) {
                AutoTradeRule rule = AutoBuyModule.getInstance().getRule(item);
                if (rule == null || !rule.canSell(stack.getCount()) || !item.matches(stack)) {
                    continue;
                }

                int handlerSlot = inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
                int hotbarSlot = inventoryIndex < 9 ? inventoryIndex : InventoryUtil.findBestSlotInHotBar();
                if (hotbarSlot == -1) {
                    hotbarSlot = currentHotbar;
                }

                return new SellCandidate(inventoryIndex, handlerSlot, hotbarSlot, item, rule, stack.getCount());
            }
        }

        return null;
    }

    private int findStorageLootSlot(GenericContainerScreenHandler chest) {
        int upper = Math.min(STORAGE_MAX_SLOT + 1, chest.slots.size());
        for (int slotId = STORAGE_MIN_SLOT; slotId < upper; slotId++) {
            Slot slot = chest.getSlot(slotId);
            if (slot != null && slot.hasStack() && !slot.getStack().isEmpty()) {
                return slot.id;
            }
        }
        return -1;
    }

    private boolean isStorageFullMessage(String message) {
        return message.contains("не удалось выставить")
                && (message.contains("освободите хранилище")
                || message.contains("/ah rent")
                || message.contains("больше слотов"));
    }

    private boolean isSoldMessage(String message) {
        return message.contains("у вас купили")
                && message.contains("на /ah");
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private int countSellableItems() {
        if (mc.player == null) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            for (AutoTradeItem item : AutoTradeItem.values()) {
                AutoTradeRule rule = AutoBuyModule.getInstance().getRule(item);
                if (rule != null && rule.canSell(stack.getCount()) && item.matches(stack)) {
                    count++;
                    break;
                }
            }
        }
        return count;
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
}
