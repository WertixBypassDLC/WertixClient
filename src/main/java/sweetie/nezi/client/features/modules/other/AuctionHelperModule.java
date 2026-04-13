package sweetie.nezi.client.features.modules.other;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.MultiModeSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.auction.ParseModeChoice;
import sweetie.nezi.api.utils.auction.PriceParser;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.client.features.modules.other.autobuy.AutoTradeHistoryRecord;
import sweetie.nezi.client.services.RenderService;
import sweetie.nezi.client.ui.auction.AuctionSettingsPanel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

@ModuleRegister(name = "Auction Helper", category = Category.OTHER)
public class AuctionHelperModule extends Module {
    private static final String SHARPNESS = "\u041e\u0441\u0442\u0440\u043e\u0442\u0430";
    private static final String UNBREAKING = "\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c";
    private static final String LOOTING = "\u0414\u043e\u0431\u044b\u0447\u0430";
    private static final String NO_KNOCKBACK = "\u0411\u0435\u0437 \u043e\u0442\u0434\u0430\u0447\u0438";
    private static final String KNOCKBACK = "\u041e\u0442\u0434\u0430\u0447\u0430";
    private static final String FIRE_ASPECT = "\u0417\u0430\u0433\u043e\u0432\u043e\u0440 \u043e\u0433\u043d\u044f";
    private static final String PROTECTION = "\u0417\u0430\u0449\u0438\u0442\u0430";
    private static final String NO_THORNS = "\u0411\u0435\u0437 \u0448\u0438\u043f\u043e\u0432";
    private static final String THORNS = "\u0428\u0438\u043f\u044b";
    private static final String MENDING = "\u041f\u043e\u0447\u0438\u043d\u043a\u0430";
    private static final String DEPTH_STRIDER = "\u041f\u043e\u0434\u0432\u043e\u0434\u043d\u0430\u044f \u0445\u043e\u0434\u044c\u0431\u0430";
    private static final String VAMPIRISM = "\u0412\u0430\u043c\u043f\u0438\u0440\u0438\u0437\u043c";
    private static final String OXIDATION = "\u041e\u043a\u0438\u0441\u043b\u0435\u043d\u0438\u0435";
    private static final String POISON = "\u042f\u0434";
    private static final String DETECTION = "\u0414\u0435\u0442\u0435\u043a\u0446\u0438\u044f";
    private static final String CROSS_POTION = "\u0421\u043a\u0440\u0435\u0449\u0435\u043d\u043e\u0435 \u0437\u0435\u043b\u044c\u0435";

    private static final String SWORD_NAME = "\u043c\u0435\u0447";
    private static final String HELMET_NAME = "\u0448\u043b\u0435\u043c";
    private static final String HELMET_ALT = "\u043a\u0430\u0441\u043a\u0430";
    private static final String CHESTPLATE_NAME = "\u043d\u0430\u0433\u0440\u0443\u0434\u043d\u0438\u043a";
    private static final String CHESTPLATE_ALT = "\u043a\u0438\u0440\u0430\u0441\u0430";
    private static final String LEGGINGS_NAME = "\u043f\u043e\u043d\u043e\u0436\u0438";
    private static final String LEGGINGS_ALT = "\u0448\u0442\u0430\u043d\u044b";
    private static final String BOOTS_NAME = "\u0431\u043e\u0442\u0438\u043d\u043a\u0438";
    private static final String BOOTS_ALT = "\u0441\u0430\u043f\u043e\u0433\u0438";
    private static final String STRENGTH_EFFECT = "minecraft:strength";
    private static final String SPEED_EFFECT = "minecraft:speed";

    private static final String[] ENCHANT_NAMES = {
            SHARPNESS, UNBREAKING, LOOTING, KNOCKBACK, FIRE_ASPECT,
            PROTECTION, THORNS, MENDING, DEPTH_STRIDER,
            VAMPIRISM, OXIDATION, POISON, DETECTION
    };

    private static final Map<String, Integer> ROMAN_MAP = Map.ofEntries(
            Map.entry("I", 1), Map.entry("II", 2), Map.entry("III", 3),
            Map.entry("IV", 4), Map.entry("V", 5), Map.entry("VI", 6),
            Map.entry("VII", 7), Map.entry("VIII", 8), Map.entry("IX", 9), Map.entry("X", 10)
    );

    @Getter
    private static final AuctionHelperModule instance = new AuctionHelperModule();

    private enum MatchState {
        NONE,
        EXACT,
        FALLBACK
    }

    private record CachedAuctionStack(
            int fingerprint,
            String lowerName,
            String itemId,
            int price,
            Map<String, Integer> enchants,
            Map<String, Integer> potionEffects
    ) { }

    private record HoverDebugState(
            String title,
            List<String> lines,
            MatchState matchState
    ) { }

