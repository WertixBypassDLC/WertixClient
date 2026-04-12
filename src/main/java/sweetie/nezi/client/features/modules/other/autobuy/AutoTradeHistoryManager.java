package sweetie.nezi.client.features.modules.other.autobuy;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AutoTradeHistoryManager {
    private static final int MAX_RECORDS = 80;

    private final List<AutoTradeHistoryRecord> records = new ArrayList<>();

    public void addSuccess(ItemStack stack, String itemName, int count, int totalPrice) {
        addRecord(new AutoTradeHistoryRecord(stack, itemName, count, totalPrice, AutoTradeHistoryRecord.Result.SUCCESS, ""));
    }

    public void addFailure(ItemStack stack, String itemName, int count, int totalPrice, String reason) {
        addRecord(new AutoTradeHistoryRecord(stack, itemName, count, totalPrice, AutoTradeHistoryRecord.Result.FAILED, reason));
    }

    public List<AutoTradeHistoryRecord> getRecords() {
        return new ArrayList<>(records);
    }

    public void clear() {
        records.clear();
    }

    private void addRecord(AutoTradeHistoryRecord record) {
        records.add(0, record);
        while (records.size() > MAX_RECORDS) {
            records.remove(records.size() - 1);
        }
    }
}
