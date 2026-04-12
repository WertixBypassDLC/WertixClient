package sweetie.nezi.api.system.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleManager;
import sweetie.nezi.api.module.setting.BindSetting;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ColorSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.module.setting.MultiModeSetting;
import sweetie.nezi.api.module.setting.Setting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.module.setting.StringSetting;
import sweetie.nezi.api.system.draggable.DraggableManager;
import sweetie.nezi.client.ui.theme.ThemeEditor;
import sweetie.nezi.client.ui.widget.Widget;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;
import sweetie.nezi.client.ui.widget.overlay.WatermarkWidget;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

public class ConfigManager {
    @Getter
    private static final ConfigManager instance = new ConfigManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public List<String> getConfigsNames() {
        return Collections.singletonList("cullleaves");
    }

    public void save() {
        JsonObject root = UnifiedConfigStore.getInstance().readRoot();

        JsonObject modulesJson = new JsonObject();
        for (Module module : ModuleManager.getInstance().getModules()) {
            modulesJson.add(module.getName(), serializeModule(module));
        }
        root.add("Modules", modulesJson);

        root.add("Friends", GSON.toJsonTree(FriendManager.getInstance().getData()));
        root.add("Staffs", GSON.toJsonTree(StaffManager.getInstance().getData()));
        root.add("Macros", GSON.toJsonTree(MacroManager.getInstance().getMacros()));
        root.add("Draggables", DraggableManager.getInstance().toJson());

        if (ThemeEditor.getInstance().getCurrentTheme() != null) {
            root.addProperty("SelectedTheme", ThemeEditor.getInstance().getCurrentTheme().getName());
        }

        JsonObject widgetsJson = new JsonObject();
        for (Widget widget : WidgetManager.getInstance().getWidgets()) {
            if (widget instanceof WatermarkWidget watermarkWidget) {
                JsonObject object = new JsonObject();
                object.addProperty("showName", watermarkWidget.showName);
                object.addProperty("showFps", watermarkWidget.showFps);
                object.addProperty("showXYZ", watermarkWidget.showXYZ);
                object.addProperty("showServer", watermarkWidget.showServer);
                widgetsJson.add("Watermark", object);
            } else if (widget instanceof NotifWidget notifWidget) {
                JsonObject object = new JsonObject();
                object.addProperty("specRequest", notifWidget.specRequest);
                object.addProperty("moduleState", notifWidget.moduleState);
                object.addProperty("lowDurability", notifWidget.lowDurability);
                widgetsJson.add("Notif", object);
            }
        }
        root.add("WidgetSettings", widgetsJson);

        UnifiedConfigStore.getInstance().writeRoot(root);
        ThemeManager.getInstance().saveAll();
    }

