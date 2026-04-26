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
import sweetie.nezi.api.utils.color.ColorUtil;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.other.ReplaceUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.ScissorUtil;
import sweetie.nezi.api.utils.render.fonts.Font;
import sweetie.nezi.client.features.modules.combat.AimAssistModule;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.features.modules.combat.TriggerBotModule;
import sweetie.nezi.client.ui.widget.Widget;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TargetInfoWidget extends Widget {
    private static final Color BG = new Color(15, 15, 20, 220);
    private static final Color OUTLINE = new Color(70, 70, 85, 150);

    private final AnimationUtil anim = new AnimationUtil();
    private LivingEntity target;
    private boolean allow;
    private long lastSeenMs;
    private float healthAnim = 0f;

    public TargetInfoWidget() {
        super(30f, 30f);
        setWidgetScale(1.0f);
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
    public void render(Render2DEvent.Render2DEventData event) {
        DrawContext context = event.context();
        MatrixStack matrixStack = context.getMatrices();

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
        float appear = (float) anim.getValue();
        if (appear < 0.005f) {
            if (hide) {
                target = null;
            }
            return;
        }

        if (target == null || mc.player == null) {
            return;
        }

        Layout layout = computeLayout();
        float x = layout.x;
        float y = layout.y;
        float centerX = x + layout.totalW * 0.5f;
        float centerY = y + layout.totalH * 0.5f;

        float targetHealth = Math.max(0f, target.getHealth() + target.getAbsorptionAmount());
        float maxHealth = Math.max(1f, target.getMaxHealth() + target.getAbsorptionAmount());
        float targetHealthProgress = MathHelper.clamp(targetHealth / maxHealth, 0f, 1f);
        healthAnim = MathUtil.interpolate(healthAnim, targetHealthProgress, 0.16f);

        String name = ReplaceUtil.protectedString(target.getName().getString());
        String hpText = (int) Math.ceil(targetHealth) + " HP";
        String distanceText = String.format("%.1fm", mc.player.distanceTo(target));
        List<ItemStack> items = collectDisplayItems(target);

        matrixStack.push();
        matrixStack.translate(centerX, centerY, 0f);
        matrixStack.scale(0.92f + appear * 0.08f, 0.92f + appear * 0.08f, 1f);
        matrixStack.translate(-centerX, -centerY + scaled(4f) * (1f - appear), 0f);

        float outlineW = scaled(0.5f);
        RenderUtil.RECT.draw(matrixStack, x - outlineW, y - outlineW,
                layout.totalW + outlineW * 2f, layout.totalH + outlineW * 2f,
                layout.borderR + outlineW, OUTLINE);
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, layout.totalW, layout.totalH, layout.borderR, BG);

        drawAvatar(context, matrixStack, layout);
        drawCenteredClippedText(matrixStack, getSemiBoldFont(), name,
                x + layout.totalW / 2f, layout.nameY, layout.totalW - layout.pad * 2f,
                scaled(7f), UIColors.textColor());
        drawStatLine(matrixStack, hpText, distanceText, layout);
        drawHealthBar(matrixStack, layout, healthAnim);
        drawItems(context, matrixStack, layout, items);

        matrixStack.pop();

        getDraggable().setWidth(layout.totalW);
        getDraggable().setHeight(layout.totalH);
    }

    @Override
    public void render(MatrixStack matrixStack) {
    }

    private void drawAvatar(DrawContext context, MatrixStack matrixStack, Layout layout) {
        drawPanel(matrixStack, layout.avatarX, layout.avatarY, layout.avatarSize, layout.avatarSize, scaled(4f));

        if (target instanceof PlayerEntity player) {
            RenderUtil.TEXTURE_RECT.drawHead(matrixStack, player,
                    layout.avatarX, layout.avatarY,
                    layout.avatarSize, layout.avatarSize,
                    scaled(1f), scaled(4f), Color.WHITE);
            return;
        }

        String letter = target.getName().getString().isEmpty() ? "?" : String.valueOf(Character.toUpperCase(target.getName().getString().charAt(0)));
        Font font = getSemiBoldFont();
        float size = scaled(18f);
        float width = font.getWidth(letter, size);
        float height = font.getHeight(size);
        font.drawText(matrixStack, letter,
                layout.avatarX + layout.avatarSize / 2f - width / 2f,
                layout.avatarY + layout.avatarSize / 2f - height / 2f,
                size, UIColors.primary());
    }

    private void drawStatLine(MatrixStack matrixStack, String left, String right, Layout layout) {
        Font font = getMediumFont();
        float size = scaled(5.9f);
        float y = layout.statY;
        font.drawText(matrixStack, left, layout.x + layout.pad, y, size, UIColors.textColor());
        float rightW = font.getWidth(right, size);
        font.drawText(matrixStack, right, layout.x + layout.totalW - layout.pad - rightW, y, size, UIColors.inactiveTextColor());
    }

    private void drawHealthBar(MatrixStack matrixStack, Layout layout, float progress) {
        RenderUtil.BLUR_RECT.draw(matrixStack, layout.barX, layout.barY, layout.barW, layout.barH, scaled(2f), new Color(22, 22, 28, 210));
        RenderUtil.RECT.draw(matrixStack, layout.barX, layout.barY, layout.barW, layout.barH, scaled(2f), new Color(40, 40, 50, 255));

        float fill = layout.barW * MathHelper.clamp(progress, 0f, 1f);
        if (fill <= 0.001f) {
            return;
        }

        Color leftColor = healthGradient(progress, 0f);
        Color rightColor = healthGradient(progress, 0.14f);
        RenderUtil.GRADIENT_RECT.draw(matrixStack, layout.barX, layout.barY, fill, layout.barH, scaled(2f),
                leftColor, leftColor, rightColor, rightColor);
    }

    private void drawItems(DrawContext context, MatrixStack matrixStack, Layout layout, List<ItemStack> items) {
        float drawX = layout.itemsX;
        for (ItemStack stack : items) {
            drawPanel(matrixStack, drawX, layout.itemsY, layout.itemSize, layout.itemSize, scaled(3f));

            if (stack != null && !stack.isEmpty()) {
                matrixStack.push();
                matrixStack.translate(drawX, layout.itemsY, 0f);
                float scale = layout.itemSize / 16f;
                matrixStack.scale(scale, scale, 1f);
                context.drawItem(stack, 0, 0);
                matrixStack.pop();
            }

            drawX += layout.itemSize + layout.itemGap;
        }
    }

    private void drawPanel(MatrixStack matrixStack, float x, float y, float width, float height, float round) {
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, round, new Color(22, 22, 28, 210));
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, round, new Color(255, 255, 255, 12));
    }

    private void drawCenteredClippedText(MatrixStack matrixStack, Font font, String text, float centerX, float y, float maxWidth, float size, Color color) {
        float width = font.getWidth(text, size);
        float startX = centerX - Math.min(width, maxWidth) / 2f;
        if (width <= maxWidth) {
            font.drawText(matrixStack, text, centerX - width / 2f, y, size, color);
            return;
        }

        ScissorUtil.start(matrixStack, startX, y - scaled(1f), maxWidth, size + scaled(2f));
        font.drawText(matrixStack, text, centerX - width / 2f, y, size, color);
        ScissorUtil.stop(matrixStack);
    }

    private Color healthGradient(float progress, float shift) {
        float p = MathHelper.clamp(progress - shift, 0f, 1f);
        if (p > 0.5f) {
            float t = (p - 0.5f) * 2f;
            Color a = new Color(255, 235, 59, 255);
            Color b = new Color(72, 220, 86, 255);
            return ColorUtil.interpolate(a, b, t);
        }

        float t = p * 2f;
        Color a = new Color(255, 62, 62, 255);
        Color b = new Color(255, 235, 59, 255);
        return ColorUtil.interpolate(a, b, t);
    }

    private List<ItemStack> collectDisplayItems(LivingEntity entity) {
        List<ItemStack> items = new ArrayList<>(6);
        items.add(entity.getMainHandStack());
        items.add(entity.getOffHandStack());
        items.add(entity.getEquippedStack(EquipmentSlot.HEAD));
        items.add(entity.getEquippedStack(EquipmentSlot.CHEST));
        items.add(entity.getEquippedStack(EquipmentSlot.LEGS));
        items.add(entity.getEquippedStack(EquipmentSlot.FEET));
        return items;
    }

    private Layout computeLayout() {
        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float pad = scaled(5f);
        float totalW = scaled(96f);
        float borderR = scaled(4f);
        float avatarSize = scaled(52f);
        float avatarX = x + (totalW - avatarSize) / 2f;
        float avatarY = y + pad;
        float nameY = avatarY + avatarSize + scaled(4f);
        float statY = nameY + scaled(8.5f);
        float barX = x + pad;
        float barY = statY + scaled(8f);
        float barW = totalW - pad * 2f;
        float barH = scaled(4f);
        float itemSize = scaled(10.5f);
        float itemGap = scaled(1.5f);
        float itemsRowW = itemSize * 6f + itemGap * 5f;
        float itemsX = x + (totalW - itemsRowW) / 2f;
        float itemsY = barY + barH + scaled(6f);
        float totalH = itemsY + itemSize + pad - y;

        return new Layout(x, y, totalW, totalH, pad, borderR, avatarX, avatarY, avatarSize,
                nameY, statY, barX, barY, barW, barH, itemsX, itemsY, itemSize, itemGap);
    }

    private LivingEntity resolveTarget() {
        if (mc.currentScreen instanceof ChatScreen) {
            return mc.player;
        }

        try {
            AuraModule aura = AuraModule.getInstance();
            if (aura != null && aura.isEnabled() && aura.target != null) {
                return aura.target;
            }
        } catch (Exception ignored) {
        }

        try {
            TriggerBotModule trigger = TriggerBotModule.getInstance();
            if (trigger != null && trigger.isEnabled()) {
                AuraModule aura = AuraModule.getInstance();
                if (aura != null && aura.target != null) {
                    return aura.target;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            AimAssistModule aim = AimAssistModule.getInstance();
            if (aim != null && aim.isEnabled() && aim.getTarget() != null) {
                return aim.getTarget();
            }
        } catch (Exception ignored) {
        }

        if (mc.targetedEntity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }

    private record Layout(float x, float y, float totalW, float totalH, float pad, float borderR,
                          float avatarX, float avatarY, float avatarSize,
                          float nameY, float statY,
                          float barX, float barY, float barW, float barH,
                          float itemsX, float itemsY, float itemSize, float itemGap) {
    }
}
