package sweetie.nezi.api.system.configs;

import lombok.Getter;
import sweetie.nezi.api.system.files.AbstractFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FriendManager extends AbstractFile {
    @Getter private static final FriendManager instance = new FriendManager();

    @Override
    public String fileName() {
        return "friends";
    }

    @Override
    public void add(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty() || contains(normalized)) {
            return;
        }

        getData().add(value.trim());
        save();
    }

    @Override
    public boolean remove(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return false;
        }

        boolean removed = getData().removeIf(entry -> normalize(entry).equals(normalized));
        if (removed) {
            save();
        }
        return removed;
    }

    @Override
    public boolean contains(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return false;
        }

        for (String entry : getData()) {
            if (normalize(entry).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getSortedData() {
        List<String> result = new ArrayList<>();
        for (String friend : getData()) {
            String cleaned = friend == null ? "" : friend.trim();
            if (cleaned.isEmpty() || containsIn(result, cleaned)) {
                continue;
            }
            result.add(cleaned);
        }

        result.sort(String::compareToIgnoreCase);
        return result;
    }

    private boolean containsIn(List<String> values, String value) {
        String normalized = normalize(value);
        for (String entry : values) {
            if (normalize(entry).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
