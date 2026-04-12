package sweetie.nezi.api.utils.render.display;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import sweetie.nezi.api.system.files.FileUtil;

import java.awt.*;

public class RectRender {
    private static final ShaderProgramKey shaderKey = new ShaderProgramKey(FileUtil.getShader("rect/rect"), VertexFormats.POSITION_COLOR, Defines.EMPTY);

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, float radius, Color color) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color);
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color) {
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        float smoothness = 0.8f;
        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        ShaderProgram shader = RenderSystem.setShader(shaderKey);
        shader.getUniform("uSize").set(width, height);
        shader.getUniform("uRadius").set(radius.x, radius.z, radius.w, radius.y);
        shader.getUniform("uSmoothness").set(smoothness);

        float z = 0f;
        int colorInt = color.getRGB();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, z).color(colorInt);
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, z).color(colorInt);
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, z).color(colorInt);
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, z).color(colorInt);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // Draw a filled polygon from vertex arrays (triangle fan)
    public void drawPolygon(MatrixStack matrixStack, float[] xs, float[] ys, Color color) {
        if (xs == null || ys == null || xs.length < 3 || ys.length < 3 || xs.length != ys.length) {
            return;
        }

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        ShaderProgram shader = RenderSystem.setShader(shaderKey);
        shader.getUniform("uSize").set(0f, 0f);
        shader.getUniform("uRadius").set(0f, 0f, 0f, 0f);
        shader.getUniform("uSmoothness").set(0f);

        float z = 0f;
        int colorInt = color.getRGB();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        
        // Calculate center point
        float centerX = 0f, centerY = 0f;
        for (int i = 0; i < xs.length; i++) {
            centerX += xs[i];
            centerY += ys[i];
        }
        centerX /= xs.length;
        centerY /= ys.length;
        
        // Add center vertex first for triangle fan
        builder.vertex(matrix4f, centerX, centerY, z).color(colorInt);
        
        // Add all polygon vertices
        for (int i = 0; i < xs.length; i++) {
            builder.vertex(matrix4f, xs[i], ys[i], z).color(colorInt);
        }
        
        // Close the fan by repeating first vertex
        builder.vertex(matrix4f, xs[0], ys[0], z).color(colorInt);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
