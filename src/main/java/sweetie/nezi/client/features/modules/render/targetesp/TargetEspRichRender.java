package sweetie.nezi.client.features.modules.render.targetesp;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.events.render.Render3DEvent;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.api.utils.color.UIColors;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TargetEspRichRender {
    private static Entity lastRenderedTarget = null;
    private static final List<Crystal> crystalList = new ArrayList<>();
    private static float rotationAngle = 0;
    private static final Identifier ESP_BLOOM_TEX = sweetie.nezi.api.system.files.FileUtil.getImage("particles/glow");
    
    // Pets
    private static net.minecraft.world.World petWorld = null;
    private static PigEntity pig = null;
    private static BatEntity bat = null;
    private static ParrotEntity parrot = null;
    private static AllayEntity fairy = null;
    private static BeeEntity bee = null;
    private static VexEntity vex = null;
    private static FoxEntity fox = null;
    private static FrogEntity frog = null;
    private static PufferfishEntity pufferfish = null;
    private static SlimeEntity slime = null;
    
    private static double kolcoStep = 0.0;
    private static final double ring2SpeedBase = 0.035;

    public static void render(String style, Render3DEvent.Render3DEventData event, LivingEntity entity, float anim, double targetX, double targetY, double targetZ) {
        if (anim <= 0.001f || entity == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        
        MatrixStack ms = event.matrixStack();
        float red = MathHelper.clamp((entity.hurtTime - mc.getRenderTickCounter().getTickDelta(false)) / 20f, 0f, 1f);
        
        float baseScaleSpeed = TargetEspModule.getInstance().getSpeed();
        if (style.equals("Circle V2")) {
            kolcoStep += ring2SpeedBase * baseScaleSpeed;
        }

        switch (style) {
            case "Circle V2" -> renderKolco2(ms, entity, anim, red, targetX, targetY, targetZ, baseScaleSpeed);
            case "Ghost V2" -> renderDoubleHelix(ms, entity, anim, red, targetX, targetY, targetZ, baseScaleSpeed);
            case "Crystals" -> {
                if (crystalList.isEmpty() || entity != lastRenderedTarget) {
                    createCrystals(entity);
                    lastRenderedTarget = entity;
                }
                renderCrystals(ms, entity, anim, red, targetX, targetY, targetZ, baseScaleSpeed);
            }
            case "Garland" -> renderGarland(ms, entity, anim, targetX, targetY, targetZ, baseScaleSpeed);
            case "Atom" -> renderAtom(ms, entity, anim, red, targetX, targetY, targetZ, baseScaleSpeed);
            case "Pig" -> renderPet(ms, entity, anim, PetKind.PIG, baseScaleSpeed, targetX, targetY, targetZ);
            case "Летучая мышь" -> renderPet(ms, entity, anim, PetKind.BAT, baseScaleSpeed, targetX, targetY, targetZ);
            case "Попугай" -> renderPet(ms, entity, anim, PetKind.PARROT, baseScaleSpeed, targetX, targetY, targetZ);
            case "Фея" -> renderPet(ms, entity, anim, PetKind.FAIRY, baseScaleSpeed, targetX, targetY, targetZ);
            case "Пчела" -> renderPet(ms, entity, anim, PetKind.BEE, baseScaleSpeed, targetX, targetY, targetZ);
            case "Векс" -> renderPet(ms, entity, anim, PetKind.VEX, baseScaleSpeed, targetX, targetY, targetZ);
            case "Лисичка" -> renderPet(ms, entity, anim, PetKind.FOX, baseScaleSpeed, targetX, targetY, targetZ);
            case "Лягушка" -> renderPet(ms, entity, anim, PetKind.FROG, baseScaleSpeed, targetX, targetY, targetZ);
            case "Иглобрюх" -> renderPet(ms, entity, anim, PetKind.PUFFERFISH, baseScaleSpeed, targetX, targetY, targetZ);
            case "Слайм" -> renderPet(ms, entity, anim, PetKind.SLIME, baseScaleSpeed, targetX, targetY, targetZ);
        }
    }

    private static Color interpolateColor(Color a, Color b, float ratio) {
        float i = 1f - ratio;
        return new Color(
                (int) (a.getRed() * i + b.getRed() * ratio),
                (int) (a.getGreen() * i + b.getGreen() * ratio),
                (int) (a.getBlue() * i + b.getBlue() * ratio),
                a.getAlpha()
        );
    }
    
    private static int getCustomColor(float red) {
        Color base = TargetEspModule.getInstance().getCustomColor();
        return interpolateColor(base, new Color(255, 0, 0), red).getRGB();
    }
    
    private static int setAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }

    // --- Circle V2 ---
    private static void renderKolco2(MatrixStack matrices, LivingEntity targetEntity, float animationAlpha, float red, double tx, double ty, double tz, float speedMul) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        
        float entityWidth = targetEntity.getWidth() * 0.9f;
        float entityHeight = targetEntity.getHeight();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        double golovkaY = Math.abs(Math.sin(kolcoStep)) * entityHeight;
        double tailBaseY = Math.abs(Math.sin(kolcoStep - 0.4)) * entityHeight;

        float golovkaSize = 0.12f * animationAlpha; // scaled with alpha to pop in
        float tailSize = 0.08f * animationAlpha;

        int totalPoints = 138;
        int tailSegments = 16;
        int baseColor = getCustomColor(red);

        for (int i = 0; i < totalPoints; i++) {
            double angleRadians = 2 * Math.PI * i / totalPoints;
            float xOffset = (float) (Math.cos(angleRadians) * entityWidth);
            float zOffset = (float) (Math.sin(angleRadians) * entityWidth);

            matrices.push();
            matrices.translate(tx + xOffset - cameraPos.x, ty + golovkaY - cameraPos.y, tz + zOffset - cameraPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            int coreColor = setAlpha(baseColor, (int)(animationAlpha * 230));
            drawGlowTexture(matrices.peek(), ESP_BLOOM_TEX, -golovkaSize / 2, -golovkaSize / 2, golovkaSize, golovkaSize, coreColor);
            matrices.pop();

            for (int t = 1; t <= tailSegments; t++) {
                float tailProgress = (float) t / (tailSegments + 1);
                double currentTailY = golovkaY + (tailBaseY - golovkaY) * tailProgress;
                float currentTailAlpha = animationAlpha * (1f - tailProgress) * 0.6f;

                matrices.push();
                matrices.translate(tx + xOffset - cameraPos.x, ty + currentTailY - cameraPos.y, tz + zOffset - cameraPos.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                int tailCoreColor = setAlpha(baseColor, (int)(currentTailAlpha * 255));
                drawGlowTexture(matrices.peek(), ESP_BLOOM_TEX, -tailSize / 2, -tailSize / 2, tailSize, tailSize, tailCoreColor);
                matrices.pop();
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
    
    private static void drawGlowTexture(MatrixStack.Entry entry, Identifier texture, float x, float y, float w, float h, int color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        Matrix4f m = entry.getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bb.vertex(m, x, y + h, 0).texture(0, 1).color(color);
        bb.vertex(m, x + w, y + h, 0).texture(1, 1).color(color);
        bb.vertex(m, x + w, y, 0).texture(1, 0).color(color);
        bb.vertex(m, x, y, 0).texture(0, 0).color(color);
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    // --- Double Helix (Ghost V2) ---
    private static void renderDoubleHelix(MatrixStack ms, LivingEntity entity, float anim, float red, double tx, double ty, double tz, float spMul) {
        float eased = 1.0f - (1.0f - anim) * (1.0f - anim) * (1.0f - anim);
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        float radius = Math.max(0.22f, entity.getWidth() * 0.75f);
        float height = Math.max(0.35f, entity.getHeight());
        double time = (double) System.currentTimeMillis() / (500.0 / (double) (3f * spMul));
        int baseColor = getCustomColor(red);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        ms.push();
        ms.translate(tx - cameraPos.x, ty - cameraPos.y, tz - cameraPos.z);
        BufferBuilder quads = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        int steps = 40;
        for (int j = 0; j < steps; j++) {
            float k = (float) j / (float) steps;
            float fade = 1.0f - k;
            float a = eased * fade * 0.9f;

            double tt = time - (double) j * 0.12;
            float sn = (float) Math.sin(tt);
            float cs = (float) Math.cos(tt);

            float y = height * 0.55f + (float) Math.sin(tt) * 0.26f;
            float x1 = cs * radius;
            float z1 = sn * radius;
            float s = (0.22f * fade + 0.06f) * eased;
            int c = setAlpha(baseColor, (int)(a * 255));

            drawBillboard(quads, ms, x1, y, z1, s, camera.getYaw(), camera.getPitch(), c);
            drawBillboard(quads, ms, -x1, y, -z1, s, camera.getYaw(), camera.getPitch(), c);
        }

        BufferRenderer.drawWithGlobalProgram(quads.end());
        ms.pop();

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    // --- Garland ---
    private static void renderGarland(MatrixStack ms, LivingEntity entity, float anim, double tx, double ty, double tz, float sp) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        float height = entity.getHeight();
        float radius = entity.getWidth() * 1.2f;
        float period = 4000f / Math.max(0.05f, sp);
        float time = (System.currentTimeMillis() % (long) period) / period;
        float offset = time * 360f;

        ms.push();
        ms.translate(tx - cameraPos.x, ty - cameraPos.y, tz - cameraPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder wire = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        int wireColor = setAlpha(0x00B3B0B, (int)(anim * 255));
        Matrix4f pm = ms.peek().getPositionMatrix();

        int lightsCount = 30;
        int spirals = 3;
        for (int i = 0; i <= lightsCount; i++) {
            float progress = (float) i / (float) lightsCount;
            float angle = (float) Math.toRadians(offset + (progress * 360f * spirals));
            float currentRadius = radius * (1.0f - (progress * 0.6f));
            float px = (float) Math.cos(angle) * currentRadius;
            float pz = (float) Math.sin(angle) * currentRadius;
            float py = progress * height;
            wire.vertex(pm, px, py, pz).color(wireColor);
        }
        BufferRenderer.drawWithGlobalProgram(wire.end());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        BufferBuilder bulbs = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int i = 0; i <= lightsCount; i++) {
            float progress = (float) i / (float) lightsCount;
            float angle = (float) Math.toRadians(offset + (progress * 360f * spirals));
            float currentRadius = radius * (1.0f - (progress * 0.6f));
            float px = (float) Math.cos(angle) * currentRadius;
            float pz = (float) Math.sin(angle) * currentRadius;
            float py = progress * height;

            float size = 0.15f * anim;
            float twinkle = (float) Math.sin(((System.currentTimeMillis() / 100.0) * sp) + i) * 0.2f + 0.8f;
            int festive = (i % 4 == 0) ? 0xFFFF0000 : (i % 4 == 1) ? 0xFFFFD700 : (i % 4 == 2) ? 0xFF00FF00 : 0xFF00BFFF;
            int color = setAlpha(festive, (int)(anim * twinkle * 255));
            drawBillboard(bulbs, ms, px, py, pz, size, camera.getYaw(), camera.getPitch(), color);
        }
        BufferRenderer.drawWithGlobalProgram(bulbs.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        ms.pop();
    }
    
    // --- Atom ---
    private static void renderAtom(MatrixStack ms, LivingEntity entity, float anim, float red, double tx, double ty, double tz, float sp) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        
        float time = (float)((System.nanoTime() / 1_000_000_000.0f) * sp);
        int baseColor = getCustomColor(red);

        ms.push();
        ms.translate(tx - cameraPos.x, ty + entity.getHeight() / 2f - cameraPos.y, tz - cameraPos.z);
        ms.scale(0.7f * anim, 0.7f * anim, 0.7f * anim);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();

        float radius = 1.0f;
        int segments = 160;
        float ringSpin = time * 78f;

        for (int ring = 0; ring < 3; ring++) {
            ms.push();
            float wobble = (float) Math.sin(time * 1.35f + ring * 0.9f) * 7.5f;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ring * 60f + wobble));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ringSpin + ring * 120f));

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            Matrix4f matrix = ms.peek().getPositionMatrix();

            for (int i = 0; i <= segments; i++) {
                float ang = (float) (2 * Math.PI * i / segments);
                bb.vertex(matrix, (float) Math.cos(ang) * radius, 0, (float) Math.sin(ang) * radius).color(setAlpha(baseColor, (int)(165 * anim)));
            }
            BufferRenderer.drawWithGlobalProgram(bb.end());

            float speed = 2.15f + ring * 0.35f;
            for (int p = 0; p < 2; p++) {
                float base = (float) (2 * Math.PI * p / 2);
                float theta = time * speed + base + ring * 0.82f;
                float px = (float) Math.cos(theta) * radius;
                float pz = (float) Math.sin(theta) * radius;

                float pulse = 0.92f + 0.08f * (float) Math.sin(time * 6.6f + ring * 1.7f + p * 2.1f);
                float s = 0.082f * anim * pulse;

                ms.push();
                ms.translate(px, 0, pz);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 190f + ring * 77f + p * 101f));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(time * 160f + ring * 53f + p * 89f));

                drawSmallSphere(ms, s * 1.25f, setAlpha(baseColor, (int)(110 * anim)));
                drawSmallSphere(ms, s * 0.78f, setAlpha(baseColor, (int)(210 * anim)));

                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
                BufferBuilder glow = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                drawBillboard(glow, ms, 0, 0, 0, s * 2.0f, camera.getYaw(), camera.getPitch(), setAlpha(baseColor, (int)(90 * anim)));
                BufferRenderer.drawWithGlobalProgram(glow.end());
                ms.pop();
            }
            ms.pop();
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
        BufferBuilder core = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        drawBillboard(core, ms, 0, 0, 0, 0.20f * anim, camera.getYaw(), camera.getPitch(), setAlpha(baseColor, (int)(220 * anim)));
        drawBillboard(core, ms, 0, 0, 0, 0.34f * anim, camera.getYaw(), camera.getPitch(), setAlpha(baseColor, (int)(90 * anim)));
        BufferRenderer.drawWithGlobalProgram(core.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        ms.pop();
    }

    private static void drawSmallSphere(MatrixStack ms, float size, int color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bb.vertex(m, -size, -size, 0).color(color);
        bb.vertex(m, -size, size, 0).color(color);
        bb.vertex(m, size, size, 0).color(color);
        bb.vertex(m, size, -size, 0).color(color);
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    // --- Crystals ---
    private static void createCrystals(Entity target) {
        crystalList.clear();
        crystalList.add(new Crystal(new Vec3d(0, 0.85, 0.8), new Vec3d(-49, 0, 40)));
        crystalList.add(new Crystal(new Vec3d(0.2, 0.85, -0.675), new Vec3d(35, 0, -30)));
        crystalList.add(new Crystal(new Vec3d(0.6, 1.35, 0.6), new Vec3d(-30, 0, 35)));
        crystalList.add(new Crystal(new Vec3d(-0.74, 1.05, 0.4), new Vec3d(-25, 0, -30)));
        crystalList.add(new Crystal(new Vec3d(0.74, 0.95, -0.4), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.475, 0.85, -0.375), new Vec3d(30, 0, -25)));
        crystalList.add(new Crystal(new Vec3d(0, 1.35, -0.6), new Vec3d(45, 0, 0)));
    }

    private static void renderCrystals(MatrixStack ms, Entity target, float anim, float red, double tx, double ty, double tz, float sp) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        rotationAngle = (rotationAngle + 0.5f * sp) % 360.0f;
        int baseColor = getCustomColor(red);

        ms.push();
        ms.translate(tx - cameraPos.x, ty - cameraPos.y, tz - cameraPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));

        for (Crystal crystal : crystalList) {
            crystal.render(ms, anim, baseColor, camera, sp);
        }
        ms.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static class Crystal {
        private final Vec3d position;
        private final Vec3d rotation;
        private final float size = 0.05f;
        private final float rotationSpeed = 0.5f + (float) (Math.random() * 1.5f);

        public Crystal(Vec3d position, Vec3d rotation) {
            this.position = position;
            this.rotation = rotation;
        }

        public void render(MatrixStack ms, float anim, int baseColor, Camera camera, float speedMul) {
            ms.push();
            ms.translate(position.x, position.y, position.z);

            float t = (System.currentTimeMillis() / 500.0f) * speedMul;
            float pulsation = 1.0f + (float) (Math.sin(t) * 0.1f);
            ms.scale(pulsation, pulsation, pulsation);

            float selfRotation = ((System.currentTimeMillis() % 36000) / 100.0f) * rotationSpeed * speedMul;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y + selfRotation));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(ms, baseColor, 0.2f, true, anim);

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(ms, baseColor, 0.3f, true, anim);
            drawCrystal(ms, baseColor, 0.8f, false, anim);

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawBloomSphere(ms, baseColor, anim, camera);

            ms.pop();
        }

        private void drawBloomSphere(MatrixStack ms, int baseColor, float anim, Camera camera) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, ESP_BLOOM_TEX);
            int bloomColor = setAlpha(baseColor, (int) (0.4f * 255 * anim));
            float bloomSize = size * 13.0f;

            for (int i = 0; i < 6; i++) {
                ms.push();
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 60f));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                Matrix4f matrix = ms.peek().getPositionMatrix();
                bb.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bb.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bb.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bb.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bb.end());
                ms.pop();
            }
        }

        private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled, float anim) {
            BufferBuilder bb = Tessellator.getInstance().begin(
                    filled ? VertexFormat.DrawMode.TRIANGLES : VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR
            );
            float h_prism = size * 1f;
            float h_pyramid = size * 1.5f;

            List<Vec3d> top = new ArrayList<>();
            List<Vec3d> bot = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                float angle = (float) (2 * Math.PI * i / 8);
                float x = (float) (size * Math.cos(angle));
                float z = (float) (size * Math.sin(angle));
                top.add(new Vec3d(x, h_prism / 2, z));
                bot.add(new Vec3d(x, -h_prism / 2, z));
            }
            Vec3d vTop = new Vec3d(0, h_prism / 2 + h_pyramid, 0);
            Vec3d vBot = new Vec3d(0, -h_prism / 2 - h_pyramid, 0);

            int col = setAlpha(baseColor, (int)(alpha * 255 * anim));

            for (int i = 0; i < 8; i++) {
                int ni = (i + 1) % 8;
                if (filled) {
                    drawTri(ms, bb, bot.get(i), bot.get(ni), top.get(ni), col, true);
                    drawTri(ms, bb, bot.get(i), top.get(ni), top.get(i), col, true);
                    drawTri(ms, bb, vTop, top.get(i), top.get(ni), col, true);
                    drawTri(ms, bb, vBot, bot.get(ni), bot.get(i), col, true);
                } else {
                    drawTri(ms, bb, vTop, top.get(i), top.get(ni), col, false);
                    drawTri(ms, bb, bot.get(i), top.get(i), top.get(ni), col, false);
                }
            }
            BufferRenderer.drawWithGlobalProgram(bb.end());
        }

        private void drawTri(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, boolean filled) {
            Matrix4f m = ms.peek().getPositionMatrix();
            if (filled) {
                bb.vertex(m, (float) v1.x, (float) v1.y, (float) v1.z).color(color);
                bb.vertex(m, (float) v2.x, (float) v2.y, (float) v2.z).color(color);
                bb.vertex(m, (float) v3.x, (float) v3.y, (float) v3.z).color(color);
            } else {
                bb.vertex(m, (float) v1.x, (float) v1.y, (float) v1.z).color(color);
                bb.vertex(m, (float) v2.x, (float) v2.y, (float) v2.z).color(color);
                bb.vertex(m, (float) v2.x, (float) v2.y, (float) v2.z).color(color);
                bb.vertex(m, (float) v3.x, (float) v3.y, (float) v3.z).color(color);
                bb.vertex(m, (float) v3.x, (float) v3.y, (float) v3.z).color(color);
                bb.vertex(m, (float) v1.x, (float) v1.y, (float) v1.z).color(color);
            }
        }
    }

    // --- Helpers ---
    private static void drawBillboard(BufferBuilder buffer, MatrixStack ms, float x, float y, float z, float scale, float yaw, float pitch, int color) {
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        Matrix4f m = ms.peek().getPositionMatrix();
        buffer.vertex(m, -scale, -scale, 0).texture(0, 0).color(color);
        buffer.vertex(m, -scale, scale, 0).texture(0, 1).color(color);
        buffer.vertex(m, scale, scale, 0).texture(1, 1).color(color);
        buffer.vertex(m, scale, -scale, 0).texture(1, 0).color(color);
        ms.pop();
    }

    enum PetKind { PIG, BAT, PARROT, FAIRY, BEE, VEX, FOX, FROG, PUFFERFISH, SLIME }

    private static Entity getPet(PetKind kind) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (petWorld != mc.world) {
            petWorld = mc.world;
            pig = null; bat = null; parrot = null; fairy = null; bee = null;
            vex = null; fox = null; frog = null; pufferfish = null; slime = null;
        }
        switch (kind) {
            case PIG -> { if (pig == null) { pig = new PigEntity(EntityType.PIG, mc.world); } return pig; }
            case BAT -> { if (bat == null) { bat = new BatEntity(EntityType.BAT, mc.world); } return bat; }
            case PARROT -> { if (parrot == null) { parrot = new ParrotEntity(EntityType.PARROT, mc.world); } return parrot; }
            case FAIRY -> { if (fairy == null) { fairy = new AllayEntity(EntityType.ALLAY, mc.world); } return fairy; }
            case BEE -> { if (bee == null) { bee = new BeeEntity(EntityType.BEE, mc.world); } return bee; }
            case VEX -> { if (vex == null) { vex = new VexEntity(EntityType.VEX, mc.world); } return vex; }
            case FOX -> { if (fox == null) { fox = new FoxEntity(EntityType.FOX, mc.world); } return fox; }
            case FROG -> { if (frog == null) { frog = new FrogEntity(EntityType.FROG, mc.world); } return frog; }
            case PUFFERFISH -> { if (pufferfish == null) { pufferfish = new PufferfishEntity(EntityType.PUFFERFISH, mc.world); } return pufferfish; }
            case SLIME -> { if (slime == null) { slime = new SlimeEntity(EntityType.SLIME, mc.world); } return slime; }
        }
        return null;
    }

    private static void renderEntityCompat(net.minecraft.client.render.entity.EntityRenderDispatcher dispatcher, Entity entity, MatrixStack ms, VertexConsumerProvider vcp, float tickDelta) {
        dispatcher.render(entity, 0.0, 0.0, 0.0, tickDelta, ms, vcp, 0xF000F0);
    }

    private static void renderPet(MatrixStack ms, LivingEntity entity, float anim, PetKind kind, float spMul, double tx, double ty, double tz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity pet = getPet(kind);
        if (pet == null) return;
        
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        float radius = Math.max(0.45f, entity.getWidth() * 0.8f);
        float yBase = Math.max(0.35f, entity.getHeight() * 0.6f);
        float time = -(System.currentTimeMillis() % 1_000_000L) * 0.00025f * spMul;
        float aoe = time * 360f;

        double[] px = new double[8], py = new double[8], pz = new double[8];
        for (int i = 0; i < 8; i++) {
            float ta = aoe + (i / 8.0f) * 360f;
            double rad = Math.toRadians(ta);
            px[i] = tx + Math.cos(rad) * radius - cameraPos.x;
            py[i] = ty + yBase + ((i % 2 == 0) ? 0.10f : -0.10f) - 0.20f - cameraPos.y;
            pz[i] = tz + Math.sin(rad) * radius - cameraPos.z;
        }

        double coreX = tx - cameraPos.x;
        double coreY = ty + Math.max(1.15f, entity.getHeight() + 0.6f) - cameraPos.y;
        double coreZ = tz - cameraPos.z;

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

            renderEntityCompat(mc.getEntityRenderDispatcher(), pet, ms, vcp, mc.getRenderTickCounter().getTickDelta(false));
            ms.pop();
        }

        vcp.draw();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
