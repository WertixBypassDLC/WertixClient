package sweetie.nezi.client.features.modules.movement;

import lombok.Getter;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.other.RotationUpdateEvent;
import sweetie.nezi.api.event.events.player.other.MovementInputEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.utils.player.MoveUtil;
import sweetie.nezi.api.utils.rotation.manager.Rotation;
import sweetie.nezi.api.utils.rotation.manager.RotationManager;
import sweetie.nezi.api.utils.rotation.manager.RotationPlan;
import sweetie.nezi.api.utils.rotation.manager.RotationStrategy;
import sweetie.nezi.api.utils.task.TaskPriority;

@ModuleRegister(name = "Strafe", category = Category.MOVEMENT)
public class StrafeModule extends Module {
    @Getter private static final StrafeModule instance = new StrafeModule();

    @Override
    public void onEvent() {
        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            RotationManager.getInstance().addRotation(new Rotation(mc.player.getYaw() + dony(), mc.player.getPitch()), RotationStrategy.TARGET, TaskPriority.LOWEST, this);
        }));

        EventListener movementInputEvent = MovementInputEvent.getInstance().subscribe(new Listener<>(event -> {
            RotationPlan currentPlan = RotationManager.getInstance().getCurrentRotationPlan();
            if (currentPlan != null && currentPlan.provider() != this) return;

            boolean w = MoveUtil.w();
            boolean s = MoveUtil.s();
            boolean a = MoveUtil.a();
            boolean d = MoveUtil.d();

            if (w && s) w = s = false;
            if (a && d) a = d = false;

            event.getDirectionalInput().setLeft(false);
            event.getDirectionalInput().setRight(false);
            event.getDirectionalInput().setBackwards(false);
            event.getDirectionalInput().setForwards(w || s || a || d);
        }));

        addEvents(rotationUpdateEvent, movementInputEvent);
    }

    private float dony() {
        boolean w = MoveUtil.w();
        boolean s = MoveUtil.s();
        boolean a = MoveUtil.a();
        boolean d = MoveUtil.d();

        if (w && s) {
            w = false;
            s = false;
        }

        if (a && d) {
            a = false;
            d = false;
        }

        if (w) {
            if (a) return -45f;
            if (d) return 45f;
            return 0f;
        }

        if (s) {
            if (a) return -135f;
            if (d) return 135f;
            return 180f;
        }

        if (a) return -90f;
        if (d) return 90f;

        return 0f;
    }
}
