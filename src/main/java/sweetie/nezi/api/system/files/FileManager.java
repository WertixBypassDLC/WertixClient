package sweetie.nezi.api.system.files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.api.system.configs.MacroManager;
import sweetie.nezi.api.system.configs.StaffManager;
import sweetie.nezi.api.system.configs.UnifiedConfigStore;
import sweetie.nezi.api.system.draggable.DraggableManager;

import java.util.List;

@Getter
public class FileManager {
    @Getter
    private static final FileManager instance = new FileManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public void load() {
        try {
            JsonObject root = UnifiedConfigStore.getInstance().readRoot();
            if (root == null) {
                return;
            }

            if (root.has("Friends")) {
                List<String> friends = GSON.fromJson(root.get("Friends"), new TypeToken<List<String>>() {}.getType());
                FriendManager.getInstance().getData().clear();
                FriendManager.getInstance().getData().addAll(friends);
            }

            if (root.has("Staffs")) {
                List<String> staffs = GSON.fromJson(root.get("Staffs"), new TypeToken<List<String>>() {}.getType());
                StaffManager.getInstance().getData().clear();
                StaffManager.getInstance().getData().addAll(staffs);
            }

            if (root.has("Macros")) {
                List<MacroManager.Macro> macros = GSON.fromJson(root.get("Macros"), new TypeToken<List<MacroManager.Macro>>() {}.getType());
                MacroManager.getInstance().getMacros().clear();
                if (macros != null) {
                    MacroManager.getInstance().getMacros().addAll(macros);
                }
            }

            if (root.has("Draggables")) {
                DraggableManager.getInstance().loadFromJson(root.getAsJsonObject("Draggables"));
            }
        } catch (JsonSyntaxException exception) {
            System.err.println("Failed to parse config file, file may be corrupted: " + exception.getMessage());
            exception.printStackTrace();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void save() {
        JsonObject root = UnifiedConfigStore.getInstance().readRoot();
        root.add("Friends", GSON.toJsonTree(FriendManager.getInstance().getData()));
        root.add("Staffs", GSON.toJsonTree(StaffManager.getInstance().getData()));
        root.add("Macros", GSON.toJsonTree(MacroManager.getInstance().getMacros()));
        root.add("Draggables", DraggableManager.getInstance().toJson());
        UnifiedConfigStore.getInstance().writeRoot(root);
    }
}
