package org.geysermc.connector.skin.resource.loaders;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.property.PropertyException;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Cape;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.SkinUtils;
import org.geysermc.connector.utils.WebUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class JavaGameProfileCapeLoader implements ResourceLoader<Cape, GameProfileSkinParams> {

    // URI form javaCape:UUID
    
    @Override
    public CompletableFuture<Cape> loadAsync(@NonNull ResourceDescriptor<Cape, GameProfileSkinParams> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getCape(descriptor);
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Cape> loadSync(@NonNull ResourceDescriptor<Cape, GameProfileSkinParams> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getCape(descriptor));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private Cape getCape(@NonNull ResourceDescriptor<Cape, GameProfileSkinParams> descriptor) throws IOException, PropertyException {
        GameProfile gameProfile = getGameProfile(descriptor);
        Map<GameProfile.TextureType, GameProfile.Texture> textures = gameProfile.getTextures(false);

        if (hasCapeUri(textures)) {
            URI capeUri = getCapeUri(textures);
            BufferedImage skinImage = WebUtils.getImage(capeUri.toString());

            return Cape.builder()
                    .resourceUri(descriptor.getUri())
                    .capeId(capeUri.toString())
                    .capeData(TextureData.of(
                            SkinUtils.bufferedImageToImageData(skinImage),
                            skinImage.getWidth(),
                            skinImage.getHeight()))
                    .build();
        }
        throw new ResourceLoadFailureException("GameProfile is missing cape uri");
    }

    private UUID getUuidFromUri(URI uri) {
        return UUID.fromString(uri.getSchemeSpecificPart());
    }

    private GameProfile getGameProfile(@NonNull ResourceDescriptor<Cape, GameProfileSkinParams> descriptor) {
        if (descriptor.getParams().getGameProfile() != null) {
            return descriptor.getParams().getGameProfile();
        }

        UUID playerUuid = getUuidFromUri(descriptor.getUri());
        GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerUuid);
        return session.getPlayerEntity().getProfile();
    }

    private boolean hasCapeUri(Map<GameProfile.TextureType, GameProfile.Texture> textureMap) {
        return textureMap.containsKey(GameProfile.TextureType.CAPE);
    }

    private URI getCapeUri(Map<GameProfile.TextureType, GameProfile.Texture> textureMap) {
        if (textureMap.containsKey(GameProfile.TextureType.CAPE)) {
            return URI.create(textureMap.get(GameProfile.TextureType.CAPE).getURL().replace("http://", "https://"));
        }
        return null;
    }
}