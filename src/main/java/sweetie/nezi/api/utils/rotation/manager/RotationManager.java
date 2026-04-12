package sweetie.nezi.api.utils.rotation.manager;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.*;
import sweetie.nezi.api.event.events.other.RotationUpdateEvent;
import sweetie.nezi.api.event.events.player.other.MovementInputEvent;
import sweetie.nezi.api.event.events.player.other.PostRotationMovementInputEvent;
import sweetie.nezi.api.event.events.player.move.VelocityEvent;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.rotation.RotationUtil;
import sweetie.nezi.api.utils.task.TaskPriority;
import sweetie.nezi.api.utils.task.TaskProcessor;

@Getter
@Setter
public class RotationManager implements QuickImports {
    @Getter private final static RotationManager instance = new RotationManager();

    private RotationPlan lastRotationPlan;
    private final TaskProcessor<RotationPlan> rotationPlanRequestProcessor = new TaskProcessor<>();
    private Rotation currentRotation;
    private Rotation previousRotation;
    private Rotation serverRotation = Rotation.DEFAULT;

    public void load() {
        VelocityEvent.getInstance().subscribe(new Listener<>(event -> {
            if (getCurrentRotationPlan() != null && getCurrentRotationPlan().moveCorrection()) {
                event.setVelocity(Entity.movementInputToVelocity(event.getMovementInput(), event.getSpeed(), getRotation().getYaw()));
            }
        }));

        PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isSend()) {
                Rotation rotation;

                if (event.packet() instanceof PlayerMoveC2SPacket packet) {
                    if (packet.changesLook()) {
                        rotation = new Rotation(packet.getYaw(1f), packet.getPitch(1f));
                    } else {
                        return;
                    }
                } else if (event.packet() instanceof PlayerPositionLookS2CPacket packet) {
                    rotation = new Rotation(packet.change().yaw(), packet.change().pitch());
                } else {
                    return;
                }

                if (!PacketEvent.getInstance().isCancel()) {
                    serverRotation = rotation;
                }
            }
        }));

        MovementInputEvent.getInstance().subscribe(new Listener<>(event -> {
            PostRotationMovementInputEvent.getInstance().call();
        }));

        GameLoopEvent.getInstance().subscribe(new Listener<>(event -> {
            if (getPlayer() == null) return;
            RotationUpdateEvent.getInstance().call();
            update();
        }));
    }

    private void setRotation(Rotation value) {
        ClientPlayerEntity player = getPlayer();
        previousRotation = (value == null)
                ? (currentRotation != null ? currentRotation : player != null ? new Rotation(player.getYaw(), player.getPitch()) : Rotation.DEFAULT)
                : currentRotation;
        currentRotation = value;
    }

    public Rotation getRotation() {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return Rotation.DEFAULT;
        return currentRotation != null ? currentRotation : RotationUtil.fromVec2f(player.getRotationClient());
    }

    public Rotation getPreviousRotation() {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return Rotation.DEFAULT;
        return previousRotation != null ? previousRotation : RotationUtil.fromVec2f(player.getRotationClient());
    }

    public RotationPlan getCurrentRotationPlan() {
        RotationPlan active = rotationPlanRequestProcessor.fetchActiveTaskValue();
        return active != null ? active : lastRotationPlan;
    }

    public void addRotation(Rotation.VecRotation vecRotation, LivingEntity entity, RotationStrategy configurable, TaskPriority requestPriority, Module provider) {
        addRotation(configurable.createRotationPlan(vecRotation.rotation(), vecRotation.vec(), entity, provider), requestPriority, provider);
    }

    public void addRotation(Rotation rotation, RotationStrategy configurable, TaskPriority requestPriority, Module provider) {
        addRotation(configurable.createRotationPlan(rotation, provider), requestPriority, provider);
    }

    private void addRotation(RotationPlan plan, TaskPriority requestPriority, Module provider) {
        rotationPlanRequestProcessor.addTask(new TaskProcessor.Task<>(plan.ticksUntilReset(), requestPriority.getPriority(), provider, plan));
    }

    private void update() {
        RotationPlan activePlan = getCurrentRotationPlan();
        if (activePlan == null) {
            return;
        }

        ClientPlayerEntity player = getPlayer();
        if (player == null) {
            return;
        }

        Rotation playerRotation = RotationUtil.fromVec2f(player.getRotationClient());

        if (rotationPlanRequestProcessor.fetchActiveTaskValue() == null) {
            double differenceFromCurrentToPlayer = computeRotationDifference(serverRotation, playerRotation);
            if (differenceFromCurrentToPlayer < activePlan.resetThreshold()) {
                if (currentRotation != null) {
                    player.setYaw(currentRotation.getYaw() + computeAngleDifference(player.getYaw(), currentRotation.getYaw()));
                    player.setPitch(currentRotation.getPitch() + computeAngleDifference(player.getPitch(), currentRotation.getPitch()));
                }
                setRotation(null);
                lastRotationPlan = null;
                return;
            }
        }
        Rotation newRotation = activePlan.nextRotation(currentRotation != null ? currentRotation : playerRotation, rotationPlanRequestProcessor.fetchActiveTaskValue() == null).adjustSensitivity();
        setRotation(newRotation);
        
        if (activePlan.clientLook()) {
            player.setYaw(newRotation.getYaw());
            player.setPitch(newRotation.getPitch());
        }

        lastRotationPlan = activePlan;

        rotationPlanRequestProcessor.tick(1);
    }

    private double computeRotationDifference(Rotation a, Rotation b) {
        return Math.hypot(MathHelper.abs(computeAngleDifference(a.getYaw(), b.getYaw())), MathHelper.abs(computeAngleDifference(a.getPitch(), b.getPitch())));
    }

    private float computeAngleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }

    private ClientPlayerEntity getPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.player;
    }
}
