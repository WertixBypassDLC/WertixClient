package sweetie.nezi.api.system.interfaces;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.packet.Packet;
import sweetie.nezi.api.utils.other.NetworkUtil;
import sweetie.nezi.api.utils.other.TextUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
        // Доступ к Unsafe через рефлексию, чтобы не было javac warning'ов о proprietary API
        private static final Object UNSAFE = lookupUnsafe();
        private static final Method PUT_OBJECT_VOLATILE = lookupMethod("putObjectVolatile", Object.class, long.class, Object.class);
        private static final Method STATIC_FIELD_BASE = lookupMethod("staticFieldBase", Field.class);
        private static final Method STATIC_FIELD_OFFSET = lookupMethod("staticFieldOffset", Field.class);
        private static final Object BASE = lookupBase();
        private static final long OFFSET = lookupOffset();

        private QuickImportsBinder() {
        }

        private static void bind(MinecraftClient client) {
            if (UNSAFE == null || BASE == null || OFFSET < 0L || client == null || PUT_OBJECT_VOLATILE == null) {
                return;
            }
            try {
                PUT_OBJECT_VOLATILE.invoke(UNSAFE, BASE, OFFSET, client);
            } catch (Throwable ignored) {
            }
        }

        private static Object lookupUnsafe() {
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field field = unsafeClass.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return field.get(null);
            } catch (Throwable throwable) {
                return null;
            }
        }

        private static Method lookupMethod(String name, Class<?>... params) {
            if (UNSAFE == null) return null;
            try {
                Method method = UNSAFE.getClass().getMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (Throwable t) {
                return null;
            }
        }

        private static Object lookupBase() {
            if (UNSAFE == null || STATIC_FIELD_BASE == null) {
                return null;
            }
            try {
                Field field = QuickImports.class.getField("mc");
                return STATIC_FIELD_BASE.invoke(UNSAFE, field);
            } catch (Throwable throwable) {
                return null;
            }
        }

        private static long lookupOffset() {
            if (UNSAFE == null || STATIC_FIELD_OFFSET == null) {
                return -1L;
            }
            try {
                Field field = QuickImports.class.getField("mc");
                Object result = STATIC_FIELD_OFFSET.invoke(UNSAFE, field);
                return result instanceof Long l ? l : -1L;
            } catch (Throwable throwable) {
                return -1L;
            }
        }
    }
}