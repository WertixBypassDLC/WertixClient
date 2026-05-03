package sweetie.nezi.client.features.modules.movement;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.network.packet.Packet;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.module.setting.SliderSetting;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@ModuleRegister(name = "BlockFly", category = Category.MOVEMENT)
public class BlockFlyModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("BlockFly");
    private static final BlockFlyModule INSTANCE = new BlockFlyModule();

    private final ModeSetting mode = new ModeSetting("Mode").value("Smart").values("Normal", "Smart");
    private final SliderSetting intervalMs = new SliderSetting("Interval").value(500.0F).range(50.0F, 5000.0F).step(10.0F)
            .setVisible(() -> mode.is("Normal"));
    private final SliderSetting maxQueueSize = new SliderSetting("Max queue").value(1000.0F).range(100.0F, 5000.0F).step(50.0F);
    private final BooleanSetting debug = new BooleanSetting("Debug").value(false);

    private final Queue<Packet<?>> storedPackets = new ArrayDeque<>();

    private boolean sending = false;
    private long lastPulseTime = 0L;
    private int currentPulseDelayMs = 0;
    private int totalPacketsSent = 0;
    private int totalPacketsQueued = 0;

    public BlockFlyModule() {
        addSettings(mode, intervalMs, maxQueueSize, debug);
    }

    public static BlockFlyModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null || mc.isIntegratedServerRunning() || mc.getNetworkHandler() == null) {
            storedPackets.clear();
            sending = false;
            if (debug.getValue()) {
                LOGGER.warn("BlockFly requires an active multiplayer connection");
            }
            return;
        }

        storedPackets.clear();
        sending = false;
        lastPulseTime = System.currentTimeMillis();
        currentPulseDelayMs = nextPulseDelay();
        totalPacketsSent = 0;
        totalPacketsQueued = 0;

        if (debug.getValue()) {
            LOGGER.info("BlockFly enabled in {} mode", mode.getValue());
        }
    }

    @Override
    public void onDisable() {
        int flushedCount = 0;

        if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null && !storedPackets.isEmpty()) {
            flushedCount = flushPackets();
        } else {
            storedPackets.clear();
        }

        sending = false;

        if (debug.getValue()) {
            LOGGER.info("BlockFly disabled");
            LOGGER.info("Queued total: {}", totalPacketsQueued);
            LOGGER.info("Sent total: {}", totalPacketsSent);
            LOGGER.info("Flushed on disable: {}", flushedCount);
        }
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null || mc.isIntegratedServerRunning() || mc.getNetworkHandler() == null) {
                return;
            }

            long now = System.currentTimeMillis();
            if (!storedPackets.isEmpty() && now - lastPulseTime >= currentPulseDelayMs) {
                flushPackets();
                lastPulseTime = now;
                currentPulseDelayMs = nextPulseDelay();
            }
        }));

        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isSend() || mc.player == null || mc.world == null || mc.isIntegratedServerRunning()
                    || mc.getNetworkHandler() == null || sending) {
                return;
            }

            Packet<?> packet = event.packet();
            if (shouldBypassPacket(packet)) {
                return;
            }

            if (storedPackets.size() >= getMaxQueueSize()) {
                if (debug.getValue()) {
                    LOGGER.warn("BlockFly queue is full, passing packet through immediately");
                }
                return;
            }

            PacketEvent.getInstance().setCancel(true);
            storedPackets.add(packet);
            totalPacketsQueued++;

            if (debug.getValue() && totalPacketsQueued % 50 == 0) {
                LOGGER.info("BlockFly queued packets: {}, total queued: {}", storedPackets.size(), totalPacketsQueued);
            }
        }));

        addEvents(updateEvent, packetEvent);
    }

    private int flushPackets() {
        if (mc.player == null || mc.getNetworkHandler() == null || storedPackets.isEmpty()) {
            return 0;
        }

        sending = true;
        int packetCount = storedPackets.size();
        int flushedCount = 0;

        try {
            while (!storedPackets.isEmpty()) {
                Packet<?> packet = storedPackets.poll();
                if (packet == null) {
                    continue;
                }

                sendSilentPacket(packet);
                totalPacketsSent++;
                flushedCount++;
            }
        } finally {
            sending = false;
        }

        if (debug.getValue() && packetCount > 0) {
            LOGGER.info("BlockFly flushed packet pulse: {}", packetCount);
        }

        return flushedCount;
    }

    private boolean shouldBypassPacket(Packet<?> packet) {
        if (packet == null) {
            return true;
        }

        String simpleName = packet.getClass().getSimpleName();
        return simpleName.contains("KeepAlive")
                || simpleName.contains("Pong")
                || simpleName.contains("ClientStatus");
    }

    private int nextPulseDelay() {
        return mode.is("Smart") ? rand(150, 250) : Math.round(intervalMs.getValue());
    }

    private int getMaxQueueSize() {
        return Math.max(1, Math.round(maxQueueSize.getValue()));
    }

    private int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
