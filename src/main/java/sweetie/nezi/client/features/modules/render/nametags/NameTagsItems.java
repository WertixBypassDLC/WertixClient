package sweetie.nezi.client.features.modules.render.nametags;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import sweetie.nezi.api.utils.render.RenderUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NameTagsItems {
    private final NameTagsModule module;

    public NameTagsItems(NameTagsModule module) {
        this.module = module;
    }

    public List<ItemStack> collectDisplayItems(PlayerEntity player) {
        List<ItemStack> result = new ArrayList<>(6);

        if (!module.options.isEnabled("Only hands")) {
            addIfPresent(result, player.getEquippedStack(EquipmentSlot.HEAD));
            addIfPresent(result, player.getEquippedStack(EquipmentSlot.CHEST));
            addIfPresent(result, player.getEquippedStack(EquipmentSlot.LEGS));
            addIfPresent(result, player.getEquippedStack(EquipmentSlot.FEET));
        }

        addIfPresent(result, player.getMainHandStack());
        addIfPresent(result, player.getOffHandStack());
        return result;
    }

    public List<String> collectSpecialItemNames(PlayerEntity player) {
        List<String> specialItems = new ArrayList<>();

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            String itemName = normalizeName(stack.getName().getString());
            boolean isTalismanCarrier = stack.getItem() == Items.TOTEM_OF_UNDYING
                    || stack.getItem() == Items.PLAYER_HEAD
                    || stack.getItem() == Items.POPPED_CHORUS_FRUIT;
            boolean isTalisman = isTalismanCarrier && (
                    itemName.contains("сфера")
                            || itemName.contains("руна")
                            || itemName.contains("шар")
                            || itemName.contains("талисман")
            );
            boolean isAngelElytra = stack.getItem() == Items.ELYTRA && itemName.contains("крылья ангела");
            boolean isKrush = itemName.contains("круш");

            if (isAngelElytra || isKrush || isTalisman) {
                specialItems.add(stack.getName().getString());
            }
        }

        return specialItems;
    }

    public float renderCompactItems(DrawContext context, List<ItemStack> items, float centerX, float bottomY, float scale) {
        if (items.isEmpty()) {
            return 0f;
        }

        MatrixStack matrices = context.getMatrices();
        float gap = 1.5f * scale;
        float itemSize = 9.5f * scale;
        float slotRound = Math.max(1f, 1.9f * scale);
        float rowWidth = items.size() * itemSize + Math.max(0, items.size() - 1) * gap;
        float startX = centerX - rowWidth / 2f;
        float startY = bottomY - itemSize;

        for (int i = 0; i < items.size(); i++) {
            float drawX = startX + i * (itemSize + gap);
            ItemStack stack = items.get(i);

            RenderUtil.RECT.draw(matrices, drawX, startY, itemSize, itemSize, slotRound, new Color(8, 10, 14, 202));
            RenderUtil.RECT.draw(matrices, drawX + 0.6f, startY + 0.6f, itemSize - 1.2f, itemSize - 1.2f, Math.max(1f, slotRound - 0.6f), new Color(255, 255, 255, 10));
            RenderUtil.RECT.draw(matrices, drawX, startY, itemSize, itemSize, slotRound, new Color(30, 34, 42, 146));

            float itemScale = itemSize / 16f;
            matrices.push();
            matrices.translate(drawX, startY, 0f);
            matrices.scale(itemScale, itemScale, 1f);

            DiffuseLighting.disableGuiDepthLighting();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            context.drawItem(stack, 0, 0);

            matrices.pop();
        }

        return itemSize;
    }

    private void addIfPresent(List<ItemStack> result, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == Items.AIR) {
            return;
        }

        result.add(stack);
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
