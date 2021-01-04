package org.geysermc.connector.skin.resource.loaders;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.property.PropertyException;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Skin;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.SkinUtils;
import org.geysermc.connector.utils.WebUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.github.steveice10.mc.auth.data.GameProfile.Texture;
import static com.github.steveice10.mc.auth.data.GameProfile.TextureType;

public class JavaGameProfileSkinLoader implements ResourceLoader<Skin, GameProfileSkinParams> {

    // URI form javaClientSkin:UUID

    @Override
    public CompletableFuture<Skin> loadAsync(@NonNull ResourceDescriptor<Skin, GameProfileSkinParams> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerSkin(descriptor);
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Skin> loadSync(@NonNull ResourceDescriptor<Skin, GameProfileSkinParams> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getPlayerSkin(descriptor));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private Skin getPlayerSkin(ResourceDescriptor<Skin, GameProfileSkinParams> descriptor) throws IOException, PropertyException {
        GameProfile gameProfile = getGameProfile(descriptor);
        Map<TextureType, Texture> textures = gameProfile.getTextures(false);

        if (hasSkinUri(textures)) {
            URI skinUri = getSkinUri(textures);
            BufferedImage skinImage = WebUtils.getImage(skinUri.toString());

            return Skin.builder()
                    .resourceUri(descriptor.getUri())
                    .skinId(skinUri.toString())
                    .skinData(TextureData.of(
                            SkinUtils.bufferedImageToImageData(skinImage),
                            skinImage.getWidth(),
                            skinImage.getHeight()))
                    .build();
        }
        throw new ResourceLoadFailureException("Game profile is missing skin uri");
    }

    private UUID getUuidFromUri(URI uri) {
        return UUID.fromString(uri.getSchemeSpecificPart());
    }

    private GameProfile getGameProfile(@NonNull ResourceDescriptor<Skin, GameProfileSkinParams> descriptor) {
        if (descriptor.getParams().getGameProfile() != null) {
            return descriptor.getParams().getGameProfile();
        }

        UUID playerUuid = getUuidFromUri(descriptor.getUri());
        GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerUuid);
        return session.getPlayerEntity().getProfile();
    }

    private boolean hasSkinUri(Map<TextureType, Texture> textureMap) {
        return textureMap.containsKey(TextureType.SKIN);
    }

    private URI getSkinUri(Map<TextureType, Texture> textureMap) {
        if (textureMap.containsKey(TextureType.SKIN)) {
            return URI.create(textureMap.get(TextureType.SKIN).getURL().replace("http://", "https://"));
        }
        return null;
    }

}