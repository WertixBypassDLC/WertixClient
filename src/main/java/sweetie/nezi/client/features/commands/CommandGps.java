package sweetie.nezi.client.features.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import org.joml.Vector2i;
import sweetie.nezi.api.command.Command;
import sweetie.nezi.api.command.CommandRegister;
import sweetie.nezi.api.system.client.GpsManager;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

@CommandRegister(name = "gps")
public class CommandGps extends Command {

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
        builder.then(literal("off").executes(context -> {
            if (GpsManager.getInstance().getGpsPosition() == null) {
                sendNotification("А чё ты выключить собираешься?");
            } else {
                GpsManager.getInstance().setGpsPosition(null);
                sendNotification("Маршрут успешно удален.");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(argument("x", IntegerArgumentType.integer()).then(argument("z", IntegerArgumentType.integer()).executes(context -> {
            int x = IntegerArgumentType.getInteger(context, "x");
            int z = IntegerArgumentType.getInteger(context, "z");

            if (x == 0 && z == 0) {
                sendNotification("Нельзя указать маршрут на нулевые координаты.");
            } else {
                Vector2i newPos = new Vector2i(x, z);

                if (GpsManager.getInstance().getGpsPosition() != null && GpsManager.getInstance().getGpsPosition().equals(newPos)) {
                    sendNotification("Такая точка уже есть в маршруте.");
                } else {
                    GpsManager.getInstance().setGpsPosition(newPos);
                    sendNotification("Установлен маршрут: " + newPos.x + " " + newPos.y);
                }
            }
            return SINGLE_SUCCESS;
        }))));
    }
}