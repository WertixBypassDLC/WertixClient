package sweetie.nezi.client.features.modules.render.nametags;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.joml.Vector2f;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.other.ReplaceUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class NameTagsRender implements QuickImports {
    private static final long PLAYER_SNAPSHOT_CACHE_MS = 200L;
    private static final float SCREEN_PADDING = 100f;
    private static final Pattern LEGACY_GHOST_TOKEN = Pattern.compile("(?i)(^|\\s)[0-9A-FK-ORX](?=\\s|$)");
    private static final Pattern NOISE_EDGE_TOKEN = Pattern.compile("(?i)(^|\\s)[a-z0-9](?=\\s|$)");

    private final NameTagsModule module;
    private final NameTagsPotions nameTagsPotions;
    private final List<RenderEntry> renderQueue = new ArrayList<>(64);
    private final Map<UUID, PlayerSnapshot> playerSnapshots = new HashMap<>(32);

    private final Color bgColor = new Color(0, 0, 0, 120);
    private final Color friendColor = new Color(132, 229, 121);

    public NameTagsRender(NameTagsModule module) {
        this.module = module;
        this.nameTagsPotions = new NameTagsPotions(module);
    }

    public void onRender(Render2DEvent.Render2DEventData event) {
        if (mc.world == null || mc.player == null || mc.getEntityRenderDispatcher().camera == null) {
            return;
        }

        renderQueue.clear();
        Set<UUID> activePlayers = new HashSet<>();
        float partialTicks = event.partialTicks();

        boolean playersOnly = module.targets.isEnabled("Игроки")
                && !module.targets.isEnabled("Животные")
                && !module.targets.isEnabled("Мобы");

        if (playersOnly) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || !module.entityFilter.isValid(player)) {
                    continue;
                }
                activePlayers.add(player.getUuid());
                queueEntity(player, partialTicks);
            }
        } else {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof LivingEntity livingEntity) || livingEntity == mc.player || !module.entityFilter.isValid(livingEntity)) {
                    continue;
                }
                if (livingEntity instanceof PlayerEntity player) {
                    activePlayers.add(player.getUuid());
                }
                queueEntity(livingEntity, partialTicks);
            }
        }

        if (module.targets.isEnabled("Себя") && !mc.options.getPerspective().isFirstPerson()) {
            activePlayers.add(mc.player.getUuid());
            queueEntity(mc.player, partialTicks);
        }

        playerSnapshots.keySet().removeIf(uuid -> !activePlayers.contains(uuid));

        renderQueue.sort(Comparator.comparingDouble(RenderEntry::distanceSq).reversed());
        for (RenderEntry entry : renderQueue) {
            renderTag(entry, event.context());
        }
    }

    private void queueEntity(LivingEntity entity, float partialTicks) {
        double distSq = mc.player.squaredDistanceTo(entity);
        if (distSq > 4096.0) {
            return;
        }

        double x = MathUtil.interpolate(entity.prevX, entity.getX(), partialTicks);
        double bottomY = MathUtil.interpolate(entity.prevY, entity.getY(), partialTicks);
        double topY = bottomY + entity.getHeight() + 0.34D;
        double z = MathUtil.interpolate(entity.prevZ, entity.getZ(), partialTicks);

        if (!ProjectionUtil.isInFrontOfCamera(x, topY, z)) {
            return;
        }

        Vector2f projectedTop = ProjectionUtil.project(x, topY, z);
        Vector2f projectedBottom = ProjectionUtil.project(x, bottomY, z);

        if (!ProjectionUtil.isProjectedOnScreen(projectedTop, SCREEN_PADDING)) {
            return;
        }

        renderQueue.add(new RenderEntry(entity, projectedTop.x, projectedTop.y, projectedBottom.x, projectedBottom.y, distSq));
    }

    private void renderTag(RenderEntry entry, DrawContext context) {
        LivingEntity entity = entry.entity();
        if (entity instanceof PlayerEntity player) {
            renderPlayerTag(player, entry, context);
            return;
        }

        renderSimpleTag(entity.getName().getString(), entry.topX(), entry.topY(), context);
    }

    private void renderPlayerTag(PlayerEntity player, RenderEntry entry, DrawContext context) {
        MatrixStack matrices = context.getMatrices();
        float scale = module.scale.getValue();
        float nameSize = 7.1f * scale;
        float infoSize = 5.4f * scale;
        float padding = 3.5f * scale;
        float rowGap = 1.5f * scale;
        float sideGap = 4f * scale;

        PlayerSnapshot snapshot = getPlayerSnapshot(player);

        float hp = getScoreboardHealth(player);
        String hpText = " [" + (int) Math.ceil(hp) + "]";
        float hpWidth = Fonts.PS_BOLD.getWidth(hpText, nameSize);

        float textWidth = snapshot.nameWidth + hpWidth;
        float cardWidth = textWidth + padding * 2f;
        float cardHeight = padding * 2f + nameSize;

        float cardX = entry.topX() - cardWidth / 2f;
        float cardY = entry.topY();

        // Минималистичный фон основного неймтега
        RenderUtil.RECT.draw(matrices, cardX, cardY, cardWidth, cardHeight, 0f, bgColor);

        float textY = cardY + padding;
        float textX = entry.topX() - textWidth / 2f;
        Color nameColor = snapshot.friend ? friendColor : Color.WHITE;
        Fonts.PS_BOLD.drawText(matrices, snapshot.displayName, textX, textY, nameSize, nameColor);

        Color hpColor = getHealthColor(hp, player.getMaxHealth() + player.getAbsorptionAmount());
        Fonts.PS_BOLD.drawText(matrices, hpText, textX + snapshot.nameWidth, textY, nameSize, hpColor);

        // Рендер зелий (если включено)
        if (!snapshot.potionLines.isEmpty()) {
            float sideX = cardX + cardWidth + sideGap;
            float sideY = cardY + Math.max(0f, (cardHeight - snapshot.effectBlockHeight) / 2f);
            float sidePad = 2.6f * scale;

            RenderUtil.RECT.draw(matrices, sideX, sideY, snapshot.effectBlockWidth, snapshot.effectBlockHeight, 0f, bgColor);

            float effectY = sideY + sidePad;
            for (NameTagsPotions.PotionLine line : snapshot.potionLines) {
                Fonts.PS_MEDIUM.drawText(matrices, line.left(), sideX + sidePad, effectY, infoSize, line.color());
                if (!line.right().isEmpty()) {
                    float rightX = sideX + snapshot.effectBlockWidth - sidePad - Fonts.PS_MEDIUM.getWidth(line.right(), infoSize);
                    Fonts.PS_MEDIUM.drawText(matrices, line.right(), rightX, effectY, infoSize, new Color(225, 225, 225));
                }
                effectY += infoSize + rowGap;
            }
        }

        // Рендер предметов в руках под игроком
        if (module.showArmor.getValue() && !snapshot.armorItems.isEmpty()) {
            drawArmorRow(context, snapshot, entry.topX(), cardY + cardHeight + (2f * scale), scale);
        }

        if (module.showHands.getValue()) {
            drawHandsText(matrices, snapshot, entry.bottomX(), entry.bottomY(), scale);
        }
    }

    private void drawHandsText(MatrixStack matrices, PlayerSnapshot snapshot, float centerX, float bottomY, float scale) {
        float textSize = 6f * scale;
        float padding = 2f * scale;
        float gap = 1f * scale;

        List<String> lines = new ArrayList<>(2);
        if (!snapshot.rightHandItem.isEmpty()) lines.add(snapshot.rightHandItem);
        if (!snapshot.leftHandItem.isEmpty()) lines.add(snapshot.leftHandItem);

        if (lines.isEmpty()) return;

        float blockHeight = lines.size() * textSize + Math.max(0, lines.size() - 1) * gap + padding * 2f;
        float maxWidth = 0f;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, Fonts.PS_MEDIUM.getWidth(line, textSize));
        }
        float blockWidth = maxWidth + padding * 2f;

        float startX = centerX - blockWidth / 2f;
        // Смещение чуть ниже ног
        float startY = bottomY + (4f * scale);

        RenderUtil.RECT.draw(matrices, startX, startY, blockWidth, blockHeight, 0f, bgColor);

        float textY = startY + padding;
        for (String line : lines) {
            float textX = centerX - Fonts.PS_MEDIUM.getWidth(line, textSize) / 2f;
            Fonts.PS_MEDIUM.drawText(matrices, line, textX, textY, textSize, Color.WHITE);
            textY += textSize + gap;
        }
    }

    private void drawArmorRow(DrawContext context, PlayerSnapshot snapshot, float centerX, float startY, float scale) {
        MatrixStack matrices = context.getMatrices();
        float iconSize = 12f * scale;
        float gap = 1.5f * scale;
        float padding = 2f * scale;
        float blockWidth = snapshot.armorItems.size() * iconSize + Math.max(0, snapshot.armorItems.size() - 1) * gap + padding * 2f;
        float blockHeight = iconSize + padding * 2f;
        float blockX = centerX - blockWidth / 2f;

        RenderUtil.RECT.draw(matrices, blockX, startY, blockWidth, blockHeight, 0f, bgColor);

        float drawX = blockX + padding;
        float drawY = startY + padding;
        float itemScale = iconSize / 16f;

        for (ItemStack stack : snapshot.armorItems) {
            matrices.push();
            matrices.translate(drawX, drawY, 0f);
            matrices.scale(itemScale, itemScale, 1f);
            context.drawItem(stack, 0, 0);
            matrices.pop();
            drawX += iconSize + gap;
        }
    }

    private void renderSimpleTag(String displayName, float centerX, float baseY, DrawContext context) {
        MatrixStack matrices = context.getMatrices();
        float scale = module.scale.getValue();
        float nameSize = 7f * scale;
        float padding = 3f * scale;
        float nameWidth = Fonts.PS_BOLD.getWidth(displayName, nameSize);
        float cardWidth = nameWidth + padding * 2f;
        float cardHeight = nameSize + padding * 2f;
        float cardX = centerX - cardWidth / 2f;

        RenderUtil.RECT.draw(matrices, cardX, baseY, cardWidth, cardHeight, 0f, bgColor);
        Fonts.PS_BOLD.drawText(matrices, displayName, centerX - nameWidth / 2f, baseY + padding, nameSize, Color.WHITE);
    }

    private Color getHealthColor(float health, float maxHealth) {
        float p = net.minecraft.util.math.MathHelper.clamp(health / Math.max(1f, maxHealth), 0f, 1f);
        int r, g, b;
        if (p > 0.5f) {
            float t = (p - 0.5f) * 2f;
            r = (int) net.minecraft.util.math.MathHelper.lerp(t, 255, 72);
            g = (int) net.minecraft.util.math.MathHelper.lerp(t, 235, 220);
            b = (int) net.minecraft.util.math.MathHelper.lerp(t, 59, 86);
        } else {
            float t = p * 2f;
            r = 255;
            g = (int) net.minecraft.util.math.MathHelper.lerp(t, 62, 235);
            b = (int) net.minecraft.util.math.MathHelper.lerp(t, 62, 59);
        }
        return new Color(r, g, b, 255);
    }

    private PlayerSnapshot getPlayerSnapshot(PlayerEntity player) {
        long now = System.currentTimeMillis();
        PlayerSnapshot snapshot = playerSnapshots.get(player.getUuid());
        if (snapshot != null && now - snapshot.updatedAt <= PLAYER_SNAPSHOT_CACHE_MS) {
            return snapshot;
        }

        PlayerSnapshot rebuilt = new PlayerSnapshot();
        String baseName = player.getGameProfile().getName();
        rebuilt.friend = FriendManager.getInstance().contains(baseName);
        rebuilt.displayName = buildDisplayName(player, baseName);

        if (module.showHands.getValue()) {
            rebuilt.rightHandItem = getRightHandName(player);
            rebuilt.leftHandItem = getLeftHandName(player);
        }

        rebuilt.armorItems = module.showArmor.getValue() ? collectArmorItems(player) : List.of();
        rebuilt.potionLines = module.showPotions.getValue() ? nameTagsPotions.collectLines(player) : List.of();

        float scale = module.scale.getValue();
        float nameSize = 7.1f * scale;
        float infoSize = 5.4f * scale;
        float rowGap = 1.5f * scale;
        float sidePad = 2.6f * scale;

        rebuilt.nameWidth = Fonts.PS_BOLD.getWidth(rebuilt.displayName, nameSize);
        rebuilt.effectBlockWidth = 0f;
        rebuilt.effectBlockHeight = 0f;

        for (NameTagsPotions.PotionLine line : rebuilt.potionLines) {
            float width = Fonts.PS_MEDIUM.getWidth(line.left(), infoSize);
            if (!line.right().isEmpty()) {
                width += (6f * scale) + Fonts.PS_MEDIUM.getWidth(line.right(), infoSize);
            }
            rebuilt.effectBlockWidth = Math.max(rebuilt.effectBlockWidth, width);
        }
        if (!rebuilt.potionLines.isEmpty()) {
            rebuilt.effectBlockWidth += sidePad * 2f;
            rebuilt.effectBlockHeight = sidePad * 2f
                    + rebuilt.potionLines.size() * infoSize
                    + Math.max(0, rebuilt.potionLines.size() - 1) * rowGap;
        }
        rebuilt.updatedAt = now;

        playerSnapshots.put(player.getUuid(), rebuilt);
        return rebuilt;
    }

    private List<ItemStack> collectArmorItems(PlayerEntity player) {
        List<ItemStack> armorItems = new ArrayList<>(4);
        addArmorItem(armorItems, player.getInventory().armor.get(3));
        addArmorItem(armorItems, player.getInventory().armor.get(2));
        addArmorItem(armorItems, player.getInventory().armor.get(1));
        addArmorItem(armorItems, player.getInventory().armor.get(0));
        return armorItems;
    }

    private void addArmorItem(List<ItemStack> armorItems, ItemStack stack) {
        if (!stack.isEmpty()) {
            armorItems.add(stack.copy());
        }
    }

    private String getRightHandName(PlayerEntity player) {
        ItemStack stack = player.getMainArm() == Arm.RIGHT ? player.getMainHandStack() : player.getOffHandStack();
        return stack.isEmpty() ? "" : stack.getName().getString();
    }

    private String getLeftHandName(PlayerEntity player) {
        ItemStack stack = player.getMainArm() == Arm.RIGHT ? player.getOffHandStack() : player.getMainHandStack();
        return stack.isEmpty() ? "" : stack.getName().getString();
    }

    private String buildDisplayName(PlayerEntity player, String baseName) {
        String decorated;
        try {
            decorated = player.getDisplayName() != null ? player.getDisplayName().getString() : baseName;
        } catch (Exception ignored) {
            decorated = baseName;
        }

        decorated = stripFormattingCodes(ReplaceUtil.replaceSymbols(decorated));
        String cleaned = sanitizeDecoratedName(decorated, baseName);
        return cleaned.isEmpty() ? baseName : cleaned;
    }

    private String sanitizeDecoratedName(String decorated, String baseName) {
        String normalized = stripFormattingCodes(LEGACY_GHOST_TOKEN.matcher(decorated).replaceAll(" "));
        normalized = normalized.replaceAll("[\\p{Cntrl}\\p{Co}]", " ");
        normalized = normalized.replaceAll("\s{2,}", " ").trim();
        int nameIndex = normalized.toLowerCase().indexOf(baseName.toLowerCase());
        if (nameIndex < 0) {
            return stripNoiseTokens(normalized);
        }

        String prefix = stripNoiseTokens(normalized.substring(0, nameIndex));
        String suffix = stripNoiseTokens(normalized.substring(nameIndex + baseName.length()));
        String combined = ((prefix.isEmpty() ? "" : prefix + " ") + baseName + (suffix.isEmpty() ? "" : " " + suffix)).trim();
        return combined.replaceAll("\s{2,}", " ").trim();
    }

    private String stripNoiseTokens(String value) {
        String cleaned = stripFormattingCodes(value);
        cleaned = cleaned.replaceAll("[\\p{Cntrl}\\p{Co}]", " ");
        cleaned = LEGACY_GHOST_TOKEN.matcher(cleaned).replaceAll(" ");
        cleaned = NOISE_EDGE_TOKEN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll("\s{2,}", " ").trim();
        return cleaned;
    }

    private String stripFormattingCodes(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("(?i)\u00A7[0-9A-FK-ORX]", "").replace(' ', ' ').trim();
    }

    private static final class PlayerSnapshot {
        private String displayName = "";
        private boolean friend;
        private String rightHandItem = "";
        private String leftHandItem = "";
        private List<ItemStack> armorItems = List.of();
        private List<NameTagsPotions.PotionLine> potionLines = List.of();
        private float nameWidth;
        private float effectBlockWidth;
        private float effectBlockHeight;
        private long updatedAt;
    }

    private record RenderEntry(LivingEntity entity, float topX, float topY, float bottomX, float bottomY, double distanceSq) { }

    private float getScoreboardHealth(LivingEntity entity) {
        if (mc.world == null || entity == null) return entity.getHealth();

        net.minecraft.scoreboard.Scoreboard scoreboard = mc.world.getScoreboard();
        net.minecraft.scoreboard.ScoreboardObjective objective =
                scoreboard.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.BELOW_NAME);

        if (objective != null) {
            net.minecraft.scoreboard.ReadableScoreboardScore score =
                    scoreboard.getScore(entity, objective);

            if (score != null) {
                return (float) score.getScore();
            }
        }

        return entity.getHealth() + entity.getAbsorptionAmount();
    }
}
