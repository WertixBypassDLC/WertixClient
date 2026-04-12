package sweetie.nezi.client.ui.widget.overlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.utils.animation.AnimationUtil;
import sweetie.nezi.api.utils.animation.Easing;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.other.ReplaceUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.combat.AimAssistModule;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.features.modules.combat.TriggerBotModule;
import sweetie.nezi.client.features.modules.render.InterfaceModule;
import sweetie.nezi.client.ui.widget.Widget;

import java.awt.*;

public class TargetInfoWidget extends Widget {

    private final AnimationUtil anim = new AnimationUtil();
    private LivingEntity target;
    private boolean allow;
    private long lastSeenMs;
    private float healthAnim = 0f;

    public TargetInfoWidget() {
        super(30f, 30f);
        setWidgetScale(1.1f);
    }

    @Override
    public String getName() {
        return "Target info";
    }

    @Override
    public boolean shouldAppearWhenInterfaceVisible() {
        if (resolveTarget() != null) {
            return true;
        }
        return target != null && (System.currentTimeMillis() - lastSeenMs) <= 800L;
    }

    @Override
    public float scaled(float value) {
        return sweetie.nezi.client.services.RenderService.getInstance().scaled(value);
    }

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        DrawContext ctx = event.context();
        MatrixStack ms = event.matrixStack();

        LivingEntity fresh = resolveTarget();
        if (fresh != null) {
            target = fresh;
            allow = true;
            lastSeenMs = System.currentTimeMillis();
        } else {
            allow = false;
        }

        boolean hide = !allow && (System.currentTimeMillis() - lastSeenMs) > 800L;

        anim.update();
        anim.run(hide ? 0.0 : 1.0, 320, Easing.QUINT_OUT);
        float av = (float) anim.getValue();
        if (av < 0.005f) {
            if (hide) target = null;
            return;
        }
        if (target == null || mc.player == null) return;

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        int alpha = animatedAlpha(MathHelper.clamp((int) (255f * av), 5, 255));
        float hr = hudRound();
        float p = scaled(3.2f);
        float headSize = scaled(21.6f);
        float headRound = scaled(3f);
        float fName = scaled(6.55f);
        float fHp = scaled(5.35f);
        float barH = scaled(3.2f);
        float itemSlotSize = scaled(8.2f);
        float itemSlotGap = scaled(1.2f);
        float itemSlotRound = scaled(2.1f);
        float itemRowGap = scaled(1.5f);

        String name = ReplaceUtil.protectedString(target.getName().getString());
        float hp = target.getHealth();
        float maxHp = Math.max(1f, target.getMaxHealth());
        healthAnim += (MathHelper.clamp(hp / maxHp, 0f, 1f) - healthAnim) * 0.15f;
        String hpText = "HP: " + (int) hp;

        float fullNameWidth = Fonts.PS_BOLD.getWidth(name, fName);
        float visibleNameWidth = Math.max(scaled(58f), Math.min(fullNameWidth, scaled(88f)));
        float rightAreaW = Math.max(
                Math.max(visibleNameWidth, Fonts.PS_MEDIUM.getWidth(hpText, fHp)),
                scaled(64f)
        );

        float cardW = p + headSize + p + rightAreaW + p;
        float cardH = headSize + p * 2f;
        float itemRowY = y;
        float cardY = y + itemSlotSize + itemRowGap;
        float totalHeight = itemSlotSize + itemRowGap + cardH;
        float appearScale = 0.9f + av * 0.1f;
        float appearOffset = scaled(5f) * (1f - av);

        ItemStack mainHand = target.getMainHandStack();
        ItemStack offHand = target.getOffHandStack();
        ItemStack helmet = target.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chest = target.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack legs = target.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack feet = target.getEquippedStack(EquipmentSlot.FEET);

        getDraggable().setWidth(cardW);
        getDraggable().setHeight(totalHeight);

        ms.push();
        float pivotX = x + cardW / 2f;
        float pivotY = y + totalHeight / 2f;
        ms.translate(pivotX, pivotY, 0f);
        ms.scale(appearScale, appearScale, 1f);
        ms.translate(-pivotX, -pivotY + appearOffset, 0f);

