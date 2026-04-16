package sweetie.nezi.client.features.modules.render.nametags;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.joml.Vector2f;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.other.ReplaceUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.other.StreamerModule;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NameTagsRender implements QuickImports {
    private static final long PLAYER_SNAPSHOT_CACHE_MS = 200L;
    private static final float SCREEN_PADDING = 100f;

    private final NameTagsModule module;
    private final NameTagsPotions nameTagsPotions;
    private final List<RenderEntry> renderQueue = new ArrayList<>(64);
    private final Map<UUID, PlayerSnapshot> playerSnapshots = new HashMap<>(32);

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

    private void drawGlass(MatrixStack ms, float x, float y, float w, float h, float round, Color strokeColor) {
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, round, UIColors.blur(220), 0.08f);
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, round, UIColors.backgroundBlur(220), 0.06f);
        RenderUtil.RECT.draw(ms, x, y, w, h, round, UIColors.panel(220));
        RenderUtil.RECT.draw(ms, x, y, w, h, round, UIColors.overlay(30));
        RenderUtil.RECT.draw(ms, x, y, w, h, round, strokeColor);
    }

    private void renderPlayerTag(PlayerEntity player, RenderEntry entry, DrawContext context) {
        MatrixStack matrices = context.getMatrices();
        float scale = module.scale.getValue();
        float nameSize = 7.1f * scale;
        float infoSize = 5.4f * scale;
        float padding = 3.5f * scale;
        float rowGap = 1.5f * scale;
        float sideGap = 4f * scale;
        float round = 3.5f * scale;

        PlayerSnapshot snapshot = getPlayerSnapshot(player);

        float hp = getScoreboardHealth(player);
        String hpText = " [" + (int) Math.ceil(hp) + "]";
        float hpWidth = Fonts.PS_BOLD.getWidth(hpText, nameSize);

        float textWidth = snapshot.nameWidth + hpWidth;
        float cardWidth = textWidth + padding * 2f;
        float cardHeight = padding * 2f + nameSize;

        float cardX = entry.topX() - cardWidth / 2f;
        float cardY = entry.topY();

        List<ItemStack> topItems = new ArrayList<>();
        if (!snapshot.armor[3].isEmpty()) topItems.add(snapshot.armor[3]);
        if (!snapshot.armor[2].isEmpty()) topItems.add(snapshot.armor[2]);
        if (!snapshot.armor[1].isEmpty()) topItems.add(snapshot.armor[1]);
        if (!snapshot.armor[0].isEmpty()) topItems.add(snapshot.armor[0]);
        if (!snapshot.mainHand.isEmpty()) topItems.add(snapshot.mainHand);
        if (!snapshot.offHand.isEmpty()) topItems.add(snapshot.offHand);

        Color cardStroke = snapshot.friend ? friendColor : UIColors.stroke(255);

        if (!topItems.isEmpty()) {
            float slotSize = 12f * scale;
            float slotGap = 2.5f * scale;
            float totalTopW = topItems.size() * slotSize + (topItems.size() - 1) * slotGap;
            float startTopX = entry.topX() - totalTopW / 2f;
            float topY = cardY - slotSize - (3.5f * scale);

            for (ItemStack stack : topItems) {
                drawGlass(matrices, startTopX, topY, slotSize, slotSize, round, cardStroke);

                matrices.push();
                matrices.translate(startTopX, topY, 0f);
                float itemScale = slotSize / 16f;
                matrices.scale(itemScale, itemScale, 1f);
                context.drawItem(stack, 0, 0);
                matrices.pop();

                startTopX += slotSize + slotGap;
            }
        }

        drawGlass(matrices, cardX, cardY, cardWidth, cardHeight, round, cardStroke);

        float textY = cardY + padding;
        float textX = entry.topX() - textWidth / 2f;

        Fonts.PS_BOLD.drawText(matrices, snapshot.displayName, textX, textY, nameSize);

        Color hpColor = getHealthColor(hp, player.getMaxHealth() + player.getAbsorptionAmount());
        Fonts.PS_BOLD.drawText(matrices, hpText, textX + snapshot.nameWidth, textY, nameSize, hpColor);

        if (!snapshot.potionLines.isEmpty()) {
            float sideX = cardX + cardWidth + sideGap;
            float sideY = cardY + Math.max(0f, (cardHeight - snapshot.effectBlockHeight) / 2f);
            float sidePad = 2.6f * scale;

            drawGlass(matrices, sideX, sideY, snapshot.effectBlockWidth, snapshot.effectBlockHeight, round, cardStroke);

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

        if (module.showHands.getValue()) {
            drawHandsText(matrices, snapshot, entry.bottomX(), entry.bottomY(), scale, round, cardStroke);
        }
    }

    private void drawHandsText(MatrixStack matrices, PlayerSnapshot snapshot, float centerX, float bottomY, float scale, float round, Color stroke) {
        float textSize = 6f * scale;
        float padding = 2.5f * scale;
        float gap = 1f * scale;

        List<ColoredItemText> lines = new ArrayList<>(2);

        addHandText(lines, snapshot.mainHand);
        addHandText(lines, snapshot.offHand);

        if (lines.isEmpty()) return;

        float blockHeight = lines.size() * textSize + Math.max(0, lines.size() - 1) * gap + padding * 2f;
        float maxWidth = 0f;
        for (ColoredItemText line : lines) {
            maxWidth = Math.max(maxWidth, Fonts.PS_MEDIUM.getWidth(line.text, textSize));
        }
        float blockWidth = maxWidth + padding * 2f;

        float startX = centerX - blockWidth / 2f;
        float startY = bottomY + (4f * scale);

        drawGlass(matrices, startX, startY, blockWidth, blockHeight, round, stroke);

        float textY = startY + padding;
        for (ColoredItemText line : lines) {
            float textX = centerX - Fonts.PS_MEDIUM.getWidth(line.text, textSize) / 2f;
            Fonts.PS_MEDIUM.drawText(matrices, line.text, textX, textY, textSize, line.color);
            textY += textSize + gap;
        }
    }

    private void addHandText(List<ColoredItemText> lines, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        String name = stack.getName().getString();
        Color color = Color.WHITE;

        if (stack.isOf(Items.TOTEM_OF_UNDYING) && stack.hasEnchantments()) {
            name = "Зачарованный тотем";
            color = new Color(85, 255, 255);
        }

        if (stack.getCount() > 1) {
            name += " x" + stack.getCount();
        }

        lines.add(new ColoredItemText(name, color));
    }

    private void renderSimpleTag(String displayName, float centerX, float baseY, DrawContext context) {
        MatrixStack matrices = context.getMatrices();
        float scale = module.scale.getValue();
        float nameSize = 7f * scale;
        float padding = 3f * scale;
        float round = 3.5f * scale;
        float nameWidth = Fonts.PS_BOLD.getWidth(displayName, nameSize);
        float cardWidth = nameWidth + padding * 2f;
        float cardHeight = nameSize + padding * 2f;
        float cardX = centerX - cardWidth / 2f;

        drawGlass(matrices, cardX, baseY, cardWidth, cardHeight, round, UIColors.stroke(255));
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
        rebuilt.displayName = buildDisplayName(player);

        rebuilt.mainHand = player.getMainHandStack();
        rebuilt.offHand = player.getOffHandStack();

        for (int i = 0; i < 4; i++) {
            rebuilt.armor[i] = player.getInventory().armor.get(i);
        }

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

    private Text buildDisplayName(PlayerEntity player) {
        if (StreamerModule.getInstance().isEnabled() && StreamerModule.getInstance().getHideNick().getValue()) {
            return Text.literal(StreamerModule.getInstance().getProtectedName());
        }

        Text displayName = player.getDisplayName();
        if (displayName == null) {
            displayName = Text.literal(player.getGameProfile().getName());
        }

        return ReplaceUtil.replaceSymbols(displayName);
    }

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

    private static final class PlayerSnapshot {
        private Text displayName;
        private boolean friend;
        private ItemStack[] armor = new ItemStack[4];
        private ItemStack mainHand = ItemStack.EMPTY;
        private ItemStack offHand = ItemStack.EMPTY;
        private List<NameTagsPotions.PotionLine> potionLines = List.of();
        private float nameWidth;
        private float effectBlockWidth;
        private float effectBlockHeight;
        private long updatedAt;
    }

    private record RenderEntry(LivingEntity entity, float topX, float topY, float bottomX, float bottomY, double distanceSq) { }
    private record ColoredItemText(String text, Color color) {}
}