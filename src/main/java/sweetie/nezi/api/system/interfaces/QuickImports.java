package sweetie.nezi.api.system.interfaces;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.packet.Packet;
import sun.misc.Unsafe;
import sweetie.nezi.api.utils.other.NetworkUtil;
import sweetie.nezi.api.utils.other.TextUtil;

import java.lang.reflect.Field;

public interface QuickImports {
    MinecraftClient mc = null;

    static void bindMinecraftClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || mc == client) {
            return;
        }
        QuickImportsBinder.bind(client);
    }

    default void print(String message) {
        TextUtil.sendMessage(message);
    }

    default void sendPacket(SequencedPacketCreator packet) {
        NetworkUtil.sendPacket(packet);
    }
    default void sendSilentPacket(SequencedPacketCreator packet) {
        NetworkUtil.sendSilentPacket(packet);
    }
    default void sendPacket(Packet<?> packet) {
        NetworkUtil.sendPacket(packet);
    }
    default void sendSilentPacket(Packet<?> packet) {
        NetworkUtil.sendSilentPacket(packet);
    }

    final class QuickImportsBinder {
        private static final Unsafe UNSAFE = lookupUnsafe();
        private static final Object BASE = lookupBase();
        private static final long OFFSET = lookupOffset();

        private QuickImportsBinder() {
        }

        private static void bind(MinecraftClient client) {
            if (UNSAFE == null || BASE == null || OFFSET < 0L || client == null) {
                return;
            }
            UNSAFE.putObjectVolatile(BASE, OFFSET, client);
        }

        private static Unsafe lookupUnsafe() {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return (Unsafe) field.get(null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return null;
            }
        }

        private static Object lookupBase() {
            if (UNSAFE == null) {
                return null;
            }
            try {
                Field field = QuickImports.class.getField("mc");
                return UNSAFE.staticFieldBase(field);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return null;
            }
        }

        private static long lookupOffset() {
            if (UNSAFE == null) {
                return -1L;
            }
            try {
                Field field = QuickImports.class.getField("mc");
                return UNSAFE.staticFieldOffset(field);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return -1L;
            }
        }
    }
}
