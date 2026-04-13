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
import sweetie.nezi.client.features.modules.render.targetesp.modes.*;

import java.awt.Color;

@ModuleRegister(name = "Target Esp", category = Category.RENDER)
public class TargetEspModule extends Module {
    @Getter private static final TargetEspModule instance = new TargetEspModule();

    private final TargetEspComets espComets = new TargetEspComets();
    private final TargetEspCrystals espCrystals = new TargetEspCrystals();
    private final TargetEspFigure espFigure = new TargetEspFigure();
    private final TargetEspSkulls espSkulls = new TargetEspSkulls();
    private final TargetEspGhost espGhost = new TargetEspGhost();
    private final TargetEspAtom espAtom = new TargetEspAtom();
    private final TargetEspGarland espGarland = new TargetEspGarland();
    private final TargetEspRofl espRofl = new TargetEspRofl();

    // ─── ОСНОВНЫЕ НАСТРОЙКИ ──────────────────────────────────────────────────────────

    private final ModeSetting mode = new ModeSetting("Режим").value("3D").values(
            "Кометы", "3D", "Рофл"
    );

    private final ModeSetting style3D = new ModeSetting("Стиль 3D").value("Призраки").values(
            "Призраки", "Кристаллы", "Фигуры", "Черепа", "Атом", "Гирлянда"
    ).setVisible(() -> mode.is("3D"));

    // ─── АНИМАЦИИ И РАЗМЕРЫ ─────────────────────────────────────────────────────────

    private final ModeSetting animation = new ModeSetting("Тип анимации").value("Появление").values("Появление", "Затухание", "Нет");
    private final SliderSetting duration = new SliderSetting("Время анимации").value(3f).range(1f, 20f).step(1f);
    private final SliderSetting size = new SliderSetting("Базовый масштаб").value(1f).range(0.1f, 2f).step(0.1f);
    private final SliderSetting smoothness = new SliderSetting("Плавность следования").value(0.24f).range(0.05f, 0.55f).step(0.01f);

    // Пояснение: Начальный размер при захвате цели
    private final SliderSetting inSize = new SliderSetting("Начальный размер").value(0f).range(0f, 1f).step(0.1f)
            .setVisible(() -> animation.is("Появление"));

    // Пояснение: Размер, в который превратится ESP при смерти/потере цели
    private final SliderSetting outSize = new SliderSetting("Размер при исчезновении").value(2f).range(1f, 2f).step(0.1f)
            .setVisible(() -> animation.is("Затухание"));

    // Пояснение: Оставлять ESP на месте убийства, чтобы он не проваливался под землю вместе с мертвой моделькой
    public final BooleanSetting lastPosition = new BooleanSetting("Оставаться на месте смерти").value(true);

    // ─── ОБЩИЕ ВИЗУАЛЬНЫЕ ───────────────────────────────────────────────────────────

    private final SliderSetting speed = new SliderSetting("Скорость вращения").value(1.0f).range(0.1f, 3.0f).step(0.1f)
            .setVisible(() -> mode.is("3D") || mode.is("Рофл"));

    private final SliderSetting lineWidth = new SliderSetting("Толщина линий").value(2.0f).range(0.5f, 4.0f).step(0.1f)
            .setVisible(() -> mode.is("3D") && (style3D.is("Фигуры") || style3D.is("Черепа") || style3D.is("Атом") || style3D.is("Гирлянда")));

    // ─── ИНДИВИДУАЛЬНЫЕ НАСТРОЙКИ СТИЛЕЙ ────────────────────────────────────────────

    // Настройки "Призраки"
    private final ModeSetting ghostTrajectory = new ModeSetting("Движение (Призраки)").value("Двойная спираль").values("Двойная спираль", "Орбита", "Хаос")
            .setVisible(() -> mode.is("3D") && style3D.is("Призраки"));
    private final SliderSetting ghostCount = new SliderSetting("Количество (Призраки)").value(40f).range(10f, 150f).step(1f)
            .setVisible(() -> mode.is("3D") && style3D.is("Призраки"));
    private final SliderSetting ghostSize = new SliderSetting("Размер (Призраки)").value(0.3f).range(0.1f, 1.5f).step(0.05f)
            .setVisible(() -> mode.is("3D") && style3D.is("Призраки"));

    // Настройки "Кристаллы"
    private final SliderSetting crystalSize = new SliderSetting("Размер (Кристаллы)").value(0.8f).range(0.1f, 2.0f).step(0.1f)
            .setVisible(() -> mode.is("3D") && style3D.is("Кристаллы"));
    private final SliderSetting crystalCount = new SliderSetting("Количество (Кристаллы)").value(7f).range(3f, 20f).step(1f)
            .setVisible(() -> mode.is("3D") && style3D.is("Кристаллы"));

