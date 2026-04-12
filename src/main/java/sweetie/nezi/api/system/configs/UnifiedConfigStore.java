package sweetie.nezi.api.system.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import sweetie.nezi.api.system.backend.ClientInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

@Getter
public class UnifiedConfigStore {
    @Getter
    private static final UnifiedConfigStore instance = new UnifiedConfigStore();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve(ClientInfo.MOD_ID + ".json");
    private final Path legacyAvatarPath = Paths.get(System.getProperty("user.dir"), "figura/config/avatar.json");
    private final Path legacyThemesPath = Paths.get(System.getProperty("user.dir"), "figura/config/model.json");
    private final Path legacyOtherPath = Paths.get(System.getProperty("user.dir"), "figura/config/config.json");
    private final Path legacySkinPath = Paths.get(System.getProperty("user.dir"), "figura/config/last_skin");

    private boolean prepared;

    public synchronized JsonObject readRoot() {
        ensurePrepared();
        try {
            if (!Files.exists(configPath)) {
                return new JsonObject();
            }

            String raw = Files.readString(configPath);
            if (raw == null || raw.isBlank()) {
                return new JsonObject();
            }

            JsonElement parsed = JsonParser.parseString(raw);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (Exception exception) {
            exception.printStackTrace();
            return new JsonObject();
        }
    }

    public synchronized void writeRoot(JsonObject root) {
        ensurePrepared();
        writeRootInternal(root == null ? new JsonObject() : root);
    }

    public synchronized void updateRoot(Consumer<JsonObject> updater) {
        JsonObject root = readRoot();
        updater.accept(root);
        writeRoot(root);
    }

    public synchronized JsonObject getObject(String key) {
        JsonObject root = readRoot();
        if (root.has(key) && root.get(key).isJsonObject()) {
            return root.getAsJsonObject(key);
        }
        return new JsonObject();
    }

    public synchronized JsonElement get(String key) {
        JsonObject root = readRoot();
        return root.get(key);
    }

    private synchronized void ensurePrepared() {
        if (prepared) {
            return;
        }

        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        if (!Files.exists(configPath)) {
            JsonObject migrated = migrateLegacyConfigs();
            writeRootInternal(migrated);
        }

        prepared = true;
    }

    private JsonObject migrateLegacyConfigs() {
        JsonObject root = new JsonObject();

        JsonObject avatarRoot = readJsonObject(legacyAvatarPath);
        merge(root, avatarRoot);

        JsonObject otherRoot = readJsonObject(legacyOtherPath);
        moveIfPresent(otherRoot, root, "friends", "Friends");
        moveIfPresent(otherRoot, root, "staffs", "Staffs");
        moveIfPresent(otherRoot, root, "macros", "Macros");
        moveIfPresent(otherRoot, root, "draggables", "Draggables");

        JsonElement themeStore = readJsonElement(legacyThemesPath);
        if (themeStore != null) {
            root.add("ThemesStore", themeStore);
        }

        String skinName = readText(legacySkinPath);
        if (skinName != null) {
            root.addProperty("Skin", skinName);
        }

        return root;
    }

    private void moveIfPresent(JsonObject from, JsonObject to, String oldKey, String newKey) {
        if (from != null && from.has(oldKey) && !to.has(newKey)) {
            to.add(newKey, from.get(oldKey));
        }
    }

    private void merge(JsonObject target, JsonObject source) {
        if (target == null || source == null) {
            return;
        }

        for (String key : source.keySet()) {
            target.add(key, source.get(key));
        }
    }

    private JsonElement readJsonElement(Path path) {
        try {
            if (!Files.exists(path)) {
                return null;
            }

            String raw = Files.readString(path);
            if (raw == null || raw.isBlank()) {
                return null;
            }

            return JsonParser.parseString(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonObject readJsonObject(Path path) {
        JsonElement element = readJsonElement(path);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private String readText(Path path) {
        try {
            if (!Files.exists(path)) {
                return null;
            }

            String raw = Files.readString(path).trim();
            return raw.isBlank() ? null : raw;
        } catch (IOException ignored) {
            return null;
        }
    }

    private void writeRootInternal(JsonObject root) {
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(configPath, GSON.toJson(root == null ? new JsonObject() : root));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