        float leftItemX = x;
        drawItemSlot(ctx, ms, leftItemX, itemRowY, itemSlotSize, itemSlotRound, alpha, mainHand);
        leftItemX += itemSlotSize + itemSlotGap;
        drawItemSlot(ctx, ms, leftItemX, itemRowY, itemSlotSize, itemSlotRound, alpha, offHand);

        float rightItemX = x + cardW - (4 * itemSlotSize + 3 * itemSlotGap);
        drawItemSlot(ctx, ms, rightItemX, itemRowY, itemSlotSize, itemSlotRound, alpha, helmet);
        rightItemX += itemSlotSize + itemSlotGap;
        drawItemSlot(ctx, ms, rightItemX, itemRowY, itemSlotSize, itemSlotRound, alpha, chest);
        rightItemX += itemSlotSize + itemSlotGap;
        drawItemSlot(ctx, ms, rightItemX, itemRowY, itemSlotSize, itemSlotRound, alpha, legs);
        rightItemX += itemSlotSize + itemSlotGap;
        drawItemSlot(ctx, ms, rightItemX, itemRowY, itemSlotSize, itemSlotRound, alpha, feet);

        drawAppearBurst(ms, x, cardY, cardW, cardH, av, alpha);
        drawTargetCard(ms, x, cardY, cardW, cardH, hr, alpha);

        float headX = x + p;
        float headY = cardY + (cardH - headSize) / 2f;
        if (target instanceof PlayerEntity player) {
            RenderUtil.TEXTURE_RECT.drawHead(ms, player,
                    headX, headY,
                    headSize, headSize,
                    scaled(1f), headRound,
                    new Color(255, 255, 255, alpha));
        }

        float contentX = headX + headSize + p;
        float contentY = cardY + p - scaled(0.2f);
        float nameY = contentY;
        boolean shouldScrollName = fullNameWidth > visibleNameWidth + scaled(1f);
        ScissorUtil.start(ms, contentX, nameY - scaled(1f), visibleNameWidth, fName + scaled(3f));
        if (shouldScrollName) {
            float gapWidth = scaled(10f);
            float cycleWidth = fullNameWidth + gapWidth;
            float scroll = (float) ((System.currentTimeMillis() * 0.028f) % cycleWidth);
            Fonts.PS_BOLD.drawText(ms, name, contentX - scroll, nameY, fName, widgetTextColor(alpha));
            Fonts.PS_BOLD.drawText(ms, name, contentX - scroll + cycleWidth, nameY, fName, widgetTextColor(alpha));
        } else {
            Fonts.PS_BOLD.drawText(ms, name, contentX, nameY, fName, widgetTextColor(alpha));
        }
        ScissorUtil.stop(ms);

        float hpY = contentY + fName + scaled(2.4f);
        Fonts.PS_MEDIUM.drawText(ms, hpText, contentX, hpY, fHp, widgetTextColor(alpha));

