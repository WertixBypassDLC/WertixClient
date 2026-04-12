package sweetie.nezi.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import sweetie.nezi.api.command.Command;
import sweetie.nezi.api.command.CommandRegister;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.client.features.commands.arguments.AnyNameArgument;
import sweetie.nezi.client.features.commands.arguments.StrictlyFriendArgument;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

@CommandRegister(name = "friend")
public class CommandFriend extends Command {

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
        builder.then(literal("clear").executes(context -> {
            if (!FriendManager.getInstance().getSortedData().isEmpty()) {
                FriendManager.getInstance().clear();
                sendNotification("Friend list cleared.");
            } else {
                sendNotification("Friend list is empty.");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(context -> {
            if (FriendManager.getInstance().getSortedData().isEmpty()) {
                sendNotification("Friend list is empty.");
            } else {
                try {
                    int friendCount = FriendManager.getInstance().getSortedData().size();
                    String friends = String.join(", ", FriendManager.getInstance().getSortedData());
                    sendNotification("Friends (" + friendCount + "): " + friends);
                } catch (Exception e) {
                    sendNotification("Failed to read friend list.");
                }
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(argument("player", AnyNameArgument.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

            if (FriendManager.getInstance().contains(nickname)) {
                sendNotification(nickname + " is already in friends.");
            } else {
                FriendManager.getInstance().add(nickname);
                sendNotification(nickname + " added to friends.");
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("remove").then(argument("player", StrictlyFriendArgument.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

            if (!FriendManager.getInstance().contains(nickname)) {
                sendNotification(nickname + " is not in friends.");
            } else {
                FriendManager.getInstance().remove(nickname);
                sendNotification(nickname + " removed from friends.");
            }
            return SINGLE_SUCCESS;
        })));
    }
}
