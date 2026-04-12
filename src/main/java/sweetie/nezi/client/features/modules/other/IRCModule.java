package sweetie.nezi.client.features.modules.other;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BindSetting;
import sweetie.nezi.api.module.setting.StringSetting;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@ModuleRegister(name = "IRC", category = Category.OTHER)
public class IRCModule extends Module {
    @Getter private static final IRCModule instance = new IRCModule();

    private static final String SERVER_HOST = "34.118.107.215";
    private static final int SERVER_PORT = 8080;

    // --- Настройки Модуля ---
    public final StringSetting customName = new StringSetting("Nickname").value("");
    public final BindSetting markKey = new BindSetting("Mark Key").value(-999);

    private static class MarkData {
        public final Vec3d pos;
        public final long timestamp;

        public MarkData(Vec3d pos) {
            this.pos = pos;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final Map<String, MarkData> activeMarks = new ConcurrentHashMap<>();
    private static final Identifier CROWN_ICON = Identifier.of("nezi", "images/pointers/crown.png");

    private boolean connected = false;
    private Thread receiveThread;
    @Getter private String username;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private final Random random = new Random();
    private boolean wasMarkKeyPressed = false;

    private final String[] NICKNAMES = {"Alex", "Steve", "Hero", "Ninja", "Shadow", "Dragon", "Phoenix", "Wolf", "Tiger"};

    public IRCModule() {
        addSettings(customName, markKey);

        // Магия тут: когда ты меняешь ник в ClickGUI, он сразу обновляется на сервере
        customName.onAction(() -> {
            if (connected) {
                String newName = customName.isEmpty() ? (NICKNAMES[random.nextInt(NICKNAMES.length)] + (random.nextInt(900) + 100)) : customName.getText();
                if (!newName.equals(username)) {
                    setUsername(newName);
                }
            }
        });
    }

    // Метод для живой смены ника
    public void setUsername(String newName) {
        if (connected && socket != null && !socket.isClosed()) {
            sendToServer("LEAVE:" + this.username + ":сменил ник на " + newName);
            this.username = newName;
            sendToServer("JOIN:" + this.username + ":присоединился");
            sendNotification("§aНик изменен на: " + newName);
        } else {
            this.username = newName;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        connect();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        disconnect();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            long now = System.currentTimeMillis();
            activeMarks.entrySet().removeIf(entry -> now - entry.getValue().timestamp > 60000);

            int key = markKey.getValue();
            if (key != -999 && mc.currentScreen == null) {
                boolean isPressed = GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;

                if (isPressed && !wasMarkKeyPressed) {
                    if (activeMarks.containsKey(username)) {
                        removeMark();
                    } else {
                        Vec3d headPos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
                        sendMark(headPos.x, headPos.y, headPos.z);
                    }
                }
                wasMarkKeyPressed = isPressed;
            }
        }));

        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            renderMarks(event.context());
        }));

        addEvents(updateEvent, render2DEvent);
    }

    private void renderMarks(DrawContext context) {
        if (mc.player == null || activeMarks.isEmpty()) return;
        MatrixStack ms = context.getMatrices();

        for (Map.Entry<String, MarkData> entry : activeMarks.entrySet()) {
            String markOwner = entry.getKey();
            Vec3d pos = entry.getValue().pos;

            Vector2f projected = ProjectionUtil.project(pos);
            float px = projected.x;
            float py = projected.y;

            if (px > 0 && px < mc.getWindow().getScaledWidth() && py > 0 && py < mc.getWindow().getScaledHeight()) {
                int dist = (int) mc.player.getPos().distanceTo(pos);
                String distText = dist + "m";

                float fontSize = 7f;
                float nameWidth = Fonts.PS_MEDIUM.getWidth(markOwner, fontSize);
                float distWidth = Fonts.PS_MEDIUM.getWidth(distText, fontSize);

                Fonts.PS_MEDIUM.drawText(ms, markOwner, px - nameWidth / 2f + 5f, py - 5f, fontSize, UIColors.textColor());
                Fonts.PS_MEDIUM.drawText(ms, distText, px - distWidth / 2f, py + 3f, fontSize, new Color(200, 200, 200));

                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.enableBlend();
                int iconSize = 8;
                int drawX = (int) (px - nameWidth / 2f - iconSize + 2);
                int drawY = (int) (py - 6);

                context.drawTexture(RenderLayer::getGuiTextured, CROWN_ICON, drawX, drawY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            }
        }
    }

    private void sendNotification(String text) {
        NotifWidget widget = (NotifWidget) WidgetManager.getInstance().getWidgets().stream()
                .filter(w -> w instanceof NotifWidget)
                .findFirst()
                .orElse(null);
        if (widget != null) {
            widget.addNotif(text);
        }
    }

    private void connect() {
        try {
            if (customName.isEmpty()) {
                username = NICKNAMES[random.nextInt(NICKNAMES.length)] + (random.nextInt(900) + 100);
            } else {
                username = customName.getText();
            }

            serverAddress = InetAddress.getByName(SERVER_HOST);
            socket = new DatagramSocket();
            connected = true;

            sendNotification("IRC Активирован. Ник: " + username);
            sendToServer("JOIN:" + username + ":присоединился");
            startReceiving();

        } catch (Exception e) {
            sendNotification("Ошибка подключения к IRC!");
            toggle();
        }
    }

    private void disconnect() {
        connected = false;
        activeMarks.clear();
        if (socket != null && !socket.isClosed()) {
            if (username != null) sendToServer("LEAVE:" + username + ":покинул");
            socket.close();
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        sendNotification("IRC Деактивирован");
    }

    private void startReceiving() {
        receiveThread = new Thread(() -> {
            while (connected && socket != null && !socket.isClosed()) {
                try {
                    byte[] buffer = new byte[2048];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    handleReceivedMessage(received);
                } catch (SocketException e) {
                    break;
                } catch (Exception ignored) {}
            }
        }, "EvaWare-IRC-Thread");
        receiveThread.start();
    }

    private void handleReceivedMessage(String message) {
        try {
            // Убираем лимит ,3 чтобы видеть все части координат
            String[] parts = message.split(":");
            if (parts.length < 3) return;

            String type = parts[0];
            String sender = parts[1];

            if (sender.equals(username)) return;

            switch (type) {
                case "MSG" -> {
                    // Собираем сообщение обратно, если в нем были двоеточия
                    StringBuilder content = new StringBuilder();
                    for (int i = 2; i < parts.length; i++) {
                        content.append(parts[i]).append(i == parts.length - 1 ? "" : ":");
                    }
                    if (mc.player != null) mc.player.sendMessage(Text.literal("§9[IRC] §f" + sender + "§7: §f" + content), false);
                }
                case "MARK" -> {
                    if (parts.length >= 5) {
                        try {
                            double x = Double.parseDouble(parts[2]);
                            double y = Double.parseDouble(parts[3]);
                            double z = Double.parseDouble(parts[4]);
                            activeMarks.put(sender, new MarkData(new Vec3d(x, y, z)));
                            sendNotification("§e" + sender + " поставил метку!");

                            // ДОБАВЬ ЭТО ДЛЯ ТЕСТА:
                            if (mc.player != null) {
                                mc.player.sendMessage(Text.literal("§7[IRC Debug] Получена метка от " + sender), false);
                            }
                        } catch (NumberFormatException e) {
                            // ...
                        }
                    }
                }
                case "UNMARK" -> {
                    activeMarks.remove(sender);
                    sendNotification("§c" + sender + " удалил метку!");
                }
                case "JOIN" -> sendNotification(sender + " присоединился");
                case "LEAVE" -> {
                    activeMarks.remove(sender);
                    sendNotification(sender + " покинул чат");
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Теперь мы увидим ошибку в консоли игры, если что-то пойдет не так
        }
    }

    public void sendMark(double x, double y, double z) {
        if (!connected) return;
        sendToServer("MARK:" + username + ":" + x + ":" + y + ":" + z);
        activeMarks.put(username, new MarkData(new Vec3d(x, y, z)));
        sendNotification("§aМетка успешно установлена!");
    }

    public void removeMark() {
        if (!connected) return;
        sendToServer("UNMARK:" + username + ":удалил");
        activeMarks.remove(username);
        sendNotification("§cВаша метка удалена!");
    }

    public void sendMessage(String message) {
        if (!connected || socket == null || socket.isClosed()) {
            sendNotification("IRC не активирован! Включите модуль.");
            return;
        }
        if (message.isEmpty()) return;

        try {
            sendToServer("MSG:" + username + ":" + message);
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§9[IRC] §f" + username + "§7: §f" + message), false);
            }
        } catch (Exception e) {
            sendNotification("Ошибка отправки: " + e.getMessage());
        }
    }

    private void sendToServer(String data) {
        new Thread(() -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    byte[] buffer = data.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_PORT);
                    socket.send(packet);
                }
            } catch (Exception ignored) {}
        }).start();
    }
}