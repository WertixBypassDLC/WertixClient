package sweetie.nezi.client.features.modules.movement;

import lombok.Getter;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.ModeSetting;

@ModuleRegister(name = "Move Fix", category = Category.MOVEMENT)
public class MoveFixModule extends Module {
    @Getter private static final MoveFixModule instance = new MoveFixModule();

    public final ModeSetting mode = new ModeSetting("Mode").value("Free").values("Free", "Focus", "Pursuit");

    public MoveFixModule() {
        addSettings(mode);
    }

    @Override
    public void onEvent() {}

    public float getMovementYaw() {
        if (!isEnabled() || mc.player == null) return (mc.player != null) ? mc.player.getYaw() : 0;

        if (mode.is("Focus")) {
            net.minecraft.entity.LivingEntity target = sweetie.nezi.client.features.modules.combat.AuraModule.getInstance().target;
            if (target != null) {
                double dx = target.getX() - mc.player.getX();
                double dz = target.getZ() - mc.player.getZ();
                return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
            }
        }
        return mc.player.getYaw();
    }

    public static boolean enabled() {
        return instance.isEnabled();
    }

    public static boolean isFree() {
        return instance.mode.is("Free");
    }
}