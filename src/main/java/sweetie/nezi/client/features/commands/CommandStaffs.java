package sweetie.nezi.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import sweetie.nezi.api.command.Command;
import sweetie.nezi.api.command.CommandRegister;
import sweetie.nezi.api.system.configs.StaffManager;
import sweetie.nezi.client.features.commands.arguments.AnyNameArgument;
import sweetie.nezi.client.features.commands.arguments.StrictlyStaffArgument;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

@CommandRegister(name = "staffs")
public class CommandStaffs extends Command {

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
            if (!StaffManager.getInstance().getData().isEmpty()) {
                StaffManager.getInstance().clear();
                sendNotification("Список лохов очищен.");
            } else {
                sendNotification("Список лохов пуст.");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(context -> {
            if (StaffManager.getInstance().getData().isEmpty()) {
                sendNotification("Список лохов пуст.");
            } else {
                try {
                    String staffs = String.join(", ", StaffManager.getInstance().getData());
                    sendNotification("Лохи: " + staffs);
                } catch (Exception e) {
                    sendNotification("Произошла ошибка при получении списка!");
                }
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(argument("player", AnyNameArgument.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

            if (StaffManager.getInstance().contains(nickname)) {
                sendNotification("Уже есть в списке лохов!");
            } else {
                StaffManager.getInstance().add(nickname);
                sendNotification(nickname + " добавлен в список лохов.");
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("remove").then(argument("player", StrictlyStaffArgument.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

            if (!StaffManager.getInstance().contains(nickname)) {
                sendNotification("Нет такого " + nickname + "!");
            } else {
                StaffManager.getInstance().remove(nickname);
                sendNotification(nickname + " удален из списка лохов.");
            }
            return SINGLE_SUCCESS;
        })));
    }
}