package sweetie.nezi.api.module;

import lombok.Getter;
import lombok.Setter;
import sweetie.nezi.api.system.backend.Configurable;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.system.language.LanguageManager;
import sweetie.nezi.client.features.modules.other.ToggleSoundsModule;
import sweetie.nezi.client.features.modules.render.ClickGUIModule;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

@Getter
public abstract class Module extends Configurable implements QuickImports {
    private final String name;
    private final Category category;
    @Setter
    private int bind;

    private boolean enabled;

    public Module() {
        ModuleRegister data = getClass().getAnnotation(ModuleRegister.class);

        if (data == null) {
            throw new IllegalStateException("No data for " + getClass().getName());
        }

        this.name = data.name();
        this.category = data.category();
        this.bind = data.bind();
    }

    public boolean hasBind() {
        return bind != -999;
    }

    public String getLocalizedName() {
        return LanguageManager.getInstance().getLocalizedModuleName(name);
    }

    public String getDescription() {
        return LanguageManager.getInstance().getModuleDescription(name);
    }

    public void toggle() {
        setEnabled(!enabled, false, false);
    }

    public void toggle(boolean fromBind) {
        setEnabled(!enabled, false, fromBind);
    }

    public void setEnabled(boolean newState) {
        setEnabled(newState, false, false);
    }

    public void setEnabled(boolean newState, boolean config) {
        setEnabled(newState, config, false);
    }

    public void setEnabled(boolean newState, boolean config, boolean fromBind) {
        if (enabled == newState) {
            return;
        }

        try {
            enabled = newState;
            if (enabled) {
                onEnable();
                onEvent();
            } else {
                onDisable();
                removeAllEvents();
            }
        } catch (Throwable throwable) {
            enabled = false;
            try {
                removeAllEvents();
            } catch (Throwable ignored) {
            }
            throwable.printStackTrace();
            return;
        }

        if (config || this instanceof ClickGUIModule) {
            return;
        }

        ToggleSoundsModule.playToggle(newState);

        if (fromBind) {
            NotifWidget widget = (NotifWidget) WidgetManager.getInstance().getWidgets().stream()
                    .filter(w -> w instanceof NotifWidget)
                    .findFirst()
                    .orElse(null);

            if (widget != null && widget.moduleState) {
                String stateText = LanguageManager.getInstance().ui(
                        newState ? " §aвключен" : " §cвыключен",
                        newState ? " §aenabled" : " §cdisabled"
                );
                widget.addNotif(getLocalizedName() + stateText);
            }
        }
    }

    public abstract void onEvent();

    public void onEnable() {
    }

    public void onDisable() {
    }
}
