package sweetie.nezi.client.features.modules.render.nametags;

import com.google.gson.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import sweetie.nezi.api.system.configs.UnifiedConfigStore;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * TimeHelper — рендерит таймеры (формат MM:SS) как nametag-визуализацию.
 * Привязан к WardenHelperModule. Поддерживает:
 * - Проверку наличия бочки/сундука в 2 блоках от таймера
 * - Красный таймер при 00:00 (не удаляется)
 * - Клиентский отсчёт при уходе далеко
 * - Сохранение таймеров per-server (по скорборду "Анархия X")
 * - Плавная очистка при отсутствии 5+ мин
 */
public class TimeHelper implements QuickImports {
    private static final TimeHelper INSTANCE = new TimeHelper();
    public static TimeHelper getInstance() { return INSTANCE; }

    private static final Pattern TIMER_PATTERN = Pattern.compile("\\d{1,3}:\\d{2}");
    private static final String STORE_KEY = "WardenTimers";
    private static final long ABSENCE_THRESHOLD_MS = 5 * 60 * 1000; // 5 минут
    private static final long CLEANUP_DURATION_MS = 30 * 1000; // 30 секунд

    // Ключ — позиция блока, значение — данные таймера
    private final Map<String, TimerData> trackedTimers = new ConcurrentHashMap<>();

    // Server tracking
    private String currentServer = null;
    private Vec3d lastPlayerPos = null;

    // Cleanup state
    private boolean cleanupActive = false;
    private long cleanupStartTime = 0;
    private List<String> cleanupQueue = null;
    private int cleanupInitialSize = 0;

    static class TimerData {
        Vec3d pos;
        int totalSeconds;
        long lastSeenRealTime;
        boolean visibleNow;

        TimerData(Vec3d pos, int totalSeconds) {
            this.pos = pos;
            this.totalSeconds = totalSeconds;
            this.lastSeenRealTime = System.currentTimeMillis();
            this.visibleNow = true;
        }

        int getClientCountedSeconds() {
            long elapsed = System.currentTimeMillis() - lastSeenRealTime;
            return Math.max(0, totalSeconds - (int)(elapsed / 1000));
        }

        String getDisplayTime() {
            int secs = getClientCountedSeconds();
            int min = secs / 60;
            int sec = secs % 60;
            return String.format("%d:%02d", min, sec);
        }
    }

