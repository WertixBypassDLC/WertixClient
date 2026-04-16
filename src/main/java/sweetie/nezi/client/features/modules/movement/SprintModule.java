package sweetie.nezi.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.player.move.SprintEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;
import sweetie.nezi.api.utils.rotation.manager.RotationPlan;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.features.modules.combat.WTapModule;

@ModuleRegister(name = "Sprint", category = Category.MOVEMENT)
public class SprintModule extends Module {
    @Getter private static final SprintModule instance = new SprintModule();

    public final ModeSetting mode = new ModeSetting("Mode").value("Legit").values("Legit", "Packet", "None");

    public SprintModule() {
        addSettings(mode);
        setEnabled(true);
    }

    @Override
    public void onEvent() {
        EventListener sprintEvent = SprintEvent.getInstance().subscribe(new Listener<>(0, event -> {
            if (isWTapSuppressing()) {
                event.setSprint(false);
            } else if (shouldForceSprint()) {
                event.setSprint(true);
            }
        }));
        addEvents(sprintEvent);
    }

    private boolean isWTapSuppressing() {
        return WTapModule.getInstance().isEnabled() && WTapModule.getInstance().isSuppressing();
    }

    public boolean shouldForceSprint() {
        if (mc.player == null) return false;
        if (mc.player.isSneaking()) return false;
        if (mc.player.horizontalCollision) return false;
        if (isWTapSuppressing()) return false;

        AuraModule aura = AuraModule.getInstance();
        boolean auraCheck = mode.is("Legit") && aura.isEnabled() && aura.target != null;

        return (mc.player.input.movementForward > 0 || auraCheck) && isActuallyMovingForward();
    }

    public boolean isActuallyMovingForward() {
        if (mode.is("None") || mc.player == null) return false;

        RotationManager rm   = RotationManager.getInstance();
        RotationPlan    plan = rm.getCurrentRotationPlan();

        if (plan != null && (plan.provider() instanceof StrafeModule || plan.moveCorrection())) return false;

        Rotation cur = rm.getCurrentRotation() != null
                ? rm.getCurrentRotation()
                : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float dy  = mc.player.getYaw() - cur.getYaw();
        float fwd = mc.player.input.movementForward;
        float sid = mc.player.input.movementSideways;

        return fwd * MathHelper.cos(dy * 0.017453292f) + sid * MathHelper.sin(dy * 0.017453292f) > 1.0E-5f;
    }
}