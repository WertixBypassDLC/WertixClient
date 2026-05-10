package sweetie.nezi.api.system.configs;

import lombok.Getter;

public class ConfigSkin {
    @Getter private static final ConfigSkin instance = new ConfigSkin();

    private volatile String cachedSkinName;

    public void load() {
        cachedSkinName = readStoredSkinName();
    }

    public void save(String skinName) {
        String normalized = skinName == null ? null : skinName.trim();
        UnifiedConfigStore.getInstance().updateRoot(root -> {
            if (normalized != null && !normalized.isEmpty()) {
                root.addProperty("Skin", normalized);
            } else {
                root.remove("Skin");
            }
        });
        cachedSkinName = (normalized == null || normalized.isEmpty()) ? null : normalized;
    }

    public String update() {
        if (cachedSkinName == null) {
            cachedSkinName = readStoredSkinName();
        }
        return cachedSkinName;
    }

    private String readStoredSkinName() {
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
