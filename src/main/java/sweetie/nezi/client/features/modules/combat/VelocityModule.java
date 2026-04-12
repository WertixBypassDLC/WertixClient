package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.player.other.MovementInputEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.MultiBooleanSetting;

@ModuleRegister(name = "Velocity", category = Category.COMBAT)
public class VelocityModule extends Module {
    @Getter private static final VelocityModule instance = new VelocityModule();

    private final ModeSetting knockback = new ModeSetting("Knockback").value("None").values("None", "Cancel", "Jump reset");

    private final MultiBooleanSetting pushing = new MultiBooleanSetting("Pushing").value(
            new BooleanSetting("Block").value(true),
            new BooleanSetting("Liquids").value(true),
            new BooleanSetting("Entity").value(true),
            new BooleanSetting("Fishing rod").value(true)
    );

    private boolean isFallDamage = false;
    private int limitUntilJump = 0;

    public VelocityModule() {
        addSettings(knockback, pushing);
    }

    @Override
    public void onEvent() {
        PacketEvent packetInstance = PacketEvent.getInstance();
        EventListener packetEvent = packetInstance.subscribe(new Listener<>(event -> {
            handlePacketEvent(packetInstance, event);
        }));

        EventListener moveInputEvent = MovementInputEvent.getInstance().subscribe(new Listener<>(event -> {
            handleMoveInputEvent(event);
        }));

        addEvents(packetEvent, moveInputEvent);
    }

    private void handleMoveInputEvent(MovementInputEvent.MovementInputEventData event) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return;
        switch (knockback.getValue()) {
            case "Jump reset" -> {
                if (player.hurtTime != 9 || !player.isOnGround() || !player.isSprinting() || isFallDamage) {
                    return;
                }

                event.setJump(true);
            }
        }
    }

    private void handlePacketEvent(PacketEvent event, PacketEvent.PacketEventData data) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return;
        if (data.packet() instanceof EntityVelocityUpdateS2CPacket velocityPacket && velocityPacket.getEntityId() == player.getId()) {
            switch (knockback.getValue()) {
                case "Cancel" -> {
                    event.setCancel(true);
                }

                case "Jump reset" -> {
                    isFallDamage = velocityPacket.getVelocityX() == 0.0
                            && velocityPacket.getVelocityY() == 0.0
                            && velocityPacket.getVelocityZ() < 0;
                }
            }
        }
    }

    private ClientPlayerEntity getPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.player;
    }

    public boolean cancelPush(PushingSource data) {
        return isEnabled() && switch (data) {
            case BLOCK -> pushing.isEnabled("Block");
            case LIQUIDS -> pushing.isEnabled("Liquids");
            case ENTITY -> pushing.isEnabled("Entity");
            case FISHING_ROD -> pushing.isEnabled("Fishing rod");
        };
    }

    public enum PushingSource {
        BLOCK, LIQUIDS, ENTITY, FISHING_ROD
    }
}
