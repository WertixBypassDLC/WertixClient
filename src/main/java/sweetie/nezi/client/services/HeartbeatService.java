package sweetie.nezi.client.services;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.KeyEvent;
import sweetie.nezi.api.event.events.other.ScreenEvent;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.ModuleManager;
import sweetie.nezi.api.system.client.GpsManager;
import sweetie.nezi.api.system.configs.MacroManager;
import sweetie.nezi.api.system.draggable.DraggableManager;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.other.ScreenUtil;
import sweetie.nezi.api.utils.other.SlownessManager;
import sweetie.nezi.client.ui.clickgui.ScreenClickGUI;

public class HeartbeatService implements QuickImports {
    @Getter private static final HeartbeatService instance = new HeartbeatService();

    public void load() {
        keyEvent();
        render2dEvent();
        tickEvent();
        screenEvent();
    }

    private void screenEvent() {
        ScreenEvent.getInstance().subscribe(new Listener<>(event -> {
            ScreenUtil.drawButton(event);
        }));
    }

    private void tickEvent() {
        TickEvent.getInstance().subscribe(new Listener<>(event -> {
            SlownessManager.tick();
        }));
    }

    private void render2dEvent() {
        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
                    if (draggable.getModule().isEnabled()) {
                        draggable.onDraw();
                    }
                });
            }

            GpsManager.getInstance().update(event.context());
        }));
    }

    private void keyEvent() {
        KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != 1 || event.key() == -999 || event.key() == -1) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;

            int key = event.key() + (event.mouse() ? -100 : 0);
            boolean allowModuleBinds = client.currentScreen == null
                    || (client.currentScreen instanceof ScreenClickGUI clickGUI && !event.mouse() && !clickGUI.blocksModuleBinds());

            if (allowModuleBinds) {
                ModuleManager.getInstance().getModules().forEach(module -> {
                    int bind = module.getBind();
                    if (bind == key && module.hasBind()) {
                        module.toggle(true);
                    }
                });
            }

            if (client.currentScreen == null) {
                MacroManager.getInstance().onKeyPressed(key);
            }
        }));
    }
}