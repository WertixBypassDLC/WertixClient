package sweetie.nezi.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.client.features.modules.combat.AuraModule;

import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "Swing Animation", category = Category.RENDER)
public class SwingAnimationModule extends Module {
    @Getter private static final SwingAnimationModule instance = new SwingAnimationModule();

    public final ModeSetting mode = new ModeSetting("Режим").value("Режим 1").values("Режим 1", "Режим 2", "Режим 3", "Режим 4", "Режим 5", "Режим 6");
    private final BooleanSetting auraOnly = new BooleanSetting("Только с аурой").value(false);
    public final SliderSetting strength = new SliderSetting("Сила").value(20f).range(20f,75f).step(0.1f).setVisible(() -> !mode.is("Режим 1") && !mode.is("Режим 5") && !mode.is("Режим 6"));

    public final BooleanSetting slow = new BooleanSetting("Замедление").value(false);
    public final SliderSetting speed = new SliderSetting("Скорость").value(12f).range(1f,50f).step(1f).setVisible(slow::getValue);
    private LivingEntity hitTarget;
    private long hitStartedAt;
    private long lastHitAt;
    private float hitOrbitSeed;
    private float hitImpactHeight;
    private float hitImpactSide;
    private float hitOrbitDirection = 1f;

    public SwingAnimationModule() {
        addSettings(mode, auraOnly, strength, slow, speed);
    }

    @Override
    public void onEvent() {

    }

