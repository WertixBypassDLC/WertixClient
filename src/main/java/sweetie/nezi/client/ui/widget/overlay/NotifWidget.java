package sweetie.nezi.client.ui.widget.overlay;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.api.utils.render.fonts.Icons;
import sweetie.nezi.client.features.modules.render.InterfaceModule;
import sweetie.nezi.client.ui.widget.Widget;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotifWidget extends Widget {
    private static final String POTION_LINE_SEPARATOR = "\u0000";

    private float hintAlpha;

    public boolean specRequest = false;
    public boolean moduleState = false;
    public boolean lowDurability = false;

    private float settingsAnim;
    private boolean settingsOpen;
    private float settingsX, settingsY;
    private boolean wasRightClick, wasLeftClick;
    private long lastDuraCheck;
    // Per-row stagger animation
    private static final int NOTIF_SETTINGS_ROWS = 3;
    private final float[] rowAnims = new float[NOTIF_SETTINGS_ROWS];
    private long settingsOpenTime = -1L;

    private final List<Notif> notifs = new CopyOnWriteArrayList<>();
    private final List<PotionNotif> potionNotifs = new CopyOnWriteArrayList<>();

    public NotifWidget() {
        super(0, 0);
        setEnabled(true);
    }

    @Override
    public String getName() {
        return "Notification";
    }

    @Override
    public float scaled(float value) {
        return sweetie.nezi.client.services.RenderService.getInstance().scaled(value);
    }

    public void addNotif(String text) {
        notifs.add(new Notif(text));
    }

    public void addPotionNotif(String playerName, List<String> lines) {
        PotionNotif existing = potionNotifs.stream()
                .filter(notif -> notif.playerName.equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.merge(lines);
        } else {
            potionNotifs.add(new PotionNotif(playerName, lines));
        }

        while (potionNotifs.size() > 5) {
            PotionNotif oldest = potionNotifs.stream()
                    .min((first, second) -> Long.compare(first.lastUpdate, second.lastUpdate))
                    .orElse(null);
            if (oldest == null) {
                break;
            }
            potionNotifs.remove(oldest);
        }
    }

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        MatrixStack ms = event.matrixStack();

        notifs.removeIf(Notif::shouldRemove);
        potionNotifs.removeIf(PotionNotif::shouldRemove);

        renderNotifs(ms);
        renderPotionNotifs(ms);

        if (lowDurability && System.currentTimeMillis() - lastDuraCheck > 5000) {
            checkDura();
            lastDuraCheck = System.currentTimeMillis();
        }

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        if (!chatOpen) {
            settingsOpen = false;
            return;
        }

        if (!notifs.isEmpty()) {
            renderSettings(ms, 0, 0);
            return;
        }

        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();

        double mx = mc.mouse.getX() * sw / (double) mc.getWindow().getWidth();
        double my = mc.mouse.getY() * sh / (double) mc.getWindow().getHeight();

        float h = scaled(18f);
        float p = scaled(8f);
        float fS = scaled(7f);
        float iS = scaled(8f);
        float iGap = scaled(5f);
        String txt = "\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f";
        float txtW = Fonts.PS_MEDIUM.getWidth(txt, fS);
        float w = p + iS + iGap + txtW + p;
        float pillR = h / 2f;
        float x = sw / 2f - w / 2f;
        float y = sh / 2f + scaled(14f);

        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean rightClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (rightClick && !wasRightClick && hover) {
            settingsOpen = !settingsOpen;
            settingsX = (float) mx;
            settingsY = (float) my;
        }
        wasRightClick = rightClick;
        hintAlpha += (hover ? 0.15f : -0.15f);
        hintAlpha = Math.max(0, Math.min(1, hintAlpha));

        if (hintAlpha > 0.01f) {
            String hint = "\u041f\u041a\u041c - \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438";
            int alpha = (int) (200 * hintAlpha);
            float hintW = Fonts.PS_MEDIUM.getWidth(hint, scaled(6f));
            Fonts.PS_MEDIUM.drawText(ms, hint, x + w / 2f - hintW / 2f, y - scaled(6f) - scaled(2f), scaled(6f),
                    new Color(150, 150, 150, alpha));
        }

        float notifMix = InterfaceModule.getGlassy();
        drawHudCard(ms, x, y, w, h, pillR, 255);

        float midY = y + h / 2f;
        Fonts.getICONS().drawText(ms, Icons.FPS.getLetter(),
                x + p, midY - iS / 2f - scaled(0.3f), iS, UIColors.primary());
        Fonts.PS_MEDIUM.drawText(ms, txt,
                x + p + iS + iGap, midY - fS / 2f + scaled(0.3f), fS, UIColors.textColor());

        renderSettings(ms, mx, my);
    }

    private void renderPotionNotifs(MatrixStack ms) {
        float y = mc.getWindow().getScaledHeight() - scaled(20f);
        float fS = scaled(6.5f);
        float p = scaled(5f);
        float round = scaled(6f);
        float mix = InterfaceModule.getGlassy();

        for (PotionNotif notification : potionNotifs) {
            float anim = notification.getAlpha();
            if (anim <= 0.05f) {
                continue;
            }

            float titleWidth = getMediumFont().getWidth(stripCodes(notification.playerName), fS);
            float maxLeft = 0f;
            float maxRight = 0f;
            for (String line : notification.lines) {
                String[] parts = splitPotionLine(line);
                maxLeft = Math.max(maxLeft, getMediumFont().getWidth(stripCodes(parts[0]), fS));
                maxRight = Math.max(maxRight, getMediumFont().getWidth(stripCodes(parts[1]), fS));
            }

            float contentWidth = maxLeft + (maxRight > 0f ? scaled(12f) + maxRight : 0f);
            float w = Math.max(titleWidth, contentWidth) + p * 2f;
            float h = (1 + notification.lines.size()) * (fS + scaled(2f)) + p * 2f;
            float x = mc.getWindow().getScaledWidth() - w - scaled(10f);
            int alpha = Math.max(5, (int) (255 * anim));

            ms.push();
            ms.translate(x + w / 2f, y - h / 2f, 0f);
            ms.scale(anim, anim, 1f);
            ms.translate(-(x + w / 2f), -(y - h / 2f), 0f);

            drawHudCard(ms, x, y - h, w, h, round, alpha);

            float currentY = y - h + p;
            float tW = getMediumFont().getWidth(stripCodes(notification.playerName), fS);
            getMediumFont().drawText(ms, notification.playerName, x + w / 2f - tW / 2f, currentY, fS, UIColors.textColor(alpha));
            currentY += fS + scaled(2f);

            for (String line : notification.lines) {
                String[] parts = splitPotionLine(line);
                String left = parts[0];
                String right = parts[1];

                getMediumFont().drawText(ms, left, x + p, currentY, fS, UIColors.textColor(alpha));
                if (!right.isEmpty()) {
                    float rightWidth = getMediumFont().getWidth(stripCodes(right), fS);
                    getMediumFont().drawText(ms, right, x + w - p - rightWidth, currentY, fS, UIColors.primary(alpha));
                }
                currentY += fS + scaled(2f);
            }

            ms.pop();
            y -= (h + scaled(5f)) * anim;
        }
    }

    private String[] splitPotionLine(String line) {
        int separatorIndex = line.indexOf(POTION_LINE_SEPARATOR);
        if (separatorIndex < 0) {
            return new String[] {line, ""};
        }

        return new String[] {
                line.substring(0, separatorIndex),
                line.substring(separatorIndex + POTION_LINE_SEPARATOR.length())
        };
    }

    private String stripCodes(String text) {
        return text.replaceAll("\u00a7[0-9a-fk-or]", "");
    }

    private void renderNotifs(MatrixStack ms) {
        if (notifs.isEmpty()) return;

        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();
        float h = scaled(12.6f);
        float pL = scaled(7.2f);
        float pR = scaled(7.2f);
        float iS = scaled(6.3f);
        float iGap = scaled(3.6f);
        float gap = scaled(2.7f);
        float fS = scaled(5.85f);
        float round = h / 2f;
        float mix = InterfaceModule.getGlassy();

        float centerX = sw / 2f;
        float startY = sh / 2f + h;

        float stackY = startY;
        for (Notif notification : notifs) {
            float anim = notification.getAlpha();
            if (anim <= 0.01f) continue;

            String raw = notification.text;
            String clean = stripCodes(raw);
            float txtW = Fonts.PS_MEDIUM.getWidth(clean, fS);
            float w = pL + iS + iGap + txtW + pR;
            float x = centerX - w / 2f;
            float y = stackY;
            int alpha = Math.max(5, (int) (255f * anim));

            ms.push();
            ms.translate(x + w / 2f, y + h / 2f, 0f);
            ms.scale(anim, anim, 1f);
            ms.translate(-(x + w / 2f), -(y + h / 2f), 0f);

            drawHudCard(ms, x, y, w, h, round, alpha);

            float midY = y + h / 2f;
            float textY = midY - fS / 2f + scaled(0.3f);

            Fonts.getICONS().drawText(ms, Icons.FPS.getLetter(),
                    x + pL, midY - iS / 2f - scaled(0.3f), iS, UIColors.primary(alpha));

            float textFieldLeft = x + pL + iS + iGap;
            float textFieldW = w - (pL + iS + iGap) - pR;
            float tX = textFieldLeft + textFieldW / 2f - txtW / 2f;
            Color white = UIColors.textColor(alpha);
            Color accent = UIColors.primary(alpha);
            Color current = white;

            String[] parts = raw.split("(?=\u00a7)|(?<=\u00a7[0-9a-fk-orA-FK-OR])");
            for (String part : parts) {
                if (part.startsWith("\u00a7") && part.length() >= 2) {
                    char code = part.charAt(1);
                    current = (code == 'r' || code == 'f') ? white : accent;
                    if (part.length() > 2) {
                        String segment = part.substring(2);
                        var font = (current == accent) ? Fonts.PS_BOLD : Fonts.PS_MEDIUM;
                        font.drawText(ms, segment, tX, textY, fS, current);
                        tX += Fonts.PS_MEDIUM.getWidth(segment, fS);
                    }
                } else {
                    var font = (current == accent) ? Fonts.PS_BOLD : Fonts.PS_MEDIUM;
                    font.drawText(ms, part, tX, textY, fS, current);
                    tX += Fonts.PS_MEDIUM.getWidth(part, fS);
                }
            }

            ms.pop();
        stackY += (h + gap) * anim;
        }
    }

    private void renderSettings(MatrixStack ms, double mx, double my) {
        settingsAnim += (settingsOpen ? 0.2f : -0.35f);
        settingsAnim = Math.max(0, Math.min(1, settingsAnim));
        if (settingsAnim <= 0.02f) {
            for (int i = 0; i < NOTIF_SETTINGS_ROWS; i++) rowAnims[i] = 0f;
            return;
        }

        float rowH  = scaled(13f);
        float gap   = scaled(1.5f); // tighter
        float fS    = scaled(6.5f);
        float togSz = scaled(8f);
        float rowPad = scaled(5f);
        float hr    = hudRound();
        String[] options = {
                "Просьба о наблюдении",
                "Состояние модулей",
                "Низкая прочность"
        };
        boolean[] states = {specRequest, moduleState, lowDurability};

        float[] rowWidths = new float[options.length];
        float maxRowW = 0f;
        for (int i = 0; i < options.length; i++) {
            rowWidths[i] = rowPad + togSz + scaled(4f) + getMediumFont().getWidth(options[i], fS) + rowPad;
            maxRowW = Math.max(maxRowW, rowWidths[i]);
        }

        float totalH = options.length * rowH + gap * (options.length - 1);
        float x = settingsX + scaled(10);
        float y = settingsY;
        if (x + maxRowW > mc.getWindow().getScaledWidth()) x = settingsX - maxRowW - scaled(10);
        if (y + totalH  > mc.getWindow().getScaledHeight()) y = mc.getWindow().getScaledHeight() - totalH - scaled(10);

        // Per-row stagger
        long now = System.currentTimeMillis();
        if (settingsOpen) {
            if (settingsOpenTime < 0) settingsOpenTime = now;
            long elapsed = now - settingsOpenTime;
            for (int i = 0; i < options.length; i++) {
                long delay = i * 60L;
                if (elapsed >= delay) {
                    float progress = Math.min(1f, (elapsed - delay) / 150f);
                    rowAnims[i] = Math.min(1f, rowAnims[i] + progress * 0.25f);
                }
            }
        } else {
            settingsOpenTime = -1L;
            for (int i = 0; i < options.length; i++) {
                rowAnims[i] = Math.max(0f, rowAnims[i] - 0.25f);
            }
        }

        boolean leftClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        float currentY = y;

        for (int i = 0; i < options.length; i++) {
            float ra = rowAnims[i];
            if (ra < 0.01f) { currentY += rowH + gap; continue; }

            float rw = rowWidths[i];
            if (mx >= x && mx <= x + rw && my >= currentY && my <= currentY + rowH && leftClick && !wasLeftClick) {
                if (i == 0) specRequest = !specRequest;
                if (i == 1) moduleState = !moduleState;
                if (i == 2) lowDurability = !lowDurability;
                states[i] = !states[i];
            }

            float pivotX = settingsX;
            float pivotY = currentY + rowH / 2f;
            ms.push();
            ms.translate(pivotX, pivotY, 0);
            ms.scale(ra, ra, 1f);
            ms.translate(-pivotX, -pivotY, 0);

            int a = Math.max(5, (int)(255 * ra));
            drawHudCard(ms, x, currentY, rw, rowH, hr, a);

            Color tC = states[i] ? UIColors.positiveColor() : UIColors.negativeColor();
            drawHudSquare(ms, x + rowPad, currentY + rowH / 2f - togSz / 2f, togSz, togSz, scaled(2.5f), a);
            RenderUtil.RECT.draw(ms,
                    x + rowPad + scaled(2f),
                    currentY + rowH / 2f - togSz / 2f + scaled(2f),
                    togSz - scaled(4f), togSz - scaled(4f),
                    scaled(1.5f),
                    new Color(tC.getRed(), tC.getGreen(), tC.getBlue(), a));
            getMediumFont().drawText(ms, options[i],
                    x + rowPad + togSz + scaled(4f),
                    currentY + rowH / 2f - fS / 2f + scaled(0.3f),
                    fS, UIColors.textColor(a));
            ms.pop();

            currentY += rowH + gap;
        }
        wasLeftClick = leftClick;
    }

    private void checkDura() {
        if (mc.player == null) return;
        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.isEmpty() || !stack.isDamageable()) continue;
            int left = stack.getMaxDamage() - stack.getDamage();
            if (left > 0 && left < stack.getMaxDamage() * 0.07) {
                addNotif("\u041d\u0438\u0437\u043a\u0430\u044f \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c: " + stack.getName().getString());
                break;
            }
        }
    }

    @Override
    public void render(MatrixStack matrixStack) {}

    private static class Notif {
        String text;
        long start = System.currentTimeMillis();
        long dur = 4000;
        boolean expired;

        Notif(String text) {
            this.text = text;
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed < 300) return elapsed / 300f;
            if (elapsed < dur - 300) return 1f;
            if (elapsed < dur) return 1f - (elapsed - (dur - 300)) / 300f;
            return 0f;
        }

        boolean shouldRemove() {
            if (!expired && System.currentTimeMillis() - start > dur) expired = true;
            return expired && getAlpha() <= 0.05f;
        }
    }

    private static class PotionNotif {
        final String playerName;
        final List<String> lines = new CopyOnWriteArrayList<>();
        long start = System.currentTimeMillis();
        long lastUpdate = start;
        final long dur = 4500;
        boolean expired;

        PotionNotif(String playerName, List<String> lines) {
            this.playerName = playerName;
            merge(lines);
        }

        void merge(List<String> newLines) {
            for (String newLine : newLines) {
                String key = extractKey(newLine);
                int existingIndex = -1;
                for (int i = 0; i < lines.size(); i++) {
                    if (extractKey(lines.get(i)).equalsIgnoreCase(key)) {
                        existingIndex = i;
                        break;
                    }
                }

                if (existingIndex >= 0) {
                    lines.set(existingIndex, newLine);
                } else {
                    lines.add(newLine);
                }
            }

            start = System.currentTimeMillis();
            lastUpdate = start;
            expired = false;
        }

        private String extractKey(String line) {
            int separatorIndex = line.indexOf(POTION_LINE_SEPARATOR);
            return separatorIndex < 0 ? line : line.substring(0, separatorIndex);
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed < 300) return elapsed / 300f;
            if (elapsed < dur - 300) return 1f;
            if (elapsed < dur) return 1f - (elapsed - (dur - 300)) / 300f;
            return 0f;
        }

        boolean shouldRemove() {
            if (!expired && System.currentTimeMillis() - start > dur) expired = true;
            return expired && getAlpha() <= 0.05f;
        }
    }
}
