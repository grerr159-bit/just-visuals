package dev.client.api.nullcry.render.core.renderers.core;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.render.core.msdf.core.MsdfFont;
import dev.client.api.nullcry.render.core.providers.ColorProvider;
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

import java.util.List;

public record BuiltText(
        MsdfFont font,
        String text,
        float size,
        float thickness,
        int color,
        float Smoothness,
        float spacing,
        int outlineColor,
        float outlineThickness,
        List<Run> runs
) implements IRenderer {

    private static final ShaderProgramKey MSDF_FONT_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("msdf_font"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        if (this.font == null) {
            return;
        }

        if (!hasRenderableGlyphs()) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        RenderSystem.setShaderTexture(0, this.font.getTextureId());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        boolean outlineEnabled = (this.outlineThickness > 0.0f);
        ShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER_KEY);
        shader.getUniform("Range").set(this.font.getAtlas().range());
        shader.getUniform("Thickness").set(this.thickness);
        shader.getUniform("Smoothness").set(this.Smoothness);
        shader.getUniform("Outline").set(outlineEnabled ? 1 : 0);

        if (outlineEnabled) {
            shader.getUniform("OutlineThickness").set(this.outlineThickness);
            float[] oc = ColorProvider.normalize(this.outlineColor);
            shader.getUniform("OutlineColor").set(oc[0], oc[1], oc[2], oc[3]);
        }

        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float penX = x;
        float penY = y + this.font.getMetrics().baselineHeight() * this.size;
        float pxRange = (this.thickness + this.outlineThickness * 0.5f) * 0.5f * this.size;

        if (this.runs != null && !this.runs.isEmpty()) {
            float lineAdvance = this.font.getLineAdvance(this.size);
            for (Run run : this.runs) {
                if (run == null || run.s == null) continue;
                String runText = run.s;
                int offset = 0;
                while (offset <= runText.length()) {
                    int newlineIndex = runText.indexOf('\n', offset);
                    String segment = (newlineIndex == -1) ? runText.substring(offset) : runText.substring(offset, newlineIndex);
                    if (!segment.isEmpty()) {
                        this.font.applyGlyphs(matrix, builder, segment, this.size, pxRange, this.spacing, penX, penY, z, run.rgba);
                        penX += this.font.getWidth(segment, this.size, this.thickness, this.outlineThickness, this.spacing);
                    }

                    if (newlineIndex == -1) {
                        break;
                    }

                    penX = x;
                    penY += lineAdvance;
                    offset = newlineIndex + 1;
                }
            }
        } else if (this.text != null) {
            this.font.applyGlyphs(matrix, builder, this.text, this.size, pxRange, this.spacing, penX, penY, z, this.color);
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private boolean hasRenderableGlyphs() {
        if (this.text != null) {
            return !this.text.isEmpty();
        }

        if (this.runs != null) {
            for (Run run : this.runs) {
                if (run == null) continue;
                String value = run.s();
                if (value == null || value.isEmpty()) continue;
                boolean hasGlyph = value.chars().anyMatch(ch -> ch != '\n');
                if (hasGlyph) {
                    return true;
                }
            }
        }

        return false;
    }

    public record Run(String s, int rgba) {
    }

    public float measureWidth() {
        if (this.font == null) return 0f;

        if (this.runs != null && !this.runs.isEmpty()) {
            float currentLineWidth = 0f;
            float maxWidth = 0f;

            for (Run run : this.runs) {
                if (run == null || run.s == null || run.s.isEmpty()) continue;

                String runText = run.s;
                int offset = 0;
                while (offset <= runText.length()) {
                    int newlineIndex = runText.indexOf('\n', offset);
                    String segment = (newlineIndex == -1)
                            ? runText.substring(offset)
                            : runText.substring(offset, newlineIndex);

                    if (!segment.isEmpty()) {
                        currentLineWidth += this.font.getWidth(segment, this.size, this.thickness, this.outlineThickness, this.spacing);
                    }

                    if (newlineIndex == -1) {
                        break;
                    }

                    maxWidth = Math.max(maxWidth, currentLineWidth);
                    currentLineWidth = 0f;
                    offset = newlineIndex + 1;
                }
            }

            return Math.max(maxWidth, currentLineWidth);
        }

        if (this.text != null) {
            return this.font.getWidth(this.text, this.size, this.thickness, this.outlineThickness, this.spacing);
        }

        return 0f;
    }
}
