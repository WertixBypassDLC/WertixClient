package sweetie.nezi.api.module.setting;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;

@Getter
public class MultiModeSetting extends Setting<List<String>> {
    private final List<String> allModes = new ArrayList<>();

    public MultiModeSetting(String name) {
        super(name);
        this.value = new ArrayList<>();
    }

    public MultiModeSetting modes(String... modes) {
        allModes.addAll(Arrays.asList(modes));
        setValue(value);
        return this;
    }

    public MultiModeSetting selected(String... selected) {
        List<String> merged = new ArrayList<>(value);
        merged.addAll(Arrays.asList(selected));
        setValue(merged);
        return this;
    }

    @Override
    public MultiModeSetting value(List<String> value) {
        setValue(value);
        return this;
    }

    @Override
    public void setValue(List<String> value) {
        List<String> normalized = normalize(value);
        if (sameValue(normalized)) return;
        super.setValue(normalized);
        runAction();
    }

    public boolean isSelected(String mode) {
        return getSelectedModes().contains(mode);
    }

    public void toggle(String mode) {
        if (!allModes.contains(mode)) {
            return;
        }

        List<String> selectedModes = new ArrayList<>(getSelectedModes());
        if (selectedModes.contains(mode)) {
            selectedModes.remove(mode);
        } else {
            selectedModes.add(mode);
        }
        setValue(selectedModes);
    }

    public List<String> getSelectedModes() {
        return normalize(value);
    }

    public int getSelectedCount() {
        return getSelectedModes().size();
    }

    private List<String> normalize(List<String> modes) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (modes == null) {
            return new ArrayList<>();
        }

        for (String mode : modes) {
            if (mode != null && allModes.contains(mode)) {
                normalized.add(mode);
            }
        }
        return new ArrayList<>(normalized);
    }

    @Override
    public MultiModeSetting setVisible(Supplier<Boolean> condition) {
        return (MultiModeSetting) super.setVisible(condition);
    }

    @Override
    public MultiModeSetting onAction(Runnable action) {
        return (MultiModeSetting) super.onAction(action);
    }
}
