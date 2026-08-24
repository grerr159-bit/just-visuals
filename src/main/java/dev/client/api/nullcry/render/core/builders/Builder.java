package dev.client.api.nullcry.render.core.builders;

import com.google.common.base.Suppliers;
import dev.client.api.nullcry.render.core.builders.core.*;
import dev.client.api.nullcry.render.core.msdf.core.MsdfFont;

import java.util.function.Supplier;

public final class Builder {
    public static final RectangleBuilder RECTANGLE_BUILDER = new RectangleBuilder();
    public static final GradientBuilder GRADIENT_BUILDER = new GradientBuilder();
    public static final OutlineBuilder BORDER_BUILDER = new OutlineBuilder();
    public static final TextureBuilder TEXTURE_BUILDER = new TextureBuilder();
    public static final HeadTextureBuilder HEAD_TEXTURE_BUILDER = new HeadTextureBuilder();
    public static final ShadowBuilder SHADOW_BUILDER = new ShadowBuilder();
    public static final GlowBuilder GLOW_BUILDER = new GlowBuilder();
    public static final BlurBuilder BLUR_BUILDER = new BlurBuilder();
    public static final TextBuilder TEXT_BUILDER = new TextBuilder();
    public static final Supplier<MsdfFont> INTER = Suppliers.memoize(() -> MsdfFont.builder().atlas("inter").data("inter").name("inter").build());
    public static final Supplier<MsdfFont> ICONS = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons").data("icons").name("icons").build());
    public static final Supplier<MsdfFont> OTHER_ICONS = Suppliers.memoize(() -> MsdfFont.builder().atlas("other_icons").data("other_icons").name("other_icons").build());
}