package sweetie.nezi.client.features.modules.other.autobuy;

public class AutoTradeRule {
    private boolean enabled;
    private int buyUnitPrice;
    private int sellPrice;
    private int minStackCount;

    public AutoTradeRule() {
        this.enabled = true;
        this.buyUnitPrice = 0;
        this.sellPrice = 0;
        this.minStackCount = 1;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBuyUnitPrice() {
        return buyUnitPrice;
    }

    public void setBuyUnitPrice(int buyUnitPrice) {
        this.buyUnitPrice = Math.max(0, buyUnitPrice);
    }

    public int getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(int sellPrice) {
        this.sellPrice = Math.max(0, sellPrice);
    }

    public int getMinStackCount() {
        return minStackCount;
    }

    public void setMinStackCount(int minStackCount) {
        this.minStackCount = Math.max(1, minStackCount);
    }

    public boolean canBuy(int totalPrice, int count) {
        if (!enabled || buyUnitPrice <= 0 || count < minStackCount || totalPrice <= 0) {
            return false;
        }

        return (totalPrice / (double) Math.max(1, count)) <= buyUnitPrice;
    }

    public boolean canSell(int count) {
        return enabled && sellPrice > 0 && count >= minStackCount;
    }
}
