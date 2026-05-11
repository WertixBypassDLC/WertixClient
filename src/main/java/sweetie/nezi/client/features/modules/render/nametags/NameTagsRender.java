package sweetie.nezi.client.features.modules.render.nametags;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.joml.Vector2f;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
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
import java.util.Locale;
import java.util.regex.Pattern;

public class NameTagsRender implements QuickImports {
    private static final long PLAYER_SNAPSHOT_CACHE_MS = 125L;
    private static final float SCREEN_PADDING = 100f;
    private static final Pattern LEGACY_GHOST_TOKEN = Pattern.compile("(?i)(^|\\s)[0-9A-FK-ORX](?=\\s|$)");
    private static final Pattern NOISE_EDGE_TOKEN = Pattern.compile("(?i)(^|\\s)[a-z0-9](?=\\s|$)");
    private static final Pattern NON_TEXT_TOKEN = Pattern.compile("[^\\p{L}\\p{N}_\\-+#]+", Pattern.UNICODE_CHARACTER_CLASS);

    private final NameTagsModule module;
    private final NameTagsPotions nameTagsPotions;
    private final List<RenderEntry> renderQueue = new ArrayList<>(64);
    private final Map<UUID, PlayerSnapshot> playerSnapshots = new HashMap<>(32);

    private final Color bgColor = new Color(12, 12, 18, 170);
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
        float nameSize = 6.15f * scale;
        float infoSize = 4.6f * scale;
        float padding = 2.7f * scale;
        float rowGap = 1.25f * scale;
        float sideGap = 3.2f * scale;
        float headSize = 10.2f * scale;
        float headGap = 2.4f * scale;
        float topRowGap = 1.9f * scale;
        float bottomGap = 4.6f * scale;

        PlayerSnapshot snapshot = getPlayerSnapshot(player);

        float hp = getScoreboardHealth(player);
        String hpText = formatHealthText(player, hp);
        float hpWidth = Fonts.PS_BOLD.getWidth(hpText, nameSize);
        float separatorWidth = Fonts.PS_MEDIUM.getWidth(" - ", nameSize);
        float actualTextWidth = snapshot.displayNameWidth + separatorWidth + hpWidth;

        float textWidth = Math.max(actualTextWidth, scaled(62f) * scale);
        float contentWidth = headSize + headGap + textWidth;
        float cardWidth = contentWidth + padding * 2f;
        float cardHeight = Math.max(headSize, nameSize) + padding * 2f;

        float cardX = entry.topX() - cardWidth / 2f;
        float cardY = entry.topY();

        if (!snapshot.topItems.isEmpty()) {
            float equipmentHeight = getTopEquipmentRowHeight(scale);
            drawTopEquipmentRow(context, snapshot, entry.topX(), cardY - equipmentHeight - topRowGap, scale);
        }

        int colorSeed = player.getUuid().hashCode();
        drawCardSurface(matrices, cardX, cardY, cardWidth, cardHeight, scaled(4.8f), colorSeed, 214);

        float headX = cardX + padding;
        float headY = cardY + cardHeight / 2f - headSize / 2f;
        RenderUtil.BLUR_RECT.draw(matrices, headX, headY, headSize, headSize, scaled(3.2f), new Color(18, 19, 27, 220));
        RenderUtil.RECT.draw(matrices, headX, headY, headSize, headSize, scaled(3.2f), new Color(54, 57, 70, 145));
        RenderUtil.TEXTURE_RECT.drawHead(matrices, player, headX + scaled(0.95f), headY + scaled(0.95f), headSize - scaled(1.9f), headSize - scaled(1.9f), 0f, scaled(2.35f), Color.WHITE);

        float textY = cardY + cardHeight / 2f - nameSize / 2f - scaled(0.1f);
        float textX = headX + headSize + headGap;
        float hpX = textX + textWidth - hpWidth;
        float separatorX = hpX - separatorWidth;
        float nameX = separatorX - snapshot.displayNameWidth;
        Color nameColor = snapshot.friend ? friendColor : Color.WHITE;
        Fonts.PS_BOLD.drawText(matrices, snapshot.displayName, nameX, textY, nameSize, nameColor);
        Fonts.PS_MEDIUM.drawText(matrices, " - ", separatorX, textY, nameSize, new Color(154, 158, 172, 220));

        Color hpColor = getHealthColor(hp, player.getMaxHealth() + player.getAbsorptionAmount());
        Fonts.PS_BOLD.drawText(matrices, hpText, hpX, textY, nameSize, hpColor);

        if (!snapshot.potionLines.isEmpty()) {
            float sideX = cardX + cardWidth + sideGap;
            float sideY = cardY + Math.max(0f, (cardHeight - snapshot.effectBlockHeight) / 2f);
            float sidePad = 2.6f * scale;

            drawCardSurface(matrices, sideX, sideY, snapshot.effectBlockWidth, snapshot.effectBlockHeight, scaled(4.3f), colorSeed + 91, 205);

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

        if (module.showHands.getValue() && (!snapshot.mainHandInfo.isEmpty() || !snapshot.offHandInfo.isEmpty())) {
            drawHandInfoRows(context, snapshot, entry.topX(), entry.bottomY() + bottomGap, cardWidth, scale, colorSeed);
        }
    }

