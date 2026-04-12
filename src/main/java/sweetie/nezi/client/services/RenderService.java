package sweetie.nezi.client.services;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.other.WindowResizeEvent;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.client.features.modules.render.InterfaceModule;

@Getter
public class RenderService implements QuickImports {
    @Getter private static final RenderService instance = new RenderService();

    public static final float HUD_LAYOUT_SCALE = 0.7f;
    public static final float CLICKGUI_LAYOUT_SCALE = 0.9f;

    @Setter private float scale = 1.0f;

    private final Listener<Render2DEvent.Render2DEventData> renderListener;
    private boolean registered;

    public RenderService() {
        this.renderListener = new Listener<>(event -> updateScale());
    }

    public void load() {
        register();
        updateScale();
        WindowResizeEvent.getInstance().subscribe(new Listener<>(event -> {
            updateScale();
            register();
        }));
    }

    private void register() {
        if (registered) {
            return;
        }
        Render2DEvent.getInstance().subscribe(renderListener);
        registered = true;
    }

    public float scaled(float value) {
        return value * scale;
    }

    public void updateScale() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        float w = client.getWindow().getScaledWidth();
        float h = client.getWindow().getScaledHeight();
        float bW = 1366f / 2f;
        float bH = 768f / 2f;
        float newScale = Math.max(w / bW, h / bH) * InterfaceModule.getScale();

        if (Math.abs(scale - newScale) < 0.001f) {
            scale = newScale;
            return;
        }

        scale = MathUtil.interpolate(scale, newScale, 0.15f);
    }
}
