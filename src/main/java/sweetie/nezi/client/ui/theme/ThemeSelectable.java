package sweetie.nezi.client.ui.theme;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MouseUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.ui.UIComponent;

import java.awt.*;

@Getter
public class ThemeSelectable extends UIComponent {
    private final Theme theme;
    private final AnimationUtil hoverAnimation = new AnimationUtil();
    private final AnimationUtil selectAnimation = new AnimationUtil();
    private final AnimationUtil presenceAnimation = new AnimationUtil();

    private float deleteX = -1f;
    private float deleteY = -1f;
    private float deleteSize;
    private boolean removing;

    public ThemeSelectable(Theme theme) {
        this(theme, true);
    }

    public ThemeSelectable(Theme theme, boolean visibleInstantly) {
        this.theme = theme;
        presenceAnimation.setValue(visibleInstantly ? 1.0 : 0.0);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();
        ThemeEditor editor = ThemeEditor.getInstance();

        boolean hovered = hovered(mouseX, mouseY);
        boolean selected = editor.getCurrentTheme() != null
                && editor.getCurrentTheme().getName().equalsIgnoreCase(theme.getName());
        boolean custom = editor.isCustomTheme(theme);

        hoverAnimation.update();
        hoverAnimation.run(hovered ? 1.0 : 0.0, 180, Easing.SINE_OUT);

        selectAnimation.update();
        selectAnimation.run(selected ? 1.0 : 0.0, 220, Easing.SINE_OUT);

        presenceAnimation.update();
        presenceAnimation.run(removing ? 0.0 : 1.0, 240, Easing.QUINT_OUT);

        float presence = (float) presenceAnimation.getValue();
        if (presence <= 0.02f) {
            return;
        }

        int fullAlpha = (int) (getAlpha() * 255f * presence);
        float round = scaled(5f);
        float pad = scaled(4f);
        float previewH = scaled(12f);
        float nameSize = scaled(5.4f) + scaled((float) hoverAnimation.getValue() * 0.18f);

        Color base = ColorUtil.interpolate(UIColors.card(Math.min(fullAlpha, 186)), UIColors.cardSecondary(Math.min(fullAlpha, 176)), (float) (0.24f * hoverAnimation.getValue()));
        Color overlay = UIColors.overlay(Math.min(fullAlpha, 72));

        float centerX = getX() + getWidth() / 2f;
        float centerY = getY() + getHeight() / 2f;
        float scale = 0.9f + presence * 0.1f;
        matrixStack.push();
        matrixStack.translate(centerX, centerY, 0f);
        matrixStack.scale(scale, scale, 1f);
        matrixStack.translate(-centerX, -centerY, 0f);

        drawGlassSurface(matrixStack, getX(), getY(), getWidth(), getHeight(), round, base, fullAlpha);

        if (selected) {
            RenderUtil.RECT.draw(matrixStack,
                    getX() - scaled(0.5f), getY() - scaled(0.5f),
                    getWidth() + scaled(1f), getHeight() + scaled(1f),
                    round + scaled(0.5f), UIColors.primary(Math.max(0, fullAlpha / 6)));
        }

        RenderUtil.GRADIENT_RECT.draw(matrixStack,
                getX() + pad, getY() + pad,
                getWidth() - pad * 2f, previewH,
                scaled(3f),
                ColorUtil.setAlpha(theme.getPrimaryColor(), fullAlpha),
                ColorUtil.setAlpha(theme.getSecondaryColor(), fullAlpha),
                ColorUtil.setAlpha(theme.getPrimaryColor(), fullAlpha),
                ColorUtil.setAlpha(theme.getSecondaryColor(), fullAlpha));

        deleteSize = custom ? scaled(10.5f) : 0f;
        deleteX = custom ? getX() + getWidth() - pad - deleteSize : -1f;
        deleteY = custom ? getY() + getHeight() - pad - deleteSize - scaled(1f) : -1f;

        if (custom) {
            Color deleteBase = ColorUtil.interpolate(UIColors.primary(Math.min(fullAlpha, 235)), Color.WHITE, 0.26f);
            RenderUtil.BLUR_RECT.draw(matrixStack, deleteX, deleteY, deleteSize, deleteSize, scaled(3.2f), deleteBase);
            RenderUtil.RECT.draw(matrixStack, deleteX, deleteY, deleteSize, deleteSize, scaled(3.2f), UIColors.stroke(fullAlpha));
            Fonts.getICONS().drawCenteredText(matrixStack, "\u00D7",
                    deleteX + deleteSize / 2f,
                    deleteY + deleteSize / 2f - scaled(3.1f),
                    scaled(6.4f), Color.WHITE, 0.1f);
        }

        float nameX = getX() + pad;
        float availableWidth = getWidth() - pad * 2f - (custom ? deleteSize + pad : 0f);
        String label = editor.getThemeDisplayName(theme);
        if (!editor.isRenaming(theme)) {
            while (Fonts.PS_BOLD.getWidth(label, nameSize) > availableWidth && label.length() > 3) {
                label = label.substring(0, label.length() - 2) + ".";
            }
        }

        Fonts.PS_BOLD.drawText(matrixStack, label,
                nameX,
                getY() + getHeight() - pad - nameSize,
                nameSize,
                selected ? UIColors.textColor(fullAlpha) : UIColors.mutedText(fullAlpha));
        matrixStack.pop();
    }

    private void drawGlassSurface(MatrixStack matrixStack, float x, float y, float width, float height, float round,
                                  Color surface, int alpha) {
        int blurAlpha = Math.max(0, Math.min(255, (int) (alpha * 1.04f)));
        int backgroundBlurAlpha = Math.max(0, Math.min(255, (int) (alpha * 0.96f)));
        int overlayAlpha = Math.max(0, Math.min(255, (int) (alpha * 0.10f)));
        int strokeAlpha = Math.max(0, Math.min(255, (int) (alpha * 0.22f)));

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, round, UIColors.blur(blurAlpha), 0.08f);
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, round, UIColors.backgroundBlur(backgroundBlurAlpha), 0.06f);
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, surface);
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, UIColors.overlay(overlayAlpha));
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, UIColors.stroke(strokeAlpha));
    }

    public boolean isDeleteHovered(double mouseX, double mouseY) {
        return deleteSize > 0f && MouseUtil.isHovered(mouseX, mouseY, deleteX, deleteY, deleteSize, deleteSize);
    }

    public void startRemoving() {
        removing = true;
    }

    public boolean isRemoving() {
        return removing;
    }

    public boolean isRemovalFinished() {
        return removing && presenceAnimation.getValue() <= 0.03f;
    }

    private boolean hovered(float mouseX, float mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight());
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseClicked(double mouseX, double mouseY, int button) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
