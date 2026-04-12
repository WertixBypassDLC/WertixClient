package sweetie.nezi.client.features.modules.other.autobuy;

import net.minecraft.item.ItemStack;

public class AutoTradeHistoryRecord {
    public enum Result {
        SUCCESS,
        FAILED
    }

    private final ItemStack displayStack;
    private final String itemName;
    private final int count;
    private final int totalPrice;
    private final Result result;
    private final String reason;
    private final long timestamp;

    public AutoTradeHistoryRecord(ItemStack displayStack, String itemName, int count, int totalPrice, Result result, String reason) {
        this.displayStack = displayStack == null ? ItemStack.EMPTY : displayStack.copy();
        this.itemName = itemName == null ? "Unknown" : itemName;
        this.count = Math.max(1, count);
        this.totalPrice = Math.max(0, totalPrice);
        this.result = result == null ? Result.FAILED : result;
        this.reason = reason == null ? "" : reason;
        this.timestamp = System.currentTimeMillis();
    }

    public ItemStack getDisplayStack() {
        return displayStack.copy();
    }

    public String getItemName() {
        return itemName;
    }

    public int getCount() {
        return count;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public Result getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