    private void drawHandInfoRows(DrawContext context, PlayerSnapshot snapshot, float centerX, float startY, float cardWidth, float scale, int colorSeed) {
        MatrixStack matrices = context.getMatrices();
        float rowHeight = scaled(8.8f) * scale;
        float rowGap = scaled(1.45f) * scale;
        float fontSize = 4.55f * scale;
        float maxWidth = Math.max(cardWidth * 0.78f, scaled(52f) * scale);
        int rowIndex = 0;

        if (!snapshot.mainHandInfo.isEmpty()) {
            float rowWidth = Math.min(Math.max(scaled(24f) * scale, snapshot.mainHandInfoWidth + scaled(7.8f) * scale), maxWidth);
            drawHandInfoRow(matrices, snapshot.mainHandInfo, centerX, startY + rowIndex * (rowHeight + rowGap), rowWidth, rowHeight, fontSize, colorSeed);
            rowIndex++;
        }

        if (!snapshot.offHandInfo.isEmpty()) {
            float rowWidth = Math.min(Math.max(scaled(24f) * scale, snapshot.offHandInfoWidth + scaled(7.8f) * scale), maxWidth);
            drawHandInfoRow(matrices, snapshot.offHandInfo, centerX, startY + rowIndex * (rowHeight + rowGap), rowWidth, rowHeight, fontSize, colorSeed + 17);
        }
    }

    private void drawHandInfoRow(MatrixStack matrices, String text, float centerX, float y, float width, float height, float fontSize, int colorSeed) {
        float rowX = centerX - width / 2f;
        drawCardSurface(matrices, rowX, y, width, height, height * 0.42f, colorSeed, 196);
        float textWidth = Fonts.PS_MEDIUM.getWidth(text, fontSize);
        float textX = rowX + Math.max(scaled(2.8f), (width - textWidth) / 2f);
        Fonts.PS_MEDIUM.drawText(matrices, text, textX, y + height / 2f - fontSize / 2f, fontSize, UIColors.textColor(228));
    }

    private void drawTopEquipmentRow(DrawContext context, PlayerSnapshot snapshot, float centerX, float startY, float scale) {
        MatrixStack matrices = context.getMatrices();
        if (snapshot.topItems.isEmpty()) {
            return;
        }

        float slotSize = 11.0f * scale;
        float gap = 1.65f * scale;
        float padding = 1.7f * scale;
        float blockWidth = snapshot.topItems.size() * slotSize + Math.max(0, snapshot.topItems.size() - 1) * gap + padding * 2f;
        float blockHeight = slotSize + padding * 2f;
        float blockX = centerX - blockWidth / 2f;
        float blockY = startY;

        drawCardSurface(matrices, blockX, blockY, blockWidth, blockHeight, scaled(4.0f), snapshot.topItems.size() * 11, 192);

        float drawX = blockX + padding;
        float drawY = blockY + padding;
        float itemScale = slotSize / 16f;
        float countSize = 4.4f * scale;

        for (ItemStack stack : snapshot.topItems) {
            Color slotFill = ColorUtil.interpolate(UIColors.cardSecondary(192), UIColors.panelSoft(186), 0.22f);
            RenderUtil.BLUR_RECT.draw(matrices, drawX, drawY, slotSize, slotSize, scaled(2.8f), slotFill);
            RenderUtil.RECT.draw(matrices, drawX, drawY, slotSize, slotSize, scaled(2.8f), UIColors.primary(112));
            matrices.push();
            matrices.translate(drawX + scaled(0.15f), drawY + scaled(0.15f), 0f);
            matrices.scale(itemScale, itemScale, 1f);
            context.drawItem(stack, 0, 0);
            matrices.pop();

            if (stack.getCount() > 1) {
                String countText = String.valueOf(stack.getCount());
                float countWidth = Fonts.PS_BOLD.getWidth(countText, countSize);
                float textX = drawX + slotSize - countWidth - scaled(1.35f);
                float textY = drawY + slotSize - countSize - scaled(1.1f);
                RenderUtil.BLUR_RECT.draw(matrices,
                        textX - scaled(1.0f),
                        textY - scaled(0.55f),
                        countWidth + scaled(2.0f),
                        countSize + scaled(1.4f),
                        scaled(2.0f),
                        new Color(10, 10, 15, 225));
                Fonts.PS_BOLD.drawText(matrices, countText, textX, textY, countSize, Color.WHITE);
            }

            drawX += slotSize + gap;
        }
    }

    private float getTopEquipmentRowHeight(float scale) {
        float slotSize = 11.0f * scale;
        float padding = 1.7f * scale;
        return slotSize + padding * 2f;
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

        drawCardSurface(matrices, cardX, baseY, cardWidth, cardHeight, scaled(4.1f), displayName.hashCode(), 192);
        Fonts.PS_BOLD.drawText(matrices, displayName, centerX - nameWidth / 2f, baseY + padding, nameSize, Color.WHITE);
    }

