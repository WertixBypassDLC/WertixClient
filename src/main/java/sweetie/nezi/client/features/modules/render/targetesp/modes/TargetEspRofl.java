package sweetie.nezi.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspMode;
import sweetie.nezi.client.features.modules.render.targetesp.TargetEspModule;

public class TargetEspRofl extends TargetEspMode {
    private net.minecraft.world.World petWorld = null;
    private PigEntity pig = null;
    private BatEntity bat = null;
    private ParrotEntity parrot = null;
    private AllayEntity fairy = null;
    private BeeEntity bee = null;
    private VexEntity vex = null;
    private FoxEntity fox = null;
    private FrogEntity frog = null;
    private PufferfishEntity pufferfish = null;
    private SlimeEntity slime = null;

    private final SmoothFloat smoothSpeed = new SmoothFloat(1.0f);

    @Override
    public void onUpdate() {
        smoothSpeed.update(TargetEspModule.getInstance().getSpeed(), 0.15f);
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;
        float partialTicks = event.partialTicks();
        float alpha = MathUtil.interpolate(prevShowAnimation, (float) showAnimation.getValue(), partialTicks);
        float sizeVal = MathUtil.interpolate(prevSizeAnimation, (float) sizeAnimation.getValue(), partialTicks);
        if (alpha <= 0.001f || sizeVal <= 0.001f) return;

        MatrixStack ms = event.matrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        float spMul = smoothSpeed.get();
        float anim = alpha * sizeVal;

        String animalSetting = TargetEspModule.getInstance().getAnimal();
        Entity pet = getPet(animalSetting);
        if (pet == null) return;

        float radius = Math.max(0.45f, currentTarget.getWidth() * 0.8f);
        float yBase = Math.max(0.35f, currentTarget.getHeight() * 0.6f);
        float time = -(System.currentTimeMillis() % 1_000_000L) * 0.00025f * spMul;
        float aoe = time * 360f;

        double[] px = new double[8], py = new double[8], pz = new double[8];
        for (int i = 0; i < 8; i++) {
            float ta = aoe + (i / 8.0f) * 360f;
            double rad = Math.toRadians(ta);
            px[i] = getTargetX() + Math.cos(rad) * radius - cameraPos.x;
            py[i] = getTargetY() + yBase + ((i % 2 == 0) ? 0.10f : -0.10f) - 0.20f - cameraPos.y;
            pz[i] = getTargetZ() + Math.sin(rad) * radius - cameraPos.z;
        }

        double coreX = getTargetX() - cameraPos.x;
        double coreY = getTargetY() + Math.max(1.15f, currentTarget.getHeight() + 0.6f) - cameraPos.y;
        double coreZ = getTargetZ() - cameraPos.z;

        float t2 = (System.currentTimeMillis() % 1_000_000L) * spMul * 0.00100f;
        float yaw2 = t2 * 180f, pitch2 = (float)(Math.sin(t2 * 1.5) * 120f), roll2 = (float)(Math.cos(t2 * 1.2) * 90f);

        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();

        for (int i = 0; i < 9; i++) {
            double wx = (i < 8) ? px[i] : coreX;
            double wy = (i < 8) ? py[i] : coreY;
            double wz = (i < 8) ? pz[i] : coreZ;

            ms.push();
            ms.translate(wx, wy, wz);

            float walkT = ((System.currentTimeMillis() % 1_000_000L) * 0.001f) * (1.2f * spMul) + (i * 0.55f);
            ms.translate(0, Math.sin(walkT * 3f) * 0.045f * spMul, 0);

            if (i == 8) {
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw2));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch2));
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll2));
            } else {
                int ni = (i + 1) % 8;
                double lx = px[ni] - wx, lz = pz[ni] - wz;
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) Math.toDegrees(Math.atan2(-lz, lx)) - 95f));
            }

            float s = (i == 8 ? 0.35f : 0.25f) * anim;
            ms.scale(s, s, s);

            mc.getEntityRenderDispatcher().render(pet, 0.0, 0.0, 0.0, partialTicks, ms, vcp, 0xF000F0);
            ms.pop();
        }

        vcp.draw();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private Entity getPet(String kind) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (petWorld != mc.world) {
            petWorld = mc.world;
            pig = null; bat = null; parrot = null; fairy = null; bee = null;
            vex = null; fox = null; frog = null; pufferfish = null; slime = null;
        }
        switch (kind) {
            case "Свинья" -> { if (pig == null) { pig = new PigEntity(EntityType.PIG, mc.world); } return pig; }
            case "Летучая мышь" -> { if (bat == null) { bat = new BatEntity(EntityType.BAT, mc.world); } return bat; }
            case "Попугай" -> { if (parrot == null) { parrot = new ParrotEntity(EntityType.PARROT, mc.world); } return parrot; }
            case "Фея" -> { if (fairy == null) { fairy = new AllayEntity(EntityType.ALLAY, mc.world); } return fairy; }
            case "Пчела" -> { if (bee == null) { bee = new BeeEntity(EntityType.BEE, mc.world); } return bee; }
            case "Векс" -> { if (vex == null) { vex = new VexEntity(EntityType.VEX, mc.world); } return vex; }
            case "Лисичка" -> { if (fox == null) { fox = new FoxEntity(EntityType.FOX, mc.world); } return fox; }
            case "Лягушка" -> { if (frog == null) { frog = new FrogEntity(EntityType.FROG, mc.world); } return frog; }
            case "Иглобрюх" -> { if (pufferfish == null) { pufferfish = new PufferfishEntity(EntityType.PUFFERFISH, mc.world); } return pufferfish; }
            case "Слайм" -> { if (slime == null) { slime = new SlimeEntity(EntityType.SLIME, mc.world); } return slime; }
        }
        return null;
    }
}