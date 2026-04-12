package sweetie.nezi.api.module.setting;

import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class StringSetting extends Setting<String> {

    public StringSetting(String name) {
        super(name);
        this.value = ""; // Значение по умолчанию
    }

    @Override
    public StringSetting value(String value) {
        setValue(value);
        return this;
    }

    @Override
    public void setValue(String value) {
        if (sameValue(value)) return;
        super.setValue(value == null ? "" : value);
        runAction();
    }

    @Override
    public StringSetting setVisible(Supplier<Boolean> condition) {
        return (StringSetting) super.setVisible(condition);
    }

    @Override
    public StringSetting onAction(Runnable action) {
        return (StringSetting) super.onAction(action);
    }

    // Удобные методы для проверок
    public boolean isEmpty() {
        return this.value == null || this.value.trim().isEmpty();
    }

    public String getText() {
        return this.value == null ? "" : this.value;
    }
}