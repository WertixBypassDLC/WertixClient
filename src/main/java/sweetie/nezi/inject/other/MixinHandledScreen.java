package sweetie.nezi.inject.other;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweetie.nezi.api.event.events.other.ScreenEvent;
import sweetie.nezi.api.system.backend.KeyStorage;
import sweetie.nezi.api.system.backend.SharedClass;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.client.features.modules.other.AuctionHelperModule;
import sweetie.nezi.client.features.modules.other.AutoBuyModule;
import sweetie.nezi.client.features.modules.other.MouseTweaksModule;
import sweetie.nezi.client.features.modules.render.ShulkerViewModule;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Shadow protected abstract boolean isPointOverSlot(Slot slotIn, double mouseX, double mouseY);
    @Shadow protected abstract void onMouseClick(Slot slotIn, int slotId, int mouseButton, SlotActionType type);
    @Shadow protected abstract Slot getSlotAt(double x, double y);
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    @Unique private final TimerUtil timerUtil = new TimerUtil();

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ScreenEvent.ScreenEventData event = new ScreenEvent.ScreenEventData(this);
        ScreenEvent.getInstance().call(event);

        for (ButtonWidget button : event.buttons()) {
            this.addDrawableChild(button);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void drawScreenHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        MouseTweaksModule mouseTweaks = MouseTweaksModule.getInstance();
        AuctionHelperModule ahModule = AuctionHelperModule.getInstance();
        if (!mouseTweaks.isEnabled() && !ahModule.isEnabled()) {
            return;
        }

        if (ahModule.isEnabled()) {
            ahModule.clearSlotDebug();
        }

        Slot hoveredSlot = getSlotAt(mouseX, mouseY);
        if (hoveredSlot == null || !hoveredSlot.isEnabled()) {
            return;
        }

        if (mouseTweaks.isEnabled() && shouldUse() && mouseIsHolding() && timerUtil.finished(mouseTweaks.delay.getValue().longValue())) {
            onMouseClick(hoveredSlot, hoveredSlot.id, 0, SlotActionType.QUICK_MOVE);
            timerUtil.reset();
        }

        if (ahModule.isEnabled() && isPointOverSlot(hoveredSlot, mouseX, mouseY)) {
            ahModule.onSlotDebug(hoveredSlot);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void drawScreenHookTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        // Панель настроек AuctionHelper — рендерим поверх контейнера
        AuctionHelperModule ahModule = AuctionHelperModule.getInstance();
        if (ahModule.isEnabled()) {
            ahModule.renderAuctionPanel(context, mouseX, mouseY, delta, this.x, this.y);
        }

        AutoBuyModule autoBuyModule = AutoBuyModule.getInstance();
        if (autoBuyModule.isEnabled()) {
            autoBuyModule.renderAuctionToggle(context, mouseX, mouseY, this.x, this.y, this.backgroundWidth);
        }

        ShulkerViewModule shulkerViewModule = ShulkerViewModule.getInstance();
        if (shulkerViewModule.isEnabled()) {
            shulkerViewModule.renderHoveredPreview(context, getSlotAt(mouseX, mouseY), mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        AutoBuyModule autoBuyModule = AutoBuyModule.getInstance();
        if (autoBuyModule.isEnabled() && autoBuyModule.handleAuctionToggleClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        AuctionHelperModule ahModule = AuctionHelperModule.getInstance();
        if (ahModule.isEnabled() && ahModule.isAuctionOpen() && ahModule.getSettingsPanel() != null) {
            ahModule.getSettingsPanel().mouseClicked(mouseX, mouseY, button);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        AuctionHelperModule ahModule = AuctionHelperModule.getInstance();
        if (ahModule.isEnabled() && ahModule.isAuctionOpen() && ahModule.getSettingsPanel() != null) {
            ahModule.getSettingsPanel().mouseReleased(mouseX, mouseY, button);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        AuctionHelperModule ahModule = AuctionHelperModule.getInstance();
        if (ahModule.isEnabled() && ahModule.isAuctionOpen() && ahModule.getSettingsPanel() != null) {
            ahModule.getSettingsPanel().mouseDragged(mouseX, mouseY);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"))
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        AuctionHelperModule ahModule = AuctionHelperModule.getInstance();
        if (ahModule.isEnabled() && ahModule.isAuctionOpen() && ahModule.getSettingsPanel() != null) {
            ahModule.getSettingsPanel().mouseScrolled(mouseX, mouseY, verticalAmount);
        }
    }

    @Unique
    private boolean shouldUse() {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 340) || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 344);
    }

    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V", at = @At("TAIL"))
    protected void drawSlotHook(DrawContext context, Slot slot, CallbackInfo ci) {
        AuctionHelperModule module = AuctionHelperModule.getInstance();
        if (module.isEnabled()) module.onRenderChest(context, slot);
    }

    @Redirect(
            method = "drawSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawItem(Lnet/minecraft/item/ItemStack;III)V"
            )
    )
    private void redirectAuctionItemRender(DrawContext context, ItemStack stack, int x, int y, int seed) {
        AuctionHelperModule module = AuctionHelperModule.getInstance();
        if (module.shouldUseFastAuctionItemRender()) {
            context.drawItemWithoutEntity(stack, x, y, seed);
            return;
        }

        context.drawItem(stack, x, y, seed);
    }

    @Unique
    private boolean mouseIsHolding() {
        return KeyStorage.isPressed(-100 + GLFW.GLFW_MOUSE_BUTTON_1);
    }
}
