package sweetie.nezi.client.ui.widget.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
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
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.combat.AuraModule;
import sweetie.nezi.client.features.modules.combat.LegitAuraModule;
import sweetie.nezi.client.features.modules.combat.TriggerBotModule;
import sweetie.nezi.client.ui.widget.Widget;

import java.awt.*;
import java.util.List;

public final class TargetInfoWidget extends Widget {

    private final AnimationUtil healthAnimation = new AnimationUtil();
    private final AnimationUtil outdatedHealthAnimation = new AnimationUtil();
    private final AnimationUtil gappleAnimation = new AnimationUtil();
    private final AnimationUtil toggleAnimation = new AnimationUtil();
    private final AnimationUtil glowAnimation = new AnimationUtil();

    private LivingEntity target;

    public TargetInfoWidget() {
        super(30f, 30f);
    }

    @Override
    public String getName() {
        return "Target info";
    }

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        MatrixStack matrixStack = event.matrixStack();
        DrawContext ctx = event.context();

        updateTarget();

        float anim = (float) toggleAnimation.getValue();
        if (target == null && anim <= 0.01f) return;
        if (target == null) return;

        float posX = getDraggable().getX();
        float posY = getDraggable().getY();
        float width = scaled(95.0F);
        float height = scaled(30.5F);

        float hp = target.getHealth();
        float maxHp = target.getMaxHealth();
        float hpPct = MathHelper.clamp(hp / maxHp, 0.0F, 1.0F);

        healthAnimation.update();
        healthAnimation.run(hpPct, 250, Easing.CUBIC_OUT);

        outdatedHealthAnimation.update();
        if (outdatedHealthAnimation.getValue() < healthAnimation.getValue()) {
            outdatedHealthAnimation.setValue(healthAnimation.getValue());
        } else {
            outdatedHealthAnimation.run(hpPct, 650, Easing.CUBIC_OUT);
        }

        gappleAnimation.update();
        gappleAnimation.run(target.getAbsorptionAmount() / maxHp, 250, Easing.CUBIC_OUT);

        glowAnimation.update();
        glowAnimation.run(1.0, 2000, Easing.LINEAR);
        if (glowAnimation.isFinished()) glowAnimation.setValue(0.0);

        int alpha = (int)(anim * 255);
        float round = scaled(6.0F);

        // Rectangle (background)
        float headSize = height; // The square
        
        String playerName = target.getName().getString();
        float nameScale = scaled(7.0F);
        float nameWidth = Fonts.PS_BOLD.getWidth(playerName, nameScale);
        float minContentW = scaled(56.0F);
        float contentW = Math.max(minContentW, nameWidth);
        
        float padding = scaled(4f); // gap between head square and text
        // Width: head + gap + content + right padding
        width = headSize + padding + contentW + scaled(9f);
        
        float headX = posX;
        float headY = posY;
        float rectX = posX;
        float rectW = width;
        
        // Subtle Theme Glow around Rect
        Color glowColor = UIColors.themeFlow(target.hashCode(), (int)(anim * 35));
        RenderUtil.RECT.draw(matrixStack, rectX - scaled(1.5f), posY - scaled(1.5f), rectW + scaled(3.0f), height + scaled(3.0f), round + scaled(1.5f), glowColor);

        // Base rect (bottom layer)
        drawHudCard(matrixStack, rectX, posY, rectW, height, round, alpha);
        // Head square (top layer — stacking)
        drawHudSquare(matrixStack, headX, headY, headSize, headSize, round, alpha);

        // Player Head texture
        if (target instanceof PlayerEntity player) {
            float innerHeadSize = headSize - scaled(8f);
            RenderUtil.TEXTURE_RECT.drawHead(matrixStack, player, headX + scaled(4f), headY + scaled(4f), innerHeadSize, innerHeadSize,
                    0f, scaled(3.0F), ColorUtil.setAlpha(Color.WHITE, (int)(anim * 255.0F)));
        }

        float contentX = headX + headSize + padding;

        // === Vertical layout inside content area ===
        // Total inner height = height. Divide into 3 equal chunks:
        //   chunk 0: name    (top)
        //   chunk 1: HP text (middle)
        //   chunk 2: HP bar  (bottom)
        float innerPad   = scaled(3.05f);
        float barHeight  = scaled(3.25F);
        float barRadius  = scaled(1.5F);
        float nameY  = posY + innerPad;
        float hpTextSize = scaled(5.8F);
        float hpTextY = nameY + nameScale + scaled(0.1f);
        float barY   = posY + height - innerPad - barHeight;
        float barX   = contentX;
        float barWidth = contentW;

        Fonts.PS_BOLD.drawText(matrixStack, playerName, contentX, nameY,
                nameScale, UIColors.textColor((int)(anim * 255.0F)));

        // HP Text
        String hpText = String.format("%.1f", hp).replace(",", ".") + " HP";
        if (target.getAbsorptionAmount() > 0) {
            hpText += " §6+" + String.format("%.1f", target.getAbsorptionAmount()).replace(",", ".");
        }
        Fonts.PS_BOLD.drawText(matrixStack, hpText, contentX, hpTextY,
                hpTextSize, UIColors.textColor((int)(anim * 255.0F)));

        // Health Bar
        float barHeight2  = scaled(3.5F);
        float barY2       = Math.max(hpTextY + hpTextSize + scaled(0.55f), posY + height - innerPad - barHeight2);