    private void handleSwordAnim(MatrixStack matrices, float swingProgress, Arm arm) {
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        float anim = (float) Math.sin(swingProgress * (Math.PI / 2) * 2);
        float isLeft = arm == Arm.LEFT ? -1f : 1f;

        switch (mode.getValue()) {
            case "Режим 1" -> {
                applyEquipOffset(matrices, arm, 0);
                applySwingOffset(matrices, arm, swingProgress);
            }
            case "Режим 2" -> {
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * -60f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * (110f + strength.getValue() * g)));
            }
            case "Режим 3" -> {
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * (-30f * (1f - g) - 30f + (strength.getValue() - 20f) * g)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * 110f));
            }
            case "Режим 4" -> {
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isLeft * 90f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isLeft * -30f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f - strength.getValue() * anim + 10f));
            }
            case "Режим 5" -> {
                float rotation = swingProgress * -360f;
                applyEquipOffset(matrices, arm, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation));
            }
            case "Режим 6" -> {
                if (!applyTargetHitAnim(matrices, arm)) {
                    applyEquipOffset(matrices, arm, 0);
                    applySwingOffset(matrices, arm, swingProgress);
                }
            }
        }
    }

    public void notifyHit(LivingEntity target) {
        if (target == null) return;
        hitTarget = target;
        long now = System.currentTimeMillis();
        if (now - lastHitAt > 220L) {
            hitStartedAt = now;
            hitOrbitSeed = ThreadLocalRandom.current().nextFloat() * 360f;
            hitImpactHeight = ThreadLocalRandom.current().nextFloat(0.18f, 0.82f);
            hitImpactSide = ThreadLocalRandom.current().nextFloat(-0.72f, 0.72f);
            hitOrbitDirection = ThreadLocalRandom.current().nextBoolean() ? 1f : -1f;
        }
        lastHitAt = now;
    }

    private boolean applyTargetHitAnim(MatrixStack matrices, Arm arm) {
        if (!isEnabled() || !mode.is("Режим 6") || hitTarget == null || mc.player == null || !hitTarget.isAlive()) {
            return false;
        }

        long now = System.currentTimeMillis();
        long age = now - hitStartedAt;
        boolean sticky = now - lastHitAt < 120L;
        if (!sticky && age > 620L) {
            hitTarget = null;
            return false;
        }

        float progress = sticky ? 0.64f : MathHelper.clamp(age / 620f, 0f, 1f);
        float fly = progress < 0.60f
                ? smooth(progress / 0.60f)
                : 1f - smooth((progress - 0.60f) / 0.40f);
        if (sticky) {
            fly = Math.max(fly, 0.82f);
        }

        int handSide = arm == Arm.RIGHT ? 1 : -1;
        Vec3d toTarget = hitTarget.getEyePos().subtract(mc.player.getEyePos());
        float yawToTarget = (float) Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0f;
        float yawDiff = MathHelper.wrapDegrees(yawToTarget - mc.player.getYaw());
        float side = MathHelper.clamp(yawDiff / 70f, -1.2f, 1.2f);
        float distance = (float) MathHelper.clamp(toTarget.length(), 1.0, 5.0);
        float orbit = (float) Math.sin(progress * Math.PI) * hitOrbitDirection;
        float orbitAngle = hitOrbitSeed + progress * 300f * hitOrbitDirection;
        float orbitX = (float) Math.cos(Math.toRadians(orbitAngle)) * 0.28f * orbit;
        float orbitY = (float) Math.sin(Math.toRadians(orbitAngle * 1.35f)) * 0.18f * orbit;
        float impactY = (hitImpactHeight - 0.5f) * 0.42f;
        float impactSide = hitImpactSide * fly;

        applyEquipOffset(matrices, arm, 0);
        matrices.translate(handSide * 0.08f + side * 0.38f * fly + impactSide + orbitX, -0.08f - 0.24f * fly + impactY + orbitY, -0.28f - (0.50f + distance * 0.30f) * fly);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(handSide * (18f + 82f * fly) + side * 22f * fly + orbit * 48f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20f - 74f * fly));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(handSide * (-10f - 62f * fly) + orbit * 38f));
        matrices.scale(1.0f + fly * 0.10f, 1.0f + fly * 0.10f, 1.0f + fly * 0.20f);
        return true;
    }

    private float smooth(float value) {
        value = MathHelper.clamp(value, 0f, 1f);
        return value * value * (3f - 2f * value);
    }

    public void handleRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!player.isUsingSpyglass()) {
            boolean bl = hand == Hand.MAIN_HAND;
            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            matrices.push();
            try {
                if (item.isOf(Items.CROSSBOW)) {
                    boolean bl2 = CrossbowItem.isCharged(item);
                    boolean bl3 = arm == Arm.RIGHT;
                    int i = bl3 ? 1 : -1;
                    if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        matrices.translate((float) i * -0.4785682F, -0.094387F, 0.05731531F);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 65.3F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * -9.785F));
                        float f = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        float g = f / (float) CrossbowItem.getPullTime(item, mc.player);
                        if (g > 1.0F) {
                            g = 1.0F;
                        }

                        if (g > 0.1F) {
                            float h = MathHelper.sin((f - 0.1F) * 1.3F);
                            float j = g - 0.1F;
                            float k = h * j;
                            matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                        }

                        matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                        matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) i * 45.0F));
                    } else {
                        float fx = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                        float gx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                        float h = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                        matrices.translate((float) i * fx, gx, h);
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        this.applySwingOffset(matrices, arm, swingProgress);
                        if (bl2 && swingProgress < 0.001F && bl) {
                            matrices.translate((float) i * -0.641864F, 0.0F, 0.0F);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 10.0F));
                        }
                    }
                    this.renderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
                } else {
                    boolean bl2 = arm == Arm.RIGHT;

                    ViewModelModule viewModel = ViewModelModule.getInstance();
                    if (viewModel.isEnabled()) {
                        if (bl2) {
                            matrices.translate(viewModel.rightX.getValue().doubleValue(), viewModel.rightY.getValue().doubleValue(), viewModel.rightZ.getValue().doubleValue());
                        } else {
                            matrices.translate(-viewModel.leftX.getValue().doubleValue(), viewModel.leftY.getValue().doubleValue(), viewModel.leftZ.getValue().doubleValue());
                        }
                    }

                    if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                        int l = bl2 ? 1 : -1;
                        switch (item.getUseAction()) {
                            case NONE, BLOCK:
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                break;
                            case EAT:
                            case DRINK:
                                this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item);
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                break;
                            case BOW:
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                matrices.translate((float) l * -0.2785682F, 0.18344387F, 0.15731531F);
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));
                                float mx = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                                float fxx = mx / 20.0F;
                                fxx = (fxx * fxx + fxx * 2.0F) / 3.0F;
                                if (fxx > 1.0F) {
                                    fxx = 1.0F;
                                }

                                if (fxx > 0.1F) {
                                    float gx = MathHelper.sin((mx - 0.1F) * 1.3F);
                                    float h = fxx - 0.1F;
                                    float j = gx * h;
                                    matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                                }

                                matrices.translate(fxx * 0.0F, fxx * 0.0F, fxx * 0.04F);
                                matrices.scale(1.0F, 1.0F, 1.0F + fxx * 0.2F);
                                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                                break;
                            case SPEAR:
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                matrices.translate((float) l * -0.5F, 0.7F, 0.1F);
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));
                                float m = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                                float fx = m / 10.0F;
                                if (fx > 1.0F) {
                                    fx = 1.0F;
                                }

                                if (fx > 0.1F) {
                                    float gx = MathHelper.sin((m - 0.1F) * 1.3F);
                                    float h = fx - 0.1F;
                                    float j = gx * h;
                                    matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                                }

                                matrices.translate(0.0F, 0.0F, fx * 0.2F);
                                matrices.scale(1.0F, 1.0F, 1.0F + fx * 0.2F);
                                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                                break;
                            case BRUSH:
                                this.applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
                        }
                    } else if (player.isUsingRiptide()) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        int l = bl2 ? 1 : -1;
                        matrices.translate((float) l * -0.4F, 0.8F, 0.3F);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 65.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -85.0F));
                    } else {
                        if (arm == mc.options.getMainArm().getValue() && isEnabled() && auraCheck()) {
                            handleSwordAnim(matrices, swingProgress, arm);
                        } else {
                            float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                            float mxx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                            float fxxx = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                            int o = bl2 ? 1 : -1;
                            matrices.translate((float) o * n, mxx, fxxx);
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            this.applySwingOffset(matrices, arm, swingProgress);
                        }
                    }
                    this.renderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
                }
            } finally {
                matrices.pop();
            }
        }
    }

    private void applyBrushTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, float equipProgress) {
        this.applyEquipOffset(matrices, arm, equipProgress);
        float f = (float) (mc.player.getItemUseTimeLeft() % 10);
        float g = f - tickDelta + 1.0F;
        float h = 1.0F - g / 10.0F;
        float n = -15.0F + 75.0F * MathHelper.cos(h * 2.0F * (float) Math.PI);
        if (arm != Arm.RIGHT) {
            matrices.translate(0.1, 0.83, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
            matrices.translate(-0.3, 0.22, 0.35);
        } else {
            matrices.translate(-0.25, 0.22, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
        }
    }

    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack) {
        float f = (float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F;
        float g = f / (float) stack.getMaxUseTime(mc.player);
        if (g < 0.8F) {
            float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            matrices.translate(0.0F, h, 0.0F);
        }

        float h = 1.0F - (float) Math.pow(g, 27.0);
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6F * (float) i, h * -0.5F, h * 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * h * 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * h * 30.0F));
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

    public void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!stack.isEmpty()) {
            mc.getItemRenderer().renderItem(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
        }
    }

    public boolean auraCheck() {
        return !auraOnly.getValue() || AuraModule.getInstance().target != null;
    }
}
