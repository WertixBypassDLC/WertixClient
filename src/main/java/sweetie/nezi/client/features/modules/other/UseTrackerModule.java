package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.utils.notification.NotificationUtil;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ModuleRegister(name = "Use Tracker", category = Category.OTHER)
public class UseTrackerModule extends Module {
    @Getter
    private static final UseTrackerModule instance = new UseTrackerModule();

    private static final long POTION_NOTIFY_COOLDOWN_MS = 1500L;
    private static final long CONSUME_NOTIFY_COOLDOWN_MS = 1600L;

    private final BooleanSetting totemPops = new BooleanSetting("Totem pops").value(true);
    private final BooleanSetting receivedPotions = new BooleanSetting("Received potions").value(true);
    private final BooleanSetting consumedItems = new BooleanSetting("Consumed items").value(true);

    private final Map<UUID, ItemStack> activeUseItem = new HashMap<>();
    private final Map<UUID, Integer> useStartTick = new HashMap<>();
    private final Map<String, Long> recentPotionNotifications = new HashMap<>();
    private final Map<String, Long> recentConsumeNotifications = new HashMap<>();

    public UseTrackerModule() {
        addSettings(totemPops, receivedPotions, consumedItems);
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> handleUpdate()));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::handlePacket));
        addEvents(update, packet);
    }

    @Override
    public void onDisable() {
        activeUseItem.clear();
        useStartTick.clear();
        recentPotionNotifications.clear();
        recentConsumeNotifications.clear();
    }

    private void handleUpdate() {
        if (!consumedItems.getValue() || mc.world == null || mc.player == null) {
            return;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player) {
                continue;
            }

            UUID uuid = player.getUuid();
            if (player.isUsingItem()) {
                activeUseItem.computeIfAbsent(uuid, ignored -> player.getActiveItem().copy());
                useStartTick.putIfAbsent(uuid, player.age);
                continue;
            }

            ItemStack usedStack = activeUseItem.remove(uuid);
            Integer startAge = useStartTick.remove(uuid);
            if (usedStack == null || usedStack.isEmpty() || startAge == null) {
                continue;
            }

            int useTicks = player.age - startAge;
            if (useTicks < 31) {
                continue;
            }

            UseAction action = usedStack.getUseAction();
            String verb = switch (action) {
                case DRINK -> "выпил";
                case EAT -> "съел";
                default -> null;
            };
            if (verb == null) {
                continue;
            }

            String itemName = usedStack.getName().getString();
            String key = player.getName().getString().toLowerCase(Locale.ROOT) + "|" + normalize(itemName);
            long now = System.currentTimeMillis();
            long last = recentConsumeNotifications.getOrDefault(key, 0L);
            if (now - last < CONSUME_NOTIFY_COOLDOWN_MS) {
                continue;
            }

            recentConsumeNotifications.put(key, now);
            NotificationUtil.add(player.getName().getString() + " " + verb + " " + itemName + formatPotionEffects(usedStack));
        }
    }

    private void handlePacket(PacketEvent.PacketEventData event) {
        if (!event.isReceive() || mc.world == null) {
            return;
        }

        if (event.packet() instanceof EntityStatusS2CPacket statusPacket) {
            handleTotemPacket(statusPacket);
            return;
        }

        if (event.packet() instanceof EntityStatusEffectS2CPacket effectPacket) {
            handlePotionPacket(effectPacket);
        }
    }

    private void handleTotemPacket(EntityStatusS2CPacket packet) {
        if (!totemPops.getValue() || packet.getStatus() != 35) {
            return;
        }

        if (!(packet.getEntity(mc.world) instanceof PlayerEntity player) || player == mc.player) {
            return;
        }

        NotificationUtil.add(player.getName().getString() + " потерял тотем");
    }

    private void handlePotionPacket(EntityStatusEffectS2CPacket packet) {
        if (!receivedPotions.getValue() || mc.player == null) {
            return;
        }

        if (!(mc.world.getEntityById(packet.getEntityId()) instanceof PlayerEntity player) || player == mc.player) {
            return;
        }

        String playerName = player.getName().getString();
        String effectName = Text.translatable(packet.getEffectId().value().getTranslationKey()).getString();
        int amplifier = packet.getAmplifier() + 1;
        int durationTicks = packet.getDuration();
        String key = playerName.toLowerCase(Locale.ROOT) + "|" + normalize(effectName) + "|" + amplifier;
        long now = System.currentTimeMillis();
        long last = recentPotionNotifications.getOrDefault(key, 0L);
        if (now - last < POTION_NOTIFY_COOLDOWN_MS) {
            return;
        }

        recentPotionNotifications.put(key, now);
        String duration = formatDuration(durationTicks);
        NotifWidget widget = getNotifWidget();
        String left = effectName + " " + amplifier;
        String right = duration;
        if (widget != null) {
            widget.addPotionNotif(playerName, List.of(left + "\u0000" + right));
        } else {
            NotificationUtil.add(playerName + " получил " + left + " на " + duration);
        }
    }

    private NotifWidget getNotifWidget() {
        return (NotifWidget) WidgetManager.getInstance().getWidgets().stream()
                .filter(widget -> widget instanceof NotifWidget)
                .findFirst()
                .orElse(null);
    }

    private String formatPotionEffects(ItemStack stack) {
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) {
            return "";
        }

        boolean hasEffects = false;
        StringBuilder builder = new StringBuilder(" (");
        boolean first = true;
        for (StatusEffectInstance effect : contents.getEffects()) {
            hasEffects = true;
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(Text.translatable(effect.getEffectType().value().getTranslationKey()).getString())
                    .append(" ")
                    .append(effect.getAmplifier() + 1);
        }
        if (!hasEffects) {
            return "";
        }
        builder.append(")");
        return builder.toString();
    }

    private String formatDuration(int durationTicks) {
        int seconds = Math.max(0, durationTicks / 20);
        int minutes = seconds / 60;
        seconds %= 60;
        return minutes > 0 ? minutes + "м " + seconds + "с" : seconds + "с";
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replaceAll("(?i)\u00a7[0-9A-FK-ORX]", "")
                .replace('\u0451', '\u0435')
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
