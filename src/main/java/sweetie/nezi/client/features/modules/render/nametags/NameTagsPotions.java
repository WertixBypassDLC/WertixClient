package sweetie.nezi.client.features.modules.render.nametags;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import sweetie.nezi.api.utils.other.TextUtil;
import sweetie.nezi.client.features.modules.other.PotionTrackerModule;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NameTagsPotions {
    private final NameTagsModule module;

    public NameTagsPotions(NameTagsModule module) {
        this.module = module;
    }

    public List<PotionLine> collectLines(PlayerEntity player) {
        Map<String, PotionLine> lines = new LinkedHashMap<>();

        for (StatusEffectInstance effect : player.getStatusEffects()) {
            lines.put(lineKey(effect.getTranslationKey(), effect.getAmplifier() + 1), new PotionLine(
                    getEffectName(effect),
                    TextUtil.getDurationText(effect.getDuration()),
                    module.textColor.getValue()
            ));
        }

        if (PotionTrackerModule.getInstance().isEnabled()) {
            List<PotionTrackerModule.TrackedEffect> tracked = PotionTrackerModule.getInstance().getPlayerCustomEffects().get(player.getName().getString());
            if (tracked != null) {
                long now = System.currentTimeMillis();
                for (PotionTrackerModule.TrackedEffect effect : tracked) {
                    if (now > effect.endTime) {
                        continue;
                    }

                    String key = effect.key != null ? effect.key : lineKey(effect.name, 0);
                    int remainingSec = (int) Math.max(1L, (effect.endTime - now) / 1000L);
                    lines.put(key, new PotionLine(
                            formatTrackedName(effect),
                            formatSeconds(remainingSec),
                            effect.color != null ? effect.color : module.textColor.getValue()
                    ));
                }
            }
        }

        return new ArrayList<>(lines.values());
    }

    private String getEffectName(StatusEffectInstance effect) {
        String translationKey = effect.getTranslationKey();
        String name = translationKey.substring(translationKey.lastIndexOf('.') + 1);
        name = name.substring(0, 1).toUpperCase() + name.substring(1);

        if (effect.getAmplifier() > 0) {
            name += " " + (effect.getAmplifier() + 1);
        }

        return name.replace("_", " ");
    }

    private String lineKey(String name, int amplifier) {
        return name + ":" + amplifier;
    }

    private String formatTrackedName(PotionTrackerModule.TrackedEffect effect) {
        if (effect.level == null || effect.level.isBlank()) {
            return effect.name;
        }
        return effect.name + " " + effect.level;
    }

    private String formatSeconds(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return minutes + ":" + String.format("%02d", remainingSeconds);
    }

    public record PotionLine(String left, String right, Color color) { }
}
