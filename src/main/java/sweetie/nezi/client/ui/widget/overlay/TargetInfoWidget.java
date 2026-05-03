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
        float height = scaled(32.0F);

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

        // Background with Glow
        int alpha = (int)(anim * 255);
        float round = scaled(6.0F);

        // Subtle Theme Glow
        Color glowColor = UIColors.themeFlow(target.hashCode(), (int)(anim * 35));
        RenderUtil.RECT.draw(matrixStack, posX - scaled(1.5f), posY - scaled(1.5f), width + scaled(3.0f), height + scaled(3.0f), round + scaled(1.5f), glowColor);

        // Standard HUD Card (Blur & Surface)
        drawHudCard(matrixStack, posX, posY, width, height, round, alpha);
        RenderUtil.STROKE(matrixStack, posX, posY, width, height, round, (int)(anim * 40));

        // Player Head
        float headSize = scaled(24.0F);
        float headX = posX + scaled(4.0F);
        float headY = posY + (height - headSize) / 2f;
        if (target instanceof PlayerEntity player) {
            RenderUtil.TEXTURE_RECT.drawHead(matrixStack, player, headX, headY, headSize, headSize,
                    0f, scaled(3.0F), ColorUtil.setAlpha(Color.WHITE, (int)(anim * 255.0F)));
        }

        float contentX = headX + headSize + scaled(6.0F);
        float contentW = width - headSize - scaled(14.0F);

        // Player Name
        String playerName = target.getName().getString();
        if (Fonts.PS_BOLD.getWidth(playerName, scaled(7.0F)) > contentW) {
            playerName = playerName.substring(0, Math.min(playerName.length(), 9)) + "..";
        }
        Fonts.PS_BOLD.drawText(matrixStack, playerName, contentX, posY + scaled(4.5F),
                scaled(7.0F), UIColors.textColor((int)(anim * 255.0F)));

        // Distance & Info
        float dist = mc.player.distanceTo(target);
        String infoText = String.format("%.1fm", dist) + " | " + (target instanceof PlayerEntity ? "Player" : "Mob");
        Fonts.PS_MEDIUM.drawText(matrixStack, infoText, contentX, posY + scaled(12.5F),
                scaled(5.5F), UIColors.inactiveTextColor((int)(anim * 255.0F)));

        // HP Text
        String hpText = String.format("%.1f", hp).replace(",", ".") + " HP";
        if (target.getAbsorptionAmount() > 0) {
            hpText += " §6+" + String.format("%.1f", target.getAbsorptionAmount()).replace(",", ".");
        }
        Fonts.PS_BOLD.drawText(matrixStack, hpText, contentX, posY + scaled(18.5F),
                scaled(6.0F), UIColors.textColor((int)(anim * 255.0F)));

        // Health Bar
        float barX = contentX;
        float barY = posY + scaled(25.5F);
        float barWidth = contentW;
        float barHeight = scaled(4.5F);
        float barRadius = scaled(1.5F);

        // Bar Background
        RenderUtil.RECT.draw(matrixStack, barX, barY, barWidth, barHeight, barRadius,
                new Color(30, 30, 35, (int)(anim * 180)));

        // Outdated health
        float outdatedWidth = MathHelper.clamp(barWidth * (float)outdatedHealthAnimation.getValue(), 0.0F, barWidth);
        if (outdatedWidth > 1f) {
            RenderUtil.RECT.draw(matrixStack, barX, barY, outdatedWidth, barHeight, barRadius,
                    ColorUtil.setAlpha(UIColors.primary(), (int)(anim * 80)));
        }

        // Current health
        float healthWidth = MathHelper.clamp(barWidth * (float)healthAnimation.getValue(), 0.0F, barWidth);
        if (healthWidth > 1f) {
            RenderUtil.GRADIENT_RECT.draw(matrixStack, barX, barY, healthWidth, barHeight, barRadius,
                    UIColors.secondary((int)(anim * 255)),
                    UIColors.primary((int)(anim * 255)),
                    UIColors.secondary((int)(anim * 255)),
                    UIColors.primary((int)(anim * 255)));
        }

        // Absorption
        float absorptionWidth = MathHelper.clamp(barWidth * (float)gappleAnimation.getValue(), 0.0F, barWidth);
        if (absorptionWidth > 1f) {
            RenderUtil.RECT.draw(matrixStack, barX, barY, absorptionWidth, barHeight, barRadius,
                    new Color(255, 200, 40, (int)(anim * 255)));
        }

        // Armor Rendering
        if (target instanceof PlayerEntity player) {
            drawArmor(ctx, matrixStack, player, posX + width + scaled(3f), posY + scaled(1f), anim);
        }

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);
    }


    private void drawArmor(DrawContext ctx, MatrixStack ms, PlayerEntity player, float x, float y, float anim) {
        float itemSize = scaled(12.0F);
        float gap = scaled(2.0F);
        
        List<ItemStack> armor = player.getInventory().armor;
        ItemStack[] items = new ItemStack[]{
                player.getMainHandStack(), 
                player.getOffHandStack(), 
                armor.get(3), 
                armor.get(2), 
                armor.get(1), 
                armor.get(0)
        };

        float curY = y;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ms.push();
                ms.translate(x, curY, 0.0F);
                float scale = (itemSize / 16.0F) * anim;
                ms.scale(scale, scale, 1.0F);
                ctx.drawItem(stack, 0, 0);
                ms.pop();
                curY += itemSize + gap;
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
        AuraModule aura = AuraModule.getInstance();
        if (aura.isEnabled() && aura.target != null) return aura.target;
        return null;
    }

    @Override public void render(MatrixStack matrixStack) {}
}
