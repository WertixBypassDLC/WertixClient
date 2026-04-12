package sweetie.nezi.api.system.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import sweetie.nezi.client.ui.theme.Theme;
import sweetie.nezi.client.ui.theme.ThemeEditor;
import sweetie.nezi.client.ui.theme.ThemeSelectable;
import sweetie.nezi.client.ui.theme.basic.BlueTheme;
import sweetie.nezi.client.ui.theme.basic.CandyLoveTheme;
import sweetie.nezi.client.ui.theme.basic.CrimsonTheme;

import java.awt.Color;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ThemeManager {
    @Getter
    private static final ThemeManager instance = new ThemeManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class ThemeStore {
        List<ThemeEntry> themes = new ArrayList<>();
        String selected;
    }

    private static final class ThemeEntry {
        String name;
        Map<String, Integer> colors = new HashMap<>();
    }

    private static final Type THEME_STORE_TYPE = new TypeToken<ThemeStore>() {}.getType();

    public void saveAll() {
        ThemeStore store = new ThemeStore();

        for (ThemeSelectable themeSelectable : ThemeEditor.getInstance().getThemeSelectables()) {
            if (themeSelectable.isRemoving()) {
                continue;
            }

            Theme theme = themeSelectable.getTheme();
            ThemeEntry entry = new ThemeEntry();
            entry.name = theme.getName();

            for (Theme.ElementColor elementColor : theme.getElementColors()) {
                entry.colors.put(elementColor.getName(), elementColor.getColor().getRGB());
            }

            store.themes.add(entry);
        }

        if (ThemeEditor.getInstance().getCurrentTheme() != null) {
            store.selected = ThemeEditor.getInstance().getCurrentTheme().getName();
        }

        UnifiedConfigStore.getInstance().updateRoot(root ->
                root.add("ThemesStore", GSON.toJsonTree(store, THEME_STORE_TYPE)));
    }

    public void save(Theme theme) {
        saveAll();
    }

    public void saveLastSelected(Theme theme) {
        saveAll();
    }

    public boolean remove(String themeName) {
        saveAll();
        return true;
    }

    public void refresh() {
        List<ThemeSelectable> selectables = ThemeEditor.getInstance().getThemeSelectables();
        selectables.clear();

        loadDefaultThemes();

        try {
            JsonElement section = UnifiedConfigStore.getInstance().get("ThemesStore");
            if (section == null || !section.isJsonObject()) {
                return;
            }

            ThemeStore store = GSON.fromJson(section, THEME_STORE_TYPE);
            if (store == null || store.themes == null) {
                return;
            }

            for (ThemeEntry entry : store.themes) {
                if (entry == null || entry.name == null) {
                    continue;
                }

                boolean exists = selectables.stream()
                        .anyMatch(themeSelectable -> themeSelectable.getTheme().getName().equals(entry.name));
                if (exists) {
                    continue;
                }

                Theme theme = new Theme(entry.name);
                if (entry.colors != null) {
                    for (Theme.ElementColor elementColor : theme.getElementColors()) {
                        Integer rgb = entry.colors.get(elementColor.getName());
                        if (rgb != null) {
                            elementColor.setColor(new Color(rgb, true));
                        }
                    }
                }

                selectables.add(new ThemeSelectable(theme, true));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public Theme loadLastSelected() {
        try {
            JsonElement section = UnifiedConfigStore.getInstance().get("ThemesStore");
            if (section == null || !section.isJsonObject()) {
                return null;
            }

            ThemeStore store = GSON.fromJson(section, THEME_STORE_TYPE);
            if (store == null || store.selected == null) {
                return null;
            }

            return ThemeEditor.getInstance().getThemeSelectables().stream()
                    .map(ThemeSelectable::getTheme)
                    .filter(theme -> theme.getName().equals(store.selected))
                    .findFirst()
                    .orElse(null);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    private void loadDefaultThemes() {
        Theme[] themes = new Theme[]{
                new Theme("EvaWare"),
                new BlueTheme().update(),
                new CandyLoveTheme().update(),
                new CrimsonTheme().update()
        };

        for (Theme theme : themes) {
            ThemeEditor.getInstance().getThemeSelectables().add(new ThemeSelectable(theme, true));
        }
    }

    public String safeFileName(String name) {
        return name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }
}
