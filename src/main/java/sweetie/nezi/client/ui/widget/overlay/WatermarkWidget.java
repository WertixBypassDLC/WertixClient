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
        float pad = scaled(4f);
        float fTit = scaled(6f);
        float iconSize = scaled(8.4f);
        float gap = scaled(2f);
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

        float totalW = cardH;
        for (Seg s : segs) {
            float textW = Fonts.PS_MEDIUM.getWidth(s.main, fTit) + Fonts.PS_MEDIUM.getWidth(s.suf, fTit);
            totalW += cardH + scaled(5f) + textW + pad * 2f + gap;
        }

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

        drawHeaderBlock(ms, curX, y, cardH, cardH, "E", alpha);
        curX += cardH + gap;

        for (Seg s : segs) {
            float textW = Fonts.PS_MEDIUM.getWidth(s.main, fTit) + Fonts.PS_MEDIUM.getWidth(s.suf, fTit);
            float segW = cardH + scaled(5f) + textW + pad * 2f;

            drawHudCard(ms, curX, y, segW, cardH, scaled(5f), alpha);
            drawHeaderBlock(ms, curX, y, cardH, cardH, s.icon, alpha);

            float textX = curX + cardH + scaled(5f);
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
        drawHudCard(ms, x, y, w, h, scaled(5f), alpha);
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
        settingsAnim = Math.max(0f, Math.min(1f, settingsAnim + (settingsOpen ? 0.15f : -0.15f)));
        if (settingsAnim < 0.02f) return;

        float p  = scaled(5f), gap = scaled(2f), rowH = scaled(13f),
              fS = scaled(7f), tog = scaled(7f), mix  = InterfaceModule.getGlassy();
        float popupRound = scaled(6f);
        String[] opts  = {
                "Ник",
                "Координаты",
                "Сервер",
                "FPS"
        };
        boolean[] vals = { showName, showXYZ, showServer, showFps };

        float mxW = 0f;
        for (String s : opts) mxW = Math.max(mxW, Fonts.PS_MEDIUM.getWidth(s, fS));
        float bw = p + tog + p + mxW + p;
        float bh = p + opts.length * rowH + gap * (opts.length - 1) + p;
        float bx = settingsX + scaled(10f), by = settingsY;
        if (bx + bw > mc.getWindow().getScaledWidth())  bx = settingsX - bw - scaled(10f);
        if (by + bh > mc.getWindow().getScaledHeight()) by = mc.getWindow().getScaledHeight() - bh - scaled(10f);

        ms.push();
        ms.translate(bx + bw / 2f, by + bh / 2f, 0);
        ms.scale(settingsAnim, settingsAnim, 1f);
        ms.translate(-(bx + bw / 2f), -(by + bh / 2f), 0);

        int a = Math.max(5, (int)(255 * settingsAnim));
        drawStrokeCard(ms, bx, by, bw, bh, popupRound, a, mix);

        boolean lmb = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        float cY = by + p;
        for (int i = 0; i < opts.length; i++) {
            if (mx >= bx && mx <= bx + bw && my >= cY && my <= cY + rowH && lmb && !wasLeftClick) {
                if (i == 0) showName   = !showName;
                if (i == 1) showXYZ    = !showXYZ;
                if (i == 2) showServer = !showServer;
                if (i == 3) showFps    = !showFps;
            }
            Color tC = vals[i] ? UIColors.positiveColor() : UIColors.negativeColor();
            RenderUtil.BLUR_RECT.draw(ms, bx + p, cY + rowH / 2f - tog / 2f, tog, tog, scaled(2f),
                    new Color(tC.getRed(), tC.getGreen(), tC.getBlue(), a), mix);
            Fonts.PS_MEDIUM.drawText(ms, opts[i], bx + p + tog + p, cY + rowH / 2f - fS / 2f + scaled(0.3f), fS,
                    widgetTextColor(a));
            cY += rowH + gap;
        }
        wasLeftClick = lmb;
        ms.pop();
    }
}
