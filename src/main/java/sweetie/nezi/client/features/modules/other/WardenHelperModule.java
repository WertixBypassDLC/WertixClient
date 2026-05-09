package sweetie.nezi.client.features.modules.other;

import com.google.gson.*;
import lombok.Getter;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.system.configs.UnifiedConfigStore;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.render.BlockESPModule;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@ModuleRegister(name = "Warden Helper", category = Category.OTHER)
public class WardenHelperModule extends Module {
    @Getter private static final WardenHelperModule instance = new WardenHelperModule();

    private static final Pattern TIMER_PATTERN = Pattern.compile("\\d{1,3}:\\d{2}");
    private static final String STORE_KEY = "WardenTimers";
    private static final double TRACK_DISTANCE = 320.0;
    private static final double TRACK_DISTANCE_SQ = TRACK_DISTANCE * TRACK_DISTANCE;
    private static final int UNDERGROUND_RENDER_Y = -30;

    private final Map<String, TimerData> trackedTimers = new ConcurrentHashMap<>();

    private String currentServer = null;
    private Vec3d lastPlayerPos = null;
    private long serverJoinTime = 0L;
    private long lastScanTime = 0L;

    private static class TimerData {
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
            return String.format("%02d:%02d", min, sec);
        }
    }

    @Override
    public void onEvent() {
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            onRender(event.context());
        }));

        addEvents(render2DEvent);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        saveTimers();
        trackedTimers.clear();
        currentServer = null;
        serverJoinTime = 0L;
    }

    private void onRender(DrawContext context) {
        if (mc.world == null || mc.player == null) return;

        String server = getServerIdentifier();
        if (server != null && !server.equals(currentServer)) {
            if (currentServer != null) saveTimers();
            currentServer = server;
            serverJoinTime = System.currentTimeMillis();
            loadTimers();
        }
        if (server != null) {
            lastPlayerPos = mc.player.getPos();
        }

        for (TimerData data : trackedTimers.values()) {
            data.visibleNow = false;
        }

        if (mc.player.getY() >= UNDERGROUND_RENDER_Y) {
            return;
        }

        if (System.currentTimeMillis() - lastScanTime > 1000) {
            lastScanTime = System.currentTimeMillis();
            Box scanBox = mc.player.getBoundingBox().expand(TRACK_DISTANCE, 48.0, TRACK_DISTANCE);
            for (ArmorStandEntity entity : mc.world.getEntitiesByClass(ArmorStandEntity.class, scanBox, Entity::isAlive)) {
                if (mc.player.squaredDistanceTo(entity) > TRACK_DISTANCE_SQ) continue;

                String name = entity.getName().getString().trim();
                String clean = name.replaceAll("§[0-9a-fk-or]", "").trim();

                if (!TIMER_PATTERN.matcher(clean).matches()) continue;
                BlockPos containerPos = findNearestTimerContainer(entity.getPos());
                if (containerPos == null) continue;
                if (!shouldRenderUnderground(containerPos)) continue;

                String[] parts = clean.split(":");
                if (parts.length != 2) continue;

                try {
                    int minutes = Integer.parseInt(parts[0]);
                    int seconds = Integer.parseInt(parts[1]);
                    int totalSec = minutes * 60 + seconds;

                    String key = blockKey(Vec3d.ofCenter(containerPos));

                    TimerData existing = trackedTimers.get(key);
                    if (existing != null) {
                        existing.pos = entity.getPos();
                        existing.totalSeconds = totalSec;
                        existing.lastSeenRealTime = System.currentTimeMillis();
                        existing.visibleNow = true;
                    } else {
                        trackedTimers.put(key, new TimerData(entity.getPos(), totalSec));
                    }
                } catch (NumberFormatException ignored) {}
            }
            
            // Move ensureExpiredUndergroundTimers inside the 500ms block too, since it scans blocks!
            ensureExpiredUndergroundTimers();
        }

        // Remove timers that have been at 00:00 for more than 40 seconds
        trackedTimers.entrySet().removeIf(entry -> {
            TimerData data = entry.getValue();
            long zeroTime = data.lastSeenRealTime + data.totalSeconds * 1000L;
            return System.currentTimeMillis() > zeroTime + 40_000L;
        });

        BlockESPModule blockESP = BlockESPModule.getInstance();
        for (TimerData data : trackedTimers.values()) {
            if (blockESP.isEnabled() && blockESP.rendersWardenContainers()
                    && isHandledByBlockEsp(data.pos, blockESP.getRenderRange())) {
                continue;
            }
            renderTimer(data, context);
        }
    }

    private void renderTimer(TimerData data, DrawContext context) {
        if (!shouldRenderUnderground(data.pos)) return;
        if (!shouldRenderAtCurrentDistance(data.pos)) return;
        if (!ProjectionUtil.isInFrontOfCamera(data.pos.x, data.pos.y + 0.5, data.pos.z)) return;

        int secs = data.getClientCountedSeconds();
        double distSq = mc.player.getPos().squaredDistanceTo(data.pos);
        if (distSq > 25.0 * 25.0 && secs >= 60) return;

        MatrixStack ms = context.getMatrices();

        Vector2f projected = ProjectionUtil.project(data.pos.x, data.pos.y + 0.5, data.pos.z);
        if (!ProjectionUtil.isProjectedOnScreen(projected, 200f)) return;

        float px = projected.x;
        float py = projected.y;

        boolean expired = secs <= 0;

        String timeText = data.getDisplayTime();
        String prefix = data.visibleNow ? "" : "~ ";
        String display = prefix + timeText;
        Color baseColor = getAnimatedTimerColor(secs);

        float fontSize = 8f;
        float gap = 2f;
        float textWidth = Fonts.PS_BOLD.getWidth(display, fontSize);
        float boxW = textWidth + gap * 2f;
        float boxH = fontSize + gap * 2f;
        float bx = px - boxW / 2f;
        float by = py - boxH;

        Color bgColor = data.visibleNow
                ? darken(baseColor, 0.86f, 152)
                : darken(baseColor, 0.90f, 132);
        RenderUtil.RECT.draw(ms, bx, by, boxW, boxH, 2f, bgColor);

        Color textColor = data.visibleNow ? baseColor : withAlpha(blend(baseColor, new Color(255, 255, 255), 0.18f), 220);
        Fonts.PS_BOLD.drawText(ms, display, bx + gap, by + gap, fontSize, textColor);

        int dist = (int) mc.player.getPos().distanceTo(data.pos);
        String distStr = dist + "m";
        float distFS = 6f;
        float distW = Fonts.PS_MEDIUM.getWidth(distStr, distFS);
        Fonts.PS_MEDIUM.drawText(ms, distStr, px - distW / 2f, by + boxH + 1f, distFS, new Color(180, 180, 180));
    }

    private boolean hasContainerNearby(Vec3d pos) {
        return findNearestTimerContainer(pos) != null;
    }

    private BlockPos findNearestTimerContainer(Vec3d pos) {
        BlockPos center = BlockPos.ofFloored(pos);
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = center.add(dx, dy, dz);
                    var state = mc.world.getBlockState(checkPos);
                    if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST)) {
                        double distance = Vec3d.ofCenter(checkPos).squaredDistanceTo(pos);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestPos = checkPos.toImmutable();
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    private void ensureExpiredUndergroundTimers() {
        if (!isEnabled() || !isAnarchyServer() || !hasBeenOnAnarchyFor(30_000L)) return;
        if (mc.player == null || mc.player.getY() >= UNDERGROUND_RENDER_Y) return;

        BlockPos playerPos = mc.player.getBlockPos();
        int range = 6; // Сужаем с 10 до 6 (в ~5 раз меньше итераций)
        double maxDistSq = 36.0;

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    if (mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > maxDistSq) continue;
                    if (!isTimerContainer(mc.world.getBlockState(pos))) continue;
                    if (!shouldRenderUnderground(pos)) continue;
                    if (getTrackedSecondsNear(pos) != Integer.MIN_VALUE) continue;

                    String key = blockKey(Vec3d.ofCenter(pos));
                    trackedTimers.putIfAbsent(key, new TimerData(Vec3d.ofCenter(pos).add(0.0, 0.5, 0.0), 0));
                }
            }
        }
    }

    private boolean isTimerContainer(BlockState state) {
        return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST);
    }

    private String getServerIdentifier() {
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

    public boolean isAnarchyServer() {
        return currentServer != null;
    }

    public boolean hasBeenOnAnarchyFor(long millis) {
        return isAnarchyServer() && serverJoinTime > 0L && System.currentTimeMillis() - serverJoinTime >= millis;
    }

    public int getTrackedSecondsNear(BlockPos pos) {
        Vec3d center = Vec3d.ofCenter(pos);
        double bestDistance = Double.MAX_VALUE;
        int bestSeconds = Integer.MIN_VALUE;

        for (TimerData data : trackedTimers.values()) {
            if (data.pos.squaredDistanceTo(center) > 9.0) continue;
            double distance = data.pos.squaredDistanceTo(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestSeconds = data.getClientCountedSeconds();
            }
        }

        return bestSeconds;
    }

    public Color getContainerColor(BlockPos pos) {
        if (mc.world != null && mc.world.getBlockState(pos).isOf(Blocks.BARREL)) {
            return null;
        }
        if (!shouldRenderUnderground(pos)) {
            return null;
        }

        int seconds = getTrackedSecondsNear(pos);
        if (seconds == Integer.MIN_VALUE) {
            return null;
        }

        double distSq = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos));
        if (distSq > 25.0 * 25.0 && seconds >= 60) {
            return null;
        }

        return withAlpha(getAnimatedTimerColor(seconds), 210);
    }

    public String getContainerLabel(BlockPos pos) {
        if (mc.world != null && mc.world.getBlockState(pos).isOf(Blocks.BARREL)) {
            return null;
        }
        if (!shouldRenderUnderground(pos)) {
            return null;
        }

        int seconds = getTrackedSecondsNear(pos);
        if (seconds == Integer.MIN_VALUE) {
            return null;
        }

        double distSq = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos));
        if (distSq > 25.0 * 25.0 && seconds >= 60) {
            return null;
        }

        seconds = Math.max(0, seconds);
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    // --- Persistence ---

    private void saveTimers() {
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

    private void loadTimers() {
        trackedTimers.clear();
        if (currentServer == null) return;

        try {
            JsonObject root = loadRootJson();
            if (!root.has("servers")) return;
            JsonObject servers = root.getAsJsonObject("servers");
            if (!servers.has(currentServer)) return;

            JsonObject serverData = servers.getAsJsonObject(currentServer);
            if (!serverData.has("timers")) return;
            JsonArray timersArray = serverData.getAsJsonArray("timers");

            for (JsonElement el : timersArray) {
                JsonObject t = el.getAsJsonObject();
                String key = t.get("key").getAsString();
                Vec3d pos = new Vec3d(t.get("x").getAsDouble(), t.get("y").getAsDouble(), t.get("z").getAsDouble());
                int savedSeconds = t.get("seconds").getAsInt();
                long savedAt = t.get("savedAt").getAsLong();

                long offlineSec = (System.currentTimeMillis() - savedAt) / 1000;
                int remainingSeconds = Math.max(0, savedSeconds - (int) offlineSec);

                TimerData td = new TimerData(pos, remainingSeconds);
                td.visibleNow = false;
                trackedTimers.put(key, td);
            }
        } catch (Exception e) {
            System.err.println("[WardenHelper] Failed to load timers: " + e.getMessage());
        }
    }

    private JsonObject loadRootJson() {
        return UnifiedConfigStore.getInstance().getObject(STORE_KEY);
    }

    private String blockKey(Vec3d pos) {
        return ((int) Math.floor(pos.x)) + "_" + ((int) Math.floor(pos.y)) + "_" + ((int) Math.floor(pos.z));
    }

    private Color getAnimatedTimerColor(int seconds) {
        float safeSeconds = Math.max(0, seconds);
        Color yellow = new Color(245, 218, 92);
        Color orange = new Color(255, 150, 60);
        Color red = new Color(255, 78, 78);

        Color base;
        if (safeSeconds >= 90f) {
            base = yellow;
        } else if (safeSeconds >= 75f) {
            base = blend(yellow, orange, 1f - ((safeSeconds - 75f) / 15f));
        } else if (safeSeconds >= 15f) {
            base = orange;
        } else {
            base = red;
        }

        float wave = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.0025 + safeSeconds * 0.035f);
        return blend(base, new Color(255, 255, 255), 0.08f + wave * 0.08f);
    }

    private Color blend(Color first, Color second, float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        int red = (int) (first.getRed() + (second.getRed() - first.getRed()) * clamped);
        int green = (int) (first.getGreen() + (second.getGreen() - first.getGreen()) * clamped);
        int blue = (int) (first.getBlue() + (second.getBlue() - first.getBlue()) * clamped);
        int alpha = (int) (first.getAlpha() + (second.getAlpha() - first.getAlpha()) * clamped);
        return new Color(red, green, blue, alpha);
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private Color darken(Color color, float amount, int alpha) {
        int red = (int) (color.getRed() * (1f - amount));
        int green = (int) (color.getGreen() * (1f - amount));
        int blue = (int) (color.getBlue() * (1f - amount));
        return new Color(Math.max(0, red), Math.max(0, green), Math.max(0, blue), alpha);
    }

    public boolean shouldRenderUnderground(BlockPos pos) {
        return pos.getY() < UNDERGROUND_RENDER_Y;
    }

    private boolean shouldRenderUnderground(Vec3d pos) {
        return pos.y < UNDERGROUND_RENDER_Y;
    }

    private boolean shouldRenderAtCurrentDistance(Vec3d pos) {
        return mc.player != null
                && mc.player.getY() < UNDERGROUND_RENDER_Y
                && (shouldRenderUnderground(pos) || mc.player.squaredDistanceTo(pos) <= TRACK_DISTANCE_SQ);
    }

    private boolean isHandledByBlockEsp(Vec3d pos, float blockEspRange) {
        return shouldRenderUnderground(pos) && mc.player.squaredDistanceTo(pos) <= blockEspRange * blockEspRange;
    }
}
