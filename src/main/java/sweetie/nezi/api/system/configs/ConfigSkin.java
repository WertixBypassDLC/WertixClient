package sweetie.nezi.api.system.configs;

import lombok.Getter;
import sweetie.nezi.api.utils.math.TimerUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigSkin {
    @Getter private static final ConfigSkin instance = new ConfigSkin();

    private final TimerUtil timerUtil = new TimerUtil();
    private final AtomicBoolean fetchingInProgress = new AtomicBoolean(false);

    public void load() {
        update();
    }

    public void save(String skinName) {
        UnifiedConfigStore.getInstance().updateRoot(root -> {
            if (skinName != null && !skinName.trim().isEmpty()) {
                root.addProperty("Skin", skinName.trim());
            } else {
                root.remove("Skin");
            }
        });
    }

    public String update() {
        try {
            String content = "";
            if (UnifiedConfigStore.getInstance().get("Skin") != null) {
                content = UnifiedConfigStore.getInstance().get("Skin").getAsString().trim();
            }

            return content.isEmpty() ? null : content;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public void fetchSkin() {
        // Skin command was removed, skin fetching is disabled
    }
}