    private final PriceParser priceParser = new PriceParser();

    @Getter
    private final ModeSetting mode = new ModeSetting("Mode")
            .value("Fun Time")
            .values("Fun Time")
            .onAction(() -> priceParser.currentMode = ParseModeChoice.FUN_TIME);

    private final BooleanSetting calculator = new BooleanSetting("Calculator").value(false);
    private final BooleanSetting unitPriceTooltip = new BooleanSetting("Unit price tooltip").value(true);
    private final MultiModeSetting itemTypes = new MultiModeSetting("Items")
            .modes("Sword", "Helmet", "Chestplate", "Leggings", "Boots", "Potion");
    private final SliderSetting slots = new SliderSetting("Slots").value(3f).range(1f, 6f).step(1f);

    private final MultiModeSetting swordEnchants = new MultiModeSetting("Sword Enchants")
            .modes(SHARPNESS, UNBREAKING, LOOTING, NO_KNOCKBACK, FIRE_ASPECT, VAMPIRISM, OXIDATION, POISON, DETECTION)
            .setVisible(() -> itemTypes.isSelected("Sword"));
    private final SliderSetting sharpnessMin = new SliderSetting(SHARPNESS).value(7f).range(1f, 10f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Sword") && swordEnchants.isSelected(SHARPNESS));
    private final SliderSetting unbreakingSwordMin = new SliderSetting(UNBREAKING + " (\u043c\u0435\u0447)").value(3f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Sword") && swordEnchants.isSelected(UNBREAKING));
    private final SliderSetting lootingMin = new SliderSetting(LOOTING).value(5f).range(1f, 10f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Sword") && swordEnchants.isSelected(LOOTING));
    private final SliderSetting fireAspectMin = new SliderSetting(FIRE_ASPECT).value(2f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Sword") && swordEnchants.isSelected(FIRE_ASPECT));
    private final SliderSetting vampirismMin = new SliderSetting(VAMPIRISM).value(1f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Sword") && swordEnchants.isSelected(VAMPIRISM));
    private final SliderSetting oxidationMin = new SliderSetting(OXIDATION).value(1f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Sword") && swordEnchants.isSelected(OXIDATION));
    private final SliderSetting poisonMin = new SliderSetting(POISON).value(1f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Sword") && swordEnchants.isSelected(POISON));
    private final SliderSetting detectionMin = new SliderSetting(DETECTION).value(1f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Sword") && swordEnchants.isSelected(DETECTION));

    private final MultiModeSetting helmetEnchants = new MultiModeSetting("Helmet Enchants")
            .modes(PROTECTION, NO_THORNS, UNBREAKING, MENDING)
            .setVisible(() -> itemTypes.isSelected("Helmet"));
    private final SliderSetting protectionHelmetMin = new SliderSetting(PROTECTION + " (\u0448\u043b\u0435\u043c)").value(5f).range(1f, 7f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Helmet") && helmetEnchants.isSelected(PROTECTION));
    private final SliderSetting unbreakingHelmetMin = new SliderSetting(UNBREAKING + " (\u0448\u043b\u0435\u043c)").value(3f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Helmet") && helmetEnchants.isSelected(UNBREAKING));

    private final MultiModeSetting chestEnchants = new MultiModeSetting("Chest Enchants")
            .modes(PROTECTION, NO_THORNS, UNBREAKING, MENDING)
            .setVisible(() -> itemTypes.isSelected("Chestplate"));
    private final SliderSetting protectionChestMin = new SliderSetting(PROTECTION + " (\u043d\u0430\u0433\u0440\u0443\u0434\u043d)").value(5f).range(1f, 7f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Chestplate") && chestEnchants.isSelected(PROTECTION));
    private final SliderSetting unbreakingChestMin = new SliderSetting(UNBREAKING + " (\u043d\u0430\u0433\u0440\u0443\u0434\u043d)").value(3f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Chestplate") && chestEnchants.isSelected(UNBREAKING));

    private final MultiModeSetting legsEnchants = new MultiModeSetting("Legs Enchants")
            .modes(PROTECTION, NO_THORNS, UNBREAKING, MENDING)
            .setVisible(() -> itemTypes.isSelected("Leggings"));
    private final SliderSetting protectionLegsMin = new SliderSetting(PROTECTION + " (\u043f\u043e\u043d\u043e\u0436\u0438)").value(5f).range(1f, 7f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Leggings") && legsEnchants.isSelected(PROTECTION));
    private final SliderSetting unbreakingLegsMin = new SliderSetting(UNBREAKING + " (\u043f\u043e\u043d\u043e\u0436\u0438)").value(3f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Leggings") && legsEnchants.isSelected(UNBREAKING));

    private final MultiModeSetting bootsEnchants = new MultiModeSetting("Boots Enchants")
            .modes(PROTECTION, NO_THORNS, UNBREAKING, MENDING, DEPTH_STRIDER)
            .setVisible(() -> itemTypes.isSelected("Boots"));
    private final SliderSetting protectionBootsMin = new SliderSetting(PROTECTION + " (\u0431\u043e\u0442\u0438\u043d\u043a\u0438)").value(5f).range(1f, 7f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Boots") && bootsEnchants.isSelected(PROTECTION));
    private final SliderSetting unbreakingBootsMin = new SliderSetting(UNBREAKING + " (\u0431\u043e\u0442\u0438\u043d\u043a\u0438)").value(3f).range(1f, 5f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Boots") && bootsEnchants.isSelected(UNBREAKING));
    private final SliderSetting depthStriderMin = new SliderSetting(DEPTH_STRIDER).value(3f).range(1f, 3f).step(1f)
            .setVisible(() -> itemTypes.isSelected("Boots") && bootsEnchants.isSelected(DEPTH_STRIDER));

    private final MultiModeSetting potionTypes = new MultiModeSetting("Potion Types")
            .modes(CROSS_POTION)
            .setVisible(() -> itemTypes.isSelected("Potion"));

    @Getter
    private AuctionSettingsPanel settingsPanel;

    private final Set<Integer> highlightedSlotIds = new HashSet<>();
    private final Set<Integer> fallbackSlotIds = new HashSet<>();
    private final Map<Integer, CachedAuctionStack> cachedStacks = new HashMap<>();
    private HoverDebugState hoverDebugState;

    @Getter
    private boolean isAuctionOpen = false;
    @Getter
    private boolean debugContainerOpen = false;

    public AuctionHelperModule() {
        priceParser.currentMode = ParseModeChoice.FUN_TIME;
        addSettings(
                calculator, unitPriceTooltip, itemTypes, slots,
                swordEnchants, sharpnessMin, unbreakingSwordMin, lootingMin, fireAspectMin, vampirismMin, oxidationMin, poisonMin, detectionMin,
                helmetEnchants, protectionHelmetMin, unbreakingHelmetMin,
                chestEnchants, protectionChestMin, unbreakingChestMin,
                legsEnchants, protectionLegsMin, unbreakingLegsMin,
                bootsEnchants, protectionBootsMin, unbreakingBootsMin, depthStriderMin,
                potionTypes
        );
    }

    @Override
    public void onEnable() {
        settingsPanel = new AuctionSettingsPanel(this);
        settingsPanel.setPosition(4f, 4f, 160f);
    }

    @Override
    public void onDisable() {
        isAuctionOpen = false;
        debugContainerOpen = false;
        highlightedSlotIds.clear();
        fallbackSlotIds.clear();
        cachedStacks.clear();
        hoverDebugState = null;
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> handleUpdateEvent()));
        addEvents(updateEvent);
    }

    public void handleUpdateEvent() {
        if (mc.currentScreen == null || mc.player == null) {
            isAuctionOpen = false;
            debugContainerOpen = false;
            highlightedSlotIds.clear();
            fallbackSlotIds.clear();
            hoverDebugState = null;
            return;
        }

        debugContainerOpen = isSupportedDebugScreen();

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest)) {
            isAuctionOpen = false;
            highlightedSlotIds.clear();
            fallbackSlotIds.clear();
            if (!debugContainerOpen) {
                hoverDebugState = null;
            }
            return;
        }

        String title = mc.currentScreen.getTitle().getString();
        if (!isAuctionTitle(title)) {
            isAuctionOpen = false;
            highlightedSlotIds.clear();
            fallbackSlotIds.clear();
            if (!debugContainerOpen) {
                hoverDebugState = null;
            }
            return;
        }

        isAuctionOpen = true;
        cachedStacks.keySet().removeIf(slotId -> slotId >= chest.slots.size());
        highlightedSlotIds.clear();
        fallbackSlotIds.clear();

        boolean hasItemFilter = hasSelectedItemTypes();

        List<Slot> exactMatches = new ArrayList<>();
        List<Slot> selectedFallbackMatches = new ArrayList<>();
        List<Slot> otherFallbackMatches = new ArrayList<>();
        for (Slot slot : chest.slots) {
            if (slot.id > 44 || slot.getStack().isEmpty()) {
                continue;
            }

            CachedAuctionStack cachedStack = getCachedStack(slot);
            if (cachedStack.price() == -1) {
                continue;
            }

            MatchState state = classifyMatch(slot.getStack(), cachedStack, hasItemFilter);
            if (state == MatchState.EXACT) {
                exactMatches.add(slot);
            } else if (state == MatchState.FALLBACK && matchesAnySelectedType(slot.getStack(), cachedStack)) {
                selectedFallbackMatches.add(slot);
            } else if (state == MatchState.FALLBACK) {
                otherFallbackMatches.add(slot);
            }
        }

        exactMatches.sort(Comparator.comparingInt(slot -> getEffectivePrice(slot.getStack(), getCachedStack(slot).price())));
        selectedFallbackMatches.sort(Comparator.comparingInt(slot -> getEffectivePrice(slot.getStack(), getCachedStack(slot).price())));
        otherFallbackMatches.sort(Comparator.comparingInt(slot -> getEffectivePrice(slot.getStack(), getCachedStack(slot).price())));

        int limit = slots.getValue().intValue();
        for (Slot slot : exactMatches) {
            if (highlightedSlotIds.size() >= limit) {
                break;
            }

            highlightedSlotIds.add(slot.id);
        }

        for (Slot slot : selectedFallbackMatches) {
            if (highlightedSlotIds.size() >= limit) {
                break;
            }

            highlightedSlotIds.add(slot.id);
            fallbackSlotIds.add(slot.id);
        }

        for (Slot slot : otherFallbackMatches) {
            if (highlightedSlotIds.size() >= limit) {
                break;
            }

            highlightedSlotIds.add(slot.id);
            fallbackSlotIds.add(slot.id);
        }
    }

    public void onSlotDebug(Slot slot) {
        if (!debugContainerOpen || slot == null || slot.getStack().isEmpty()) {
            hoverDebugState = null;
            return;
        }

        CachedAuctionStack cachedStack = getCachedStack(slot);
        boolean hasItemFilter = hasSelectedItemTypes();
        MatchState matchState = classifyMatch(slot.getStack(), cachedStack, hasItemFilter);
        String containerTitle = mc.currentScreen != null ? mc.currentScreen.getTitle().getString() : "unknown";
        int containerSlots = mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest
                ? chest.getRows() * 9
                : Math.min(54, mc.player.currentScreenHandler.slots.size());
        List<Text> tooltip = slot.getStack().getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);

        List<String> lines = new ArrayList<>();
        lines.add("container: " + trimDebugLine(containerTitle, 42));
        lines.add("slot: " + slot.id + " | idx " + slot.getIndex() + " | " + formatContainerSection(slot.id, containerSlots));
        lines.add("item: " + cachedStack.itemId() + " x" + slot.getStack().getCount());
        lines.add("state: " + formatMatchState(matchState));
        lines.add("price: " + formatPrice(cachedStack.price()) + formatPerItemPrice(slot.getStack(), cachedStack.price()));

        String detectedTypes = describeDetectedTypes(slot.getStack(), cachedStack);
        if (!detectedTypes.isEmpty()) {
            lines.add("type: " + detectedTypes);
        }

        if (!cachedStack.enchants().isEmpty()) {
            lines.add("ench: " + formatDebugMap(cachedStack.enchants()));
        }

        if (!cachedStack.potionEffects().isEmpty()) {
            lines.add("fx: " + formatDebugMap(cachedStack.potionEffects()));
        }

        addTooltipPreviewLines(lines, tooltip);
        hoverDebugState = new HoverDebugState(slot.getStack().getName().getString(), lines, matchState);
    }

    public void clearSlotDebug() {
        hoverDebugState = null;
    }

    public void onRenderChest(DrawContext context, Slot slot) {
        if (!highlightedSlotIds.contains(slot.id)) {
            return;
        }

        int alpha = (int) (20 + 110 * Math.abs(Math.sin(System.currentTimeMillis() * 0.005)));
        Color color = fallbackSlotIds.contains(slot.id)
                ? new Color(255, 176, 64, alpha)
                : new Color(0, 255, 0, alpha);
        RenderUtil.RECT.draw(context.getMatrices(), slot.x, slot.y, 16f, 16f, 0f, color);
    }

    public boolean shouldUseFastAuctionItemRender() {
        return isEnabled() && isAuctionOpen;
    }

    public void renderAuctionPanel(DrawContext context, int mouseX, int mouseY, float delta, int containerX, int containerY) {
        if (!isEnabled() || !isAuctionOpen) {
            return;
        }

        AutoBuyModule autoBuy = AutoBuyModule.getInstance();
        if (!autoBuy.isEnabled()) {
            return;
        }

        var records = autoBuy.getHistoryManager().getRecords();
        if (records.isEmpty()) {
            return;
        }

        float scaleMultiplier = RenderService.getInstance().getScale();
        float scaledContainerX = containerX / scaleMultiplier;
        float scaledContainerY = containerY / scaleMultiplier;

        float w = 180f;
        float x = scaledContainerX - w - 8f;
        float y = scaledContainerY;

        float padding = 8f;
        int limit = Math.min(6, records.size());
        float h = 18f + limit * 18f + padding;
        float round = 6f;

        context.getMatrices().push();
        context.getMatrices().scale(scaleMultiplier, scaleMultiplier, 1f);

        drawGlassSurface(context.getMatrices(), x, y, w, h, round, UIColors.card(180), 255, false);

        Fonts.PS_BOLD.drawText(context.getMatrices(), "История автобая",
                x + 8f, y + 6f, 6f, UIColors.textColor(255));

        for (int i = 0; i < limit; i++) {
            AutoTradeHistoryRecord record = records.get(i);
            float rowY = y + 16f + i * 18f;
            Color accent = record.getResult() == AutoTradeHistoryRecord.Result.SUCCESS
                    ? UIColors.primary(210)
                    : UIColors.secondary(210);

            RenderUtil.RECT.draw(context.getMatrices(), x + 8f, rowY + 3f, 4f, 4f, 1.5f, accent);

            context.drawItem(record.getDisplayStack(), Math.round(x + 16f), Math.round(rowY - 2f));

            String status = record.getResult() == AutoTradeHistoryRecord.Result.SUCCESS
                    ? "куплено"
                    : "не успел";

            String line = abbreviateHistory(record.getItemName(), 18)
                    + " x" + record.getCount();
            Fonts.PS_MEDIUM.drawText(context.getMatrices(), line, x + 34f, rowY - 1f, 4.8f, UIColors.textColor(255));

            String subLine = formatCompactPrice(record.getTotalPrice())
                    + " • " + status
                    + " • " + formatHistoryAge(record.getTimestamp());
            Fonts.PS_MEDIUM.drawText(context.getMatrices(), subLine, x + 34f, rowY + 6f, 4.2f, UIColors.mutedText(220));
        }

        context.getMatrices().pop();
    }

    private void drawGlassSurface(net.minecraft.client.util.math.MatrixStack ms, float x, float y, float width, float height, float round, Color surface, int alpha, boolean compact) {
        int blurAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 1.04f) : (int) (alpha * 1.10f)));
        int backgroundBlurAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.96f) : (int) (alpha * 1.00f)));
        int overlayAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.10f) : (int) (alpha * 0.13f)));
        int strokeAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.22f) : (int) (alpha * 0.24f)));

