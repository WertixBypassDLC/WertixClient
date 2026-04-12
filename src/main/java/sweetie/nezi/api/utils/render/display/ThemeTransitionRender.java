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

public class ThemeTransitionRender {
    private static final ThemeTransitionRender instance = new ThemeTransitionRender();
    public static ThemeTransitionRender getInstance() { return instance; }

    private final ShaderProgramKey shaderKey = new ShaderProgramKey(FileUtil.getShader("rect/theme_mask"), VertexFormats.POSITION_TEXTURE, Defines.EMPTY);
    
    private SimpleFramebuffer fbo;
    private boolean isTransitioning = false;
    private float progress = 0f;
    private final sweetie.nezi.api.utils.animation.AnimationUtil transitionAnim = new sweetie.nezi.api.utils.animation.AnimationUtil();

    public void startTransition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();

        if (fbo == null || fbo.textureWidth != width || fbo.textureHeight != height) {
            if (fbo != null) fbo.delete();
            fbo = new SimpleFramebuffer(width, height, true);
        }

        fbo.setClearColor(0f, 0f, 0f, 0f);
        fbo.clear();

        // Copy the main framebuffer to our FBO right this moment!
        // We can just bind our fbo and execute a blit, or tell Minecraft to bind it before the NEXT clickgui frame.
        // But since we want to capture the current state, we do it in ScreenClickGUI directly.
        isTransitioning = true;
        progress = 0f;
        transitionAnim.setValue(0.0);
    }

    public void updateProgress() {
        transitionAnim.update();
        if (transitionAnim.getToValue() != 1.2) {
            transitionAnim.run(1.2, 550, sweetie.nezi.api.utils.animation.Easing.EXPO_OUT);
        }
        progress = (float) transitionAnim.getValue();
        if (progress > 1.15f) {
            isTransitioning = false;
        }
    }

    public boolean isTransitioning() {
        return isTransitioning;
    }

    public SimpleFramebuffer getFbo() {
        return fbo;
    }

    public void drawMaskedTransition(MatrixStack matrixStack, int screenWidth, int screenHeight) {
        if (fbo == null || !isTransitioning) return;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        try {
            RenderSystem.setShaderTexture(0, fbo.getColorAttachment());
            ShaderProgram shader = RenderSystem.setShader(shaderKey);
            if (shader != null) {
                if (shader.getUniform("uResolution") != null) {
                    shader.getUniform("uResolution").set((float)screenWidth, (float)screenHeight);
                }
                if (shader.getUniform("uProgress") != null) {
                    shader.getUniform("uProgress").set(progress);
                }
            }
            
            float uLeft = 0f;
            float uRight = 1f;
            // Minecraft FBOs are inverted on Y axis often when reading from them directly to screen
            float vTop = 1f;
            float vBottom = 0f;

            var matrix4f = matrixStack.peek().getPositionMatrix();
            
            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            builder.vertex(matrix4f, 0f, 0f, 0f).texture(uLeft, vTop);
            builder.vertex(matrix4f, 0f, (float)screenHeight, 0f).texture(uLeft, vBottom);
            builder.vertex(matrix4f, (float)screenWidth, (float)screenHeight, 0f).texture(uRight, vBottom);
            builder.vertex(matrix4f, (float)screenWidth, 0f, 0f).texture(uRight, vTop);
            BufferRenderer.drawWithGlobalProgram(builder.end());
        } catch (Exception e) {
            e.printStackTrace();
            isTransitioning = false;
        } finally {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }
}
