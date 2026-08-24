package dev.client.api.nullcry.render.core.renderers.core;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.render.core.builders.states.QuadColorState;
import dev.client.api.nullcry.render.core.builders.states.QuadRadiusState;
import dev.client.api.nullcry.render.core.builders.states.SizeState;
import dev.client.api.nullcry.render.core.providers.ResourceProvider;
import dev.client.api.nullcry.render.core.renderers.IRenderer;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import static dev.client.api.nullcry.render.core.providers.ColorProvider.colorToArray;

public record BuiltGradient(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness
) implements IRenderer {

    private static final ShaderProgramKey GRADIENT_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("gradient"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float width = this.size.width();
        float height = this.size.height();

        ShaderProgram shader = RenderSystem.setShader(GRADIENT_KEY);

        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(radius.radius1(), radius.radius2(), radius.radius3(), radius.radius4());
        shader.getUniform("Smoothness").set(smoothness);

        shader.getUniform("color1").set(colorToArray(color.color1()));
        shader.getUniform("color2").set(colorToArray(color.color2()));
        shader.getUniform("color3").set(colorToArray(color.color3()));
        shader.getUniform("color4").set(colorToArray(color.color4()));

        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        builder.vertex(matrix, x, y, z).color(color.color1());
        builder.vertex(matrix, x, y + height, z).color(color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(color.color3());
        builder.vertex(matrix, x + width, y, z).color(color.color4());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}