    // Настройки "Фигуры"
    private final SliderSetting figureSize = new SliderSetting("Размер (Фигуры)").value(0.9f).range(0.3f, 2.4f).step(0.1f)
            .setVisible(() -> mode.is("3D") && style3D.is("Фигуры"));
    private final SliderSetting figureDepth = new SliderSetting("Глубина 3D (Фигуры)").value(0.18f).range(0.0f, 0.6f).step(0.02f)
            .setVisible(() -> mode.is("3D") && style3D.is("Фигуры"));
    private final ModeSetting figurePreset = new ModeSetting("Форма (Фигуры)").value("Звезда").values("Звезда", "Ромб", "Треугольник", "Волна", "Свой")
            .setVisible(() -> mode.is("3D") && style3D.is("Фигуры"));
    private final StringSetting customFigure = new StringSetting("Точки (Форма)").value("0,1;0.28,0.34;1,0;0.35,-0.22;0,-1;-0.35,-0.22;-1,0;-0.28,0.34")
            .setVisible(() -> mode.is("3D") && style3D.is("Фигуры") && figurePreset.is("Свой"));

    // Настройки "Черепа"
    private final ModeSetting skullPreset = new ModeSetting("Стиль (Черепа)").value("Призраки").values("Призраки", "Орбита", "Вращение")
            .setVisible(() -> mode.is("3D") && style3D.is("Черепа"));
    private final SliderSetting skullSize = new SliderSetting("Размер (Черепа)").value(0.9f).range(0.4f, 1.8f).step(0.1f)
            .setVisible(() -> mode.is("3D") && style3D.is("Черепа"));
    private final SliderSetting skullOrbitDistance = new SliderSetting("Радиус (Черепа)").value(1.0f).range(0.3f, 3.0f).step(0.1f)
            .setVisible(() -> mode.is("3D") && style3D.is("Черепа"));
    private final SliderSetting skullOpacity = new SliderSetting("Прозрачность (Черепа)").value(0.35f).range(0.05f, 1.0f).step(0.05f)
            .setVisible(() -> mode.is("3D") && style3D.is("Черепа"));
    private final BooleanSetting ghostGlow = new BooleanSetting("Аура (Черепа)").value(true)
            .setVisible(() -> mode.is("3D") && style3D.is("Черепа"));
    private final SliderSetting ghostGlowIntensity = new SliderSetting("Яркость ауры (Черепа)").value(0.6f).range(0.1f, 1.0f).step(0.05f)
            .setVisible(() -> mode.is("3D") && style3D.is("Черепа") && ghostGlow.getValue());

    // Настройки "Рофл"
    private final ModeSetting animal = new ModeSetting("Животное (Рофл)").value("Свинья").values(
            "Свинья", "Летучая мышь", "Попугай", "Фея", "Пчела", "Векс", "Лисичка", "Лягушка", "Иглобрюх", "Слайм"
    ).setVisible(() -> mode.is("Рофл"));

    public TargetEspModule() {
        addSettings(
                mode, style3D,
                animation, duration, size, speed, smoothness, inSize, outSize, lastPosition,
                lineWidth,
                ghostTrajectory, ghostCount, ghostSize,
                crystalSize, crystalCount,
                figureSize, figureDepth, figurePreset, customFigure,
                skullPreset, skullSize, skullOrbitDistance, skullOpacity, ghostGlow, ghostGlowIntensity,
                animal
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

    private TargetEspMode getActiveMode() {
        if (mode.is("Рофл")) return espRofl;
        if (mode.is("Кометы")) return espComets;

        return switch (style3D.getValue()) {
            case "Кристаллы" -> espCrystals;
            case "Фигуры" -> espFigure;
            case "Черепа" -> espSkulls;
            case "Атом" -> espAtom;
            case "Гирлянда" -> espGarland;
            default -> espGhost;
        };
    }

    public float getSpeed() { return speed.getValue(); }
    public float getLineWidth() { return lineWidth.getValue(); }

    public String getGhostTrajectory() { return ghostTrajectory.getValue(); }
    public float getGhostCount() { return ghostCount.getValue(); }
    public float getGhostSize() { return ghostSize.getValue(); }

    public float getCrystalSize() { return crystalSize.getValue(); }
    public float getCrystalCount() { return crystalCount.getValue(); }

    public float getFigureSize() { return figureSize.getValue(); }
    public float getFigureDepth() { return figureDepth.getValue(); }
    public String getFigurePreset() { return figurePreset.getValue(); }
    public String getCustomFigure() { return customFigure.getValue(); }

    public String getSkullPreset() { return skullPreset.getValue(); }
    public float getSkullSize() { return skullSize.getValue(); }
    public float getSkullOrbitDistance() { return skullOrbitDistance.getValue(); }
    public float getSkullOpacity() { return skullOpacity.getValue(); }
    public boolean isGhostGlowEnabled() { return ghostGlow.getValue(); }
    public float getGhostGlowIntensity() { return ghostGlowIntensity.getValue(); }

    public float getSmoothness() { return smoothness.getValue(); }
    public String getAnimal() { return animal.getValue(); }

    public Color getCustomColor(float red) {
        Color base = sweetie.nezi.api.utils.color.UIColors.primary(255);
        float i = 1f - red;
        return new Color(
                (int) (base.getRed() * i + 255 * red),
                (int) (base.getGreen() * i + 0 * red),
                (int) (base.getBlue() * i + 0 * red),
                base.getAlpha()
        );
    }
}