        float barY = hpY + fHp + scaled(3f);
        float barW = Math.max(scaled(54f), rightAreaW - scaled(2.8f));
        drawHpBar(ms, contentX, barY, barW, barH, healthAnim, alpha);
        ms.pop();
    }

    private void drawItemSlot(DrawContext ctx, MatrixStack ms, float x, float y, float size,
                              float round, int alpha, ItemStack stack) {
        drawClickGuiBlur(ms, x, y, size, size, round, alpha, true);

        if (stack == null || stack.isEmpty()) {
            return;
        }

        float itemScale = size / 16f;
        ms.push();
        ms.translate(x, y, 0f);
        ms.scale(itemScale, itemScale, 1f);
        ctx.drawItem(stack, 0, 0);
        ms.pop();
    }

    private void drawHpBar(MatrixStack ms, float x, float y, float w, float h, float percent, int alpha) {
        RenderUtil.RECT.draw(ms, x, y, w, h, h / 2f, UIColors.cardSecondary(alpha));
        float fillW = w * MathHelper.clamp(percent, 0f, 1f);
        if (fillW > 0f) {
            // Soft gradient: left color based on health (green→yellow→red), right slightly shifted
            Color leftColor = healthGradientColor(percent, alpha, 0f);
            Color rightColor = healthGradientColor(percent, alpha, 0.15f);
            RenderUtil.GRADIENT_RECT.draw(ms, x, y, fillW, h, h / 2f,
                    leftColor, leftColor,
                    rightColor, rightColor);
        }
    }

    private Color healthGradientColor(float percent, int alpha, float shift) {
        float p = MathHelper.clamp(percent - shift, 0f, 1f);
        int r, g, b;
        if (p > 0.5f) {
            // Green to Yellow transition
            float t = (p - 0.5f) * 2f;
            r = (int) MathHelper.lerp(t, 255, 72);
            g = (int) MathHelper.lerp(t, 235, 220);
            b = (int) MathHelper.lerp(t, 59, 86);
        } else {
            // Yellow to Red transition
            float t = p * 2f;
            r = 255;
            g = (int) MathHelper.lerp(t, 62, 235);
            b = (int) MathHelper.lerp(t, 62, 59);
        }
        return new Color(
                MathHelper.clamp(r, 0, 255),
                MathHelper.clamp(g, 0, 255),
                MathHelper.clamp(b, 0, 255),
                MathHelper.clamp(alpha, 0, 255));
    }

    private void drawTargetCard(MatrixStack ms, float x, float y, float w, float h, float round, int alpha) {
        drawClickGuiBlur(ms, x, y, w, h, round, animatedAlpha(alpha), false);
    }

    private void drawClickGuiBlur(MatrixStack ms, float x, float y, float w, float h, float round, int alpha, boolean compact) {
        int blurAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 1.06f) : (int) (alpha * 1.14f)));
        int backgroundAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.98f) : (int) (alpha * 1.06f)));
        int surfaceAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.28f) : (int) (alpha * 0.44f)));
        int overlayAlpha = Math.max(0, Math.min(255, compact ? (int) (alpha * 0.10f) : (int) (alpha * 0.16f)));
        int strokeAlpha = Math.max(0, Math.min(255, (int) (alpha * 0.20f)));

        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, round, UIColors.blur(blurAlpha), 0.08f);
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, round, UIColors.backgroundBlur(backgroundAlpha), 0.06f);
        RenderUtil.RECT.draw(ms, x, y, w, h, round, compact ? UIColors.panel(surfaceAlpha) : UIColors.card(surfaceAlpha));
        RenderUtil.RECT.draw(ms, x, y, w, h, round, UIColors.overlay(overlayAlpha));
        RenderUtil.RECT.draw(ms, x, y, w, h, round, UIColors.stroke(strokeAlpha));
    }

    private void drawAppearBurst(MatrixStack ms, float x, float y, float w, float h, float appear, int alpha) {
        float burst = Math.max(0f, 1f - appear * 1.35f);
        if (burst <= 0.01f) {
            return;
        }

        float cx = x + w / 2f;
        float cy = y + h / 2f;

        for (int i = 0; i < 10; i++) {
            float angle = (float) Math.toRadians(i * 36f + 12f);
            float distance = scaled(2f) + scaled(18f) * burst;
            float px = cx + MathHelper.cos(angle) * distance;
            float py = cy + MathHelper.sin(angle) * distance * 0.65f;
            float size = scaled(1.6f + (i % 3) * 0.7f) * (0.8f + burst * 0.7f);
            Color color = i % 2 == 0 ? UIColors.primary(animatedAlpha((int) (alpha * burst))) : UIColors.secondary(animatedAlpha((int) (alpha * burst)));
            RenderUtil.RECT.draw(ms, px - size / 2f, py - size / 2f, size, size, size / 2f, color);
        }
    }

    @Override
    public void render(MatrixStack matrixStack) {
    }

    private LivingEntity resolveTarget() {
        if (mc.currentScreen instanceof ChatScreen) return mc.player;

        try {
            AuraModule aura = AuraModule.getInstance();
            if (aura != null && aura.isEnabled() && aura.target != null) return aura.target;
        } catch (Exception ignored) {}

        try {
            TriggerBotModule trigger = TriggerBotModule.getInstance();
            if (trigger != null && trigger.isEnabled()) {
                // TriggerBot sets AuraModule.target for esp/widget compatibility
                AuraModule aura = AuraModule.getInstance();
                if (aura != null && aura.target != null) return aura.target;
            }
        } catch (Exception ignored) {}

        try {
            AimAssistModule aim = AimAssistModule.getInstance();
            if (aim != null && aim.isEnabled() && aim.getTarget() != null) return aim.getTarget();
        } catch (Exception ignored) {}

        if (mc.targetedEntity instanceof LivingEntity le) return le;
        return null;
    }
}
