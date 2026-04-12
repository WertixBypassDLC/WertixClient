package sweetie.nezi.client.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import sweetie.nezi.api.command.Command;
import sweetie.nezi.api.command.CommandRegister;
import sweetie.nezi.api.system.backend.KeyStorage;
import sweetie.nezi.api.system.configs.MacroManager;
import sweetie.nezi.client.features.commands.arguments.AnyStringArgument;
import sweetie.nezi.client.features.commands.arguments.StrictlyKeyArgument;
import sweetie.nezi.client.features.commands.arguments.StrictlyMacroNameArgument;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

@CommandRegister(name = "macro")
public class CommandMacro extends Command {
    private final MacroManager macroManager = MacroManager.getInstance();

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
        builder.then(literal("add").then(argument("name", new AnyStringArgument()).then(argument("key", new StrictlyKeyArgument()).then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String name = StringArgumentType.getString(context, "name");
            String keyName = StringArgumentType.getString(context, "key");
            String message = StringArgumentType.getString(context, "message");

            int keyCode = KeyStorage.getBind(keyName);
            if (keyCode == -1) {
                sendNotification("Клавиша " + keyName + " не найдена!");
                return 0;
            }

            if (macroManager.has(name)) {
                sendNotification("Макрос с таким именем уже существует!");
                return 0;
            }

            macroManager.add(name, message, keyCode);
            sendNotification("Добавлен макрос " + name + " (Кнопка: " + keyName + ")");
            return SINGLE_SUCCESS;
        })))));

        builder.then(literal("remove").then(argument("name", new StrictlyMacroNameArgument()).executes(context -> {
            String name = StringArgumentType.getString(context, "name");

            if (!macroManager.has(name)) {
                sendNotification("Макрос с таким именем не найден!");
                return 0;
            }

            macroManager.remove(name);
            sendNotification("Макрос " + name + " был успешно удален!");

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("clear").executes(context -> {
            macroManager.getMacros().clear();
            sendNotification("Все макросы были удалены.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(context -> {
            if (macroManager.getMacros().isEmpty()) {
                sendNotification("Список макросов пустой");
                return 0;
            }

            macroManager.getMacros().forEach(macro -> {
                sendNotification(macro.getName() + " -> " + macro.getMessage() + " [" + KeyStorage.getBind(macro.getKey()) + "]");
            });

            return SINGLE_SUCCESS;
        }));
    }
}