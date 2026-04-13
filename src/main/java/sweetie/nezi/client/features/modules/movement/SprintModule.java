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
        EventListener sprintEvent = SprintEvent.getInstance().subscribe(new Listener<>(1, event -> {
            if (shouldForceSprint()) {
                event.setSprint(true);
            }
        }));

        addEvents(sprintEvent);
    }

    public boolean shouldForceSprint() {
        if (mc.player == null) return false;
        
        // If WTap is actively suppressing movement to reset sprint, do NOT force sprint.
        if (WTapModule.getInstance().isEnabled() && WTapModule.getInstance().isSuppressing()) {
            return false;
        }

        AuraModule auraModule = AuraModule.getInstance();
        boolean auraCheck = mode.is("Legit") && auraModule.target != null && auraModule.isEnabled() && auraModule.combatExecutor.combatManager().clickScheduler().isOneTickBeforeAttack();

        return (!mc.options.sprintKey.isPressed() || mc.player.input.movementForward != 0) && (isActuallyMovingForward() || auraCheck);
    }

    @SuppressWarnings("MagicNumber")
    public boolean isActuallyMovingForward() {
        RotationManager rotationManager = RotationManager.getInstance();
        RotationPlan plan = rotationManager.getCurrentRotationPlan();
        if (mode.is("None")) {
            return false;
        }
        if (plan != null && (plan.provider() instanceof StrafeModule || plan.moveCorrection())) {
            return false;
        }

        Rotation currentRotation = rotationManager.getCurrentRotation() != null ? rotationManager.getCurrentRotation() : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float deltaYaw = mc.player.getYaw() - currentRotation.getYaw();
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;

        return forward * MathHelper.cos(deltaYaw * 0.017453292f) + sideways * MathHelper.sin(deltaYaw * 0.017453292f) > 1.0E-5f;
    }

}
