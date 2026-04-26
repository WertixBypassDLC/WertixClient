package sweetie.nezi.api.utils.combat;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.utils.rotation.manager.Rotation;

import java.util.List;

@Getter
@Accessors(fluent = true, chain = true)
public class CombatExecutor {
    private final CombatManager combatManager = new CombatManager();
    private final MultiBooleanSetting options = new MultiBooleanSetting("Attack options").value(
            new BooleanSetting("Only crits").value(true),
            new BooleanSetting("Smart crits").value(false),
            new BooleanSetting("Dynamic cooldown").value(true),
            new BooleanSetting("Shield break").value(true),
            new BooleanSetting("UnPress shield").value(true),
            new BooleanSetting("Ignore walls").value(false),
            new BooleanSetting("No attack if eat").value(false)
    );
    private final SliderSetting sprintResetTicks = new SliderSetting("Sprint reset ticks").value(2.0F).range(0.0F, 6.0F).step(1.0F);

    public void performAttack() {
        combatManager.handleAttack();
    }

    public static class CombatConfigurable {
        public final LivingEntity target;
        public final Rotation rotation;
        public final float distance;
        public final Box hitBox;

        public final boolean onlyCrits;
        public final boolean smartCrits;
        public final boolean dynamicCooldown;
        public final boolean raytrace;
        public final boolean shieldBreak;
        public final boolean unPressShield;
        public final boolean ignoreWalls;
        public final boolean noAttackIfEat;
        public final int sprintResetTicks;

        public CombatConfigurable(
                LivingEntity target,
                Rotation rotation,
                float distance,
                Box hitBox,
                boolean onlyCrits,
                boolean smartCrits,
                boolean dynamicCooldown,
                boolean raytrace,
                boolean shieldBreak,
                boolean unPressShield,
                boolean ignoreWalls,
                boolean noAttackIfEat,
                int sprintResetTicks
        ) {
            this.target = target;
            this.rotation = rotation;
            this.distance = distance;
            this.hitBox = hitBox;
            this.onlyCrits = onlyCrits;
            this.smartCrits = smartCrits;
            this.dynamicCooldown = dynamicCooldown;
            this.raytrace = raytrace;
            this.shieldBreak = shieldBreak;
            this.unPressShield = unPressShield;
            this.ignoreWalls = ignoreWalls;
            this.noAttackIfEat = noAttackIfEat;
            this.sprintResetTicks = sprintResetTicks;
        }

        public CombatConfigurable(
                LivingEntity target,
                Rotation rotation,
                float distance,
                Box hitBox,
                List<String> options,
                float sprintResetTicks
        ) {
            this(
                    target,
                    rotation,
                    distance,
                    hitBox,
                    options.contains("Only crits") || options.contains("Only Critical"),
                    options.contains("Smart crits") || options.contains("Smart Crits"),
                    options.contains("Dynamic cooldown") || options.contains("Dynamic Cooldown"),
                    true,
                    options.contains("Shield break") || options.contains("Break Shield"),
                    options.contains("UnPress shield") || options.contains("UnPress Shield") || options.contains("Always shield"),
                    options.contains("Ignore walls") || options.contains("Ignore The Walls"),
                    options.contains("No attack if eat") || options.contains("No Attack When Eat"),
                    Math.round(sprintResetTicks)
            );
        }
    }
}
