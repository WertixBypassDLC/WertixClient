package sweetie.nezi.client.ui.widget.overlay;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import sweetie.nezi.api.system.configs.StaffManager;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.framelimiter.FrameLimiter;
import sweetie.nezi.api.utils.other.ReplaceUtil;
import sweetie.nezi.api.utils.player.PlayerUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.api.utils.render.fonts.Icons;
import sweetie.nezi.client.features.modules.render.InterfaceModule;
import sweetie.nezi.client.ui.widget.ContainerWidget;
import net.minecraft.client.gui.screen.ChatScreen;

import java.awt.*;
import java.util.*;
import java.util.List;

public class StaffsWidget extends ContainerWidget {
    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private List<Staff> cacheStaffs = new ArrayList<>();
    private final Map<String, Float> animMap = new HashMap<>();
    private float heightAnim = 0f;

    public record Staff(String name, Status status) {}

    @Getter
    @RequiredArgsConstructor
    public enum Status {
        ONLINE("Online"), NEAR("Near"), GM3("Gm3");
        private final String label;
    }

    public StaffsWidget() { super(100f, 100f); }
    @Override public String getName() { return "Staff List"; }
    @Override protected Map<String, ContainerElement.ColoredString> getCurrentData() { return null; }

    @Override
    public boolean shouldAppearWhenInterfaceVisible() {
        return mc.currentScreen instanceof ChatScreen || !getStaffList().isEmpty();
    }

    @Override
    public void render(MatrixStack ms) {
        List<Staff> list = getStaffList();
        Set<String> active = new HashSet<>();
        for (Staff s : list) active.add(s.name());

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        if (list.isEmpty() && !chatOpen) {
            // Убираем анимации, когда нет стаффов и не в чате
            for (Map.Entry<String, Float> e : animMap.entrySet()) {
                e.setValue(e.getValue() + (0f - e.getValue()) * 0.15f);
            }
            animMap.entrySet().removeIf(e -> e.getValue() < 0.01f);
            if (animMap.isEmpty()) return;
        }

        if (list.isEmpty() && chatOpen) {
            // Мок-данные в ChatScreen
            list = new ArrayList<>(list);
            list.add(new Staff("Ukraine", Status.ONLINE));
            active.add("Ukraine");
        }

        // Анимируем появление для активных, исчезновение для неактивных
        for (String key : active) {
            float cur = animMap.getOrDefault(key, 0f);
            animMap.put(key, cur + (1f - cur) * 0.15f);
        }
        for (Map.Entry<String, Float> e : animMap.entrySet()) {
            if (!active.contains(e.getKey())) {
                e.setValue(e.getValue() + (0f - e.getValue()) * 0.15f);
            }
        }
        animMap.entrySet().removeIf(e -> !active.contains(e.getKey()) && e.getValue() < 0.01f);

        float x = getDraggable().getX(), y = getDraggable().getY();
        float hr = hudRound();
        float hdrH = scaled(13.4f);
        float p = scaled(4f);
        float hGap = scaled(2f);
        float fTit = scaled(5.8f);
        float fRow = scaled(5.3f);
        float iS = scaled(7.9f);
        float rowH = scaled(10f);
        float rowG = scaled(1.5f);
        float dotSize = scaled(4f);
        float sqS = hdrH - scaled(3f);

        float maxNameW = 0f;
        for (Staff s : list) {
            maxNameW = Math.max(maxNameW, mc.textRenderer.getWidth(s.name()));
        }

        float hdrContentW = sqS + scaled(5f) + mc.textRenderer.getWidth("Staff List");
        float rowInnW = maxNameW + scaled(6f) + dotSize;
        float cardW = Math.max(hdrContentW, rowInnW) + p * 2f + scaled(2f);

        list.sort(Comparator.comparing((Staff s) -> s.status().ordinal()).thenComparing(Staff::name));

        float totRows = 0f;
        for (Staff s : list) totRows += (rowH + rowG) * animMap.getOrDefault(s.name(), 0f);
        float targetH = hdrH + totRows + (list.isEmpty() ? hGap * 2 : p);
        heightAnim += (targetH - heightAnim) * 0.15f;
        float cardH = heightAnim;

        ms.push();

        drawHudCard(ms, x, y, cardW, cardH, hr, 255);
        drawJoinedHeader(ms, x + hGap, y + hGap, cardW - hGap * 2f, hdrH,
                scaled(4.6f), 255, Icons.STAFFS.getLetter(), "Staff List", iS, fTit);

        if (!list.isEmpty()) {
            float cY = y + hGap + hdrH + scaled(4f);
            for (Staff s : list) {
                float anim = animMap.getOrDefault(s.name(), 0f);
                if (anim < 0.01f) continue;
                int al = Math.max(5, (int)(255f * anim));
                float midY = cY + rowH / 2f;

                Fonts.PS_MEDIUM.drawText(ms, s.name(),
                        x + p, midY - fRow / 2f + scaled(0.5f),
                        fRow, widgetTextColor(al));

                Color statusC = switch (s.status()) {
                    case ONLINE -> UIColors.positiveColor(al);
                    case NEAR   -> UIColors.middleColor(al);
                    case GM3    -> UIColors.negativeColor(al);
                };
                RenderUtil.RECT.draw(ms, x + cardW - p - dotSize, midY - dotSize / 2f,
                        dotSize, dotSize, dotSize / 2f, statusC);
                cY += (rowH + rowG) * anim;
            }
        }

        ms.pop();

        getDraggable().setWidth(cardW);
        getDraggable().setHeight(cardH);
    }

    private List<Staff> getStaffList() {
        frameLimiter.execute(15, () -> {
            List<Staff> list = new ArrayList<>();
            if (!mc.isInSingleplayer()) {
                list.addAll(getOnlineStaff());
            }
            cacheStaffs = list;
        });
        return cacheStaffs;
    }

    private List<Staff> getOnlineStaff() {
        List<Staff> staff = new ArrayList<>();
        if (mc.player == null || mc.player.networkHandler == null || mc.world == null) return staff;
        for (PlayerListEntry player : mc.player.networkHandler.getPlayerList()) {
            Team team = player.getScoreboardTeam();
            if (team == null) continue;
            String name = player.getProfile().getName();
            if (!PlayerUtil.isValidName(name)) continue;
            String prefix = ReplaceUtil.replaceSymbols(team.getPrefix().getString());

            if (StaffManager.getInstance().contains(name) || isStaffPrefix(prefix.toLowerCase())) {
                Status status = Status.ONLINE;
                if (player.getGameMode() == GameMode.SPECTATOR) status = Status.GM3;
                else if (mc.world.getPlayers().stream().anyMatch(p -> p.getGameProfile().getName().equals(name))) status = Status.NEAR;
                staff.add(new Staff((prefix.isEmpty() ? "" : (prefix + " ")) + name, status));
            }
        }
        return staff;
    }

    private boolean isStaffPrefix(String prefix) {
        return (prefix.contains("helper") || prefix.contains("moder") || prefix.contains("admin") || prefix.contains("owner") || prefix.contains("developer") || prefix.contains("staff") || prefix.contains("curator") || prefix.contains("куратор") || prefix.contains("разраб") || prefix.contains("модер") || prefix.contains("админ") || prefix.contains("стажер") || prefix.contains("стажёр") || prefix.contains("хелпер"));
    }
}
