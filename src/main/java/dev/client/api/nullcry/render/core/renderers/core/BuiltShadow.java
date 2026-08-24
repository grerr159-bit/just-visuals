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
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

import static dev.client.api.nullcry.render.core.providers.ColorProvider.colorToArray;

public record BuiltShadow(
        SizeState size,
        QuadColorState color,
        QuadRadiusState radiusState,
        float softness,
        float shadowRadius
) implements IRenderer {

    private final static ShaderProgramKey SHADOW_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("shadow"),
            VertexFormats.POSITION_COLOR, Defines.EMPTY);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float width = this.size.width(), height = this.size.height();
        ShaderProgram shader = RenderSystem.setShader(SHADOW_SHADER_KEY);
        shader.getUniform("Size").set(width + shadowRadius * 2f, height + shadowRadius * 2f);
        shader.getUniform("Softness").set(softness);
        shader.getUniform("Radius").set(radiusState.radius1(), radiusState.radius2(), radiusState.radius3(), radiusState.radius4());
        shader.getUniform("color1").set(colorToArray(color.color1()));
        shader.getUniform("color2").set(colorToArray(color.color2()));
        shader.getUniform("color3").set(colorToArray(color.color3()));
        shader.getUniform("color4").set(colorToArray(color.color4()));
        shader.getUniform("ShadowRadius").set(shadowRadius);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        quad(builder, matrix, x - shadowRadius, y - shadowRadius, z, width + shadowRadius * 2f, height + shadowRadius * 2f, color);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
