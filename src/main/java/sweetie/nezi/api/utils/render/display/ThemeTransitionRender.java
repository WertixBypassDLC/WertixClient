package sweetie.nezi.api.utils.render.display;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.client.ui.theme.Theme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ThemeTransitionRender {
    private static final ThemeTransitionRender instance = new ThemeTransitionRender();
    public static ThemeTransitionRender getInstance() { return instance; }

    private final ShaderProgramKey shaderKey = new ShaderProgramKey(FileUtil.getShader("rect/theme_mask"), VertexFormats.POSITION_TEXTURE, Defines.EMPTY);
    
    private final List<Layer> layers = new ArrayList<>();

    public void startTransition() {
        startTransition(null);
    }

    public void startTransition(Theme targetTheme) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();

        SimpleFramebuffer fbo;
        if (layers.size() >= 4) {
            Layer oldest = layers.remove(0);
            oldest.fbo.delete();
        }

        fbo = new SimpleFramebuffer(width, height, true);

        fbo.setClearColor(0f, 0f, 0f, 0f);
        fbo.clear();

        Color paint = targetTheme == null ? Color.WHITE : targetTheme.getPrimaryColor();
        Layer layer = new Layer(fbo, ThreadLocalRandom.current().nextFloat(), paint);
        layers.add(layer);
    }

    public void updateProgress() {
        Iterator<Layer> iterator = layers.iterator();
        while (iterator.hasNext()) {
            Layer layer = iterator.next();
            layer.transitionAnim.update();
            if (layer.transitionAnim.getToValue() != 1.18) {
                layer.transitionAnim.run(1.18, 1350, sweetie.nezi.api.utils.animation.Easing.SINE_OUT);
            }
            layer.progress = (float) layer.transitionAnim.getValue();
            if (layer.progress > 1.14f) {
                layer.fbo.delete();
                iterator.remove();
            }
        }
    }

    public boolean isTransitioning() {
        return !layers.isEmpty();
    }

    public SimpleFramebuffer getFbo() {
        return layers.isEmpty() ? null : layers.get(layers.size() - 1).fbo;
    }

    public void drawMaskedTransition(MatrixStack matrixStack, int screenWidth, int screenHeight) {
        if (layers.isEmpty()) return;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        try {
            for (Layer layer : new ArrayList<>(layers)) {
                RenderSystem.setShaderTexture(0, layer.fbo.getColorAttachment());
                ShaderProgram shader = RenderSystem.setShader(shaderKey);
                if (shader != null) {
                    if (shader.getUniform("uResolution") != null) {
                        shader.getUniform("uResolution").set((float)screenWidth, (float)screenHeight);
                    }
                    if (shader.getUniform("uProgress") != null) {
                        shader.getUniform("uProgress").set(layer.progress);
                    }
                    if (shader.getUniform("uSeed") != null) {
                        shader.getUniform("uSeed").set(layer.seed);
                    }
                    if (shader.getUniform("uPaint") != null) {
                        shader.getUniform("uPaint").set(layer.paint.getRed() / 255f, layer.paint.getGreen() / 255f, layer.paint.getBlue() / 255f);
                    }
                }

                float uLeft = 0f;
                float uRight = 1f;
                float vTop = 1f;
                float vBottom = 0f;

                var matrix4f = matrixStack.peek().getPositionMatrix();

                BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                builder.vertex(matrix4f, 0f, 0f, 0f).texture(uLeft, vTop);
                builder.vertex(matrix4f, 0f, (float)screenHeight, 0f).texture(uLeft, vBottom);
                builder.vertex(matrix4f, (float)screenWidth, (float)screenHeight, 0f).texture(uRight, vBottom);
                builder.vertex(matrix4f, (float)screenWidth, 0f, 0f).texture(uRight, vTop);
                BufferRenderer.drawWithGlobalProgram(builder.end());
            }
        } catch (Exception e) {
            e.printStackTrace();
            layers.clear();
        } finally {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static class Layer {
        private final SimpleFramebuffer fbo;
        private final float seed;
        private final Color paint;
        private float progress;
        private final sweetie.nezi.api.utils.animation.AnimationUtil transitionAnim = new sweetie.nezi.api.utils.animation.AnimationUtil();

        private Layer(SimpleFramebuffer fbo, float seed, Color paint) {
            this.fbo = fbo;
            this.seed = seed;
            this.paint = paint;
            this.progress = 0f;
            this.transitionAnim.setValue(0.0);
        }
    }
}
