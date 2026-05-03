package sweetie.nezi.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.utils.other.NetworkUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleRegister(name = "Inventory Move", category = Category.MOVEMENT)
public class InventoryMoveModule extends Module {
    @Getter private static final InventoryMoveModule instance = new InventoryMoveModule();

    public final ModeSetting mode = new ModeSetting("Mode").value("Bypass").values("Default", "Bypass", "Legit");

    private final List<Packet<?>> delayedPackets = new CopyOnWriteArrayList<>();
    private boolean processingPackets = false;
    private int scriptTick = 0;

    public InventoryMoveModule() {
        addSettings(mode);
    }

    @Override
    public void onEnable() {
        resetValues();
    }

    @Override
    public void onDisable() {
        cleanup();
    }

    @Override
    public void onEvent() {
        // Логика обновлений (Tick)
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.options == null) return;

            // 1. Логика скрипта (Bypass/Legit)
            if (processingPackets) {
                runScriptLogic();
                // Пока скрипт работает - блокируем движение
                if (processingPackets) {
                    lockMovement();
                }
            }
            // 2. Обычная работа (когда инвентарь открыт, но скрипт не идет)
            else if (isValidScreen()) {
                syncKeys(); // Используем syncKeys для постоянного обновления
            }
            // 3. Защита от зависания
            else {
                if (processingPackets && scriptTick > 20) {
                    cleanup();
                }
            }
        }));

        // Логика пакетов
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mode.is("Default") || !event.isSend()) return;

            Packet<?> packet = event.packet();

            if (packet instanceof ClickSlotC2SPacket && mc.currentScreen instanceof InventoryScreen) {
                // Если двигаемся или прыгаем - откладываем клик
                if (isMoving() || mc.options.jumpKey.isPressed()) {
                    delayedPackets.add(packet);
                    PacketEvent.getInstance().setCancel(true);
                }
            }
            else if (packet instanceof CloseHandledScreenC2SPacket) {
                if (!delayedPackets.isEmpty() && !processingPackets) {
                    delayedPackets.add(packet);
                    PacketEvent.getInstance().setCancel(true);
                    startProcessing();
                }
            }
        }));

        addEvents(updateEvent, packetEvent);
    }

    private void startProcessing() {
        processingPackets = true;
        scriptTick = 0;
    }

    private void runScriptLogic() {
        if (mode.is("Bypass")) {
            switch (scriptTick) {
                case 0 -> lockMovement(); // Стоп
                case 1 -> {
                    sendAll();
                    processingPackets = false;
                    // ВАЖНО: Моментально читаем клавиатуру, чтобы не было "мертвой остановки"
                    syncKeys();
                }
            }
        } else if (mode.is("Legit")) {
            switch (scriptTick) {
                case 0 -> lockMovement();
                case 3 -> {
                    for (Packet<?> p : delayedPackets) {
                        if (!(p instanceof CloseHandledScreenC2SPacket)) NetworkUtil.sendSilentPacket(p);
                    }
                }
                case 6 -> {
                    for (Packet<?> p : delayedPackets) {
                        if (p instanceof CloseHandledScreenC2SPacket) NetworkUtil.sendSilentPacket(p);
                    }
                    delayedPackets.clear();
                }
                case 9 -> {
                    processingPackets = false;
                    // ВАЖНО: Восстанавливаем нажатия
                    syncKeys();
                }
            }
        }
        scriptTick++;
    }

    // --- Исправленная логика ввода ---

    /**
     * Этот метод читает состояние ФИЗИЧЕСКОЙ клавиатуры.
     * Даже если lockMovement() сбросил кнопки в игре, этот метод вернет их обратно,
     * если ты держишь клавишу пальцем.
     */
    private void syncKeys() {
        if (mc.currentScreen instanceof ChatScreen) {
            lockMovement();
            return;
        }
        if (mc.getWindow() == null) return;

        KeyBinding[] bindings = {
                mc.options.forwardKey, mc.options.backKey,
                mc.options.leftKey, mc.options.rightKey,
                mc.options.jumpKey, mc.options.sprintKey
        };

        long handle = mc.getWindow().getHandle();
        for (KeyBinding binding : bindings) {
            boolean isPressed = InputUtil.isKeyPressed(handle, binding.getDefaultKey().getCode());
            binding.setPressed(isPressed);
        }
    }

    private void lockMovement() {
        KeyBinding[] bindings = {
                mc.options.forwardKey, mc.options.backKey,
                mc.options.leftKey, mc.options.rightKey,
                mc.options.jumpKey, mc.options.sprintKey
        };
        for (KeyBinding binding : bindings) {
            binding.setPressed(false);
        }
    }

    private void sendAll() {
        for (Packet<?> p : delayedPackets) NetworkUtil.sendSilentPacket(p);
        delayedPackets.clear();
    }

    private void resetValues() {
        delayedPackets.clear();
        processingPackets = false;
        scriptTick = 0;
    }

    private void cleanup() {
        if (!delayedPackets.isEmpty()) sendAll();
        resetValues();
        if (mc.options != null) syncKeys(); // При выключении восстанавливаем управление
    }

    private boolean isMoving() {
        return mc.player != null && mc.player.input != null
                && (mc.player.input.movementForward != 0f || mc.player.input.movementSideways != 0f);
    }

    private boolean isValidScreen() {
        return mc.currentScreen != null
                && !(mc.currentScreen instanceof ChatScreen)
                && !(mc.currentScreen instanceof SignEditScreen)
                && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof AbstractCommandBlockScreen)
                && !(mc.currentScreen instanceof StructureBlockScreen);
    }

    public boolean isProcessingPackets() {
        return processingPackets;
    }

    public boolean isLegit() {
        return !mode.is("Default");
    }

    public boolean usesBypassFlow() {
        return !mode.is("Default");
    }

    public boolean usesLegitItemBypass() {
        return mode.is("Legit");
    }

    public boolean isBasic() {
        return mode.is("Default");
    }
}

