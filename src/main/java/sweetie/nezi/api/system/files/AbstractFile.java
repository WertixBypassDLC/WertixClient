package sweetie.nezi.api.system.files;

import sweetie.nezi.api.system.backend.ClientInfo;
import java.util.ArrayList;
import java.util.List;

// Этот класс теперь просто контейнер данных, сохранение идет через FileManager
public abstract class AbstractFile {
    private List<String> data = new ArrayList<>();

    public abstract String fileName(); // Оставляем для совместимости, но не используем

    public void save() {
        // Переадресация на общий менеджер
        FileManager.getInstance().save();
    }

    public void load() {
        // Пусто, загрузка идет из FileManager
    }

    public void add(String value) {
        if (value != null && !value.trim().isEmpty() && !data.contains(value)) {
            data.add(value);
            save();
        }
    }

    public boolean remove(String value) {
        boolean b = data.remove(value);
        if (b) save();
        return b;
    }

    public List<String> getData() {
        return data; // Возвращаем ссылку на лист, чтобы FileManager мог его заполнять
    }

    public void clear() {
        data.clear();
        save();
    }

    public boolean contains(String value) {
        return data.contains(value);
    }

    public int size() {
        return data.size();
    }
}