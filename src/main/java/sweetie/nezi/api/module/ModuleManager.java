package sweetie.nezi.api.module;

import lombok.Getter;
import sweetie.nezi.client.features.modules.combat.*;
import sweetie.nezi.client.features.modules.movement.*;
import sweetie.nezi.client.features.modules.movement.fly.FlightModule;
import sweetie.nezi.client.features.modules.movement.noslow.NoSlowModule;
import sweetie.nezi.client.features.modules.movement.speed.SpeedModule;
import sweetie.nezi.client.features.modules.movement.spider.SpiderModule;
import sweetie.nezi.client.features.modules.other.*;
import sweetie.nezi.client.features.modules.player.*;
import sweetie.nezi.client.features.modules.render.*;
import sweetie.nezi.client.features.modules.render.nametags.NameTagsModule;
import sweetie.nezi.client.features.modules.render.particles.ParticlesModule;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspModule;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleManager {
    @Getter private final static ModuleManager instance = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();

    public void load() {
        register(
                SprintModule.getInstance(),
                ClickGUIModule.getInstance(),
                AmbienceModule.getInstance(),
                AuraModule.getInstance(),
                LegitAuraModule.getInstance(),
                AimAssistModule.getInstance(),
                TriggerBotModule.getInstance(), // Добавлен TriggerBot
                AutoRespawnModule.getInstance(),
                InterfaceModule.getInstance(),
                NoDelayModule.getInstance(),
                PotionTrackerModule.getInstance(),
                UseTrackerModule.getInstance(),
                AutoLeaveModule.getInstance(),
                AntiAfkModule.getInstance(),
                NameTagsModule.getInstance(),
                IRCModule.getInstance(),
                WaterSpeedModule.getInstance(),
                BlockFlyModule.getInstance(),
                AutoToolModule.getInstance(),
                ChestStealerModule.getInstance(),
                SpeedModule.getInstance(),
                TPAcceptModule.getInstance(),
                VelocityModule.getInstance(),
                NoSlowModule.getInstance(),
                AutoTotemModule.getInstance(),
                AspectRatioModule.getInstance(),
                InventoryMoveModule.getInstance(),
                ItemSwapModule.getInstance(),
                RemovalsModule.getInstance(),
                ClickPearlModule.getInstance(),
                ElytraSwapModule.getInstance(),
                SwingAnimationModule.getInstance(),
                ViewModelModule.getInstance(),
                ChinaHatModule.getInstance(),
                PointersModule.getInstance(),
                FuntimeHelperModule.getInstance(),
                PredictionsModule.getInstance(),
                NoFriendHurtModule.getInstance(),
                NoEntityTraceModule.getInstance(),
                ParticlesModule.getInstance(),
                TapeMouseModule.getInstance(),
                SeeInvisiblesModule.getInstance(),
                StreamerModule.getInstance(),
                ToggleSoundsModule.getInstance(),
                NightVisionModule.getInstance(),
                AutoBuyModule.getInstance(),
                AutoSetupModule.getInstance(),
                AutoSellModule.getInstance(),
                AuctionHelperModule.getInstance(),
                WardenHelperModule.getInstance(),
                TargetEspModule.getInstance(),
                BlockHighlightModule.getInstance(),
                BlockESPModule.getInstance(),
                ShulkerViewModule.getInstance(),
                SpiderModule.getInstance(),
                FlightModule.getInstance(),
                WTapModule.getInstance(),
                ItemEspModule.getInstance()
        );

        modules.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public void register(Module... modules) {
        this.modules.addAll(List.of(modules));
    }
}
