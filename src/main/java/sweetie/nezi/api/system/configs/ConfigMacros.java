package sweetie.nezi.api.system.configs;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;

public class ConfigMacros {
    @Getter
    private static final ConfigMacros instance = new ConfigMacros();

    private final GsonBuilder gson = new GsonBuilder().setPrettyPrinting();

    public void load(File file, List<MacroManager.Macro> macros) {
        try {
            Type type = new TypeToken<List<MacroManager.Macro>>() {}.getType();
            JsonObject root = UnifiedConfigStore.getInstance().readRoot();
            List<MacroManager.Macro> loaded = root.has("Macros")
                    ? gson.create().fromJson(root.get("Macros"), type)
                    : null;
            macros.clear();
            if (loaded != null) {
                macros.addAll(loaded);
            }
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    public void save(File file, List<MacroManager.Macro> macros) {
        try {
            UnifiedConfigStore.getInstance().updateRoot(root ->
                    root.add("Macros", gson.create().toJsonTree(macros)));
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }
}