    public void load() {
        try {
            JsonObject root = UnifiedConfigStore.getInstance().readRoot();
            if (root == null || root.size() == 0) {
                return;
            }

            if (root.has("Modules")) {
                JsonObject modulesJson = root.getAsJsonObject("Modules");
                for (Module module : ModuleManager.getInstance().getModules()) {
                    if (modulesJson.has(module.getName())) {
                        applyConfigToModule(module, modulesJson.getAsJsonObject(module.getName()));
                    }
                }
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

            if (root.has("WidgetSettings")) {
                JsonObject widgetsJson = root.getAsJsonObject("WidgetSettings");
                for (Widget widget : WidgetManager.getInstance().getWidgets()) {
                    if (widget instanceof WatermarkWidget watermarkWidget && widgetsJson.has("Watermark")) {
                        JsonObject object = widgetsJson.getAsJsonObject("Watermark");
                        if (object.has("showName")) watermarkWidget.showName = object.get("showName").getAsBoolean();
                        if (object.has("showFps")) watermarkWidget.showFps = object.get("showFps").getAsBoolean();
                        if (object.has("showXYZ")) watermarkWidget.showXYZ = object.get("showXYZ").getAsBoolean();
                        if (object.has("showServer")) watermarkWidget.showServer = object.get("showServer").getAsBoolean();
                    } else if (widget instanceof NotifWidget notifWidget && widgetsJson.has("Notif")) {
                        JsonObject object = widgetsJson.getAsJsonObject("Notif");
                        if (object.has("specRequest")) notifWidget.specRequest = object.get("specRequest").getAsBoolean();
                        if (object.has("moduleState")) notifWidget.moduleState = object.get("moduleState").getAsBoolean();
                        if (object.has("lowDurability")) notifWidget.lowDurability = object.get("lowDurability").getAsBoolean();
                    }
                }
            }
        } catch (JsonSyntaxException exception) {
            System.err.println("Failed to parse config file, file may be corrupted: " + exception.getMessage());
            exception.printStackTrace();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private JsonObject serializeModule(Module module) {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", module.isEnabled());
        json.addProperty("bind", module.getBind());
        JsonObject settings = new JsonObject();
        module.getSettings().forEach(setting -> {
            JsonElement value = serializeSetting(setting);
            if (value != null) {
                settings.add(setting.getName(), value);
            }
        });
        json.add("settings", settings);
        return json;
    }

    private void applyConfigToModule(Module module, JsonObject json) {
        if (json.has("enabled")) module.setEnabled(json.get("enabled").getAsBoolean(), true);
        if (json.has("bind")) module.setBind(json.get("bind").getAsInt());
        if (json.has("settings")) {
            JsonObject settings = json.getAsJsonObject("settings");
            module.getSettings().forEach(setting -> {
                if (settings.has(setting.getName())) {
                    deserializeSetting(setting, settings.get(setting.getName()));
                }
            });
        }
    }

    private JsonElement serializeSetting(Setting<?> setting) {
        if (setting instanceof BooleanSetting booleanSetting) return GSON.toJsonTree(booleanSetting.getValue());
        if (setting instanceof ModeSetting modeSetting) return GSON.toJsonTree(modeSetting.getValue());
        if (setting instanceof SliderSetting sliderSetting) return GSON.toJsonTree(sliderSetting.getValue());
        if (setting instanceof BindSetting bindSetting) return GSON.toJsonTree(bindSetting.getValue());
        if (setting instanceof StringSetting stringSetting) return GSON.toJsonTree(stringSetting.getValue());
        if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
            JsonObject object = new JsonObject();
            multiBooleanSetting.getValue().forEach(booleanSetting ->
                    object.addProperty(booleanSetting.getName(), booleanSetting.getValue()));
            return object;
        }
        if (setting instanceof MultiModeSetting multiModeSetting) {
            return GSON.toJsonTree(multiModeSetting.getValue());
        }
        if (setting instanceof ColorSetting colorSetting) {
            Color color = colorSetting.getValue();
            JsonObject object = new JsonObject();
            object.addProperty("r", color.getRed());
            object.addProperty("g", color.getGreen());
            object.addProperty("b", color.getBlue());
            object.addProperty("a", color.getAlpha());
            return object;
        }
        return null;
    }

    private void deserializeSetting(Setting<?> setting, JsonElement element) {
        try {
            if (setting instanceof BooleanSetting booleanSetting) booleanSetting.setValue(element.getAsBoolean());
            else if (setting instanceof ModeSetting modeSetting) modeSetting.setValue(element.getAsString());
            else if (setting instanceof SliderSetting sliderSetting) sliderSetting.setValue(element.getAsFloat());
            else if (setting instanceof BindSetting bindSetting) bindSetting.setValue(element.getAsInt());
            else if (setting instanceof StringSetting stringSetting) stringSetting.setValue(element.getAsString());
            else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
                JsonObject object = element.getAsJsonObject();
                multiBooleanSetting.getValue().forEach(booleanSetting -> {
                    if (object.has(booleanSetting.getName())) {
                        booleanSetting.setValue(object.get(booleanSetting.getName()).getAsBoolean());
                    }
                });
            } else if (setting instanceof MultiModeSetting multiModeSetting) {
                List<String> list = GSON.fromJson(element, new TypeToken<List<String>>() {}.getType());
                if (list != null) {
                    multiModeSetting.getValue().clear();
                    multiModeSetting.getValue().addAll(list);
                }
            } else if (setting instanceof ColorSetting colorSetting) {
                JsonObject object = element.getAsJsonObject();
                colorSetting.setValue(new Color(
                        object.get("r").getAsInt(),
                        object.get("g").getAsInt(),
                        object.get("b").getAsInt(),
                        object.get("a").getAsInt()
                ));
            }
        } catch (Exception ignored) {
        }
    }
}
