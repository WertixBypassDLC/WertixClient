package sweetie.nezi.api.utils.render;

import lombok.experimental.UtilityClass;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.render.display.*;

@UtilityClass
public class RenderUtil {
    public RectRender RECT = new RectRender();
    public BlurRectRender BLUR_RECT = new BlurRectRender();
    public GradientRectRender GRADIENT_RECT = new GradientRectRender();
    public TextureRectRender TEXTURE_RECT = new TextureRectRender();

    public OtherRender OTHER = new OtherRender();
    public WorldRender WORLD = new WorldRender();
    public BoxRender BOX = new BoxRender();

    public void STROKE(MatrixStack matrixStack, float x, float y, float width, float height, float round, int alpha) {
        RECT.draw(matrixStack, x, y, width, height, round, UIColors.stroke(alpha));
    }

    public void STROKE(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radii, int alpha) {
        RECT.draw(matrixStack, x, y, width, height, radii, UIColors.stroke(alpha));
    }

    public void PANEL(MatrixStack matrixStack, float x, float y, float width, float height, float round) {
        RECT.draw(matrixStack, x, y, width, height, round, UIColors.panel(232));
        RECT.draw(matrixStack, x, y, width, height, round, UIColors.overlay(112));
        RECT.draw(matrixStack, x + 0.7f, y + 0.7f, width - 1.4f, height - 1.4f, Math.max(0f, round - 0.7f), UIColors.stroke(42));
    }

    public void CARD(MatrixStack matrixStack, float x, float y, float width, float height, float round) {
        RECT.draw(matrixStack, x, y, width, height, round, UIColors.card(228));
        RECT.draw(matrixStack, x, y, width, height, round, UIColors.overlay(104));
        RECT.draw(matrixStack, x + 0.7f, y + 0.7f, width - 1.4f, height - 1.4f, Math.max(0f, round - 0.7f), UIColors.stroke(38));
    }

    public void SLOT(MatrixStack matrixStack, float x, float y, float width, float height, float round) {
        RECT.draw(matrixStack, x, y, width, height, round, UIColors.cardSecondary(218));
        RECT.draw(matrixStack, x, y, width, height, round, UIColors.overlay(96));
        RECT.draw(matrixStack, x + 0.6f, y + 0.6f, width - 1.2f, height - 1.2f, Math.max(0f, round - 0.6f), UIColors.stroke(34));
    }
}
