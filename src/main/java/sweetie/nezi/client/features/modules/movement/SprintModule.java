package sweetie.nezi.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.move.SprintEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
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

    public int tickStop = 0;

    public final ModeSetting mode = new ModeSetting("Mode").value("Legit").values("Legit", "Packet", "None");

    public SprintModule() {
        addSettings(mode);
        setEnabled(true);
    }

    @Override
    public void onEvent() {
        EventListener sprintEvent = SprintEvent.getInstance().subscribe(new Listener<>(0, event -> {
            if (mc.player == null) {
                event.setSprint(false);
                return;
            }

            if (isWTapSuppressing()) {
                event.setSprint(false);
            } else if (mode.is("Packet") && shouldForceSprint()) {
                event.setSprint(true);
            }
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) {
                return;
            }

            if (mode.is("Legit")) {
                boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
                boolean sneaking = mc.player.isSneaking() && !mc.player.isSwimming();

                if (tickStop > 0 || sneaking || isWTapSuppressing()) {
                    mc.player.setSprinting(false);
                } else if (canStartSprinting() && !horizontal && !mc.options.sprintKey.isPressed()) {
                    mc.player.setSprinting(true);
                }
            }

            tickStop--;
        }));

        addEvents(sprintEvent, updateEvent);
    }

    private boolean isWTapSuppressing() {
        return WTapModule.getInstance().isEnabled() && WTapModule.getInstance().isSuppressing();
    }

    private boolean canStartSprinting() {
        if (mc.player == null) {
            return false;
        }

        boolean hasBlindness = mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
        boolean hasForwardMovement = mc.player.input != null && mc.player.input.hasForwardMovement();
        return !mc.player.isSprinting() && hasForwardMovement && !hasBlindness && !mc.player.isGliding();
    }

    public boolean shouldForceSprint() {
        if (mc.player == null) return false;
        if (mc.player.isSneaking()) return false;
        if (mc.player.horizontalCollision) return false;
        if (isWTapSuppressing()) return false;

        AuraModule aura = AuraModule.getInstance();
        boolean auraCheck = aura.isEnabled() && aura.target != null;

        return (mc.player.input.movementForward > 0 || auraCheck) && isActuallyMovingForward();
    }

    public boolean isActuallyMovingForward() {
        if (mode.is("None") || mc.player == null) return false;

        RotationManager rm = RotationManager.getInstance();
        RotationPlan plan = rm.getCurrentRotationPlan();

        if (plan != null && plan.provider() instanceof StrafeModule) return false;

        Rotation cur = rm.getCurrentRotation() != null
                ? rm.getCurrentRotation()
                : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float dy = mc.player.getYaw() - cur.getYaw();
        float fwd = mc.player.input.movementForward;
        float sid = mc.player.input.movementSideways;

        return fwd * MathHelper.cos(dy * 0.017453292f) + sid * MathHelper.sin(dy * 0.017453292f) > 1.0E-5f;
    }
}
