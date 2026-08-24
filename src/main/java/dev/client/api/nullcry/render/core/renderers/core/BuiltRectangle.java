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

public record BuiltRectangle(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float Smoothness
    ) implements IRenderer {

    private static final ShaderProgramKey RECTANGLE_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("rectangle"),
            VertexFormats.POSITION_COLOR, Defines.EMPTY);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float width = this.size.width(), height = this.size.height();

        ShaderProgram shaderProgram = RenderSystem.setShader(RECTANGLE_KEY);

        shaderProgram.getUniform("Size").set(width, height);
        shaderProgram.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
        shaderProgram.getUniform("Smoothness").set(this.Smoothness);
        shaderProgram.getUniform("color1").set(colorToArray(color.color1()));
        shaderProgram.getUniform("color2").set(colorToArray(color.color2()));
        shaderProgram.getUniform("color3").set(colorToArray(color.color3()));
        shaderProgram.getUniform("color4").set(colorToArray(color.color4()));

        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x, y, z).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).color(this.color.color4());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}