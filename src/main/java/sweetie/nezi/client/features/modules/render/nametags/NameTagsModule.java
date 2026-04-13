package sweetie.nezi.client.features.modules.render.nametags;

import lombok.Getter;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.utils.combat.TargetManager;

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
    public final BooleanSetting showPotions = new BooleanSetting("Показывать зелья").value(true);
    public final BooleanSetting showHands = new BooleanSetting("Предметы в руках").value(true);

    public final TargetManager.EntityFilter entityFilter = new TargetManager.EntityFilter(targets.getList());

    private final NameTagsRender nameTagsRender = new NameTagsRender(this);

    public NameTagsModule() {
        addSettings(targets, scale, showPotions, showHands);
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