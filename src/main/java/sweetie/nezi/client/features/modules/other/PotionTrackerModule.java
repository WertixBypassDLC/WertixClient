package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.client.ui.widget.WidgetManager;
import sweetie.nezi.client.ui.widget.overlay.NotifWidget;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "Potion Tracker", category = Category.OTHER)
public class PotionTrackerModule extends Module {
    @Getter private static final PotionTrackerModule instance = new PotionTrackerModule();

    private static final double SPLASH_RADIUS = 4.0D;
    private static final double DIRECT_HIT_DISTANCE = 0.35D;
    private static final int MIN_APPLIED_DURATION_TICKS = 20;
    private static final long SPLASH_CONFIRM_DELAY_TICKS = 2L;
    private static final long PARTICLE_IMPACT_COOLDOWN_MS = 900L;
    private static final double PARTICLE_TRACK_DISTANCE_SQ = 64.0D * 64.0D;
    private static final long POTION_NOTIFICATION_COOLDOWN_MS = 1_800L;
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#?([0-9A-Fa-f]{6})");
    private static final List<KnownPotionDefinition> KNOWN_POTIONS = List.of(
            new KnownPotionDefinition("\u0421\u0432\u044f\u0442\u0430\u044f \u0432\u043e\u0434\u0430", 0xFFFFFF, List.of(
                    new PotionEffectDefinition("effect.minecraft.regeneration:2", "\u0420\u0435\u0433\u0435\u043d\u0435\u0440\u0430\u0446\u0438\u044f", "II", 900, false),
                    new PotionEffectDefinition("effect.minecraft.invisibility:2", "\u041d\u0435\u0432\u0438\u0434\u0438\u043c\u043e\u0441\u0442\u044c", "II", 12000, false),
                    new PotionEffectDefinition("effect.minecraft.instant_health:2", "\u0418\u0441\u0446\u0435\u043b\u0435\u043d\u0438\u0435", "II", 0, true)
            )),
            new KnownPotionDefinition("\u0417\u0435\u043b\u044c\u0435 \u041f\u0430\u043b\u043b\u0430\u0434\u0438\u043d\u0430", 0x00FFFF, List.of(
                    new PotionEffectDefinition("effect.minecraft.resistance:1", "\u0421\u043e\u043f\u0440\u043e\u0442\u0438\u0432\u043b\u0435\u043d\u0438\u0435", "", 12000, false),
                    new PotionEffectDefinition("effect.minecraft.fire_resistance:1", "\u041e\u0433\u043d\u0435\u0441\u0442\u043e\u0439\u043a\u043e\u0441\u0442\u044c", "", 12000, false),
                    new PotionEffectDefinition("effect.minecraft.health_boost:3", "\u041f\u0440\u0438\u043b\u0438\u0432 \u0437\u0434\u043e\u0440\u043e\u0432\u044c\u044f", "III", 1200, false),
                    new PotionEffectDefinition("effect.minecraft.invisibility:1", "\u041d\u0435\u0432\u0438\u0434\u0438\u043c\u043e\u0441\u0442\u044c", "", 18000, false)
            )),
            new KnownPotionDefinition("\u0417\u0435\u043b\u044c\u0435 \u0420\u0430\u0434\u0438\u0430\u0446\u0438\u0438", 0x32CD32, List.of(
                    new PotionEffectDefinition("effect.minecraft.poison:2", "\u041e\u0442\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435", "II", 1200, false),
                    new PotionEffectDefinition("effect.minecraft.wither:2", "\u0418\u0441\u0441\u0443\u0448\u0435\u043d\u0438\u0435", "II", 1200, false),
                    new PotionEffectDefinition("effect.minecraft.slowness:3", "\u0417\u0430\u043c\u0435\u0434\u043b\u0435\u043d\u0438\u0435", "III", 1800, false),
                    new PotionEffectDefinition("effect.minecraft.hunger:5", "\u0413\u043e\u043b\u043e\u0434", "V", 1200, false),
                    new PotionEffectDefinition("effect.minecraft.glowing:1", "\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435", "", 2400, false)
            )),
            new KnownPotionDefinition("\u0425\u043b\u043e\u043f\u0443\u0448\u043a\u0430", 0xFF69B4, List.of(
                    new PotionEffectDefinition("effect.minecraft.slowness:9", "\u0417\u0430\u043c\u0435\u0434\u043b\u0435\u043d\u0438\u0435", "IX", 200, false),
                    new PotionEffectDefinition("effect.minecraft.speed:5", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c", "V", 400, false),
                    new PotionEffectDefinition("effect.minecraft.blindness:9", "\u0421\u043b\u0435\u043f\u043e\u0442\u0430", "IX", 100, false),
                    new PotionEffectDefinition("effect.minecraft.glowing:1", "\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435", "", 3600, false)
            )),
            new KnownPotionDefinition("\u0417\u0435\u043b\u044c\u0435 \u0413\u043d\u0435\u0432\u0430", 0x993333, List.of(
                    new PotionEffectDefinition("effect.minecraft.strength:5", "\u0421\u0438\u043b\u0430", "V", 600, false),
                    new PotionEffectDefinition("effect.minecraft.slowness:4", "\u0417\u0430\u043c\u0435\u0434\u043b\u0435\u043d\u0438\u0435", "IV", 600, false)
            )),
            new KnownPotionDefinition("\u0421\u043d\u043e\u0442\u0432\u043e\u0440\u043d\u043e\u0435", 0x484848, List.of(
                    new PotionEffectDefinition("effect.minecraft.weakness:2", "\u0421\u043b\u0430\u0431\u043e\u0441\u0442\u044c", "II", 1800, false),
                    new PotionEffectDefinition("effect.minecraft.mining_fatigue:2", "\u0423\u0442\u043e\u043c\u043b\u0435\u043d\u0438\u0435", "II", 200, false),
                    new PotionEffectDefinition("effect.minecraft.wither:3", "\u0418\u0441\u0441\u0443\u0448\u0435\u043d\u0438\u0435", "III", 1800, false),
                    new PotionEffectDefinition("effect.minecraft.blindness:1", "\u0421\u043b\u0435\u043f\u043e\u0442\u0430", "", 200, false)
            )),
            new KnownPotionDefinition("\u0417\u0435\u043b\u044c\u0435 \u0410\u0441\u0441\u0430\u0441\u0438\u043d\u0430", 0x333333, List.of(
                    new PotionEffectDefinition("effect.minecraft.strength:4", "\u0421\u0438\u043b\u0430", "IV", 1200, false),
                    new PotionEffectDefinition("effect.minecraft.speed:3", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c", "III", 6000, false),
                    new PotionEffectDefinition("effect.minecraft.haste:1", "\u0421\u043f\u0435\u0448\u043a\u0430", "", 1200, false),
                    new PotionEffectDefinition("effect.minecraft.instant_damage:2", "\u041c\u043e\u043c\u0435\u043d\u0442\u0430\u043b\u044c\u043d\u044b\u0439 \u0443\u0440\u043e\u043d", "II", 0, true)
            ))
    );

    private final Map<Integer, PotionData> trackedPotions = new HashMap<>();
    private final List<PendingPotionImpact> pendingImpacts = new ArrayList<>();
    private final Map<String, Long> recentParticleImpacts = new HashMap<>();
    private final Map<String, Long> recentPotionNotifications = new HashMap<>();

    @Getter private final Map<String, List<TrackedEffect>> playerCustomEffects = new HashMap<>();

    public static class TrackedEffect {
        public String key;
        public String name;
        public String level;
        public long startTime;
        public long endTime;
        public long durationMs;
        public float splashPercent;
        public Color color;
    }

    private static class PotionData {
        ItemStack stack;
        Vec3d lastPos;

        PotionData(ItemStack stack, Vec3d pos) {
            this.stack = stack;
            this.lastPos = pos;
        }
    }

    private static class PendingPotionImpact {
        ItemStack stack;
        Vec3d pos;
        long readyAtTick;

        PendingPotionImpact(ItemStack stack, Vec3d pos, long readyAtTick) {
            this.stack = stack;
            this.pos = pos;
            this.readyAtTick = readyAtTick;
        }
    }

    private record PotionEffectDefinition(String key, String name, String level, int durationTicks, boolean instant) {
        String displayName() {
            return level == null || level.isBlank() ? name : name + " " + level;
        }
    }

    private record KnownPotionDefinition(String name, int color, List<PotionEffectDefinition> effects) { }

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (getWorld() == null || event.isSend()) {
                return;
            }

            if (event.packet() instanceof PlaySoundS2CPacket playSound) {
                handleHolyAura(playSound);
            }
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (getPlayer() == null || getWorld() == null) {
                return;
            }

            long now = System.currentTimeMillis();
            cleanupTrackedEffects(now);
            cleanupRecentParticleImpacts(now);
            trackActivePotions();
            processPendingImpacts(now);
        }));

        addEvents(updateEvent, packetEvent);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        trackedPotions.clear();
        pendingImpacts.clear();
        recentParticleImpacts.clear();
        recentPotionNotifications.clear();
        playerCustomEffects.clear();
    }

    private void handleHolyAura(PlaySoundS2CPacket playSound) {
        ClientWorld world = getWorld();
        if (world == null) {
            return;
        }
        String soundPath = playSound.getSound().getIdAsString();
        if (!soundPath.contains("beacon.activate") && !soundPath.contains("illusioner.cast_spell")) {
            return;
        }

        Vec3d soundPos = new Vec3d(playSound.getX(), playSound.getY(), playSound.getZ());
        long now = System.currentTimeMillis();

        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(soundPos) >= 16.0D) {
                continue;
            }

            boolean hasMembrane = player.getMainHandStack().isOf(Items.PHANTOM_MEMBRANE)
                    || player.getOffHandStack().isOf(Items.PHANTOM_MEMBRANE);
            if (!hasMembrane) {
                continue;
            }

            String playerName = player.getName().getString();
            upsertTrackedEffect(playerName, "holy_invisibility", "\u041d\u0435\u0432\u0438\u0434\u0438\u043c\u043e\u0441\u0442\u044c", "I", now, 480000L, 480000L, 1.0f, new Color(127, 131, 146));
            upsertTrackedEffect(playerName, "holy_speed", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c", "II", now, 180000L, 180000L, 1.0f, new Color(124, 175, 198));
            upsertTrackedEffect(playerName, "holy_strength", "\u0421\u0438\u043b\u0430", "II", now, 180000L, 180000L, 1.0f, new Color(147, 36, 35));
        }
    }

    private void trackActivePotions() {
        ClientWorld world = getWorld();
        ClientPlayerEntity player = getPlayer();
        if (world == null || player == null) {
            return;
        }
        Set<Integer> currentPotions = new HashSet<>();

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof PotionEntity potionEntity)) {
                continue;
            }

            if (!potionEntity.getStack().isOf(Items.SPLASH_POTION)) {
                continue;
            }

            if (player.distanceTo(entity) > 50.0F) {
                continue;
            }

            int id = entity.getId();
            currentPotions.add(id);
            trackedPotions.compute(id, (ignored, data) -> {
                if (data == null) {
                    return new PotionData(potionEntity.getStack().copy(), entity.getPos());
                }

                data.lastPos = entity.getPos();
                data.stack = potionEntity.getStack().copy();
                return data;
            });
        }

        trackedPotions.keySet().removeIf(id -> !currentPotions.contains(id) && schedulePendingPotion(id));
    }

    private boolean schedulePendingPotion(int id) {
        PotionData data = trackedPotions.get(id);
        ClientWorld world = getWorld();
        if (data == null || world == null) {
            return true;
        }

        pendingImpacts.add(new PendingPotionImpact(data.stack.copy(), data.lastPos, world.getTime() + SPLASH_CONFIRM_DELAY_TICKS));
        return true;
    }

    private void processPendingImpacts(long now) {
        ClientWorld world = getWorld();
        if (world == null) {
            pendingImpacts.clear();
            return;
        }
        Iterator<PendingPotionImpact> iterator = pendingImpacts.iterator();
        while (iterator.hasNext()) {
            PendingPotionImpact pending = iterator.next();
            if (world.getTime() < pending.readyAtTick) {
                continue;
            }

            processPendingImpact(pending, now);
            iterator.remove();
        }
    }

    private void processPendingImpact(PendingPotionImpact pending, long now) {
        PotionContentsComponent potionContents = pending.stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
        String potionName = pending.stack.getName().getString();
        int colorInt = potionContents.getColor();
        KnownPotionDefinition knownPotion = findKnownPotionDefinition(potionName, colorInt);

        if (knownPotion == null && !potionContents.getEffects().iterator().hasNext()) {
            return;
        }

        if (knownPotion != null) {
            String impactKey = buildImpactKey(colorInt, pending.pos);
            Long previousImpact = recentParticleImpacts.get(impactKey);
            if (previousImpact != null && now - previousImpact < PARTICLE_IMPACT_COOLDOWN_MS) {
                return;
            }
            recentParticleImpacts.put(impactKey, now);
        }

        processSplashAtPosition(potionName, colorInt, potionContents, knownPotion, pending.pos, now);
    }

    private List<String> collectKnownEffects(String playerName, KnownPotionDefinition definition, Color effectColor, long now, float splashPercent) {
        List<String> effectLines = new ArrayList<>();

        for (PotionEffectDefinition effect : definition.effects()) {
            if (effect.instant()) {
                effectLines.add(packEffectLine(effect.displayName(), ""));
                continue;
            }

            int scaledTicks = scaleDurationTicks(effect.durationTicks(), splashPercent);
            if (scaledTicks <= MIN_APPLIED_DURATION_TICKS) {
                continue;
            }

            long remainingMs = scaledTicks * 50L;
            long totalMs = effect.durationTicks() * 50L;
            effectLines.add(packEffectLine(effect.displayName(), formatDurationTicks(scaledTicks)));
            upsertTrackedEffect(playerName, effect.key(), effect.name(), effect.level(), now, remainingMs, totalMs, splashPercent, effectColor);
        }

        return effectLines;
    }

    private List<String> collectFallbackEffects(PlayerEntity player, PotionContentsComponent potionContents, Color effectColor, long now, float splashPercent) {
        List<String> effectLines = new ArrayList<>();
        String playerName = player.getName().getString();

        potionContents.getEffects().forEach(expectedEffect -> {
            StatusEffectInstance actualEffect = player.getStatusEffect(expectedEffect.getEffectType());
            String effectName = Language.getInstance().get(expectedEffect.getTranslationKey());
            int amplifier = actualEffect != null ? actualEffect.getAmplifier() + 1 : expectedEffect.getAmplifier() + 1;
            String level = amplifier > 1 ? toRoman(amplifier) : "";

            if (actualEffect != null && actualEffect.getDuration() > 0) {
                int totalTicks = expectedEffect.getDuration() > 0
                        ? Math.max(scaleDurationTicks(expectedEffect.getDuration(), splashPercent), actualEffect.getDuration())
                        : actualEffect.getDuration();
                int remainingTicks = actualEffect.getDuration();
                effectLines.add(packEffectLine(formatEffectName(effectName, level), formatDurationTicks(remainingTicks)));
                upsertTrackedEffect(
                        playerName,
                        expectedEffect.getTranslationKey() + ":" + amplifier,
                        effectName,
                        level,
                        now,
                        remainingTicks * 50L,
                        Math.max(totalTicks, remainingTicks) * 50L,
                        splashPercent,
                        effectColor
                );
                return;
            }

            if (expectedEffect.getDuration() <= 0) {
                effectLines.add(packEffectLine(formatEffectName(effectName, level), ""));
                return;
            }

            int scaledTicks = scaleDurationTicks(expectedEffect.getDuration(), splashPercent);
            if (scaledTicks <= MIN_APPLIED_DURATION_TICKS) {
                return;
            }

            effectLines.add(packEffectLine(formatEffectName(effectName, level), formatDurationTicks(scaledTicks)));
            upsertTrackedEffect(
                    playerName,
                    expectedEffect.getTranslationKey() + ":" + amplifier,
                    effectName,
                    level,
                    now,
                    scaledTicks * 50L,
                    expectedEffect.getDuration() * 50L,
                    splashPercent,
                    effectColor
            );
        });

        return effectLines;
    }

    private void cleanupTrackedEffects(long now) {
        Iterator<Map.Entry<String, List<TrackedEffect>>> iterator = playerCustomEffects.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<TrackedEffect>> entry = iterator.next();
            entry.getValue().removeIf(effect -> now > effect.endTime);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void cleanupRecentParticleImpacts(long now) {
        recentParticleImpacts.entrySet().removeIf(entry -> now - entry.getValue() > PARTICLE_IMPACT_COOLDOWN_MS);
        recentPotionNotifications.entrySet().removeIf(entry -> now - entry.getValue() > POTION_NOTIFICATION_COOLDOWN_MS);
    }

    private void upsertTrackedEffect(String playerName, String key, String name, String level,
                                     long now, long remainingMs, long totalMs, float splashPercent, Color color) {
        List<TrackedEffect> effects = playerCustomEffects.computeIfAbsent(playerName, ignored -> new ArrayList<>());
        long endTime = now + Math.max(remainingMs, 0L);

        for (TrackedEffect effect : effects) {
            if (!Objects.equals(effect.key, key)) {
                continue;
            }

            if (endTime >= effect.endTime || totalMs >= effect.durationMs) {
                effect.name = name;
                effect.level = level;
                effect.startTime = now;
                effect.endTime = endTime;
                effect.durationMs = Math.max(totalMs, remainingMs);
                effect.splashPercent = splashPercent;
                effect.color = color;
            }
            return;
        }

        TrackedEffect effect = new TrackedEffect();
        effect.key = key;
        effect.name = name;
        effect.level = level;
        effect.startTime = now;
        effect.endTime = endTime;
        effect.durationMs = Math.max(totalMs, remainingMs);
        effect.splashPercent = splashPercent;
        effect.color = color;
        effects.add(effect);
    }

    private KnownPotionDefinition findKnownPotionDefinition(String potionName, int colorInt) {
        String normalizedName = normalizePotionName(potionName);
        for (KnownPotionDefinition potion : KNOWN_POTIONS) {
            if (normalizePotionName(potion.name()).equals(normalizedName)) {
                return potion;
            }
        }

        for (KnownPotionDefinition potion : KNOWN_POTIONS) {
            if ((potion.color() & 0xFFFFFF) == (colorInt & 0xFFFFFF)) {
                return potion;
            }
        }

        KnownPotionDefinition nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (KnownPotionDefinition potion : KNOWN_POTIONS) {
            double distance = colorDistance(potion.color(), colorInt);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = potion;
            }
        }

        return bestDistance <= 18.0D ? nearest : null;
    }

    private void handleParticleImpact(ParticleS2CPacket packet) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) {
            return;
        }

        Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        if (player.squaredDistanceTo(pos) > PARTICLE_TRACK_DISTANCE_SQ) {
            return;
        }

        Integer colorInt = extractParticleColor(packet.getParameters());
        if (colorInt == null) {
            return;
        }

        KnownPotionDefinition knownPotion = findKnownPotionDefinition("", colorInt);
        if (knownPotion == null) {
            return;
        }

        long now = System.currentTimeMillis();
        String impactKey = buildImpactKey(colorInt, pos);
        Long previousImpact = recentParticleImpacts.get(impactKey);
        if (previousImpact != null && now - previousImpact < PARTICLE_IMPACT_COOLDOWN_MS) {
            return;
        }

        recentParticleImpacts.put(impactKey, now);
        processSplashAtPosition(knownPotion.name(), colorInt, PotionContentsComponent.DEFAULT, knownPotion, pos, now);
    }

    private void processSplashAtPosition(String potionName, int colorInt, PotionContentsComponent potionContents,
                                         KnownPotionDefinition knownPotion, Vec3d pos, long now) {
        ClientWorld world = getWorld();
        if (world == null) {
            return;
        }
        Box box = new Box(
                pos.x - SPLASH_RADIUS, pos.y - 2.0D, pos.z - SPLASH_RADIUS,
                pos.x + SPLASH_RADIUS, pos.y + 2.0D, pos.z + SPLASH_RADIUS
        );

        Color effectColor = unpackColor(colorInt);
        Formatting formatColor = getColorFromInt(colorInt);
        NotifWidget widget = (NotifWidget) WidgetManager.getInstance().getWidgets().stream()
                .filter(current -> current instanceof NotifWidget)
                .findFirst()
                .orElse(null);

        for (LivingEntity hitEntity : world.getEntitiesByClass(LivingEntity.class, box, entity -> entity instanceof PlayerEntity)) {
            if (!(hitEntity instanceof PlayerEntity player)) {
                continue;
            }

            double distance = Math.sqrt(player.squaredDistanceTo(pos));
            if (distance > SPLASH_RADIUS) {
                continue;
            }

            float splashPercent = getSplashPercent(distance);
            if (splashPercent <= 0.0f) {
                continue;
            }

            List<String> effectLines = knownPotion != null
                    ? collectKnownEffects(player.getName().getString(), knownPotion, effectColor, now, splashPercent)
                    : collectFallbackEffects(player, potionContents, effectColor, now, splashPercent);
            if (effectLines.isEmpty()) {
                continue;
            }

            String playerName = player.getName().getString();
            String title = "\u00a7f" + playerName + " \u00a77\u043f\u043e\u043b\u0443\u0447\u0438\u043b " + formatColor + potionName;
            if (widget != null) {
                if (shouldNotifyPotion(playerName, potionName, pos, now)) {
                    widget.addNotif(title);
                }
                widget.addPotionNotif(playerName, effectLines);
            }
        }
    }

    private boolean shouldNotifyPotion(String playerName, String potionName, Vec3d pos, long now) {
        String key = playerName.toLowerCase(Locale.ROOT) + "|" + normalizePotionName(potionName);
        Long last = recentPotionNotifications.get(key);
        if (last != null && now - last < POTION_NOTIFICATION_COOLDOWN_MS + 1200L) {
            return false;
        }

        recentPotionNotifications.put(key, now);
        return true;
    }

    private String normalizePotionName(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("\u00a7", "")
                .replace('\u0451', '\u0435')
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private double colorDistance(int first, int second) {
        int r1 = (first >> 16) & 255;
        int g1 = (first >> 8) & 255;
        int b1 = first & 255;
        int r2 = (second >> 16) & 255;
        int g2 = (second >> 8) & 255;
        int b2 = second & 255;
        int dr = r1 - r2;
        int dg = g1 - g2;
        int db = b1 - b2;
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private float getSplashPercent(double distance) {
        if (distance <= DIRECT_HIT_DISTANCE) {
            return 1.0f;
        }

        return MathHelper.clamp((float) (1.0D - distance / SPLASH_RADIUS), 0.0f, 1.0f);
    }

    private int scaleDurationTicks(int baseTicks, float splashPercent) {
        return Math.max(0, (int) (baseTicks * splashPercent + 0.5f));
    }

    private Color unpackColor(int colorInt) {
        return new Color((colorInt >> 16) & 255, (colorInt >> 8) & 255, colorInt & 255);
    }

    private String formatEffectName(String effectName, String level) {
        return level == null || level.isBlank() ? effectName : effectName + " " + level;
    }

    private String packEffectLine(String name, String duration) {
        return name + "\u0000" + duration;
    }

    private String formatDurationTicks(int ticks) {
        return formatDurationSeconds(Math.max(1, ticks / 20));
    }

    private String formatDurationSeconds(int durationSec) {
        int minutes = durationSec / 60;
        int seconds = durationSec % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    private Formatting getColorFromInt(int color) {
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        if (r > g && r > b) return Formatting.RED;
        if (g > r && g > b) return Formatting.GREEN;
        if (b > r && b > g) return Formatting.BLUE;
        if (r > 240 && g > 100 && b < 100) return Formatting.GOLD;
        if (r > 174 && g < 100 && b > 174) return Formatting.LIGHT_PURPLE;
        return (r < 100 && g > 174 && b > 174) ? Formatting.AQUA : Formatting.WHITE;
    }

    private String buildImpactKey(int color, Vec3d pos) {
        int px = (int) Math.round(pos.x * 2.0D);
        int py = (int) Math.round(pos.y * 2.0D);
        int pz = (int) Math.round(pos.z * 2.0D);
        return (color & 0xFFFFFF) + ":" + px + ":" + py + ":" + pz;
    }

    private Integer extractParticleColor(Object parameters) {
        Integer reflected = extractColorReflective(parameters, 0, new IdentityHashMap<>());
        if (reflected != null) {
            return reflected & 0xFFFFFF;
        }

        String raw = String.valueOf(parameters);
        Matcher matcher = HEX_COLOR_PATTERN.matcher(raw);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1), 16) & 0xFFFFFF;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private Integer extractColorReflective(Object object, int depth, IdentityHashMap<Object, Boolean> visited) {
        if (object == null || depth > 3 || visited.containsKey(object)) {
            return null;
        }
        visited.put(object, Boolean.TRUE);

        Class<?> type = object.getClass();

        Integer intColor = invokeIntColorMethod(object, type);
        if (intColor != null) {
            return intColor;
        }

        Integer rgbColor = collectRgbComponents(object, type);
        if (rgbColor != null) {
            return rgbColor;
        }

        for (Method method : type.getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.getReturnType().isPrimitive()) {
                continue;
            }

            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("color") && !name.contains("parameter") && !name.contains("value") && !name.contains("vector")) {
                continue;
            }

            try {
                Object nested = method.invoke(object);
                Integer nestedColor = extractColorReflective(nested, depth + 1, visited);
                if (nestedColor != null) {
                    return nestedColor;
                }
            } catch (Exception ignored) {
            }
        }

        for (Field field : type.getDeclaredFields()) {
            if (field.getType().isPrimitive()) {
                continue;
            }

            String name = field.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("color") && !name.contains("value") && !name.contains("vector")) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object nested = field.get(object);
                Integer nestedColor = extractColorReflective(nested, depth + 1, visited);
                if (nestedColor != null) {
                    return nestedColor;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Integer invokeIntColorMethod(Object object, Class<?> type) {
        for (Method method : type.getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }

            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("color")) {
                continue;
            }

            try {
                if (method.getReturnType() == int.class || method.getReturnType() == Integer.class) {
                    Object value = method.invoke(object);
                    if (value instanceof Number number) {
                        return number.intValue() & 0xFFFFFF;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        for (Field field : type.getDeclaredFields()) {
            String name = field.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("color")) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(object);
                if (value instanceof Number number) {
                    return number.intValue() & 0xFFFFFF;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Integer collectRgbComponents(Object object, Class<?> type) {
        Float red = readFloatComponent(object, type, "red", "r");
        Float green = readFloatComponent(object, type, "green", "g");
        Float blue = readFloatComponent(object, type, "blue", "b");
        if (red == null || green == null || blue == null) {
            return null;
        }

        int r = normalizeColorComponent(red);
        int g = normalizeColorComponent(green);
        int b = normalizeColorComponent(blue);
        return (r << 16) | (g << 8) | b;
    }

    private Float readFloatComponent(Object object, Class<?> type, String... names) {
        for (String name : names) {
            for (Method method : type.getMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }

                String lowered = method.getName().toLowerCase(Locale.ROOT);
                if (!lowered.equals(name) && !lowered.equals("get" + name)) {
                    continue;
                }

                try {
                    Object value = method.invoke(object);
                    if (value instanceof Number number) {
                        return number.floatValue();
                    }
                } catch (Exception ignored) {
                }
            }

            for (Field field : type.getDeclaredFields()) {
                if (!field.getName().equalsIgnoreCase(name)) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(object);
                    if (value instanceof Number number) {
                        return number.floatValue();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private int normalizeColorComponent(float value) {
        float normalized = value <= 1.0F ? value * 255.0F : value;
        return MathHelper.clamp(Math.round(normalized), 0, 255);
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(num);
        };
    }

    private ClientWorld getWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.world;
    }

    private ClientPlayerEntity getPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null ? null : client.player;
    }
}
