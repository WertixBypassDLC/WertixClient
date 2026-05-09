package sweetie.nezi.client.ui.widget.overlay;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.api.utils.render.fonts.Icons;
import sweetie.nezi.client.features.modules.other.IRCModule;
import sweetie.nezi.client.features.modules.render.InterfaceModule;
import sweetie.nezi.client.features.modules.render.RemovalsModule;
import sweetie.nezi.client.ui.widget.Widget;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;

public class WatermarkWidget extends Widget {

    public boolean showName = true, showFps = true, showServer = true;
    public boolean showXYZ = true;

    private float displayedFps = 0f;
    private int lastRealFps = 0;
    private static final float FPS_LERP_SPEED = 0.08f;
    private float bossBarOffsetAnim = 0f;

    private boolean settingsOpen;
    private float settingsAnim, settingsX, settingsY;
    private boolean wasRightClick, wasLeftClick;
    private float hintAlpha;
    // Per-row stagger animation state
    private long settingsOpenTime = -1L;
    private static final int SETTINGS_ROWS = 4;
    private final float[] rowAnims = new float[SETTINGS_ROWS];

    public WatermarkWidget() { super(4f, 4f); }
    @Override public String getName() { return "Watermark"; }

    private String resolveServerName() {
        try {
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null) {
                String addr = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
                if (addr.contains("funtime")) return "FunTime";
                if (addr.contains("holyworld")) return "HolyWorld";
                if (addr.contains("reallyworld")) return "ReallyWorld";
                return mc.getNetworkHandler().getServerInfo().address;
            }
        } catch (Exception ignored) {}
        return "Singleplayer";
    }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null || mc.getWindow() == null) return;

        float cardH = scaled(16f);
        float pad = scaled(7f);
        float fTit = scaled(6f);
        float iconSize = scaled(8.4f);
        float gap = scaled(4f);
        int alpha = animatedAlpha(255);

        String nick = IRCModule.getInstance().getUsername();
        if (nick == null || nick.isEmpty()) nick = mc.getSession().getUsername();
        String coords = (int) mc.player.getX() + ", " + (int) mc.player.getY() + ", " + (int) mc.player.getZ();
        String server = resolveServerName();

        int realFps = mc.getCurrentFps();
        if (displayedFps == 0f) displayedFps = realFps;
        if (realFps != lastRealFps) {
            lastRealFps = realFps;
        }
        displayedFps += (realFps - displayedFps) * FPS_LERP_SPEED;
        if (Math.abs(displayedFps - realFps) < 0.5f) displayedFps = realFps;
        String fpsNum = String.valueOf(Math.round(displayedFps));

        class Seg {
            final String icon;
            final String main;
            final String suf;
            Seg(String icon, String main, String suf) { this.icon = icon; this.main = main; this.suf = suf; }
        }

        List<Seg> segs = new ArrayList<>();
        if (showName) segs.add(new Seg(Icons.USER.getLetter(), nick, ""));
        if (showXYZ) segs.add(new Seg(Icons.COORDS.getLetter(), coords, ""));
        if (showServer) segs.add(new Seg(Icons.SERVER.getLetter(), server, ""));
        if (showFps) segs.add(new Seg(Icons.FPS.getLetter(), fpsNum, " fps"));
        if (segs.isEmpty()) return;

        float totalW = cardH + gap; // logo block + its gap
        for (Seg s : segs) {
            float textW = Fonts.PS_MEDIUM.getWidth(s.main, fTit) + Fonts.PS_MEDIUM.getWidth(s.suf, fTit);
            float segW  = cardH + scaled(3f) + textW + scaled(3f); // icon + left gap + text + right gap
            totalW += segW + gap;
        }
        totalW -= gap; // remove trailing gap

        float sw = mc.getWindow().getScaledWidth();
        float x = sw / 2f - totalW / 2f;

        float targetBossBarOffset = 0f;
        try {
            if (mc.inGameHud != null && !RemovalsModule.getInstance().isBossBar()) {
                BossBarHud bossBarHud = mc.inGameHud.getBossBarHud();
                Map<UUID, ClientBossBar> bossBars = bossBarHud.bossBars;
                if (!bossBars.isEmpty()) {
                    targetBossBarOffset = scaled(3f) + bossBars.size() * scaled(11f);
                }
            }
        } catch (Exception ignored) {}

        bossBarOffsetAnim += (targetBossBarOffset - bossBarOffsetAnim) * 0.12f;
        if (Math.abs(bossBarOffsetAnim - targetBossBarOffset) < 0.5f) bossBarOffsetAnim = targetBossBarOffset;

        float y = scaled(4f) + bossBarOffsetAnim;
        float curX = x;

        // Leading logo block (pure icon square)
        drawHudSquare(ms, curX, y, cardH, cardH, scaled(5f), alpha);
        {
            String logoText = "E";
            float logoSize = scaled(8.6f);
            Color left  = UIColors.themeFlow(logoText.hashCode(), alpha);
            Color right = UIColors.themeFlowAlt(logoText.hashCode(), alpha);
            Fonts.PS_BOLD.drawGradientText(ms, logoText,
                    curX + cardH / 2f - Fonts.PS_BOLD.getWidth(logoText, logoSize) / 2f,
                    y + cardH / 2f - logoSize / 2f + scaled(0.15f),
                    logoSize, left, right, Math.max(scaled(10f), cardH * 0.9f));
        }
        curX += cardH + gap;

        for (Seg s : segs) {
            float textW = Fonts.PS_MEDIUM.getWidth(s.main, fTit) + Fonts.PS_MEDIUM.getWidth(s.suf, fTit);
            float segW  = cardH + scaled(3f) + textW + scaled(3f);

            // Base rect for this segment
            drawHudCard(ms, curX, y, segW, cardH, scaled(5f), alpha);
            // Icon square stacked on top
            drawHeaderBlock(ms, curX, y, cardH, cardH, s.icon, alpha);

            float textX = curX + cardH + scaled(3f);
            float textY = y + cardH / 2f - fTit / 2f + scaled(0.2f);
            Fonts.PS_MEDIUM.drawText(ms, s.main, textX, textY, fTit, widgetTextColor(alpha));
            if (!s.suf.isEmpty()) {
                float mainW = Fonts.PS_MEDIUM.getWidth(s.main, fTit);
                Fonts.PS_MEDIUM.drawText(ms, s.suf, textX + mainW, textY, fTit, widgetTextColor(alpha));
            }

            curX += segW + gap;
        }

        getDraggable().setX(x);
        getDraggable().setY(y);
        getDraggable().setWidth(totalW);
        getDraggable().setHeight(cardH);
        handleSettings(ms, x, y, totalW, cardH);
    }

    private void drawHeaderBlock(MatrixStack ms, float x, float y, float w, float h, String text, int alpha) {
        drawHudSquare(ms, x, y, w, h, scaled(5f), alpha);
        float textSize = Fonts.getICONS() == Fonts.PS_BOLD ? scaled(8f) : scaled(8.4f);
        float drawSize = text.length() == 1 && Character.isLetter(text.charAt(0)) ? scaled(8.6f) : textSize;
        Color left = UIColors.themeFlow(text.hashCode(), alpha);
        Color right = UIColors.themeFlowAlt(text.hashCode(), alpha);
        float gradientOffset = Math.max(scaled(10f), w * 0.9f);
        if (text.length() == 1 && Character.isLetter(text.charAt(0))) {
            Fonts.PS_BOLD.drawGradientText(ms,
                    text,
                    x + w / 2f - Fonts.PS_BOLD.getWidth(text, drawSize) / 2f,
                    y + h / 2f - drawSize / 2f + scaled(0.15f),
                    drawSize,
                    left,
                    right,
                    gradientOffset);
        } else {
            Fonts.getICONS().drawGradientText(ms,
                    text,
                    x + w / 2f - scaled(4.2f),
                    y + h / 2f - scaled(4.2f),
                    scaled(8.4f),
                    left,
                    right,
                    gradientOffset);
        }
    }

    private void handleSettings(MatrixStack ms, float x, float y, float w, float h) {
        if (!(mc.currentScreen instanceof ChatScreen)) { settingsOpen = false; return; }
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth()  / (double) mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean rmb   = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (rmb && !wasRightClick && hover) { settingsOpen = !settingsOpen; settingsX = (float) mx; settingsY = (float) my; }
        wasRightClick = rmb;
        hintAlpha = Math.max(0f, Math.min(1f, hintAlpha + (hover ? 0.15f : -0.15f)));
        float fS = scaled(6f);
        if (hintAlpha > 0.01f)
            Fonts.PS_MEDIUM.drawText(ms, "ПКМ - настройки", x, y - fS - scaled(2f), fS,
                    new Color(160, 160, 160, (int)(200 * hintAlpha)));
        renderSettings(ms, mx, my);
    }

    private void renderSettings(MatrixStack ms, double mx, double my) {
        // Global open/close anim
        settingsAnim = Math.max(0f, Math.min(1f, settingsAnim + (settingsOpen ? 0.2f : -0.35f)));
        if (settingsAnim < 0.02f) {
            // Reset row anims when fully closed
            for (int i = 0; i < SETTINGS_ROWS; i++) rowAnims[i] = 0f;
            return;
        }

        float gap    = scaled(1.5f); // tighter rows
        float rowH   = scaled(13f);
        float fS     = scaled(6.5f);
        float togSz  = scaled(8f);
        float rowPad = scaled(5f);
        float hr     = hudRound();

        String[] opts  = { "Ник", "Координаты", "Сервер", "FPS" };
        boolean[] vals = { showName, showXYZ, showServer, showFps };

        float[] rowWidths = new float[opts.length];
        float maxRowW = 0f;
        for (int i = 0; i < opts.length; i++) {
            rowWidths[i] = togSz + scaled(4f) + Fonts.PS_MEDIUM.getWidth(opts[i], fS) + rowPad * 2f;
            maxRowW = Math.max(maxRowW, rowWidths[i]);
        }

        float totalH = opts.length * rowH + gap * (opts.length - 1);
        float bx = settingsX + scaled(10f);
        float by = settingsY;
        if (bx + maxRowW > mc.getWindow().getScaledWidth())  bx = settingsX - maxRowW - scaled(10f);
        if (by + totalH  > mc.getWindow().getScaledHeight()) by = mc.getWindow().getScaledHeight() - totalH - scaled(10f);

        // Per-row stagger: row i appears ~80ms after row i-1
        long now = System.currentTimeMillis();
        if (settingsOpen) {
            if (settingsOpenTime < 0) settingsOpenTime = now;
            long elapsed = now - settingsOpenTime;
            for (int i = 0; i < opts.length; i++) {
                long delay = i * 60L; // 60ms stagger between rows
                if (elapsed >= delay) {
                    float progress = Math.min(1f, (elapsed - delay) / 150f);
                    rowAnims[i] = Math.min(1f, rowAnims[i] + progress * 0.25f);
                }
            }
        } else {
            settingsOpenTime = -1L;
            // Collapse: all rows shrink quickly together
            for (int i = 0; i < opts.length; i++) {
                rowAnims[i] = Math.max(0f, rowAnims[i] - 0.25f);
            }
        }

        boolean lmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        float cY = by;
        for (int i = 0; i < opts.length; i++) {
            float ra = rowAnims[i];
            if (ra < 0.01f) { cY += rowH + gap; continue; }

            float rw = rowWidths[i];
            // Click detection
            if (mx >= bx && mx <= bx + rw && my >= cY && my <= cY + rowH && lmb && !wasLeftClick) {
                if (i == 0) showName   = !showName;
                if (i == 1) showXYZ    = !showXYZ;
                if (i == 2) showServer = !showServer;
                if (i == 3) showFps    = !showFps;
            }

            // Scale each row from the cursor (pivot at bx, cY)
            float pivotX = settingsX;
            float pivotY = cY + rowH / 2f;
            ms.push();
            ms.translate(pivotX, pivotY, 0);
            ms.scale(ra, ra, 1f);
            ms.translate(-pivotX, -pivotY, 0);

            int a = Math.max(5, (int)(255 * ra));
            drawHudCard(ms, bx, cY, rw, rowH, hr, a);

            Color tC = vals[i] ? UIColors.positiveColor() : UIColors.negativeColor();
            drawHudSquare(ms, bx + rowPad, cY + rowH / 2f - togSz / 2f, togSz, togSz, scaled(2.5f), a);
            RenderUtil.RECT.draw(ms,
                    bx + rowPad + scaled(2f),
                    cY + rowH / 2f - togSz / 2f + scaled(2f),
                    togSz - scaled(4f), togSz - scaled(4f),
                    scaled(1.5f),
                    new Color(tC.getRed(), tC.getGreen(), tC.getBlue(), a));
            Fonts.PS_MEDIUM.drawText(ms, opts[i],
                    bx + rowPad + togSz + scaled(4f),
                    cY + rowH / 2f - fS / 2f + scaled(0.3f),
                    fS, widgetTextColor(a));
            ms.pop();

            cY += rowH + gap;
        }
        wasLeftClick = lmb;
    }
}
