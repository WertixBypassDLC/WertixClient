package sweetie.nezi;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.command.CommandManager;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleManager;
import sweetie.nezi.api.system.configs.ConfigManager;
import sweetie.nezi.api.system.configs.ConfigSkin;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.other.SoundUtil;
import sweetie.nezi.api.utils.render.KawaseBlurProgram;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;
import sweetie.nezi.client.services.HeartbeatService;
import sweetie.nezi.client.services.RenderService;
import sweetie.nezi.client.ui.clickgui.ScreenClickGUI;
import sweetie.nezi.client.ui.theme.ThemeEditor;
import sweetie.nezi.client.ui.widget.WidgetManager;

import java.util.HashMap;
import java.util.Map;

public class NeziClient implements ClientModInitializer {

    @Getter
    private static NeziClient instance = new NeziClient();

    @Getter
    private boolean clientActive = true;
    private long f12PressStartTime = 0;
    private boolean f12WasPressed = false;
    private final Map<Module, Boolean> moduleStatesBeforePanic = new HashMap<>();

    @Override
    public void onInitializeClient() {
        instance = this;
        QuickImports.bindMinecraftClient();

        SoundUtil.load();

        loadManagers();
        loadServices();

        // Загружаем конфиг avatar.json
        ConfigManager.getInstance().load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handlePanicKey();
        });
    }

    private void handlePanicKey() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        long windowHandle = client.getWindow().getHandle();

        boolean f12Pressed = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_F12) == GLFW.GLFW_PRESS;

        if (f12Pressed && !f12WasPressed) {
            f12PressStartTime = System.currentTimeMillis();
            f12WasPressed = true;
        } else if (f12Pressed && f12WasPressed) {
            if (f12PressStartTime > 0 && System.currentTimeMillis() - f12PressStartTime >= 3000) {
                toggleClientActive();
                f12PressStartTime = 0;
            }
        } else if (!f12Pressed && f12WasPressed) {
            f12PressStartTime = 0;
            f12WasPressed = false;
        }
    }

    private void toggleClientActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        clientActive = !clientActive;

        if (clientActive) {
            for (Map.Entry<Module, Boolean> entry : moduleStatesBeforePanic.entrySet()) {
                if (entry.getValue()) {
                    entry.getKey().setEnabled(true);
                }
            }
            moduleStatesBeforePanic.clear();
        } else {
            moduleStatesBeforePanic.clear();
            for (Module module : ModuleManager.getInstance().getModules()) {
                moduleStatesBeforePanic.put(module, module.isEnabled());
                if (module.isEnabled()) {
                    module.setEnabled(false);
                }
            }
            if (client != null && client.currentScreen instanceof ScreenClickGUI) {
                client.setScreen(null);
            }
        }
    }

    public void postLoad() {
        QuickImports.bindMinecraftClient();
        try {
            ModuleManager.getInstance().getModules().sort((a, b) -> Float.compare(
                    Fonts.PS_MEDIUM.getWidth(b.getName(), 7f),
                    Fonts.PS_MEDIUM.getWidth(a.getName(), 7f)
            ));
        } catch (Throwable throwable) {
            System.err.println("Failed to sort modules with custom font. Falling back to name length sort.");
            throwable.printStackTrace();
            ModuleManager.getInstance().getModules().sort((a, b) -> Integer.compare(b.getName().length(), a.getName().length()));
        }

        try {
            KawaseBlurProgram.load();
        } catch (Throwable throwable) {
            System.err.println("Failed to initialize Kawase blur program.");
            throwable.printStackTrace();
        }
    }

    private void loadManagers() {
        WidgetManager.getInstance().load();
        RotationManager.getInstance().load();
        ModuleManager.getInstance().load();
        CommandManager.getInstance().load();
        ThemeEditor.getInstance().load();
    }

    private void loadServices() {
        HeartbeatService.getInstance().load();
        RenderService.getInstance().load();
        ConfigSkin.getInstance().load();
    }

    public void onClose() {
        ConfigManager.getInstance().save();
    }
}
