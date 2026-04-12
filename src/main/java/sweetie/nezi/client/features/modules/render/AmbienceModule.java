package sweetie.nezi.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ColorSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.system.backend.Choice;
import sweetie.nezi.api.utils.color.UIColors;

import java.awt.*;

@ModuleRegister(name = "Ambience", category = Category.RENDER)
public class AmbienceModule extends Module {
    @Getter private static final AmbienceModule instance = new AmbienceModule();

    @AllArgsConstructor
    private enum WorldTime implements ModeSetting.NamedChoice {
        NO_CHANGE("No change"),
        DAWN("Dawn"),
        DAY("Day"),
        NOON("Noon"),
        DUSK("Dusk"),
        NIGHT("Night"),
        MID_NIGHT("Mid Night");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    @AllArgsConstructor
    public enum Weather implements ModeSetting.NamedChoice {
        NO_CHANGE("No change"),
        SUNNY("Sunny"),
        RAINY("Rainy"),
        SNOWY("Snowy"),
        THUNDER("Thunder");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    private final ModeSetting time = new ModeSetting("Time").value(WorldTime.DAY).values(WorldTime.values());
    public final ModeSetting weather = new ModeSetting("Weather").value(Weather.SUNNY).values(Weather.values());

    private final BooleanSetting customFog = new BooleanSetting("Custom fog").value(false);
    private final BooleanSetting syncFogWithTheme = new BooleanSetting("Sync with client").value(true).setVisible(customFog::getValue);
    private final ColorSetting fogColor = new ColorSetting("Fog color").value(new Color(200, 200, 200)).setVisible(() -> customFog.getValue() && !syncFogWithTheme.getValue());
    private final SliderSetting fogDistance = new SliderSetting("Fog distance").value(-8f).range(-8f, 25f).step(1f).setVisible(customFog::getValue);
    private final SliderSetting fogDensity = new SliderSetting("Fog density").value(100f).range(0f, 100f).step(1f).setVisible(customFog::getValue);


    public AmbienceModule() {
        addSettings(
                time, weather,
                customFog, syncFogWithTheme, fogColor, fogDistance, fogDensity
        );
    }

    private Color getCurrentFogColor() {
        if (syncFogWithTheme.getValue()) {
            Color themeFog = UIColors.primary(230);
            return new Color(
                    Math.max(0, (int) (themeFog.getRed() * 0.72f)),
                    Math.max(0, (int) (themeFog.getGreen() * 0.72f)),
                    Math.max(0, (int) (themeFog.getBlue() * 0.72f)),
                    themeFog.getAlpha()
            );
        }
        return fogColor.getValue();
    }

    public long getTime(long original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || !isEnabled()) return original;

        WorldTime selected = Choice.getChoiceByName(time.getValue(), WorldTime.values());

        return switch (selected) {
            case NO_CHANGE -> original;
            case DAWN -> 23041L;
            case DAY -> 1000L;
            case NOON -> 6000L;
            case DUSK -> 12610L;
            case NIGHT -> 13000L;
            case MID_NIGHT -> 18000L;
        };
    }

    public boolean applyBackgroundColor() {
        if (!isEnabled() || !customFog.getValue()) return false;

        Color currentFogColor = getCurrentFogColor();

        GlStateManager._clearColor(
                currentFogColor.getRed() / 255f,
                currentFogColor.getGreen() / 255f,
                currentFogColor.getBlue() / 255f,
                currentFogColor.getAlpha() / 255f
        );

        return true;
    }

    public Fog applyCustomFog(Camera camera, float viewDistance, Fog fog) {
        if (!isEnabled() || !customFog.getValue()) return fog;

        float start = MathHelper.clamp(fogDistance.getValue(), -8f, viewDistance);
        float end = MathHelper.clamp(fogDistance.getValue() + fogDensity.getValue(), 0f, viewDistance);
        Color currentFogColor = getCurrentFogColor();

        FogShape shape = fog.shape();
        CameraSubmersionType type = camera.getSubmersionType();

        if (type == CameraSubmersionType.NONE) {
            shape = FogShape.SPHERE;
        }

        return new Fog(start, end, shape,
                currentFogColor.getRed() / 255f,
                currentFogColor.getGreen() / 255f,
                currentFogColor.getBlue() / 255f,
                currentFogColor.getAlpha() / 255f
        );
    }

    @Override public void onEvent() {}
}
