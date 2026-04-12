package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.system.draggable.DraggableManager;
import sweetie.nezi.client.ui.clickgui.ScreenClickGUI;

@ModuleRegister(name = "Click GUI", category = Category.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGUIModule extends Module {
    @Getter private static final ClickGUIModule instance = new ClickGUIModule();

    public ClickGUIModule() {
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen instanceof ScreenClickGUI) return;

        DraggableManager.getInstance().releaseAll();
        mc.setScreen(null);
        mc.setScreen(ScreenClickGUI.getInstance());
    }

    @Override
    public void onEvent() {
        toggle();
    }

}
