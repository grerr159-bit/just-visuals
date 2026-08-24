package dev.client.api.nullcry.render.core.msdf.core;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.core.msdf.FontData;
import dev.client.api.nullcry.render.core.msdf.FontData.AtlasData;
import dev.client.api.nullcry.render.core.msdf.FontData.MetricsData;
import dev.client.api.nullcry.render.core.providers.ResourceProvider;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class MsdfFont {
    @Getter private final String name;private final AbstractTexture texture;
    @Getter private final AtlasData atlas;
    @Getter private final MetricsData metrics;
    private final Map<Integer, MsdfGlyph> glyphs;
    private final Map<Integer, Map<Integer, Float>> kernings;


    private MsdfFont(String name, AbstractTexture texture, AtlasData atlas, MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
        this.name = name;
        this.texture = texture;
        this.atlas = atlas;
        this.metrics = metrics;
        this.glyphs = glyphs;
        this.kernings = kernings;
    }

    public int getTextureId() {
        return this.texture.getGlId();
    }

    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color) {
        int prevChar = -1;
        float startX = x;

        for (int i = 0; i < text.length(); i++) {
            char currentChar = text.charAt(i);

            if (currentChar == '\n') {
                x = startX;
                y += this.getLineAdvance(prevChar, size);
                prevChar = -1;
                continue;
            }

            if ((currentChar == '&' || currentChar == '§') && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                if (code == 'l') {
                    thickness += 1f;
                    i++;
                    continue;
                }
                Integer newColor = Formatting.byCode(code) != null ? Formatting.byCode(code).getColorValue() : null;
                if (newColor != null) {
                    color = newColor | ColorUtils.rgb(0, 0, 0);
                    i++;
                    continue;
                }
            }

            MsdfGlyph glyph = this.glyphs.get((int) currentChar);

            if (glyph == null) continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                x += kerning.getOrDefault((int) currentChar, 0.0f) * size;
            }

            x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
            prevChar = currentChar;
        }
    }

    // Formatted
    public float getWidth(Text text, float size) {
        OrderedText ordered = text.asOrderedText();
        final float basePxRange = 0.05f;
        final float spacing = 0f;

        final float[] currentLineWidth = {0f};
        final float[] maxWidth = {0f};
        final int[] prev = {-1};

        ordered.accept((idx, style, cp) -> {
            if (cp == '\n') {
                maxWidth[0] = Math.max(maxWidth[0], currentLineWidth[0]);
                currentLineWidth[0] = 0f;
                prev[0] = -1;
                return true;
            }

            MsdfGlyph g = glyphs.get(cp);
            if (g != null) {
                Map<Integer, Float> kern = kernings.get(prev[0]);
                if (kern != null) {
                    currentLineWidth[0] += kern.getOrDefault(cp, 0.0f) * size;
                }

                float pxRange = basePxRange + (style.isBold() ? 1f : 0f);
                currentLineWidth[0] += g.getWidth(size) + pxRange + spacing;
                prev[0] = cp;
            } else {
                prev[0] = cp;
            }
            return true;
        });

        return Math.max(maxWidth[0], currentLineWidth[0]);
    }

    public float getHeight(Text text, float size) {
        return getHeight(text.getString(), size);
    }

    public float getWidth(String text, float size) {
        return Arrays.stream(text.split("\n"))
                .map(line -> calculateWidth(line, size, 0.05f, 0f))
                .max(Float::compareTo)
                .orElse(0.0f);
    }

    public float getWidth(String text, float size, float thickness, float spacing) {
        return Arrays.stream(text.split("\n"))
                .map(line -> calculateWidth(line, size, (thickness + 0f * 0.5f) * 0.5f * size, spacing))
                .max(Float::compareTo)
                .orElse(0.0f);
    }

    public float getWidth(String text, float size, float thickness, float outlineThickness, float spacing) {
        return Arrays.stream(text.split("\n"))
                .map(line -> calculateWidth(line, size, (thickness + outlineThickness * 0.5f) * 0.5f * size, spacing))
                .max(Float::compareTo)
                .orElse(0.0f);
    }

    private float calculateWidth(String text, float size, float thickness, float spacing) {
        final float basePxRange = thickness;
        float curPxRange = basePxRange;

        float width = 0f;
        int prevCp = -1;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);

            if (cp == '\n') break;

            if (cp == '§' || cp == '&') {
                int j = i + Character.charCount(cp);
                if (j < text.length()) {
                    char code = Character.toLowerCase(text.charAt(j));
                    if (code == 'l') {
                        curPxRange += 1f;
                    } else if (code == 'r') {
                        curPxRange = basePxRange;
                    }
                    i = j + 1;
                    continue;
                } else {
                    i = j;
                    continue;
                }
            }

            MsdfGlyph glyph = this.glyphs.get(cp);
            if (glyph != null) {
                Map<Integer, Float> kern = this.kernings.get(prevCp);
                if (kern != null) {
                    width += kern.getOrDefault(cp, 0.0f) * size;
                }
                width += glyph.getWidth(size) + curPxRange + spacing;
                prevCp = cp;
            } else {
                prevCp = cp;
            }

            i += Character.charCount(cp);
        }

        return width;
    }

    public float getLineAdvance(float size) {
        return this.getLineAdvance(-1, size);
    }

    private float getLineAdvance(int prevChar, float size) {
        MsdfGlyph glyph = this.glyphs.get(prevChar);
        if (glyph == null) {
            glyph = this.glyphs.get((int) ' ');
        }
        if (glyph != null) {
            return glyph.getHeight(size) + 1.5f;
        }
        return (this.metrics != null ? this.metrics.lineHeight() : 1.0f) * size;
    }

    public float getHeight(String text, float size) {
        float height = 0;

        for (int i = 0; i < text.length(); i++) {
            int _char = text.charAt(i);

            if ((_char == '&' || _char == '§') && i + 1 < text.length()) {
                i += 1;
                continue;
            }

            if (_char == '\n') {
                height += this.getLineAdvance(size);
                continue;
            }

            MsdfGlyph glyph = this.glyphs.get(_char);
            if (glyph != null) {
                height = Math.max(glyph.getHeight(size), height);
            } else {
            }
        }

        return height;
    }

    public static MsdfFont.Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name = "?";
        private Identifier dataIdentifer;
        private Identifier atlasIdentifier;

        private Builder() {
        }

        public MsdfFont.Builder name(String name) {
            this.name = name;
            return this;
        }

        public MsdfFont.Builder data(String dataFileName) {
            this.dataIdentifer = Identifier.of("just", "fonts/" + dataFileName + ".json");
            return this;
        }

        public MsdfFont.Builder atlas(String atlasFileName) {
            this.atlasIdentifier = Identifier.of("just", "fonts/" + atlasFileName + ".png");
            return this;
        }

        public MsdfFont build() {
            FontData data = ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
            AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(this.atlasIdentifier);

            if (data == null) {
                throw new RuntimeException("Failed to read font data file: " + this.dataIdentifer.toString() +
                        "; Are you sure this is json file? Try to check the correctness of its syntax.");
            }

            RenderSystem.recordRenderCall(() -> texture.setFilter(true, false));

            float aWidth = data.atlas().width();
            float aHeight = data.atlas().height();
            Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream()
                    .collect(Collectors.toMap(
                            glyphData -> glyphData.unicode(),
                            glyphData -> new MsdfGlyph(glyphData, aWidth, aHeight),
                            (g1, g2) -> g1
                    ));

            Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
            data.kernings().forEach((kerning) -> {
                Map<Integer, Float> map = kernings.computeIfAbsent(kerning.leftChar(), k -> new HashMap<>());

                map.put(kerning.rightChar(), kerning.advance());
            });

            return new MsdfFont(this.name, texture, data.atlas(), data.metrics(), glyphs, kernings);
        }
    }
}