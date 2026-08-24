package dev.client.api.nullcry.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientTexture {
    private static final Set<Identifier> REGISTERED = ConcurrentHashMap.newKeySet();

    private ClientTexture() {}

    public static AbstractTexture of(String path) {
        return getTexture(Identifier.of("just", path));
    }

    public static Identifier id(String path) {
        Identifier identifier = Identifier.of("just", path);
        ensureRegistered(identifier);
        return identifier;
    }

    private static AbstractTexture getTexture(Identifier identifier) {
        ensureRegistered(identifier);
        TextureManager manager = MinecraftClient.getInstance().getTextureManager();
        return manager.getTexture(identifier);
    }

    private static void ensureRegistered(Identifier identifier) {
        TextureManager manager = MinecraftClient.getInstance().getTextureManager();
        if (manager == null || identifier == null) return;

        if (REGISTERED.add(identifier)) {
            manager.registerTexture(identifier, new DirectResourceTexture(identifier));
        }
    }

    private static final class DirectResourceTexture extends ReloadableTexture {
        private final Identifier id;

        private DirectResourceTexture(@NotNull Identifier id) {
            super(id);
            this.id = id;
        }

        @Override
        public TextureContents loadContents(ResourceManager manager) throws IOException {
            try {
                return TextureContents.load(manager, this.id);
            } catch (FileNotFoundException notFound) {
                Identifier prefixed = this.id.withPrefixedPath("textures/");
                if (prefixed.equals(this.id)) {
                    throw notFound;
                }
                try {
                    return TextureContents.load(manager, prefixed);
                } catch (FileNotFoundException ignored) {
                    throw notFound;
                }
            }
        }

    }
}
