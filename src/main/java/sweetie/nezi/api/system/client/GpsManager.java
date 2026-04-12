package sweetie.nezi.api.system.client;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import org.joml.Vector2i;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.render.PointersModule;
import sweetie.nezi.client.services.RenderService;

@Getter
@Setter
public class GpsManager implements QuickImports {
    @Getter private static final GpsManager instance = new GpsManager();

    private Vector2i gpsPosition = null;
    private Vector2i lastGpsPosition = null;

    private final AnimationUtil distanceAnimation = new AnimationUtil();
    private final AnimationUtil switchAnimation = new AnimationUtil();

    public void update(DrawContext context) {
        if (gpsPosition != null) {
            lastGpsPosition = gpsPosition;
        }

        if (context == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client == null ? null : client.player;
        if (client == null || client.getWindow() == null || player == null) return;

        float scale = RenderService.getInstance().getScale();

        boolean noGps = gpsPosition == null;
        boolean noLastGps = lastGpsPosition == null;
        float distance = !noLastGps ? getDistance(lastGpsPosition) : getDistance(new Vector2i(0, 0));
        float maxDistance = 10f;
        float minDistance = 3f;

        distanceAnimation.update();
        switchAnimation.update();

        distanceAnimation.run(distance <= minDistance ? 0.0 : distance >= maxDistance ? 1.0 : (distance - minDistance) / (maxDistance - minDistance), 500, Easing.EXPO_OUT);
        switchAnimation.run(noGps ? 0.0 : 1.0, 500, Easing.EXPO_OUT);

        if (distanceAnimation.getValue() < 0.1 || switchAnimation.getValue() < 0.1) return;

        float switchAnim = (float) switchAnimation.getValue();

        double combinedAnim = distanceAnimation.getValue() * switchAnim;

        float x = client.getWindow().getScaledWidth() / 2f;
        float y = client.getWindow().getScaledHeight() / 6f;

        float targetX = noGps ? 0f : (float) lastGpsPosition.x;
        float targetY = noGps ? 0f : (float) lastGpsPosition.y;
        float rotation = getRotations(new Vec2f(targetX, targetY)) - player.getYaw();

        float arrowHeight = 12f * scale;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0.0f);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        context.getMatrices().translate(-x, -y, 0.0f);
        PointersModule.getInstance().drawPointer(context, x, y - arrowHeight * 1.75f, 30f * scale, ColorUtil.setAlpha(UIColors.gradient((int) combinedAnim), (int) (255 * combinedAnim)), true);
        RenderUtil.OTHER.scaleStop(context.getMatrices());

        float textY = (y + arrowHeight * (2f - switchAnim));
        Fonts.PS_BOLD.drawCenteredText(context.getMatrices(), String.format("%.1f", distance) + "m", x, textY, 8f * scale, UIColors.textColor((int) (255 * combinedAnim)));
    }

    private float getDistance(Vector2i targetVec) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0f;
        double x = player.getPos().x - targetVec.x;
        double z = player.getPos().z - targetVec.y;
        return MathHelper.sqrt((float) (x * x + z * z));
    }

    private float getRotations(Vec2f vec) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0.0f;
        double x = vec.x - player.getPos().x;
        double z = vec.y - player.getPos().z;
        return (float) -(Math.toDegrees(Math.atan2(x, z)));
    }

    private ClientPlayerEntity getPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.player;
    }
}