        // Bar Background
        RenderUtil.RECT.draw(matrixStack, barX, barY2, barWidth, barHeight2, barRadius,
                new Color(30, 30, 35, (int)(anim * 180)));

        // Outdated health
        float outdatedWidth = MathHelper.clamp(barWidth * (float)outdatedHealthAnimation.getValue(), 0.0F, barWidth);
        if (outdatedWidth > 1f) {
            RenderUtil.RECT.draw(matrixStack, barX, barY2, outdatedWidth, barHeight2, barRadius,
                    ColorUtil.setAlpha(UIColors.primary(), (int)(anim * 80)));
        }

        // Current health
        float healthWidth = MathHelper.clamp(barWidth * (float)healthAnimation.getValue(), 0.0F, barWidth);
        if (healthWidth > 1f) {
            RenderUtil.GRADIENT_RECT.draw(matrixStack, barX, barY2, healthWidth, barHeight2, barRadius,
                    UIColors.secondary((int)(anim * 255)),
                    UIColors.primary((int)(anim * 255)),
                    UIColors.secondary((int)(anim * 255)),
                    UIColors.primary((int)(anim * 255)));
        }

        // Absorption
        float absorptionWidth = MathHelper.clamp(barWidth * (float)gappleAnimation.getValue(), 0.0F, barWidth);
        if (absorptionWidth > 1f) {
            RenderUtil.RECT.draw(matrixStack, barX, barY2, absorptionWidth, barHeight2, barRadius,
                    new Color(255, 200, 40, (int)(anim * 255)));
        }

        // Armor & Hands Rendering
        if (target instanceof PlayerEntity player) {
            drawArmor(ctx, matrixStack, player, posX, posY, width, anim);
        }

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);
    }


    private void drawArmor(DrawContext ctx, MatrixStack ms, PlayerEntity player, float x, float y, float width, float anim) {
        float itemSize = scaled(11.0F); // Smaller squares
        float gap = scaled(3.0F);
        float round = scaled(3.0F);
        int alpha = (int)(anim * 255);
        
        List<ItemStack> armor = player.getInventory().armor;
        ItemStack[] armors = new ItemStack[]{ armor.get(3), armor.get(2), armor.get(1), armor.get(0) };
        ItemStack[] hands = new ItemStack[]{ player.getMainHandStack(), player.getOffHandStack() };

        // Draw Hands (Top Left, 2 items)
        float hx = x;
        float hy = y - itemSize - gap;
        for (ItemStack stack : hands) {
            drawItemSquare(ctx, ms, stack, hx, hy, itemSize, round, alpha, player);
            hx += itemSize + gap;
        }

        // Draw Armor (Top Right, 4 items)
        float totalArmorWidth = 4 * itemSize + 3 * gap;
        float ax = x + width - totalArmorWidth;
        float ay = y - itemSize - gap;
        for (ItemStack stack : armors) {
            drawItemSquare(ctx, ms, stack, ax, ay, itemSize, round, alpha, player);
            ax += itemSize + gap;
        }
    }

    private void drawItemSquare(DrawContext ctx, MatrixStack ms, ItemStack stack, float x, float y, float size, float round, int alpha, LivingEntity entity) {
        drawHudSquare(ms, x, y, size, size, round, alpha);
        if (stack == null || stack.isEmpty()) return;

        float scale = (size - scaled(4f)) / 16.0F; // pad 2px on each side
        ms.push();
        ms.translate(x + scaled(2f), y + scaled(2f), 0.0F);
        ms.scale(scale, scale, 1.0F);
        ctx.drawItem(stack, 0, 0);
        ms.pop();

        // Item use logic
        if (entity != null && entity.isUsingItem() && entity.getActiveItem() == stack) {
            int maxUse = stack.getMaxUseTime(entity);
            int left = entity.getItemUseTimeLeft();
            if (maxUse > 0) {
                float progress = 1f - ((float)left / (float)maxUse);
                float fillHeight = size * progress;
                RenderUtil.RECT.draw(ms, x, y + size - fillHeight, size, fillHeight, round, UIColors.primary((int)(alpha * 0.5f)));
                
                String text = String.format("%.1fs", left / 20f);
                float textScale = scaled(4.5f);
                Fonts.PS_MEDIUM.drawText(ms, text, x + size/2f - Fonts.PS_MEDIUM.getWidth(text, textScale)/2f, y + size/2f - textScale/2f, textScale, UIColors.textColor(alpha));
            }
        }
    }

    private void updateTarget() {
        LivingEntity currentTarget = getAuraTarget();
        if (currentTarget == null && mc.currentScreen instanceof ChatScreen) {
            currentTarget = mc.player;
        }

        toggleAnimation.update();
        if (currentTarget != null) {
            target = currentTarget;
            toggleAnimation.run(1.0, 250, Easing.CUBIC_OUT);
        } else {
            toggleAnimation.run(0.0, 350, Easing.CUBIC_OUT);
            if (toggleAnimation.getValue() <= 0.01f) {
                target = null;
            }
        }
    }

    private LivingEntity getAuraTarget() {
        LegitAuraModule legitAura = LegitAuraModule.getInstance();
        if (legitAura.target != null && legitAura.isEnabled()) return legitAura.target;

        AuraModule aura = AuraModule.getInstance();
        if (aura.target != null && (aura.isEnabled() || TriggerBotModule.getInstance().isEnabled())) return aura.target;
        return null;
    }

    @Override public void render(MatrixStack matrixStack) {}
}