        RenderUtil.BLUR_RECT.draw(ms, x, y, width, height, round, UIColors.blur(blurAlpha), 0.08f);
        RenderUtil.BLUR_RECT.draw(ms, x, y, width, height, round, UIColors.backgroundBlur(backgroundBlurAlpha), 0.06f);
        RenderUtil.RECT.draw(ms, x, y, width, height, round, surface);
        RenderUtil.RECT.draw(ms, x, y, width, height, round, UIColors.overlay(overlayAlpha));
        RenderUtil.RECT.draw(ms, x, y, width, height, round, UIColors.stroke(strokeAlpha));
    }

    private String formatCompactPrice(int value) {
        if (value >= 1_000_000) {
            return formatCompactUnit(value / 1_000_000f, "кк");
        }
        if (value >= 1_000) {
            return formatCompactUnit(value / 1_000f, "к");
        }
        return String.valueOf(value);
    }

    private String formatCompactUnit(float value, String suffix) {
        if (value >= 10f || Math.abs(value - Math.round(value)) < 0.05f) {
            return Math.round(value) + suffix;
        }
        return String.format(Locale.US, "%.1f%s", value, suffix);
    }

    private String formatHistoryAge(long timestamp) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - timestamp);
        long seconds = elapsed / 1000L;
        if (seconds < 60L) {
            return seconds + "с";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + "м";
        }
        long hours = minutes / 60L;
        return hours + "ч";
    }

    private String abbreviateHistory(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "…";
    }

    public boolean shouldAppendUnitPriceTooltip() {
        return isEnabled() && unitPriceTooltip.getValue();
    }

    public ParseModeChoice getCurrentParseMode() {
        return priceParser.currentMode;
    }

    private boolean isSupportedDebugScreen() {
        if (!(mc.currentScreen instanceof HandledScreen<?> handledScreen)) {
            return false;
        }

        return !(handledScreen instanceof InventoryScreen)
                && !(handledScreen instanceof CreativeInventoryScreen);
    }

    public static boolean isAuctionTitleLike(String title) {
        String lower = title.toLowerCase(Locale.ROOT);
        return lower.contains("\u0430\u0443\u043a\u0446\u0438\u043e\u043d")
                || lower.contains("\u043f\u043e\u0438\u0441\u043a")
                || lower.contains("\u043c\u0430\u0440\u043a\u0435\u0442")
                || lower.contains("[\u2603] \u0430\u0443\u043a\u0446\u0438\u043e\u043d\u044b")
                || lower.contains("\u2603 \u043f\u043e")
                || lower.contains("\u2603 \u043f:")
                || lower.contains("0a2z1/")
                || lower.matches("^[0-9a-z]{5}/.*$");
    }

    private boolean isAuctionTitle(String title) {
        return isAuctionTitleLike(title);
    }

    private CachedAuctionStack getCachedStack(Slot slot) {
        ItemStack stack = slot.getStack();
        int fingerprint = Objects.hash(
                Registries.ITEM.getId(stack.getItem()),
                stack.getCount(),
                stack.getName().getString(),
                stack.getComponents()
        );

        CachedAuctionStack cached = cachedStacks.get(slot.id);
        if (cached != null && cached.fingerprint() == fingerprint) {
            return cached;
        }

        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
        CachedAuctionStack rebuilt = new CachedAuctionStack(
                fingerprint,
                stack.getName().getString().toLowerCase(Locale.ROOT),
                Registries.ITEM.getId(stack.getItem()).toString(),
                getPrice(stack, tooltip),
                parseEnchantments(stack, tooltip),
                parsePotionEffects(stack)
        );
        cachedStacks.put(slot.id, rebuilt);
        return rebuilt;
    }

    private MatchState classifyMatch(ItemStack stack, CachedAuctionStack cachedStack, boolean hasItemFilter) {
        if (!hasItemFilter) {
            return MatchState.EXACT;
        }

        boolean matchedSelectedType = false;
        MatchState best = MatchState.NONE;
        for (String type : itemTypes.getValue()) {
            if (!matchesType(type, stack, cachedStack.lowerName(), cachedStack.itemId(), cachedStack.potionEffects())) {
                continue;
            }

            matchedSelectedType = true;
            if (!hasActiveRequirement(type)) {
                return MatchState.EXACT;
            }

            if (matchesTypeRequirements(type, cachedStack.enchants(), cachedStack.potionEffects())) {
                return MatchState.EXACT;
            }

            best = MatchState.FALLBACK;
        }

        return matchedSelectedType ? best : MatchState.FALLBACK;
    }

    private boolean hasSelectedItemTypes() {
        return !itemTypes.getSelectedModes().isEmpty();
    }

    private boolean matchesAnySelectedType(ItemStack stack, CachedAuctionStack cachedStack) {
        for (String type : itemTypes.getValue()) {
            if (matchesType(type, stack, cachedStack.lowerName(), cachedStack.itemId(), cachedStack.potionEffects())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesType(String type, ItemStack stack, String lowerName, String itemId, Map<String, Integer> potionEffects) {
        return switch (type) {
            case "Sword" -> stack.getItem() instanceof SwordItem || lowerName.contains(SWORD_NAME) || itemId.contains("sword");
            case "Helmet" -> lowerName.contains(HELMET_NAME) || lowerName.contains(HELMET_ALT) || itemId.contains("helmet");
            case "Chestplate" -> lowerName.contains(CHESTPLATE_NAME) || lowerName.contains(CHESTPLATE_ALT) || itemId.contains("chestplate");
            case "Leggings" -> lowerName.contains(LEGGINGS_NAME) || lowerName.contains(LEGGINGS_ALT) || itemId.contains("leggings");
            case "Boots" -> lowerName.contains(BOOTS_NAME) || lowerName.contains(BOOTS_ALT) || itemId.contains("boots");
            case "Potion" -> isPotionStack(itemId, potionEffects);
            default -> false;
        };
    }

    private boolean isPotionStack(String itemId, Map<String, Integer> potionEffects) {
        return !potionEffects.isEmpty()
                || itemId.endsWith("potion")
                || itemId.endsWith("splash_potion")
                || itemId.endsWith("lingering_potion");
    }

    private boolean hasActiveRequirement(String type) {
        return switch (type) {
            case "Sword" -> !swordEnchants.getValue().isEmpty();
            case "Helmet" -> !helmetEnchants.getValue().isEmpty();
            case "Chestplate" -> !chestEnchants.getValue().isEmpty();
            case "Leggings" -> !legsEnchants.getValue().isEmpty();
            case "Boots" -> !bootsEnchants.getValue().isEmpty();
            case "Potion" -> !potionTypes.getValue().isEmpty();
            default -> false;
        };
    }

    private boolean matchesTypeRequirements(String type, Map<String, Integer> enchants, Map<String, Integer> potionEffects) {
        return switch (type) {
            case "Sword" -> checkEnchants(enchants, swordEnchants, "Sword");
            case "Helmet" -> checkEnchants(enchants, helmetEnchants, "Helmet");
            case "Chestplate" -> checkEnchants(enchants, chestEnchants, "Chestplate");
            case "Leggings" -> checkEnchants(enchants, legsEnchants, "Leggings");
            case "Boots" -> checkEnchants(enchants, bootsEnchants, "Boots");
            case "Potion" -> checkPotionFilters(potionEffects);
            default -> false;
        };
    }

    private boolean checkEnchants(Map<String, Integer> enchants, MultiModeSetting filter, String type) {
        for (String enchName : filter.getValue()) {
            if (NO_KNOCKBACK.equals(enchName)) {
                if (enchants.containsKey(KNOCKBACK)) return false;
                continue;
            }
            if (NO_THORNS.equals(enchName)) {
                if (enchants.containsKey(THORNS)) return false;
                continue;
            }
            if (MENDING.equals(enchName)) {
                if (!enchants.containsKey(MENDING)) return false;
                continue;
            }

            if (enchants.getOrDefault(enchName, 0) < getMinLevelForEnchant(enchName, type)) {
                return false;
            }
        }
        return true;
    }

    private int getMinLevelForEnchant(String enchName, String type) {
        return switch (type) {
            case "Sword" -> switch (enchName) {
                case SHARPNESS -> sharpnessMin.getValue().intValue();
                case UNBREAKING -> unbreakingSwordMin.getValue().intValue();
                case LOOTING -> lootingMin.getValue().intValue();
                case FIRE_ASPECT -> fireAspectMin.getValue().intValue();
                case VAMPIRISM -> vampirismMin.getValue().intValue();
                case OXIDATION -> oxidationMin.getValue().intValue();
                case POISON -> poisonMin.getValue().intValue();
                case DETECTION -> detectionMin.getValue().intValue();
                default -> 1;
            };
            case "Helmet" -> switch (enchName) {
                case PROTECTION -> protectionHelmetMin.getValue().intValue();
                case UNBREAKING -> unbreakingHelmetMin.getValue().intValue();
                default -> 1;
            };
            case "Chestplate" -> switch (enchName) {
                case PROTECTION -> protectionChestMin.getValue().intValue();
                case UNBREAKING -> unbreakingChestMin.getValue().intValue();
                default -> 1;
            };
            case "Leggings" -> switch (enchName) {
                case PROTECTION -> protectionLegsMin.getValue().intValue();
                case UNBREAKING -> unbreakingLegsMin.getValue().intValue();
                default -> 1;
            };
            case "Boots" -> switch (enchName) {
                case PROTECTION -> protectionBootsMin.getValue().intValue();
                case UNBREAKING -> unbreakingBootsMin.getValue().intValue();
                case DEPTH_STRIDER -> depthStriderMin.getValue().intValue();
                default -> 1;
            };
            default -> 1;
        };
    }

    private boolean checkPotionFilters(Map<String, Integer> potionEffects) {
        for (String potionType : potionTypes.getValue()) {
            if (CROSS_POTION.equals(potionType)) {
                if (potionEffects.getOrDefault(STRENGTH_EFFECT, 0) < 3 || potionEffects.getOrDefault(SPEED_EFFECT, 0) < 3) {
                    return false;
                }
            }
        }

        return true;
    }

    private Map<String, Integer> parseEnchantments(ItemStack stack, List<Text> tooltip) {
        Map<String, Integer> result = new LinkedHashMap<>();

        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            String mappedName = mapEnchantId(entry.getKey().getIdAsString());
            if (mappedName != null) {
                result.put(mappedName, entry.getIntValue());
            }
        }

        for (Text text : tooltip) {
            String clean = text.getString().replaceAll("§[0-9a-fk-or]", "").trim();
            if (clean.isEmpty()) {
                continue;
            }

            for (String enchName : ENCHANT_NAMES) {
                if (clean.startsWith(enchName)) {
                    String rest = clean.substring(enchName.length()).trim();
                    result.put(enchName, rest.isEmpty() ? 1 : Math.max(1, parseLevel(rest)));
                    break;
                }
            }
        }

        return result;
    }

    private String mapEnchantId(String enchantId) {
        return switch (enchantId) {
            case "minecraft:sharpness" -> SHARPNESS;
            case "minecraft:unbreaking" -> UNBREAKING;
            case "minecraft:looting" -> LOOTING;
            case "minecraft:knockback" -> KNOCKBACK;
            case "minecraft:fire_aspect" -> FIRE_ASPECT;
            case "minecraft:protection" -> PROTECTION;
            case "minecraft:thorns" -> THORNS;
            case "minecraft:mending" -> MENDING;
            case "minecraft:depth_strider" -> DEPTH_STRIDER;
            default -> null;
        };
    }

    private Map<String, Integer> parsePotionEffects(ItemStack stack) {
        Map<String, Integer> result = new LinkedHashMap<>();
        PotionContentsComponent potionContents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
        for (StatusEffectInstance effect : potionContents.getEffects()) {
            Identifier effectId = Registries.STATUS_EFFECT.getId(effect.getEffectType().value());
            if (effectId != null) {
                result.put(effectId.toString(), effect.getAmplifier() + 1);
            }
        }
        return result;
    }

    private int parseLevel(String value) {
        Integer roman = ROMAN_MAP.get(value.trim());
        if (roman != null) {
            return roman;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int getPrice(ItemStack stack) {
        return priceParser.getPrice(stack);
    }

    private int getPrice(ItemStack stack, List<Text> tooltip) {
        return priceParser.getPrice(stack, tooltip);
    }

    private int getEffectivePrice(ItemStack stack, int price) {
        if (price <= 0) {
            return price;
        }
        return calculator.getValue() ? price / Math.max(1, stack.getCount()) : price;
    }

    private void renderSlotDebug(DrawContext context, int mouseX, int mouseY, int containerX, int containerY) {
        if (hoverDebugState == null) {
            return;
        }

        float titleSize = 7f;
        float lineSize = 6f;
        float pad = 4f;
        float gap = 2f;

        float boxWidth = Fonts.PS_BOLD.getWidth(hoverDebugState.title(), titleSize);
        for (String line : hoverDebugState.lines()) {
            boxWidth = Math.max(boxWidth, Fonts.PS_MEDIUM.getWidth(line, lineSize));
        }
        boxWidth += pad * 2f;

        float boxHeight = pad * 2f + titleSize + gap;
        boxHeight += hoverDebugState.lines().size() * (lineSize + 1f);

        float maxX = mc.getWindow().getScaledWidth() - boxWidth - 6f;
        float maxY = mc.getWindow().getScaledHeight() - boxHeight - 6f;
        float preferredX = containerX - boxWidth - 8f;
        float preferredY = containerY;
        float boxX = Math.max(6f, Math.min(preferredX, maxX));
        float boxY = Math.max(6f, Math.min(preferredY, maxY));

        Color accent = switch (hoverDebugState.matchState()) {
            case EXACT -> new Color(92, 255, 132, 235);
            case FALLBACK -> new Color(255, 184, 82, 235);
            default -> new Color(180, 186, 194, 220);
        };

        RenderUtil.RECT.draw(context.getMatrices(), boxX, boxY, boxWidth, boxHeight, 4f, new Color(10, 12, 16, 190));
        RenderUtil.RECT.draw(context.getMatrices(), boxX, boxY, boxWidth, 1.2f, 4f, accent);

        float textX = boxX + pad;
        float textY = boxY + pad;
        Fonts.PS_BOLD.drawText(context.getMatrices(), hoverDebugState.title(), textX, textY, titleSize, accent);
        textY += titleSize + gap;

        for (String line : hoverDebugState.lines()) {
            Fonts.PS_MEDIUM.drawText(context.getMatrices(), line, textX, textY, lineSize, new Color(228, 232, 236, 235));
            textY += lineSize + 1f;
        }
    }

    private String describeDetectedTypes(ItemStack stack, CachedAuctionStack cachedStack) {
        List<String> types = new ArrayList<>();
        for (String type : itemTypes.getAllModes()) {
            if (matchesType(type, stack, cachedStack.lowerName(), cachedStack.itemId(), cachedStack.potionEffects())) {
                types.add(type);
            }
        }
        return String.join(", ", types);
    }

    private String formatMatchState(MatchState matchState) {
        return switch (matchState) {
            case EXACT -> "exact";
            case FALLBACK -> "fallback";
            case NONE -> "other";
        };
    }

    private String formatPrice(int price) {
        if (price < 0) {
            return "n/a";
        }
        return String.format(Locale.US, "%,d", price).replace(',', ' ');
    }

    private String formatPerItemPrice(ItemStack stack, int price) {
        if (!calculator.getValue() || price <= 0 || stack.getCount() <= 1) {
            return "";
        }
        return " | each " + formatPrice(getEffectivePrice(stack, price));
    }

    private String formatDebugMap(Map<String, Integer> values) {
        StringJoiner joiner = new StringJoiner(", ");
        int index = 0;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (index >= 3) {
                joiner.add("...");
                break;
            }

            String key = entry.getKey();
            int separator = key.indexOf(':');
            if (separator >= 0 && separator + 1 < key.length()) {
                key = key.substring(separator + 1);
            }
            key = key.replace('_', ' ');
            joiner.add(key + " " + entry.getValue());
            index++;
        }
        return joiner.toString();
    }

    private void addTooltipPreviewLines(List<String> lines, List<Text> tooltip) {
        int added = 0;
        for (Text text : tooltip) {
            String clean = text.getString().trim();
            if (clean.isEmpty()) {
                continue;
            }
            if (added == 0) {
                added++;
                continue;
            }
            lines.add("tip: " + trimDebugLine(clean, 38));
            added++;
            if (added >= 4) {
                break;
            }
        }
    }

    private String formatContainerSection(int slotId, int containerSlots) {
        if (slotId < containerSlots) {
            return "container";
        }
        return "player";
    }

    private String trimDebugLine(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}