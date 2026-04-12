package sweetie.nezi.api.system.language;

import lombok.Getter;
import sweetie.nezi.api.module.Category;

import java.util.HashMap;
import java.util.Map;

@Getter
public class LanguageManager {
    @Getter
    private static final LanguageManager instance = new LanguageManager();

    private static final Map<String, String> CLICK_GUI_TEXT = new HashMap<>();

    static {
        CLICK_GUI_TEXT.put("Авто /ah", "Auto /ah");
        CLICK_GUI_TEXT.put("Атака", "Attack");
        CLICK_GUI_TEXT.put("Без задержки прыжка", "No jump delay");
        CLICK_GUI_TEXT.put("Божья аура", "God aura");
        CLICK_GUI_TEXT.put("Быстрая установка", "Fast place");
        CLICK_GUI_TEXT.put("Быстрые опытки", "Fast XP");
        CLICK_GUI_TEXT.put("Виджеты", "Widgets");
        CLICK_GUI_TEXT.put("Второй предмет", "Second item");
        CLICK_GUI_TEXT.put("Дезорентация", "Disorientation");
        CLICK_GUI_TEXT.put("Друзья", "Friends");
        CLICK_GUI_TEXT.put("Забирать из хранилища", "Loot storage");
        CLICK_GUI_TEXT.put("Задержка атаки", "Attack delay");
        CLICK_GUI_TEXT.put("Задержка использования", "Use delay");
        CLICK_GUI_TEXT.put("Задержка опыток", "XP delay");
        CLICK_GUI_TEXT.put("Задержка продажи", "Sell delay");
        CLICK_GUI_TEXT.put("Задержка установки", "Place delay");
        CLICK_GUI_TEXT.put("Использование", "Use");
        CLICK_GUI_TEXT.put("Кнопка перки", "Perk key");
        CLICK_GUI_TEXT.put("Кнопка свапа", "Swap key");
        CLICK_GUI_TEXT.put("Кнопка фейерверка", "Firework key");
        CLICK_GUI_TEXT.put("Команда анархии", "Anarchy command");
        CLICK_GUI_TEXT.put("Команда аукциона", "Auction command");
        CLICK_GUI_TEXT.put("Масштаб", "Scale");
        CLICK_GUI_TEXT.put("Медленное обновление", "Slow refresh");
        CLICK_GUI_TEXT.put("Ник", "Nickname");
        CLICK_GUI_TEXT.put("Обновление страниц", "Page refresh");
        CLICK_GUI_TEXT.put("Огненный смерч", "Fire vortex");
        CLICK_GUI_TEXT.put("Открыть меню", "Open menu");
        CLICK_GUI_TEXT.put("Первый предмет", "First item");
        CLICK_GUI_TEXT.put("Плавность", "Smoothness");
        CLICK_GUI_TEXT.put("Пласт", "Plate");
        CLICK_GUI_TEXT.put("Размер HUD", "HUD size");
        CLICK_GUI_TEXT.put("Таймер", "Timer");
        CLICK_GUI_TEXT.put("Темы", "Themes");
        CLICK_GUI_TEXT.put("Трапка", "Trapka");
        CLICK_GUI_TEXT.put("Частицы HUD", "HUD particles");
        CLICK_GUI_TEXT.put("Явная пыль", "Visible dust");
    }

    private ClientLanguage currentLanguage = ClientLanguage.RUSSIAN;

    private LanguageManager() {
    }

    public void setCurrentLanguage(ClientLanguage language) {
        currentLanguage = language == null ? ClientLanguage.RUSSIAN : language;
    }

    public boolean isRussian() {
        return currentLanguage == ClientLanguage.RUSSIAN;
    }

    public String ui(String russian, String english) {
        return currentLanguage == ClientLanguage.RUSSIAN ? russian : english;
    }

    public String getLocalizedModuleName(String englishName) {
        return ModuleTextRegistry.getLocalizedName(englishName, currentLanguage);
    }

    public String getModuleDescription(String englishName) {
        return ModuleTextRegistry.getDescription(englishName, currentLanguage);
    }

    public String getClickGuiText(String raw) {
        return CLICK_GUI_TEXT.getOrDefault(raw, raw);
    }

    public String getCategoryLabel(Category category) {
        return currentLanguage == ClientLanguage.RUSSIAN ? category.getRussianLabel() : category.getEnglishLabel();
    }

    public String getClickGuiCategoryLabel(Category category) {
        return category.getEnglishLabel();
    }
}
