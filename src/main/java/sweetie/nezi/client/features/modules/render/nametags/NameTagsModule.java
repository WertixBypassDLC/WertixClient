package sweetie.nezi.client.features.modules.render.nametags;

import lombok.Getter;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ColorSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.utils.combat.TargetManager;

import java.awt.*;
import java.util.function.Supplier;

@ModuleRegister(name = "Name Tags", category = Category.RENDER)
public class NameTagsModule extends Module {
    @Getter private static final NameTagsModule instance = new NameTagsModule();

    public final MultiBooleanSetting targets = new MultiBooleanSetting("Цели").value(
            new BooleanSetting("Себя").value(false),
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Голые").value(true),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Мобы").value(false),
            new BooleanSetting("Жители").value(false)
    );
    public final SliderSetting scale = new SliderSetting("Масштаб").value(1f).range(0.1f, 2f).step(0.1f);
    public final MultiBooleanSetting information = new MultiBooleanSetting("Информация").value(
            new BooleanSetting("Предметы").value(true),
            new BooleanSetting("Зелья").value(true)
    );

    private final Supplier<Boolean> itemsIsEnabled = () -> information.isEnabled("Предметы");

    public final MultiBooleanSetting options = new MultiBooleanSetting("Настройки").value(
            new BooleanSetting("Особые предметы").value(false).setVisible(itemsIsEnabled),
            new BooleanSetting("Только в руках").value(false).setVisible(itemsIsEnabled)
    );

    public final SliderSetting glassy = new SliderSetting("Стеклянность").value(0.5f).range(0.0f, 1f).step(0.1f);
    public final ColorSetting textColor = new ColorSetting("Цвет текста").value(new Color(255, 255, 255));
    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(20, 20, 20));
    public final ColorSetting friendColor = new ColorSetting("Цвет друга").value(new Color(132, 229, 121)).setVisible(() -> targets.isEnabled("Игроки") || targets.isEnabled("Голые") || targets.isEnabled("Себя"));

    public final TargetManager.EntityFilter entityFilter = new TargetManager.EntityFilter(targets.getList());

    private final NameTagsRender nameTagsRender = new NameTagsRender(this);

    public NameTagsModule() {
        addSettings(targets, scale, information, options, glassy, textColor, color, friendColor);
    }

    @Override
    public void onEvent() {
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(1, event -> {
            entityFilter.targetSettings = targets.getList();
            entityFilter.needFriends = true;

            nameTagsRender.onRender(event);
        }));

        addEvents(render2DEvent);
    }
}