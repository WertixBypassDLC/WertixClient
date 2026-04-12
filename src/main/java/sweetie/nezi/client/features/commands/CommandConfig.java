package sweetie.nezi.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import sweetie.nezi.api.command.Command;
import sweetie.nezi.api.command.CommandRegister;
import sweetie.nezi.api.system.backend.SharedClass;
import sweetie.nezi.api.system.configs.ConfigManager;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

@CommandRegister(name = "config")
public class CommandConfig extends Command {

    private void sendNotification(String text) {
        NotifWidget widget = (NotifWidget) WidgetManager.getInstance().getWidgets().stream()
                .filter(w -> w instanceof NotifWidget)
                .findFirst()
                .orElse(null);
        if (widget != null) {
            widget.addNotif(text);
        }
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("load")
                .executes(context -> {
                    ConfigManager.getInstance().load();
                    sendNotification("Конфигурация загружена.");
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("save")
                .executes(context -> {
                    ConfigManager.getInstance().save();
                    sendNotification("Конфигурация сохранена.");
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("folder").executes(context -> {
            if (SharedClass.openFolder(FabricLoader.getInstance().getConfigDir().toString())) {
                sendNotification("Открываю папку с конфигом...");
            } else {
                sendNotification("Не удалось открыть папку.");
            }
            return SINGLE_SUCCESS;
        }));
    }
}
