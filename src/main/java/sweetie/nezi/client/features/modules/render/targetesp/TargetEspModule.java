package sweetie.nezi.client.features.modules.render.targetesp;

import lombok.Getter;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.module.setting.StringSetting;
import sweetie.nezi.client.features.modules.render.targetesp.modes.TargetEspComets;
import sweetie.nezi.client.features.modules.render.targetesp.modes.TargetEspThreeDimensional;

@ModuleRegister(name = "Target Esp", category = Category.RENDER)
public class TargetEspModule extends Module {
    @Getter private static final TargetEspModule instance = new TargetEspModule();

    private final TargetEspComets espComets = new TargetEspComets();
    private final TargetEspThreeDimensional espThreeDimensional = new TargetEspThreeDimensional();

    private final ModeSetting mode = new ModeSetting("Mode").value("Comets").values("Comets", "3D");
    private final ModeSetting threeDimensionalStyle = new ModeSetting("3D Style").value("Crystals").values("Crystals", "Figure", "Skulls")
            .setVisible(this::isThreeDimensionalMode);
    private final ModeSetting animation = new ModeSetting("Animation").value("In").values("In", "Out", "None");
    private final SliderSetting duration = new SliderSetting("Duration").value(3f).range(1f, 20f).step(1f);
    private final SliderSetting size = new SliderSetting("Size").value(1f).range(0.1f, 2f).step(0.1f);
    private final SliderSetting smoothness = new SliderSetting("Плавность").value(0.24f).range(0.05f, 0.55f).step(0.01f);
    private final SliderSetting inSize = new SliderSetting("In size").value(0f).range(0f, 1f).step(0.1f).setVisible(() -> animation.is("In"));
    private final SliderSetting outSize = new SliderSetting("Out size").value(2f).range(1f, 2f).step(0.1f).setVisible(() -> animation.is("Out"));
    public final BooleanSetting lastPosition = new BooleanSetting("Last position").value(true);
    private final SliderSetting lineWidth = new SliderSetting("Line width").value(2.0f).range(0.5f, 4.0f).step(0.1f)
            .setVisible(this::isThreeDimensionalMode);
    private final SliderSetting crystalSize = new SliderSetting("Crystal size").value(0.8f).range(0.1f, 2.0f).step(0.1f)
            .setVisible(this::isCrystalsStyle);
    private final SliderSetting crystalCount = new SliderSetting("Crystal count").value(20.0f).range(8.0f, 30.0f).step(1.0f)
            .setVisible(this::isCrystalsStyle);
    private final SliderSetting figureSize = new SliderSetting("Figure size").value(0.9f).range(0.3f, 2.4f).step(0.1f)
            .setVisible(this::isFigureStyle);
    private final SliderSetting figureDepth = new SliderSetting("Figure depth").value(0.18f).range(0.0f, 0.6f).step(0.02f)
            .setVisible(this::isFigureStyle);
    private final ModeSetting figurePreset = new ModeSetting("Figure preset").value("Star").values("Star", "Diamond", "Triangle", "Wave", "Custom")
            .setVisible(this::isFigureStyle);
    private final StringSetting customFigure = new StringSetting("Figure points")
            .value("0,1;0.28,0.34;1,0;0.35,-0.22;0,-1;-0.35,-0.22;-1,0;-0.28,0.34")
            .setVisible(() -> isFigureStyle() && figurePreset.is("Custom"));
    private final ModeSetting skullPreset = new ModeSetting("Skull preset").value("Призраки").values("Призраки", "Орбита", "Вращение")
            .setVisible(this::isSkullsStyle);
    private final SliderSetting skullSize = new SliderSetting("Skull size").value(0.9f).range(0.4f, 1.8f).step(0.1f)
            .setVisible(this::isSkullsStyle);
    private final SliderSetting skullOrbitDistance = new SliderSetting("Orbit distance").value(1.0f).range(0.3f, 3.0f).step(0.1f)
            .setVisible(this::isSkullsStyle);
    private final SliderSetting skullOpacity = new SliderSetting("Skull opacity").value(0.35f).range(0.05f, 1.0f).step(0.05f)
            .setVisible(this::isSkullsStyle);
    private final BooleanSetting ghostGlow = new BooleanSetting("Ghost glow").value(true)
            .setVisible(this::isSkullsStyle);
    private final SliderSetting ghostGlowIntensity = new SliderSetting("Glow intensity").value(0.6f).range(0.1f, 1.0f).step(0.05f)
            .setVisible(() -> isSkullsStyle() && ghostGlow.getValue());

    public TargetEspModule() {
        addSettings(
                mode, threeDimensionalStyle,
                animation, duration, size, smoothness, inSize, outSize, lastPosition,
                lineWidth, crystalSize, crystalCount,
                figureSize, figureDepth, figurePreset, customFigure,
                skullPreset, skullSize, skullOrbitDistance, skullOpacity, ghostGlow, ghostGlowIntensity
        );
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            TargetEspMode.updatePositions();
            getActiveMode().onRender3D(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            TargetEspMode activeMode = getActiveMode();
            activeMode.updateAnimation(duration.getValue().longValue() * 50, animation.getValue(), size.getValue(), inSize.getValue(), outSize.getValue());
            activeMode.updateTarget();
            activeMode.onUpdate();
        }));

        addEvents(render3DEvent, updateEvent);
    }

    public boolean isThreeDimensionalMode() {
        return mode.is("3D");
    }

    public boolean isCrystalsStyle() {
        return isThreeDimensionalMode() && threeDimensionalStyle.is("Crystals");
    }

    public boolean isFigureStyle() {
        return isThreeDimensionalMode() && threeDimensionalStyle.is("Figure");
    }

    public boolean isSkullsStyle() {
        return isThreeDimensionalMode() && threeDimensionalStyle.is("Skulls");
    }

    public float getLineWidth() {
        return lineWidth.getValue();
    }

    public float getCrystalSize() {
        return crystalSize.getValue();
    }

    public int getCrystalCount() {
        return crystalCount.getValue().intValue();
    }

    public float getFigureSize() {
        return figureSize.getValue();
    }

    public float getFigureDepth() {
        return figureDepth.getValue();
    }

    public float getSmoothness() {
        return smoothness.getValue();
    }

    public String getFigurePreset() {
        return figurePreset.getValue();
    }

    public String getCustomFigure() {
        return customFigure.getValue();
    }

    public String getSkullPreset() {
        return skullPreset.getValue();
    }

    public float getSkullSize() {
        return skullSize.getValue();
    }

    public float getSkullOrbitDistance() {
        return skullOrbitDistance.getValue();
    }

    public float getSkullOpacity() {
        return skullOpacity.getValue();
    }

    public boolean isGhostGlowEnabled() {
        return ghostGlow.getValue();
    }

    public float getGhostGlowIntensity() {
        return ghostGlowIntensity.getValue();
    }

    private TargetEspMode getActiveMode() {
        return isThreeDimensionalMode() ? espThreeDimensional : espComets;
    }
}
