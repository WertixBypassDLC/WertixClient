package sweetie.nezi.api.system.language;

import java.util.HashMap;
import java.util.Map;

public final class ModuleTextRegistry {
    private record ModuleText(String russianName, String englishDescription, String russianDescription) { }

    private static final Map<String, ModuleText> MODULES = new HashMap<>();

    static {
        register("Aim Bot", "ÐÐ¸Ð¼ Ð°ÑÑÐ¸ÑÑ‚", "Softly assists aim on nearby targets.", "ÐŸÐ»Ð°Ð²Ð½Ð¾ Ð¿Ð¾Ð¼Ð¾Ð³Ð°ÐµÑ‚ Ð´Ð¾Ð²Ð¾Ð´Ð¸Ñ‚ÑŒ Ð¿Ñ€Ð¸Ñ†ÐµÐ» Ð´Ð¾ Ð±Ð»Ð¸Ð¶Ð°Ð¹ÑˆÐ¸Ñ… Ñ†ÐµÐ»ÐµÐ¹.");
        register("Aura", "ÐÑƒÑ€Ð°", "Automatically attacks selected targets.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ Ð°Ñ‚Ð°ÐºÑƒÐµÑ‚ Ð²Ñ‹Ð±Ñ€Ð°Ð½Ð½Ñ‹Ðµ Ñ†ÐµÐ»Ð¸.");
        register("Auto Totem", "ÐÐ²Ñ‚Ð¾ Ñ‚Ð¾Ñ‚ÐµÐ¼", "Moves a totem into the off-hand when needed.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ ÑÑ‚Ð°Ð²Ð¸Ñ‚ Ñ‚Ð¾Ñ‚ÐµÐ¼ Ð² Ð»ÐµÐ²ÑƒÑŽ Ñ€ÑƒÐºÑƒ, ÐºÐ¾Ð³Ð´Ð° ÑÑ‚Ð¾ Ð½ÑƒÐ¶Ð½Ð¾.");
        register("Auto Swap", "ÐÐ²Ñ‚Ð¾ ÑÐ²Ð°Ð¿", "Quickly swaps between configured combat items.", "Ð‘Ñ‹ÑÑ‚Ñ€Ð¾ Ð¼ÐµÐ½ÑÐµÑ‚ Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ñ‹ Ð¼ÐµÐ¶Ð´Ñƒ Ð·Ð°Ð´Ð°Ð½Ð½Ñ‹Ð¼Ð¸ Ð±Ð¾ÐµÐ²Ñ‹Ð¼Ð¸ ÑÐ»Ð¾Ñ‚Ð°Ð¼Ð¸.");
        register("No Entity Trace", "Ð‘ÐµÐ· ÑÐ½Ñ‚Ð¸Ñ‚Ð¸ Ñ‚Ñ€ÐµÐ¹ÑÐ°", "Prevents entities from blocking block interaction.", "ÐÐµ Ð´Ð°ÐµÑ‚ ÑÑƒÑ‰Ð½Ð¾ÑÑ‚ÑÐ¼ Ð¼ÐµÑˆÐ°Ñ‚ÑŒ Ð²Ð·Ð°Ð¸Ð¼Ð¾Ð´ÐµÐ¹ÑÑ‚Ð²Ð¸ÑŽ Ñ Ð±Ð»Ð¾ÐºÐ°Ð¼Ð¸.");
        register("No Friend Hurt", "Ð‘ÐµÐ· ÑƒÑ€Ð¾Ð½Ð° Ð´Ñ€ÑƒÐ·ÑŒÑÐ¼", "Stops accidental hits on friends.", "ÐÐµ Ð´Ð°ÐµÑ‚ ÑÐ»ÑƒÑ‡Ð°Ð¹Ð½Ð¾ Ð±Ð¸Ñ‚ÑŒ Ð´Ñ€ÑƒÐ·ÐµÐ¹.");
        register("TriggerBot", "Ð¢Ñ€Ð¸Ð³Ð³ÐµÑ€ Ð±Ð¾Ñ‚", "Hits targets when your crosshair is over them.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ ÑƒÐ´Ð°Ñ€ÑÐµÑ‚, ÐºÐ¾Ð³Ð´Ð° Ð¿Ñ€Ð¸Ñ†ÐµÐ» Ð½Ð°Ñ…Ð¾Ð´Ð¸Ñ‚ÑÑ Ð½Ð° Ñ†ÐµÐ»Ð¸.");
        register("Velocity", "ÐÐ¾ÐºÐ±ÑÐº", "Controls or reduces incoming knockback.", "ÐšÐ¾Ð½Ñ‚Ñ€Ð¾Ð»Ð¸Ñ€ÑƒÐµÑ‚ Ð¸Ð»Ð¸ ÑƒÐ¼ÐµÐ½ÑŒÑˆÐ°ÐµÑ‚ Ð²Ñ…Ð¾Ð´ÑÑ‰Ð¸Ð¹ Ð½Ð¾ÐºÐ±ÑÐº.");
        register("Elytra Target", "Ð­Ð»Ð¸Ñ‚Ñ€Ð° Ñ‚Ð°Ñ€Ð³ÐµÑ‚", "Targets players using elytra-specific logic.", "Ð’Ñ‹Ð±Ð¸Ñ€Ð°ÐµÑ‚ Ñ†ÐµÐ»Ð¸ Ñ Ð´Ð¾Ð¿Ð¾Ð»Ð½Ð¸Ñ‚ÐµÐ»ÑŒÐ½Ð¾Ð¹ Ð»Ð¾Ð³Ð¸ÐºÐ¾Ð¹ Ð´Ð»Ñ ÑÐ»Ð¸Ñ‚Ñ€.");

        register("Inventory Move", "Ð˜Ð½Ð²ÐµÐ½Ñ‚Ð¾Ñ€Ð¸ Ð¼ÑƒÐ²", "Lets you move while inventory screens are open.", "ÐŸÐ¾Ð·Ð²Ð¾Ð»ÑÐµÑ‚ Ð´Ð²Ð¸Ð³Ð°Ñ‚ÑŒÑÑ Ñ Ð¾Ñ‚ÐºÑ€Ñ‹Ñ‚Ñ‹Ð¼ Ð¸Ð½Ð²ÐµÐ½Ñ‚Ð°Ñ€ÐµÐ¼.");
        register("Move Fix", "ÐœÑƒÐ² Ñ„Ð¸ÐºÑ", "Keeps movement natural during rotations.", "Ð¡Ð¾Ñ…Ñ€Ð°Ð½ÑÐµÑ‚ ÐµÑÑ‚ÐµÑÑ‚Ð²ÐµÐ½Ð½Ð¾Ðµ Ð´Ð²Ð¸Ð¶ÐµÐ½Ð¸Ðµ Ð²Ð¾ Ð²Ñ€ÐµÐ¼Ñ Ñ€Ð¾Ñ‚Ð°Ñ†Ð¸Ð¹.");
        register("No Clip", "ÐÐ¾Ñƒ ÐºÐ»Ð¸Ð¿", "Disables collision in supported situations.", "ÐžÑ‚ÐºÐ»ÑŽÑ‡Ð°ÐµÑ‚ ÐºÐ¾Ð»Ð»Ð¸Ð·Ð¸Ð¸ Ð² Ð¿Ð¾Ð´Ð´ÐµÑ€Ð¶Ð¸Ð²Ð°ÐµÐ¼Ñ‹Ñ… ÑÑ†ÐµÐ½Ð°Ñ€Ð¸ÑÑ….");
        register("No Web", "Ð‘ÐµÐ· Ð¿Ð°ÑƒÑ‚Ð¸Ð½Ñ‹", "Reduces slowdown inside cobwebs.", "Ð£Ð±Ð¸Ñ€Ð°ÐµÑ‚ Ð·Ð°Ð¼ÐµÐ´Ð»ÐµÐ½Ð¸Ðµ Ð² Ð¿Ð°ÑƒÑ‚Ð¸Ð½Ðµ.");
        register("Sprint", "Ð¡Ð¿Ñ€Ð¸Ð½Ñ‚", "Automatically keeps sprint active.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ Ð¿Ð¾Ð´Ð´ÐµÑ€Ð¶Ð¸Ð²Ð°ÐµÑ‚ Ð±ÐµÐ³.");
        register("Strafe", "Ð¡Ñ‚Ñ€ÐµÐ¹Ñ„", "Improves lateral movement control.", "Ð£Ð»ÑƒÑ‡ÑˆÐ°ÐµÑ‚ Ð±Ð¾ÐºÐ¾Ð²Ð¾Ðµ Ð´Ð²Ð¸Ð¶ÐµÐ½Ð¸Ðµ Ð¸ ÐºÐ¾Ð½Ñ‚Ñ€Ð¾Ð»ÑŒ ÑÐºÐ¾Ñ€Ð¾ÑÑ‚Ð¸.");
        register("Water Speed", "Ð¡ÐºÐ¾Ñ€Ð¾ÑÑ‚ÑŒ Ð² Ð²Ð¾Ð´Ðµ", "Improves movement speed in water.", "ÐŸÐ¾Ð²Ñ‹ÑˆÐ°ÐµÑ‚ ÑÐºÐ¾Ñ€Ð¾ÑÑ‚ÑŒ Ð´Ð²Ð¸Ð¶ÐµÐ½Ð¸Ñ Ð² Ð²Ð¾Ð´Ðµ.");
        register("Flight", "Ð¤Ð»Ð°Ð¹", "Provides flight using available modes.", "Ð”Ð°ÐµÑ‚ Ð²Ð¾Ð·Ð¼Ð¾Ð¶Ð½Ð¾ÑÑ‚ÑŒ Ð»ÐµÑ‚Ð°Ñ‚ÑŒ Ð² Ð²Ñ‹Ð±Ñ€Ð°Ð½Ð½Ð¾Ð¼ Ñ€ÐµÐ¶Ð¸Ð¼Ðµ.");
        register("No Slow", "Ð‘ÐµÐ· Ð·Ð°Ð¼ÐµÐ´Ð»ÐµÐ½Ð¸Ñ", "Removes item-use slowdown.", "Ð£Ð±Ð¸Ñ€Ð°ÐµÑ‚ Ð·Ð°Ð¼ÐµÐ´Ð»ÐµÐ½Ð¸Ðµ Ð¿Ñ€Ð¸ Ð¸ÑÐ¿Ð¾Ð»ÑŒÐ·Ð¾Ð²Ð°Ð½Ð¸Ð¸ Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ð¾Ð².");
        register("Speed", "Ð¡Ð¿Ð¸Ð´", "Increases movement speed with different modes.", "Ð£ÑÐºÐ¾Ñ€ÑÐµÑ‚ Ð´Ð²Ð¸Ð¶ÐµÐ½Ð¸Ðµ Ð² Ñ€Ð°Ð·Ð½Ñ‹Ñ… Ñ€ÐµÐ¶Ð¸Ð¼Ð°Ñ….");
        register("Spider", "Ð¡Ð¿Ð°Ð¹Ð´ÐµÑ€", "Lets you climb walls.", "ÐŸÐ¾Ð·Ð²Ð¾Ð»ÑÐµÑ‚ Ð²Ð·Ð±Ð¸Ñ€Ð°Ñ‚ÑŒÑÑ Ð¿Ð¾ ÑÑ‚ÐµÐ½Ð°Ð¼.");

        register("Auction Helper", "ÐÑƒÐºÑ†Ð¸Ð¾Ð½ Ñ…ÐµÐ»Ð¿ÐµÑ€", "Analyzes auction items and highlights useful lots.", "ÐÐ½Ð°Ð»Ð¸Ð·Ð¸Ñ€ÑƒÐµÑ‚ Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ñ‹ Ð½Ð° Ð°ÑƒÐºÑ†Ð¸Ð¾Ð½Ðµ Ð¸ Ð¿Ð¾Ð´ÑÐ²ÐµÑ‡Ð¸Ð²Ð°ÐµÑ‚ Ð¿Ð¾Ð»ÐµÐ·Ð½Ñ‹Ðµ Ð»Ð¾Ñ‚Ñ‹.");
        register("Auto Buy", "ÐÐ²Ñ‚Ð¾Ð±Ð°Ð¹", "Automatically refreshes the auction and buys configured items.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ Ð¾Ð±Ð½Ð¾Ð²Ð»ÑÐµÑ‚ Ð°ÑƒÐºÑ†Ð¸Ð¾Ð½ Ð¸ Ð¿Ð¾ÐºÑƒÐ¿Ð°ÐµÑ‚ Ð½Ð°ÑÑ‚Ñ€Ð¾ÐµÐ½Ð½Ñ‹Ðµ Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ñ‹.");
        register("AutoEpshtein", "ÐÐ²Ñ‚Ð¾Ð­Ð¿ÑˆÑ‚ÐµÐ¹Ð½", "Handles helper automation and similarity actions.", "ÐžÐ±Ñ€Ð°Ð±Ð°Ñ‚Ñ‹Ð²Ð°ÐµÑ‚ Ð²ÑÐ¿Ð¾Ð¼Ð¾Ð³Ð°Ñ‚ÐµÐ»ÑŒÐ½ÑƒÑŽ Ð°Ð²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸ÐºÑƒ Ð¸ Ð´ÐµÐ¹ÑÑ‚Ð²Ð¸Ñ ÑÐ¾ ÑÑ…Ð¾Ð¶ÐµÑÑ‚ÑŒÑŽ ÑÐ¾Ð¾Ð±Ñ‰ÐµÐ½Ð¸Ð¹.");
        register("Auto Respawn", "ÐÐ²Ñ‚Ð¾ Ñ€ÐµÑÐ¿Ð°Ð²Ð½", "Respawns after death automatically.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ Ð²Ð¾Ð·Ñ€Ð¾Ð¶Ð´Ð°ÐµÑ‚ Ð¿Ð¾ÑÐ»Ðµ ÑÐ¼ÐµÑ€Ñ‚Ð¸.");
        register("Auto Sell", "ÐÐ²Ñ‚Ð¾Ð¿Ñ€Ð¾Ð´Ð°Ð¶Ð°", "Automatically lists configured inventory items for sale.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ Ð²Ñ‹ÑÑ‚Ð°Ð²Ð»ÑÐµÑ‚ Ð½Ð°ÑÑ‚Ñ€Ð¾ÐµÐ½Ð½Ñ‹Ðµ Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ñ‹ Ð¸Ð· Ð¸Ð½Ð²ÐµÐ½Ñ‚Ð°Ñ€Ñ Ð½Ð° Ð¿Ñ€Ð¾Ð´Ð°Ð¶Ñƒ.");
        register("Auto Setup", "ÐÐ²Ñ‚Ð¾Ð¿Ð°Ñ€ÑÐµÑ€", "Scans auction prices and builds buy and sell rules.", "Ð¡ÐºÐ°Ð½Ð¸Ñ€ÑƒÐµÑ‚ Ñ†ÐµÐ½Ñ‹ Ð½Ð° Ð°ÑƒÐºÑ†Ð¸Ð¾Ð½Ðµ Ð¸ Ð²Ñ‹ÑÑ‚Ð°Ð²Ð»ÑÐµÑ‚ Ð¿Ñ€Ð°Ð²Ð¸Ð»Ð° Ð¿Ð¾ÐºÑƒÐ¿ÐºÐ¸ Ð¸ Ð¿Ñ€Ð¾Ð´Ð°Ð¶Ð¸.");
        register("Fast Break", "Ð¤Ð°ÑÑ‚ Ð±Ñ€ÐµÐ¹Ðº", "Speeds up block breaking in supported cases.", "Ð£ÑÐºÐ¾Ñ€ÑÐµÑ‚ Ð»Ð¾Ð¼Ð°Ð½Ð¸Ðµ Ð±Ð»Ð¾ÐºÐ¾Ð² Ð² Ð¿Ð¾Ð´Ð´ÐµÑ€Ð¶Ð¸Ð²Ð°ÐµÐ¼Ñ‹Ñ… ÑÐ»ÑƒÑ‡Ð°ÑÑ….");
        register("IRC", "IRC", "Enables the built-in client chat bridge.", "Ð’ÐºÐ»ÑŽÑ‡Ð°ÐµÑ‚ Ð²ÑÑ‚Ñ€Ð¾ÐµÐ½Ð½Ñ‹Ð¹ Ð¼Ð¾ÑÑ‚ ÐºÐ»Ð¸ÐµÐ½Ñ‚ÑÐºÐ¾Ð³Ð¾ Ñ‡Ð°Ñ‚Ð°.");
        register("Joiner", "Ð”Ð¶Ð¾Ð¹Ð½ÐµÑ€", "Automates repetitive join interactions.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ð·Ð¸Ñ€ÑƒÐµÑ‚ Ð¿Ð¾Ð²Ñ‚Ð¾Ñ€ÑÑŽÑ‰Ð¸ÐµÑÑ Ð´ÐµÐ¹ÑÑ‚Ð²Ð¸Ñ Ð¿Ñ€Ð¸ Ð·Ð°Ñ…Ð¾Ð´Ðµ.");
        register("Mouse Tweaks", "Ð¢Ð²Ð¸ÐºÐ¸ Ð¼Ñ‹ÑˆÐ¸", "Adds extra click and inventory mouse behavior.", "Ð”Ð¾Ð±Ð°Ð²Ð»ÑÐµÑ‚ Ð´Ð¾Ð¿Ð¾Ð»Ð½Ð¸Ñ‚ÐµÐ»ÑŒÐ½Ñ‹Ðµ Ð´ÐµÐ¹ÑÑ‚Ð²Ð¸Ñ Ð¼Ñ‹ÑˆÐ¸ Ð¸ ÐºÐ»Ð¸ÐºÐ¾Ð².");
        register("Potion Tracker", "Ð¢Ñ€ÐµÐºÐµÑ€ Ð·ÐµÐ»Ð¸Ð¹", "Tracks thrown splash potions and affected players.", "ÐžÑ‚ÑÐ»ÐµÐ¶Ð¸Ð²Ð°ÐµÑ‚ Ð±Ñ€Ð¾ÑˆÐµÐ½Ð½Ñ‹Ðµ ÑÐ¿Ð»ÑÑˆ-Ð·ÐµÐ»ÑŒÑ Ð¸ Ð·Ð°Ñ‚Ñ€Ð¾Ð½ÑƒÑ‚Ñ‹Ñ… Ð¸Ð³Ñ€Ð¾ÐºÐ¾Ð².");
        register("Use Tracker", "Ð¢Ñ€ÐµÐºÐµÑ€ Ð¸ÑÐ¿Ð¾Ð»ÑŒÐ·Ð¾Ð²Ð°Ð½Ð¸Ñ", "Shows notifications for totems, consumed items, and received effects.", "ÐŸÐ¾ÐºÐ°Ð·Ñ‹Ð²Ð°ÐµÑ‚ ÑƒÐ²ÐµÐ´Ð¾Ð¼Ð»ÐµÐ½Ð¸Ñ Ð¾ Ñ‚Ð¾Ñ‚ÐµÐ¼Ð°Ñ…, ÑÑŠÐµÐ´ÐµÐ½Ð½Ñ‹Ñ… Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ð°Ñ… Ð¸ Ð¿Ð¾Ð»ÑƒÑ‡ÐµÐ½Ð½Ñ‹Ñ… ÑÑ„Ñ„ÐµÐºÑ‚Ð°Ñ….");
        register("NameProtect", "Ð—Ð°Ñ‰Ð¸Ñ‚Ð° Ð½Ð¸ÐºÐ°", "Hides your nickname and friend names.", "Ð¡ÐºÑ€Ñ‹Ð²Ð°ÐµÑ‚ Ð²Ð°Ñˆ Ð½Ð¸Ðº Ð¸ Ð½Ð¸ÐºÐ¸ Ð´Ñ€ÑƒÐ·ÐµÐ¹.");
        register("Tape Mouse", "Ð¢ÐµÐ¹Ð¿ Ð¼Ð°ÑƒÑ", "Automates repeated attack or use clicks.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ð·Ð¸Ñ€ÑƒÐµÑ‚ Ð¿Ð¾Ð²Ñ‚Ð¾Ñ€Ð½Ñ‹Ðµ ÐºÐ»Ð¸ÐºÐ¸ Ð°Ñ‚Ð°ÐºÐ¸ Ð¸Ð»Ð¸ Ð¸ÑÐ¿Ð¾Ð»ÑŒÐ·Ð¾Ð²Ð°Ð½Ð¸Ñ.");
        register("Toggle Sounds", "Ð—Ð²ÑƒÐºÐ¸ Ñ‚Ð¾Ð³Ð³Ð»Ð°", "Plays sounds when modules are toggled.", "ÐŸÑ€Ð¾Ð¸Ð³Ñ€Ñ‹Ð²Ð°ÐµÑ‚ Ð·Ð²ÑƒÐºÐ¸ Ð¿Ñ€Ð¸ Ð²ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ð¸ Ð¸ Ð²Ñ‹ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ð¸ Ð¼Ð¾Ð´ÑƒÐ»ÐµÐ¹.");
        register("TP Accept", "Ð¢ÐŸ Ð°ÐºÑ†ÐµÐ¿Ñ‚", "Automatically accepts teleport requests.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ Ð¿Ñ€Ð¸Ð½Ð¸Ð¼Ð°ÐµÑ‚ Ñ‚ÐµÐ»ÐµÐ¿Ð¾Ñ€Ñ‚-Ð·Ð°Ð¿Ñ€Ð¾ÑÑ‹.");
        register("Warden Helper", "Ð’Ð°Ñ€Ð´ÐµÐ½ Ñ‚Ð°Ð¹Ð¼ÐµÑ€", "Tracks underground timers and related positions.", "ÐžÑ‚ÑÐ»ÐµÐ¶Ð¸Ð²Ð°ÐµÑ‚ Ð¿Ð¾Ð´Ð·ÐµÐ¼Ð½Ñ‹Ðµ Ñ‚Ð°Ð¹Ð¼ÐµÑ€Ñ‹ Ð¸ ÑÐ²ÑÐ·Ð°Ð½Ð½Ñ‹Ðµ Ð¿Ð¾Ð·Ð¸Ñ†Ð¸Ð¸.");

        register("Anti AFK", "ÐÐ½Ñ‚Ð¸ ÐÐ¤Ðš", "Prevents AFK kick with safe movement actions.", "ÐÐµ Ð´Ð°ÐµÑ‚ Ð¿Ð¾Ð»ÑƒÑ‡Ð¸Ñ‚ÑŒ ÐºÐ¸Ðº Ð·Ð° AFK Ñ Ð±ÐµÐ·Ð¾Ð¿Ð°ÑÐ½Ñ‹Ð¼Ð¸ Ð´ÐµÐ¹ÑÑ‚Ð²Ð¸ÑÐ¼Ð¸.");
        register("Funtime Helper", "Ð¤Ð°Ð½Ñ‚Ð°Ð¹Ð¼ Ñ…ÐµÐ»Ð¿ÐµÑ€", "Automates helper hotkeys and timer actions for the server.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ð·Ð¸Ñ€ÑƒÐµÑ‚ Ñ…Ð¾Ñ‚ÐºÐµÐ¸ Ð¿Ð¾Ð¼Ð¾Ñ‰Ð½Ð¸ÐºÐ° Ð¸ Ð´ÐµÐ¹ÑÑ‚Ð²Ð¸Ñ Ñ‚Ð°Ð¹Ð¼ÐµÑ€Ð° Ð´Ð»Ñ ÑÐµÑ€Ð²ÐµÑ€Ð°.");
        register("Auto Leave", "ÐÐ²Ñ‚Ð¾ Ð²Ñ‹Ñ…Ð¾Ð´", "Leaves the server when configured danger appears.", "ÐÐ²Ñ‚Ð¾Ð¼Ð°Ñ‚Ð¸Ñ‡ÐµÑÐºÐ¸ Ð²Ñ‹Ñ…Ð¾Ð´Ð¸Ñ‚ Ñ ÑÐµÑ€Ð²ÐµÑ€Ð° Ð¿Ñ€Ð¸ Ð¾Ð¿Ð°ÑÐ½Ñ‹Ñ… ÑƒÑÐ»Ð¾Ð²Ð¸ÑÑ….");
        register("Auto Tool", "ÐÐ²Ñ‚Ð¾ Ð¸Ð½ÑÑ‚Ñ€ÑƒÐ¼ÐµÐ½Ñ‚", "Chooses the best tool for the block you mine.", "Ð’Ñ‹Ð±Ð¸Ñ€Ð°ÐµÑ‚ Ð»ÑƒÑ‡ÑˆÐ¸Ð¹ Ð¸Ð½ÑÑ‚Ñ€ÑƒÐ¼ÐµÐ½Ñ‚ Ð´Ð»Ñ Ð»Ð¾Ð¼Ð°ÐµÐ¼Ð¾Ð³Ð¾ Ð±Ð»Ð¾ÐºÐ°.");
        register("Chest Stealer", "Ð§ÐµÑÑ‚ ÑÑ‚Ð¸Ð»ÐµÑ€", "Quickly loots container inventories.", "Ð‘Ñ‹ÑÑ‚Ñ€Ð¾ Ð·Ð°Ð±Ð¸Ñ€Ð°ÐµÑ‚ Ð²ÐµÑ‰Ð¸ Ð¸Ð· ÐºÐ¾Ð½Ñ‚ÐµÐ¹Ð½ÐµÑ€Ð¾Ð².");
        register("Click Pearl", "ÐšÐ»Ð¸Ðº Ð¿ÐµÑ€Ð»", "Throws an ender pearl from a shortcut.", "Ð‘Ñ€Ð¾ÑÐ°ÐµÑ‚ ÑÐ½Ð´ÐµÑ€-Ð¿ÐµÑ€Ð» Ð¿Ð¾ Ð³Ð¾Ñ€ÑÑ‡ÐµÐ¼Ñƒ Ð´ÐµÐ¹ÑÑ‚Ð²Ð¸ÑŽ.");
        register("Elytra Swap", "Ð­Ð»Ð¸Ñ‚Ñ€Ð° ÑÐ²Ð°Ð¿", "Swaps chestplate and elytra quickly.", "Ð‘Ñ‹ÑÑ‚Ñ€Ð¾ Ð¼ÐµÐ½ÑÐµÑ‚ Ð½Ð°Ð³Ñ€ÑƒÐ´Ð½Ð¸Ðº Ð¸ ÑÐ»Ð¸Ñ‚Ñ€Ñƒ.");
        register("No Delay", "Ð‘ÐµÐ· Ð·Ð°Ð´ÐµÑ€Ð¶ÐºÐ¸", "Removes delays from supported item actions.", "Ð£Ð±Ð¸Ñ€Ð°ÐµÑ‚ Ð·Ð°Ð´ÐµÑ€Ð¶ÐºÐ¸ Ñƒ Ð¿Ð¾Ð´Ð´ÐµÑ€Ð¶Ð¸Ð²Ð°ÐµÐ¼Ñ‹Ñ… Ð´ÐµÐ¹ÑÑ‚Ð²Ð¸Ð¹ Ñ Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ð°Ð¼Ð¸.");

        register("Ambience", "ÐÐ¼Ð±Ð¸ÐµÐ½Ñ", "Adjusts world ambience and sky colors.", "ÐÐ°ÑÑ‚Ñ€Ð°Ð¸Ð²Ð°ÐµÑ‚ Ð°Ñ‚Ð¼Ð¾ÑÑ„ÐµÑ€Ñƒ Ð¼Ð¸Ñ€Ð° Ð¸ Ñ†Ð²ÐµÑ‚Ð° Ð½ÐµÐ±Ð°.");
        register("AspectRatio", "Ð¡Ð¾Ð¾Ñ‚Ð½Ð¾ÑˆÐµÐ½Ð¸Ðµ ÑÑ‚Ð¾Ñ€Ð¾Ð½", "Changes the rendered aspect ratio.", "ÐœÐµÐ½ÑÐµÑ‚ ÑÐ¾Ð¾Ñ‚Ð½Ð¾ÑˆÐµÐ½Ð¸Ðµ ÑÑ‚Ð¾Ñ€Ð¾Ð½ Ñ€ÐµÐ½Ð´ÐµÑ€Ð°.");
        register("Block ESP", "Ð‘Ð»Ð¾Ðº ESP", "Highlights selected blocks in the world.", "ÐŸÐ¾Ð´ÑÐ²ÐµÑ‡Ð¸Ð²Ð°ÐµÑ‚ Ð²Ñ‹Ð±Ñ€Ð°Ð½Ð½Ñ‹Ðµ Ð±Ð»Ð¾ÐºÐ¸ Ð² Ð¼Ð¸Ñ€Ðµ.");
        register("Block Highlight", "ÐŸÐ¾Ð´ÑÐ²ÐµÑ‚ÐºÐ° Ð±Ð»Ð¾ÐºÐ°", "Improves the selected block outline.", "Ð£Ð»ÑƒÑ‡ÑˆÐ°ÐµÑ‚ Ð¾Ð±Ð²Ð¾Ð´ÐºÑƒ Ð²Ñ‹Ð±Ñ€Ð°Ð½Ð½Ð¾Ð³Ð¾ Ð±Ð»Ð¾ÐºÐ°.");
        register("Camera Clip", "ÐšÐ°Ð¼ÐµÑ€Ð° ÐºÐ»Ð¸Ð¿", "Lets the camera move more freely near walls.", "Ð”Ð°ÐµÑ‚ ÐºÐ°Ð¼ÐµÑ€Ðµ ÑÐ²Ð¾Ð±Ð¾Ð´Ð½ÐµÐµ Ð²ÐµÑÑ‚Ð¸ ÑÐµÐ±Ñ Ñ€ÑÐ´Ð¾Ð¼ ÑÐ¾ ÑÑ‚ÐµÐ½Ð°Ð¼Ð¸.");
        register("Click GUI", "ÐšÐ»Ð¸Ðº GUI", "Opens the main click interface.", "ÐžÑ‚ÐºÑ€Ñ‹Ð²Ð°ÐµÑ‚ Ð³Ð»Ð°Ð²Ð½Ð¾Ðµ ÐºÐ»Ð¸Ðº-Ð¼ÐµÐ½ÑŽ ÐºÐ»Ð¸ÐµÐ½Ñ‚Ð°.");
        register("China Hat", "ÐšÐ¸Ñ‚Ð°Ð¹ÑÐºÐ°Ñ ÑˆÐ»ÑÐ¿Ð°", "Draws a conical hat above players.", "Ð Ð¸ÑÑƒÐµÑ‚ ÐºÐ¾Ð½Ð¸Ñ‡ÐµÑÐºÑƒÑŽ ÑˆÐ»ÑÐ¿Ñƒ Ð½Ð°Ð´ Ð¸Ð³Ñ€Ð¾ÐºÐ°Ð¼Ð¸.");
        register("Glow ESP", "Ð“Ð»Ð¾Ñƒ ESP", "Draws a glow or fill effect on players.", "Ð Ð¸ÑÑƒÐµÑ‚ ÑÐ²ÐµÑ‡ÐµÐ½Ð¸Ðµ Ð¸Ð»Ð¸ Ð·Ð°Ð»Ð¸Ð²ÐºÑƒ Ð½Ð° Ð¸Ð³Ñ€Ð¾ÐºÐ°Ñ….");
        register("HUD", "HUD", "Controls widgets, HUD style, and interface visuals.", "Ð£Ð¿Ñ€Ð°Ð²Ð»ÑÐµÑ‚ Ð²Ð¸Ð´Ð¶ÐµÑ‚Ð°Ð¼Ð¸, ÑÑ‚Ð¸Ð»ÐµÐ¼ HUD Ð¸ Ð²Ð¸Ð·ÑƒÐ°Ð»Ð¾Ð¼ Ð¸Ð½Ñ‚ÐµÑ€Ñ„ÐµÐ¹ÑÐ°.");
        register("Jump Circle", "Ð”Ð¶Ð°Ð¼Ð¿ ÑÐµÑ€ÐºÐ»", "Draws a landing circle or jump effect.", "Ð Ð¸ÑÑƒÐµÑ‚ ÐºÑ€ÑƒÐ³ Ð¿Ñ€Ð¸Ð·ÐµÐ¼Ð»ÐµÐ½Ð¸Ñ Ð¸Ð»Ð¸ ÑÑ„Ñ„ÐµÐºÑ‚ Ð¿Ñ€Ñ‹Ð¶ÐºÐ°.");
        register("Fullbright", "Ð¤ÑƒÐ»Ð±Ñ€Ð°Ð¹Ñ‚", "Makes the world fully bright.", "Ð”ÐµÐ»Ð°ÐµÑ‚ Ð¼Ð¸Ñ€ Ð¿Ð¾Ð»Ð½Ð¾ÑÑ‚ÑŒÑŽ ÑÑ€ÐºÐ¸Ð¼.");
        register("Pointers", "Ð£ÐºÐ°Ð·Ð°Ñ‚ÐµÐ»Ð¸", "Draws pointers toward important entities.", "Ð Ð¸ÑÑƒÐµÑ‚ ÑƒÐºÐ°Ð·Ð°Ñ‚ÐµÐ»Ð¸ Ð½Ð° Ð²Ð°Ð¶Ð½Ñ‹Ðµ ÑÑƒÑ‰Ð½Ð¾ÑÑ‚Ð¸.");
        register("Predictions", "ÐŸÑ€ÐµÐ´Ð¸ÐºÑ‚Ñ‹", "Renders predicted motion or impact positions.", "Ð Ð¸ÑÑƒÐµÑ‚ Ð¿Ñ€ÐµÐ´ÑÐºÐ°Ð·Ð°Ð½Ð½Ñ‹Ðµ Ñ‚Ñ€Ð°ÐµÐºÑ‚Ð¾Ñ€Ð¸Ð¸ Ð¸ Ð¿Ð¾Ð·Ð¸Ñ†Ð¸Ð¸.");
        register("Removals", "ÐžÑ‚ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ñ", "Removes selected visual effects.", "Ð£Ð±Ð¸Ñ€Ð°ÐµÑ‚ Ð²Ñ‹Ð±Ñ€Ð°Ð½Ð½Ñ‹Ðµ Ð²Ð¸Ð·ÑƒÐ°Ð»ÑŒÐ½Ñ‹Ðµ ÑÑ„Ñ„ÐµÐºÑ‚Ñ‹.");
        register("See Invisibles", "Ð’Ð¸Ð´ÐµÑ‚ÑŒ Ð½ÐµÐ²Ð¸Ð´Ð¸Ð¼Ñ‹Ñ…", "Makes invisible entities easier to notice.", "ÐŸÐ¾Ð¼Ð¾Ð³Ð°ÐµÑ‚ Ð·Ð°Ð¼ÐµÑ‡Ð°Ñ‚ÑŒ Ð½ÐµÐ²Ð¸Ð´Ð¸Ð¼Ñ‹Ñ… ÑÑƒÑ‰Ð½Ð¾ÑÑ‚ÐµÐ¹.");
        register("Shulker View", "ÐŸÑ€Ð¾ÑÐ¼Ð¾Ñ‚Ñ€ ÑˆÐ°Ð»ÐºÐµÑ€Ð¾Ð²", "Shows container contents inside shulker boxes.", "ÐŸÐ¾ÐºÐ°Ð·Ñ‹Ð²Ð°ÐµÑ‚ ÑÐ¾Ð´ÐµÑ€Ð¶Ð¸Ð¼Ð¾Ðµ ÐºÐ¾Ð½Ñ‚ÐµÐ¹Ð½ÐµÑ€Ð° Ð²Ð½ÑƒÑ‚Ñ€Ð¸ ÑˆÐ°Ð»ÐºÐµÑ€Ð¾Ð².");
        register("Swing Animation", "ÐÐ½Ð¸Ð¼Ð°Ñ†Ð¸Ñ ÑƒÐ´Ð°Ñ€Ð°", "Changes first-person swing animation.", "ÐœÐµÐ½ÑÐµÑ‚ Ð°Ð½Ð¸Ð¼Ð°Ñ†Ð¸ÑŽ ÑƒÐ´Ð°Ñ€Ð° Ð¾Ñ‚ Ð¿ÐµÑ€Ð²Ð¾Ð³Ð¾ Ð»Ð¸Ñ†Ð°.");
        register("Trails", "Ð¡Ð»ÐµÐ´Ñ‹", "Draws motion trails behind entities or actions.", "Ð Ð¸ÑÑƒÐµÑ‚ ÑÐ»ÐµÐ´Ñ‹ Ð·Ð° Ð´Ð²Ð¸Ð¶ÐµÐ½Ð¸ÐµÐ¼ ÑÑƒÑ‰Ð½Ð¾ÑÑ‚ÐµÐ¹ Ð¸Ð»Ð¸ Ð´ÐµÐ¹ÑÑ‚Ð²Ð¸Ð¹.");
        register("View Model", "Ð’Ð¸Ð´ Ð¼Ð¾Ð´ÐµÐ»ÑŒ", "Adjusts first-person hand and item positions.", "ÐÐ°ÑÑ‚Ñ€Ð°Ð¸Ð²Ð°ÐµÑ‚ Ð¿Ð¾Ð»Ð¾Ð¶ÐµÐ½Ð¸Ðµ Ñ€ÑƒÐº Ð¸ Ð¿Ñ€ÐµÐ´Ð¼ÐµÑ‚Ð¾Ð² Ð¾Ñ‚ Ð¿ÐµÑ€Ð²Ð¾Ð³Ð¾ Ð»Ð¸Ñ†Ð°.");
        register("Name Tags", "ÐÐµÐ¹Ð¼Ñ‚ÐµÐ³Ð¸", "Draws enhanced nametags above players.", "Ð Ð¸ÑÑƒÐµÑ‚ ÑƒÐ»ÑƒÑ‡ÑˆÐµÐ½Ð½Ñ‹Ðµ Ð½ÐµÐ¹Ð¼Ñ‚ÐµÐ³Ð¸ Ð½Ð°Ð´ Ð¸Ð³Ñ€Ð¾ÐºÐ°Ð¼Ð¸.");
        register("Particles", "Ð§Ð°ÑÑ‚Ð¸Ñ†Ñ‹", "Configures custom visual particles.", "ÐÐ°ÑÑ‚Ñ€Ð°Ð¸Ð²Ð°ÐµÑ‚ ÐºÐ°ÑÑ‚Ð¾Ð¼Ð½Ñ‹Ðµ Ð²Ð¸Ð·ÑƒÐ°Ð»ÑŒÐ½Ñ‹Ðµ Ñ‡Ð°ÑÑ‚Ð¸Ñ†Ñ‹.");
        register("Target Esp", "Ð¢Ð°Ñ€Ð³ÐµÑ‚ ESP", "Highlights the current combat target.", "ÐŸÐ¾Ð´ÑÐ²ÐµÑ‡Ð¸Ð²Ð°ÐµÑ‚ Ñ‚ÐµÐºÑƒÑ‰ÑƒÑŽ Ð±Ð¾ÐµÐ²ÑƒÑŽ Ñ†ÐµÐ»ÑŒ.");
    }

    private ModuleTextRegistry() {
    }

    public static String getLocalizedName(String englishName, ClientLanguage language) {
        ModuleText text = MODULES.get(englishName);
        if (text == null || language == ClientLanguage.ENGLISH) {
            return englishName;
        }
        return text.russianName;
    }

    public static String getDescription(String englishName, ClientLanguage language) {
        ModuleText text = MODULES.get(englishName);
        if (text == null) {
            return language == ClientLanguage.RUSSIAN
                    ? "ÐžÐ¿Ð¸ÑÐ°Ð½Ð¸Ðµ Ð¿Ð¾ÐºÐ° Ð½Ðµ Ð´Ð¾Ð±Ð°Ð²Ð»ÐµÐ½Ð¾."
                    : "Description is not available yet.";
        }
        return language == ClientLanguage.RUSSIAN ? text.russianDescription : text.englishDescription;
    }

    private static void register(String englishName, String russianName, String englishDescription, String russianDescription) {
        MODULES.put(englishName, new ModuleText(russianName, englishDescription, russianDescription));
    }
}