    private void drawCardSurface(MatrixStack matrices, float x, float y, float width, float height, float round, int colorSeed, int alpha) {
        Color blurColor = new Color(7, 7, 11, Math.min(alpha, 190));
        Color surfaceColor = new Color(14, 15, 21, Math.min(alpha, 232));
        RenderUtil.BLUR_RECT.draw(matrices, x - scaled(0.35f), y - scaled(0.35f), width + scaled(0.7f), height + scaled(0.7f), round + scaled(0.25f), blurColor);
        RenderUtil.BLUR_RECT.draw(matrices, x, y, width, height, round, surfaceColor);
        RenderUtil.RECT.draw(matrices, x, y, width, height, round, surfaceColor);
        RenderUtil.RECT.draw(matrices, x, y, width, height, round, new Color(255, 255, 255, Math.min(alpha, 10)));
        RenderUtil.RECT.draw(matrices, x, y, width, height, round, new Color(52, 55, 66, Math.min(alpha, 132)));
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

        rebuilt.topItems = collectTopItems(player);
        rebuilt.mainHandInfo = module.showHands.getValue() ? buildHandInfo(player.getMainHandStack()) : "";
        rebuilt.offHandInfo = module.showHands.getValue() ? buildHandInfo(player.getOffHandStack()) : "";
        rebuilt.potionLines = module.showPotions.getValue() ? nameTagsPotions.collectLines(player) : List.of();

        float scale = module.scale.getValue();
        float nameSize = 6.3f * scale;
        float infoSize = 4.55f * scale;
        float rowGap = 1.5f * scale;
        float sidePad = 2.6f * scale;

        rebuilt.displayNameWidth = Fonts.PS_BOLD.getWidth(rebuilt.displayName, nameSize);
        rebuilt.mainHandInfoWidth = rebuilt.mainHandInfo.isEmpty() ? 0f : Fonts.PS_MEDIUM.getWidth(rebuilt.mainHandInfo, infoSize);
        rebuilt.offHandInfoWidth = rebuilt.offHandInfo.isEmpty() ? 0f : Fonts.PS_MEDIUM.getWidth(rebuilt.offHandInfo, infoSize);
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

    private List<ItemStack> collectTopItems(PlayerEntity player) {
        List<ItemStack> topItems = new ArrayList<>(6);
        if (module.showArmor.getValue()) {
            addPreviewItem(topItems, player.getInventory().armor.get(3));
            addPreviewItem(topItems, player.getInventory().armor.get(2));
            addPreviewItem(topItems, player.getInventory().armor.get(1));
            addPreviewItem(topItems, player.getInventory().armor.get(0));
        }
        if (module.showHands.getValue()) {
            addPreviewItem(topItems, player.getMainHandStack());
            addPreviewItem(topItems, player.getOffHandStack());
        }
        return topItems;
    }

    private void addPreviewItem(List<ItemStack> topItems, ItemStack stack) {
        if (!stack.isEmpty()) {
            topItems.add(stack.copy());
        }
    }

    private String buildHandInfo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }

        String itemName = ReplaceUtil.replaceSymbols(stack.getName().getString()).trim();
        if (itemName.isEmpty()) {
            itemName = "Unknown";
        }

        if (stack.getCount() > 1 && stack.isStackable()) {
            itemName += " x" + stack.getCount();
        }

        return itemName;
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
        cleaned = cleaned.replaceAll("\s{2,}", " ").trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String token : cleaned.split("\\s+")) {
            String normalizedToken = NON_TEXT_TOKEN.matcher(token).replaceAll("").trim();
            if (normalizedToken.isEmpty()) {
                continue;
            }
            if (NOISE_EDGE_TOKEN.matcher(" " + normalizedToken).find() && normalizedToken.length() <= 1) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(normalizedToken);
        }
        return builder.toString().replaceAll("\s{2,}", " ").trim();
    }

    private String stripFormattingCodes(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("(?i)\u00A7[0-9A-FK-ORX]", "").replace(' ', ' ').trim();
    }

    private String formatHealthText(PlayerEntity player, float hp) {
        float displayHealth = Math.max(0f, hp);
        String healthText = (Math.abs(displayHealth - Math.round(displayHealth)) < 0.05f
                ? String.valueOf(Math.round(displayHealth))
                : String.format(Locale.US, "%.1f", displayHealth)) + "HP";
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.1f) {
            healthText += " +" + (Math.abs(absorption - Math.round(absorption)) < 0.05f
                    ? Math.round(absorption)
                    : String.format(Locale.US, "%.1f", absorption));
        }
        return healthText;
    }

    private float scaled(float value) {
        return sweetie.nezi.client.services.RenderService.getInstance().scaled(value);
    }

    private static final class PlayerSnapshot {
        private String displayName = "";
        private boolean friend;
        private List<ItemStack> topItems = List.of();
        private String mainHandInfo = "";
        private String offHandInfo = "";
        private List<NameTagsPotions.PotionLine> potionLines = List.of();
        private float displayNameWidth;
        private float mainHandInfoWidth;
        private float offHandInfoWidth;
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

