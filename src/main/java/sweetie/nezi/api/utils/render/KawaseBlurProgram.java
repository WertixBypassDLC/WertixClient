package sweetie.nezi.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.other.FramebufferResizeEvent;
import sweetie.nezi.api.system.files.FileUtil;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.framelimiter.FrameLimiter;
import sweetie.nezi.client.features.modules.render.InterfaceModule;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class KawaseBlurProgram implements QuickImports {
    private final ShaderProgramKey upShader = new ShaderProgramKey(FileUtil.getShader("post/blur/upscale"), VertexFormats.POSITION, Defines.EMPTY);
    private final ShaderProgramKey downShader = new ShaderProgramKey(FileUtil.getShader("post/blur/downscale"), VertexFormats.POSITION, Defines.EMPTY);

    private final int blurPasses = InterfaceModule.getPasses();

    public final List<SimpleFramebuffer> fbos = new ArrayList<>();

    private boolean init = false;
    private final FrameLimiter f = new FrameLimiter(false);

    public void load() {
        ensureInitialized();
        FramebufferResizeEvent.getInstance().subscribe(new Listener<>(event -> recreate()));
    }

    public void recreate() {
        if (!hasFramebufferContext()) {
            init = false;
            fbos.clear();
            return;
        }

        fbos.forEach(Framebuffer::delete);
        fbos.clear();
        for (int i = 0; i <= InterfaceModule.getPasses(); i++) {
            fbos.add(createFbo());
        }
        init = true;
    }

    public void render(MatrixStack matrixStack) {
        if (!ensureInitialized()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getFramebuffer() == null || InterfaceModule.getGlassy() == 1f) {
            return;
        }

        if (!isFramebufferSetValid(client)) {
            recreate();
            if (!isFramebufferSetValid(client)) {
                return;
            }
        }

        f.execute(40, () -> {
            try {
                int actualPasses = Math.max(fbos.size() - 1, 1);
                applyBlurPass(matrixStack, downShader, client.getFramebuffer(), fbos.getFirst(), 0);

                for (int i = 0; i < actualPasses; i++) {
                    applyBlurPass(matrixStack, downShader, fbos.get(i), fbos.get(i + 1), i + 1);
                }

                for (int i = actualPasses; i > 0; i--) {
                    applyBlurPass(matrixStack, upShader, fbos.get(i), fbos.get(i - 1), i);
                }

                client.getFramebuffer().beginWrite(false);
            } catch (Exception ignored) {
                recreate();
                if (client.getFramebuffer() != null) {
                    client.getFramebuffer().beginWrite(false);
                }
            }
        });
    }

    private void applyBlurPass(MatrixStack matrixStack, ShaderProgramKey shaderKey, Framebuffer source, Framebuffer destination, int pass) {
        if (source == null || destination == null || source.getColorAttachment() <= 0 || destination.getColorAttachment() <= 0) {
            return;
        }

        destination.beginWrite(false);
        RenderSystem.setShaderTexture(0, source.getColorAttachment());
        ShaderProgram shader = RenderSystem.setShader(shaderKey);
        shader.getUniform("uHalfTexelSize").set(0.5f / (float) source.textureWidth, 0.5f / (float) source.textureHeight);
        shader.getUniform("uOffset").set(InterfaceModule.getOffset() * (pass / (float) blurPasses));
        drawFullQuad(matrixStack.peek().getPositionMatrix());
        destination.endWrite();
    }

    private void drawFullQuad(Matrix4f matrix4f) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        float width = client.getWindow().getScaledWidth();
        float height = client.getWindow().getScaledHeight();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        builder.vertex(matrix4f, 0, 0, 0);
        builder.vertex(matrix4f, 0, height, 0);
        builder.vertex(matrix4f, width, height, 0);
        builder.vertex(matrix4f, width, 0, 0);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private SimpleFramebuffer createFbo() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            throw new IllegalStateException("Cannot create Kawase blur framebuffer before the client window is ready");
        }
        return new SimpleFramebuffer(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), false);
    }

    private boolean ensureInitialized() {
        if (!hasFramebufferContext()) {
            return false;
        }
        if (!init || !isFramebufferSetValid(MinecraftClient.getInstance())) {
            recreate();
        }
        return !fbos.isEmpty() && isFramebufferSetValid(MinecraftClient.getInstance());
    }

    private boolean hasFramebufferContext() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getWindow() != null && client.getFramebuffer() != null;
    }

    private boolean isFramebufferSetValid(MinecraftClient client) {
        if (client == null || client.getWindow() == null || fbos.isEmpty()) {
            return false;
        }

        int expected = InterfaceModule.getPasses() + 1;
        if (fbos.size() != expected) {
            return false;
        }

        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        for (Framebuffer fbo : fbos) {
            if (fbo == null || fbo.getColorAttachment() <= 0 || fbo.textureWidth != width || fbo.textureHeight != height) {
                return false;
            }
        }
        return true;
    }
}
