package sweetie.nezi.client.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import sweetie.nezi.api.command.Command;
import sweetie.nezi.api.command.CommandRegister;
import sweetie.nezi.client.features.modules.other.IRCModule;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

@CommandRegister(name = "irc")
public class CommandIRC extends Command {

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

        builder.then(argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    String message = StringArgumentType.getString(context, "message");

                    if (!IRCModule.getInstance().isEnabled()) {
                        sendNotification("§cIRC модуль не активирован!");
                        return 0;
                    }

                    // сообщение
                    IRCModule.getInstance().sendMessage(message);
                    return SINGLE_SUCCESS;
                })
        );
    }
}