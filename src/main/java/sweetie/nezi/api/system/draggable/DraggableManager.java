package sweetie.nezi.api.system.draggable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import sweetie.nezi.api.module.Module;

import java.util.LinkedHashMap;
import java.util.Map;

public class DraggableManager {
    @Getter private static final DraggableManager instance = new DraggableManager();

    @Getter private final LinkedHashMap<String, Draggable> draggables = new LinkedHashMap<>();
    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public Draggable create(Module module, String name, float x, float y) {
        draggables.put(name, new Draggable(module, name, x, y));
        return draggables.get(name);
    }

    public void save() {
    }

    public void load() {
    }

    public void releaseAll() {
        draggables.values().forEach(draggable -> draggable.onRelease(0));
    }

    public JsonObject toJson() {
        return gson.toJsonTree(draggables).getAsJsonObject();
    }

    public void loadFromJson(JsonObject json) {
        Map<String, Draggable> loaded = gson.fromJson(json, new TypeToken<Map<String, Draggable>>() {}.getType());
        if (loaded == null) {
            return;
        }

        for (Map.Entry<String, Draggable> entry : loaded.entrySet()) {
            String name = entry.getKey();
            Draggable saved = entry.getValue();
            Draggable current = draggables.get(name);
            if (current == null || saved == null) {
                continue;
            }

            current.setX(saved.getX());
            current.setY(saved.getY());
        }
    }
}