    /** Вызывается из WardenHelperModule каждый кадр */
    public void onRender(DrawContext context) {
        if (mc.world == null || mc.player == null) return;

        // Обновляем текущий сервер и позицию
        String server = getServerIdentifier();
        if (server != null && !server.equals(currentServer)) {
            // Сменился сервер — сохраняем старый, загружаем новый
            if (currentServer != null) saveTimers();
            currentServer = server;
            loadTimers();
        }
        if (server != null) {
            lastPlayerPos = mc.player.getPos();
        }

        // Обрабатываем плавную очистку
        processCleanup();

        // Помечаем все таймеры как невидимые
        for (TimerData data : trackedTimers.values()) {
            data.visibleNow = false;
        }

        // Сканируем entity в мире на наличие таймеров
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity)) continue;

            String name = entity.getName().getString().trim();
            String clean = name.replaceAll("§[0-9a-fk-or]", "").trim();

            if (!TIMER_PATTERN.matcher(clean).matches()) continue;

            // Проверяем наличие бочки/сундука в 2 блоках
            if (!hasContainerNearby(entity.getPos())) continue;

            String[] parts = clean.split(":");
            if (parts.length != 2) continue;

            try {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                int totalSec = minutes * 60 + seconds;

                String key = blockKey(entity.getPos());

                TimerData existing = trackedTimers.get(key);
                if (existing != null) {
                    existing.pos = entity.getPos();
                    existing.totalSeconds = totalSec;
                    existing.lastSeenRealTime = System.currentTimeMillis();
                    existing.visibleNow = true;
                    // Если таймер обновился реальными данными — убираем из очистки
                    if (cleanupQueue != null) cleanupQueue.remove(key);
                } else {
                    trackedTimers.put(key, new TimerData(entity.getPos(), totalSec));
                }
            } catch (NumberFormatException ignored) {}
        }

        // НЕ удаляем таймеры на 0 — показываем красным
        // Рендерим все таймеры
        for (TimerData data : trackedTimers.values()) {
            renderTimer(data, context);
        }
    }

    private void renderTimer(TimerData data, DrawContext context) {
        MatrixStack ms = context.getMatrices();

        Vector2f projected = ProjectionUtil.project(data.pos.x, data.pos.y + 0.5, data.pos.z);
        if (projected == null || projected.x == Float.MAX_VALUE || projected.y == Float.MAX_VALUE) return;

        float px = projected.x;
        float py = projected.y;

        if (px < -200 || px > mc.getWindow().getScaledWidth() + 200 ||
            py < -200 || py > mc.getWindow().getScaledHeight() + 200) return;

        int secs = data.getClientCountedSeconds();
        boolean expired = secs <= 0;

        String timeText = data.getDisplayTime();
        String prefix = data.visibleNow ? "" : "~ ";
        String display = prefix + timeText;

        float fontSize = 8f;
        float gap = 2f;
        float textWidth = Fonts.PS_BOLD.getWidth(display, fontSize);
        float boxW = textWidth + gap * 2f;
        float boxH = fontSize + gap * 2f;
        float bx = px - boxW / 2f;
        float by = py - boxH;

        // Фон
        Color bgColor;
        if (expired) {
            bgColor = new Color(50, 5, 5, 160);
        } else if (data.visibleNow) {
            bgColor = new Color(10, 10, 10, 140);
        } else {
            bgColor = new Color(30, 15, 5, 140);
        }
        RenderUtil.RECT.draw(ms, bx, by, boxW, boxH, 2f, bgColor);

        // Текст
        Color textColor;
        if (expired) {
            // Пульсирующий красный для истёкших
            int alpha = (int)(180 + 75 * Math.abs(Math.sin(System.currentTimeMillis() * 0.004)));
            textColor = new Color(255, 40, 40, Math.min(255, alpha));
        } else if (data.visibleNow) {
            textColor = new Color(100, 255, 100);
        } else {
            textColor = new Color(255, 200, 80);
        }
        Fonts.PS_BOLD.drawText(ms, display, bx + gap, by + gap, fontSize, textColor);

        // Расстояние
        int dist = (int) mc.player.getPos().distanceTo(data.pos);
        String distStr = dist + "m";
        float distFS = 6f;
        float distW = Fonts.PS_MEDIUM.getWidth(distStr, distFS);
        Fonts.PS_MEDIUM.drawText(ms, distStr, px - distW / 2f, by + boxH + 1f, distFS, new Color(180, 180, 180));
    }

    /** Проверяет наличие бочки или сундука в радиусе 2 блоков */
    private boolean hasContainerNearby(Vec3d pos) {
        BlockPos center = BlockPos.ofFloored(pos);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = center.add(dx, dy, dz);
                    var state = mc.world.getBlockState(checkPos);
                    if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.BARREL) || state.isOf(Blocks.TRAPPED_CHEST)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Определяет сервер по первой строчке скорборда "Анархия X" */
    public String getServerIdentifier() {
        if (mc.world == null) return null;
        try {
            Scoreboard sb = mc.world.getScoreboard();
            ScoreboardObjective sidebar = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (sidebar != null) {
                String title = sidebar.getDisplayName().getString().replaceAll("§[0-9a-fk-or]", "").trim();
                if (title.contains("Анархия")) {
                    return title;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // --- Persistence ---

    /** Сохраняет таймеры текущего сервера */
    public void saveTimers() {
        if (currentServer == null || trackedTimers.isEmpty()) return;

        try {
            JsonObject root = loadRootJson();
            JsonObject servers = root.has("servers") ? root.getAsJsonObject("servers") : new JsonObject();

            JsonObject serverData = new JsonObject();
            serverData.addProperty("lastOnlineTime", System.currentTimeMillis());
            if (lastPlayerPos != null) {
                JsonObject posJson = new JsonObject();
                posJson.addProperty("x", lastPlayerPos.x);
                posJson.addProperty("y", lastPlayerPos.y);
                posJson.addProperty("z", lastPlayerPos.z);
                serverData.add("lastPlayerPos", posJson);
            }

            JsonArray timersArray = new JsonArray();
            for (Map.Entry<String, TimerData> entry : trackedTimers.entrySet()) {
                TimerData td = entry.getValue();
                if (td.getClientCountedSeconds() <= 0) continue; // Не сохраняем истёкшие
                JsonObject t = new JsonObject();
                t.addProperty("key", entry.getKey());
                t.addProperty("x", td.pos.x);
                t.addProperty("y", td.pos.y);
                t.addProperty("z", td.pos.z);
                t.addProperty("seconds", td.getClientCountedSeconds());
                t.addProperty("savedAt", System.currentTimeMillis());
                timersArray.add(t);
            }
            serverData.add("timers", timersArray);
            servers.add(currentServer, serverData);
            root.add("servers", servers);

            UnifiedConfigStore.getInstance().updateRoot(configRoot -> configRoot.add(STORE_KEY, root));
        } catch (Exception e) {
            System.err.println("[WardenHelper] Failed to save timers: " + e.getMessage());
        }
    }

    /** Загружает таймеры для текущего сервера */
    public void loadTimers() {
        trackedTimers.clear();
        cleanupActive = false;
        cleanupQueue = null;
        if (currentServer == null) return;

        try {
            JsonObject root = loadRootJson();
            if (!root.has("servers")) return;
            JsonObject servers = root.getAsJsonObject("servers");
            if (!servers.has(currentServer)) return;

            JsonObject serverData = servers.getAsJsonObject(currentServer);
            long lastOnline = serverData.has("lastOnlineTime") ? serverData.get("lastOnlineTime").getAsLong() : 0;
            long absenceMs = System.currentTimeMillis() - lastOnline;

            Vec3d savedPlayerPos = null;
            if (serverData.has("lastPlayerPos")) {
                JsonObject p = serverData.getAsJsonObject("lastPlayerPos");
                savedPlayerPos = new Vec3d(p.get("x").getAsDouble(), p.get("y").getAsDouble(), p.get("z").getAsDouble());
            }

            if (!serverData.has("timers")) return;
            JsonArray timersArray = serverData.getAsJsonArray("timers");

            for (JsonElement el : timersArray) {
                JsonObject t = el.getAsJsonObject();
                String key = t.get("key").getAsString();
                Vec3d pos = new Vec3d(t.get("x").getAsDouble(), t.get("y").getAsDouble(), t.get("z").getAsDouble());
                int savedSeconds = t.get("seconds").getAsInt();
                long savedAt = t.get("savedAt").getAsLong();

                // Вычитаем время offline
                long offlineSec = (System.currentTimeMillis() - savedAt) / 1000;
                int remainingSeconds = Math.max(0, savedSeconds - (int) offlineSec);

                TimerData td = new TimerData(pos, remainingSeconds);
                td.visibleNow = false;
                trackedTimers.put(key, td);
            }

            // Если отсутствовали 5+ минут — запускаем плавную очистку
            if (absenceMs >= ABSENCE_THRESHOLD_MS && !trackedTimers.isEmpty()) {
                startCleanup(savedPlayerPos);
            }
        } catch (Exception e) {
            System.err.println("[WardenHelper] Failed to load timers: " + e.getMessage());
        }
    }

    private void startCleanup(Vec3d fromPos) {
        cleanupActive = true;
        cleanupStartTime = System.currentTimeMillis();
        // Сортируем по расстоянию от последней позиции игрока — дальние первыми
        Vec3d sortPos = fromPos != null ? fromPos : (mc.player != null ? mc.player.getPos() : Vec3d.ZERO);
        cleanupQueue = new ArrayList<>(trackedTimers.keySet());
        cleanupQueue.sort((a, b) -> {
            TimerData da = trackedTimers.get(a);
            TimerData db = trackedTimers.get(b);
            if (da == null || db == null) return 0;
            return Double.compare(db.pos.squaredDistanceTo(sortPos), da.pos.squaredDistanceTo(sortPos));
        });
        cleanupInitialSize = cleanupQueue.size();
    }

    private void processCleanup() {
        if (!cleanupActive || cleanupQueue == null || cleanupQueue.isEmpty()) {
            cleanupActive = false;
            return;
        }

        long elapsed = System.currentTimeMillis() - cleanupStartTime;
        // Сколько таймеров должно быть удалено к текущему моменту
        int shouldBeRemoved = (int)((float) elapsed / CLEANUP_DURATION_MS * cleanupInitialSize);
        int currentRemoved = cleanupInitialSize - cleanupQueue.size();

        while (currentRemoved < shouldBeRemoved && !cleanupQueue.isEmpty()) {
            String key = cleanupQueue.remove(0);
            trackedTimers.remove(key);
            currentRemoved++;
        }

        if (cleanupQueue.isEmpty() || elapsed >= CLEANUP_DURATION_MS) {
            // Удаляем все оставшиеся в очереди
            for (String key : cleanupQueue) {
                trackedTimers.remove(key);
            }
            cleanupActive = false;
            cleanupQueue = null;
        }
    }

    /** Вызывается при выключении модуля или выходе с сервера */
    public void onDisable() {
        saveTimers();
        trackedTimers.clear();
        currentServer = null;
        cleanupActive = false;
    }

    private JsonObject loadRootJson() {
        return UnifiedConfigStore.getInstance().getObject(STORE_KEY);
    }

    private String blockKey(Vec3d pos) {
        return ((int) Math.floor(pos.x)) + "_" + ((int) Math.floor(pos.y)) + "_" + ((int) Math.floor(pos.z));
    }
}
