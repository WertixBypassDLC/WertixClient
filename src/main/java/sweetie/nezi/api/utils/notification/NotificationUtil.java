package sweetie.nezi.api.utils.notification;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleManager;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class NotificationUtil implements QuickImports {
    @Getter
    private final List<Notif> notifs = new ArrayList<>();
    private final Map<String, Boolean> modStates = new HashMap<>();

    public void add(String text) {
        notifs.add(new Notif(text));
    }

    public void update() {
        notifs.removeIf(n -> n.alpha <= 0);
        notifs.forEach(Notif::update);
        checkMods();
    }

    private void checkMods() {
        NotifWidget widget = (NotifWidget) WidgetManager.getInstance().getWidgets().stream()
                .filter(w -> w instanceof NotifWidget)
                .findFirst()
                .orElse(null);

        if (widget == null || !widget.moduleState) return;

        for (Module m : ModuleManager.getInstance().getModules()) {
            String name = m.getName();
            boolean was = modStates.getOrDefault(name, m.isEnabled());
            boolean now = m.isEnabled();

            if (was != now) {
                add(name + (now ? " §aвключен" : " §cвыключен"));
                modStates.put(name, now);
            }
        }
    }

    public static class Notif {
        public final String text;
        private final long time;
        public float alpha = 0;
        public float scale = 0.8f;

        public Notif(String text) {
            this.text = text;
            this.time = System.currentTimeMillis();
        }

        public void update() {
            long elapsed = System.currentTimeMillis() - time;

            if (elapsed < 300) {
                float p = elapsed / 300f;
                alpha = p;
                scale = 0.8f + p * 0.2f;
            } else if (elapsed < 3700) {
                alpha = 1f;
                scale = 1f;
            } else if (elapsed < 4000) {
                float p = (elapsed - 3700) / 300f;
                alpha = 1f - p;
                scale = 1f - p * 0.2f;
            } else {
                alpha = 0;
            }
        }
    }
}
