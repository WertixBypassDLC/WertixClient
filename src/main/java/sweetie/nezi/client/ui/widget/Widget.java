package sweetie.nezi.client.ui.widget;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import lombok.Getter;
import lombok.Setter;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.system.draggable.Draggable;
import sweetie.nezi.api.system.draggable.DraggableManager;
import sweetie.nezi.api.system.interfaces.IRenderer;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Font;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.render.InterfaceModule;
import sweetie.nezi.client.services.RenderService;

import java.awt.*;

import org.joml.Vector4f;

@Getter
@Setter
public abstract class Widget implements QuickImports, IRenderer {
    protected Widget(float x, float y) {
        this.draggable = create(x, y, getName());
    }

    private final Easing easing = Easing.SINE_OUT;
    private final long duration = 100;

    public abstract String getName();
    private final Draggable draggable;
    private boolean enabled;

    private float prevX;
    private float tiltAngle;
    private float dragJitter;
    private float dragJitterX;
    private float dragJitterY;
    private float widgetScale = 1.0f;
    private final AnimationUtil appearAnimation = new AnimationUtil();
    private boolean targetVisible;

    private Draggable create(float x, float y, String name) {
        return DraggableManager.getInstance().create(InterfaceModule.getInstance(), name, x, y);
    }

    public float getWidgetScale() {
        return widgetScale * InterfaceModule.getWidgetScale();
    }

    public void setWidgetScale(float widgetScale) {
        this.widgetScale = widgetScale;
    }

    protected float hudRound() {
        return scaled(4f);
    }

    protected Color widgetBlurColor(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return UIColors.blur(Math.max(0, Math.min(255, (int) (clamped * 1.08f))));
    }

    protected Color widgetBackgroundBlurColor(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return UIColors.backgroundBlur(Math.max(0, Math.min(255, (int) (clamped * 1.02f))));
    }

    protected Color widgetSurface(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return UIColors.card(Math.max(0, Math.min(255, (int) (clamped * 0.44f))));
    }

    protected Color widgetOverlaySurface(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return UIColors.overlay(Math.max(0, Math.min(255, (int) (clamped * 0.12f))));
    }

    protected Color widgetStrokeColor(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return UIColors.stroke(Math.max(0, Math.min(255, (int) (clamped * 0.24f))));
    }

    protected Color widgetTextColor(int alpha) {
        return UIColors.textColor(Math.max(0, Math.min(255, alpha)));
    }

    protected float getAppearVisibility() {
        return MathHelper.clamp(getAppearProgress(), 0f, 1f);
    }

    protected float getSurfaceVisibility() {
        float appear = getAppearProgress();
        if (targetVisible) {
            return MathHelper.clamp((appear - 0.56f) / 0.44f, 0f, 1f);
        }
        return MathHelper.clamp(appear / 0.58f, 0f, 1f);
    }

    protected int animatedAlpha(int alpha) {
        return MathHelper.clamp((int) (alpha * getSurfaceVisibility()), 0, 255);
    }

    protected void drawHudCard(MatrixStack matrixStack, float x, float y, float width, float height) {
        drawHudCard(matrixStack, x, y, width, height, hudRound(), 255);
    }

    /**
     * Base HUD rectangle – rgba(23,23,34, 0.42) + background blur.
     * alpha 107 = 42% of 255.
     */
    protected void drawHudCard(MatrixStack matrixStack, float x, float y, float width, float height, float round, int alpha) {
        alpha = animatedAlpha(alpha);
        Color bg     = new Color(15, 15, 23, (int)(96 * (alpha / 255f)));
        Color stroke = new Color(58, 58, 68, (int)(150 * (alpha / 255f)));
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, round, widgetBlurColor((int) (alpha * 0.68f)), 0.045f);
        RenderUtil.RECT.draw(matrixStack, x - scaled(0.7f), y - scaled(0.7f),
                width + scaled(1.4f), height + scaled(1.4f), round + scaled(0.7f), stroke);
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, bg);
    }

    /**
     * Overlay square drawn ON TOP of the base card.
     * No additional blur (avoids the "solid" look from double-blur).
     * Same fill alpha as card, so the stacking effect is visible but subtle.
     */
    protected void drawHudSquare(MatrixStack matrixStack, float x, float y, float width, float height, float round, int alpha) {
        alpha = animatedAlpha(alpha);
        Color bg     = new Color(15, 15, 23, (int)(96 * (alpha / 255f)));
        Color stroke = new Color(58, 58, 68, (int)(150 * (alpha / 255f)));
        RenderUtil.RECT.draw(matrixStack, x - scaled(0.7f), y - scaled(0.7f),
                width + scaled(1.4f), height + scaled(1.4f), round + scaled(0.7f), stroke);
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, bg);
    }

    protected void drawHudAccent(MatrixStack matrixStack, float x, float y, float width, float height, int alpha) {
        alpha = animatedAlpha(alpha);
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, Math.min(width, height) / 2f, UIColors.primary(alpha));
    }

    protected void drawShadow(MatrixStack matrixStack, float x, float y, float width, float height, float round, int alpha) {
        float offset = scaled(3f);
        float spread = scaled(6f);
        for (int i = 3; i >= 1; i--) {
            float s = spread * i / 3f;
            int a = Math.max(0, Math.min(255, (int) (alpha / (i * 1.2f))));
            RenderUtil.RECT.draw(matrixStack, x - s, y + offset - s, width + s * 2, height + s * 2, round + s, new Color(0, 0, 0, a));
        }
    }

    protected void drawNeonOutline(MatrixStack matrixStack, float x, float y, float width, float height, float round, int alpha) {
        RenderUtil.RECT.draw(matrixStack, x - scaled(0.7f), y - scaled(0.7f),
                width + scaled(1.4f), height + scaled(1.4f),
                round + scaled(0.7f), UIColors.stroke(animatedAlpha(Math.max(0, alpha))));
    }

    protected void drawNeonOutline(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, int alpha) {
        float rGrow = scaled(0.7f);
        Vector4f grown = new Vector4f(
                radius.x + rGrow,
                radius.y + rGrow,
                radius.z + rGrow,
                radius.w + rGrow
        );
        RenderUtil.RECT.draw(matrixStack, x - rGrow, y - rGrow,
                width + rGrow * 2f, height + rGrow * 2f,
                grown, UIColors.stroke(animatedAlpha(Math.max(0, alpha))));
    }

    protected void drawGlassCard(MatrixStack matrixStack, float x, float y, float width, float height, float round, int alpha, float mix) {
        alpha = animatedAlpha(alpha);
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, round, widgetBlurColor((int) (alpha * 0.68f)), Math.max(0.035f, mix * 0.45f));
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, widgetSurface(alpha));
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, widgetOverlaySurface(alpha));
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, widgetStrokeColor(alpha));
    }

    /**
     * Draws a small muted label above the widget card (as seen in screenshots).
     */
    protected void drawWidgetLabel(MatrixStack matrixStack, float x, float y, String label, int alpha) {
        alpha = animatedAlpha(alpha);
        float labelSize = scaled(5.5f);
        getMediumFont().drawText(matrixStack, label.toLowerCase(), x, y - labelSize - scaled(2f), labelSize,
                widgetTextColor(Math.max(5, (int)(alpha * 0.55f))));
    }

    public void updateTilt() {
        Draggable d = getDraggable();
        float currentX = d.getX();
        float dx = currentX - prevX;
        prevX = currentX;

        // Cursor-movement-only: do not tilt unless the user is dragging this widget.
        if (!d.isDragging()) {
            tiltAngle += (0f - tiltAngle) * 0.25f;
            dragJitter += (0f - dragJitter) * 0.20f;
            dragJitterX += (0f - dragJitterX) * 0.25f;
            dragJitterY += (0f - dragJitterY) * 0.25f;
            if (Math.abs(tiltAngle) < 0.01f) tiltAngle = 0f;
            if (Math.abs(dragJitter) < 0.01f) dragJitter = 0f;
            if (Math.abs(dragJitterX) < 0.01f) dragJitterX = 0f;
            if (Math.abs(dragJitterY) < 0.01f) dragJitterY = 0f;
            return;
        }

        // Exaggerated tilt amplitude.
        // Moving left: left side goes down, right side goes up.
        // Screen-space Y grows downward, so the sign here is important.
        float filteredDx = Math.abs(dx) < 0.25f ? 0f : dx;
        float target = Math.max(-16f, Math.min(16f, filteredDx * 2.2f));
        tiltAngle += (target - tiltAngle) * 0.22f;
        dragJitter += (1f - dragJitter) * 0.16f;
        float time = (System.currentTimeMillis() % 100000L) / 1000f;
        dragJitterX += ((float) Math.sin(time * 17f + getName().hashCode() * 0.11f) * scaled(0.85f) * dragJitter - dragJitterX) * 0.34f;
        dragJitterY += ((float) Math.cos(time * 14f + getName().hashCode() * 0.09f) * scaled(0.55f) * dragJitter - dragJitterY) * 0.34f;
        if (Math.abs(tiltAngle) < 0.01f) tiltAngle = 0f;
    }

    public void updateAppear(boolean visible) {
        targetVisible = visible;
        appearAnimation.update();
        appearAnimation.run(visible ? 1.0 : 0.0, 760, Easing.QUINT_OUT);
    }

    public float getAppearProgress() {
        return (float) appearAnimation.getValue();
    }

    public boolean isTargetVisible() {
        return targetVisible;
    }

    public boolean shouldAppearWhenInterfaceVisible() {
        return true;
    }

    public void renderTransitionParticles(MatrixStack ms, boolean behindWidget) {
    }

    private void renderAssembleParticles(MatrixStack ms) {
        float appear = getAppearProgress();
        float entryProgress = smoothProgress(appear / 0.20f);
        float fillProgress = smoothProgress((appear - 0.04f) / 0.64f);
        float particlePhase = smoothProgress(1f - getSurfaceVisibility());
        if (particlePhase <= 0.01f || entryProgress <= 0.01f) {
            return;
        }

        float x = draggable.getX();
        float y = draggable.getY();
        float w = Math.max(draggable.getWidth(), scaled(28f));
        float h = Math.max(draggable.getHeight(), scaled(14f));
        int alpha = MathHelper.clamp((int) (255f * particlePhase * (0.12f + entryProgress * 0.88f)), 0, 255);
        int columns = MathHelper.clamp((int) (w / scaled(3.2f)), 14, 28);
        int rows = MathHelper.clamp((int) (h / scaled(3.1f)), 8, 18);
        int step = particleStep(columns, rows, 210);
        int seed = getName().hashCode();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index % step != 0) {
                    continue;
                }

                float tx = x + ((column + 0.5f) / columns) * w;
                float ty = y + ((row + 0.5f) / rows) * h;

                float jitterX = (particleNoise(seed, index) - 0.5f) * scaled(5.2f);
                float jitterY = (particleNoise(seed, index + 37) - 0.5f) * scaled(5.2f);
                tx += jitterX;
                ty += jitterY;

                float revealNoise = particleNoise(seed, index + 199);
                float localEntry = smoothProgress(MathHelper.clamp(
                        (entryProgress - revealNoise * 0.34f) / (0.66f + revealNoise * 0.24f),
                        0f, 1f
                ));
                if (localEntry <= 0.015f) {
                    continue;
                }

                float angle = particleNoise(seed, index + 71) * (float) (Math.PI * 2.0);
                float orbit = scaled(14f + particleNoise(seed, index + 113) * 22f);
                float drift = (1f - fillProgress) * (0.55f + (1f - localEntry) * 0.85f);
                float px = tx + MathHelper.cos(angle) * orbit * drift * 1.45f;
                float py = ty + MathHelper.sin(angle) * orbit * drift * 1.2f;
                float heat = 1f - fillProgress;
                float size = scaled(0.75f + particleNoise(seed, index + 151) * 1.2f)
                        * (0.46f + localEntry * 0.54f)
                        * (0.9f + heat * 1.15f);
                int localAlpha = MathHelper.clamp((int) (alpha * localEntry), 0, 255);
                if (localAlpha <= 2) {
                    continue;
                }

                Color themeColor = index % 3 == 0 ? UIColors.secondary(localAlpha) : UIColors.primary(localAlpha);
                Color hotColor = ColorUtil.interpolate(themeColor, Color.WHITE, 0.82f);
                Color coolColor = ColorUtil.interpolate(hotColor, themeColor, fillProgress * 0.72f);
                drawParticleGlow(ms, px, py, size, coolColor, localAlpha, 0.46f + heat * 0.18f + localEntry * 0.12f);
            }
        }
    }

    private void renderExplodeParticles(MatrixStack ms) {
        float appear = getAppearProgress();
        float explode = MathHelper.clamp(1f - appear, 0f, 1f);
        if (explode <= 0.01f) {
            return;
        }

        float x = draggable.getX();
        float y = draggable.getY();
        float w = Math.max(draggable.getWidth(), scaled(28f));
        float h = Math.max(draggable.getHeight(), scaled(14f));
        int alpha = MathHelper.clamp((int) (255f * (1f - explode * 0.35f)), 0, 255);
        int columns = MathHelper.clamp((int) (w / scaled(3.2f)), 14, 28);
        int rows = MathHelper.clamp((int) (h / scaled(3.1f)), 8, 18);
        int step = particleStep(columns, rows, 220);
        int seed = getName().hashCode() ^ 0x5f3759df;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index % step != 0) {
                    continue;
                }

                float tx = x + ((column + 0.5f) / columns) * w;
                float ty = y + ((row + 0.5f) / rows) * h;

                float jitterX = (particleNoise(seed, index) - 0.5f) * scaled(5.2f);
                float jitterY = (particleNoise(seed, index + 37) - 0.5f) * scaled(5.2f);
                tx += jitterX;
                ty += jitterY;

                float angle = particleNoise(seed, index + 71) * (float) (Math.PI * 2.0);
                float orbit = scaled(9f + particleNoise(seed, index + 113) * 26f);
                float px = tx + MathHelper.cos(angle) * orbit * explode * 2.1f;
                float py = ty + MathHelper.sin(angle) * orbit * explode * 1.7f;
                float size = scaled(0.8f + particleNoise(seed, index + 151) * 1.25f) * (0.95f + explode * 0.95f);

                Color themeColor = index % 3 == 0 ? UIColors.primary(alpha) : UIColors.secondary(alpha);
                Color color = ColorUtil.interpolate(themeColor, Color.WHITE, 0.78f);
                drawParticleGlow(ms, px, py, size, color, alpha, 0.56f);
            }
        }
    }

    private void drawParticleGlow(MatrixStack ms, float x, float y, float size, Color baseColor, int alpha, float glowScale) {
        int glowAlpha = Math.max(0, Math.min(255, (int) (alpha * 0.34f)));
        float glowSize = size * (1.8f + glowScale);
        Color glowColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), glowAlpha);
        RenderUtil.RECT.draw(ms, x - glowSize / 2f, y - glowSize / 2f, glowSize, glowSize, glowSize / 2f, glowColor);

        Color coreColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        RenderUtil.RECT.draw(ms, x - size / 2f, y - size / 2f, size, size, size / 2f, coreColor);
    }

    private int particleStep(int columns, int rows, int maxParticles) {
        int total = Math.max(1, columns * rows);
        return Math.max(1, (int) Math.ceil(total / (double) maxParticles));
    }

    private float smoothProgress(float value) {
        value = MathHelper.clamp(value, 0f, 1f);
        return value * value * (3f - 2f * value);
    }

    protected float getHeaderContentProgress() {
        return MathHelper.clamp((getSurfaceVisibility() - 0.18f) / 0.82f, 0f, 1f);
    }

    protected void drawJoinedHeader(MatrixStack ms, float x, float y, float width, float height,
                                    float round, int alpha, String iconText, String title,
                                    float iconSize, float titleSize) {
        float squareSize = height;
        float titleWidth = Fonts.PS_BOLD.getWidth(title, titleSize);
        float gap = scaled(3f);
        float textAreaX = x + squareSize + gap;
        float textAreaWidth = Math.max(0f, width - squareSize - gap);
        // Center text in the area to the right of the icon square
        float textX = textAreaX + Math.max(0f, (textAreaWidth - titleWidth) / 2f);
        float contentProgress = getHeaderContentProgress();
        int contentAlpha = Math.max(0, Math.min(255, (int) (alpha * contentProgress)));
        float contentOffset = scaled(1.7f) * (1f - contentProgress);

        // 1) Base header rect (full width) – 42% opacity
        drawHudCard(ms, x, y, width, height, round, alpha);
        // 2) Icon square ON TOP of rect – stacks, appears more opaque
        drawHudSquare(ms, x, y, squareSize, squareSize, round, alpha);
        // 3) Icon glyph
        drawWatermarkIconBlock(ms, x, y, squareSize, squareSize, iconText, contentAlpha, iconSize);
        // 4) Title text centred in the right portion
        Fonts.PS_BOLD.drawText(ms, title,
                textX,
                y + height / 2f - titleSize / 2f + scaled(0.2f) + contentOffset,
                titleSize,
                widgetTextColor(contentAlpha));
    }

    public float getDragJitterX() {
        return dragJitterX;
    }

    public float getDragJitterY() {
        return dragJitterY;
    }

    public void renderReleasePulse(MatrixStack ms) {
        float pulse = getDraggable().getReleasePulse() * getSurfaceVisibility();
        if (pulse <= 0.01f) {
            return;
        }

        float expansion = scaled(2.5f) + scaled(10f) * (1f - pulse);
        int alpha = Math.max(0, Math.min(255, (int) (pulse * 120f)));
        float x = getDraggable().getX() - expansion;
        float y = getDraggable().getY() - expansion;
        float width = getDraggable().getWidth() + expansion * 2f;
        float height = getDraggable().getHeight() + expansion * 2f;
        float round = hudRound() + expansion * 0.7f;

        RenderUtil.RECT.draw(ms, x, y, width, height, round, UIColors.stroke(alpha));
        RenderUtil.RECT.draw(ms,
                x - scaled(0.6f),
                y - scaled(0.6f),
                width + scaled(1.2f),
                height + scaled(1.2f),
                round + scaled(0.6f),
                UIColors.overlay(Math.max(0, alpha / 2)));
    }

    protected void drawLineAssembleParticles(MatrixStack ms, float x, float y, float width, float height,
                                             float progress, int alpha, int seed) {
    }

    private void drawWatermarkIconBlock(MatrixStack ms, float x, float y, float width, float height, String text,
                                        int alpha, float iconSize) {
        drawHudSquare(ms, x, y, width, height, scaled(5f), alpha);
        if (text == null || text.isBlank()) {
            return;
        }

        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        Color left = UIColors.themeFlow(text.hashCode(), clampedAlpha);
        Color right = UIColors.themeFlowAlt(text.hashCode(), clampedAlpha);
        float gradientOffset = Math.max(scaled(10f), width * 0.9f);

        if (text.length() == 1 && Character.isLetter(text.charAt(0))) {
            Fonts.PS_BOLD.drawGradientText(ms,
                    text,
                    x + width / 2f - Fonts.PS_BOLD.getWidth(text, iconSize) / 2f,
                    y + height / 2f - iconSize / 2f + scaled(0.05f),
                    iconSize,
                    left,
                    right,
                    gradientOffset);
            return;
        }

        Fonts.getICONS().drawGradientText(ms,
                text,
                x + width / 2f - iconSize / 2f + scaled(0.25f),
                y + height / 2f - iconSize / 2f + scaled(0.05f),
                iconSize,
                left,
                right,
                gradientOffset);
    }

    private float particleNoise(int seed, int salt) {
        int value = seed * 73428767 + salt * 912931;
        value ^= value >>> 15;
        value *= 0x2c1b3c6d;
        value ^= value >>> 12;
        value *= 0x297a2d39;
        value ^= value >>> 15;
        return (value & Integer.MAX_VALUE) / (float) Integer.MAX_VALUE;
    }

    protected void drawStrokeCard(MatrixStack ms, float x, float y, float w, float h, float round, int alpha, float mix) {
        alpha = animatedAlpha(alpha);
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, round, widgetBlurColor((int) (alpha * 0.68f)), Math.max(0.035f, mix * 0.45f));
        RenderUtil.RECT.draw(ms, x, y, w, h, round, widgetSurface(alpha));
        RenderUtil.RECT.draw(ms, x, y, w, h, round, widgetOverlaySurface(alpha));
        RenderUtil.RECT.draw(ms, x, y, w, h, round, widgetStrokeColor(alpha));
    }

    /**
     * Asymmetric teardrop-style card: uses different corner radii on BLUR_RECT.
     */
    protected void drawTeardropCard(MatrixStack ms, float x, float y, float w, float h,
                                     float round, int alpha, float mix) {
        alpha = animatedAlpha(alpha);
        float big = Math.max(1f, round);
        float small = Math.max(1f, round * 0.65f);
        Vector4f radii = new Vector4f(big, small, small, big);
        Color base = UIColors.card((int) (alpha * 0.78f));
        Color overlay = UIColors.overlay((int) (alpha * 0.78f));

        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, radii, base, base, base, base, Math.max(0.035f, mix * 0.45f));
        RenderUtil.RECT.draw(ms, x, y, w, h, radii, overlay);
        RenderUtil.STROKE(ms, x, y, w, h, radii, alpha);
    }

    protected void drawNestedHeader(MatrixStack ms, float x, float y, float cardW, float hdrH,
                                    float round, int alpha, float mix,
                                    String iconLetter, String title, float iS, float fTit) {
        float ip = scaled(4f);
        float innerR = Math.max(1f, round - ip);
        float innerX = x + ip;
        float innerY = y + ip;
        float innerW = cardW - ip * 2;
        float innerH = hdrH - ip * 2;
        float big = innerR;
        float small = Math.max(1f, innerR * 0.65f);
        Vector4f radii = new Vector4f(big, small, small, big);
        RenderUtil.BLUR_RECT.draw(ms, innerX, innerY, innerW, innerH, radii,
                UIColors.cardSecondary(alpha), UIColors.cardSecondary(alpha), UIColors.cardSecondary(alpha), UIColors.cardSecondary(alpha), mix);

        // Icon square must visually match header rounding/size (no small "button" look).
        float iconX = innerX;
        float iconY = innerY;
        float iconSz = innerH;
        float hdrMidY = y + hdrH / 2f;
        float iconBig = Math.max(1f, innerR);
        float iconSmall = Math.max(1f, innerR * 0.65f);
        Vector4f iconRadii = new Vector4f(iconBig, iconSmall, iconSmall, iconBig);
        RenderUtil.BLUR_RECT.draw(ms, iconX, iconY, iconSz, iconSz, iconRadii,
                UIColors.panel(alpha), UIColors.panel(alpha), UIColors.panel(alpha), UIColors.panel(alpha), mix);
        Fonts.getICONS().drawText(ms, iconLetter,
                iconX + iconSz / 2f - iS / 2f,
                hdrMidY - iS / 2f - scaled(0.3f),
                iS, UIColors.primary(alpha));
        Fonts.PS_BOLD.drawText(ms, title,
                iconX + iconSz + scaled(3.5f),
                hdrMidY - fTit / 2f + scaled(0.3f),
                fTit, UIColors.textColor(alpha));
    }

    public void render(Render2DEvent.Render2DEventData event) {
        render(event.matrixStack());
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        return false;
    }

    public float scaled(float value) {
        return RenderService.getInstance().scaled(value) * getWidgetScale();
    }

    public float getScale() { return RenderService.getInstance().getScale(); }
    public float getGap() { return scaled(3f); }
    public Font getMediumFont() { return Fonts.PS_MEDIUM; }
    public Font getSemiBoldFont() { return Fonts.PS_BOLD; }
}
