package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.PlayerSkin;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.SkinUtils;
import org.geysermc.connector.utils.WebUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class StdUrlSkinLoader implements ResourceLoader<PlayerSkin, Void> {
    @Override
    public CompletableFuture<PlayerSkin> loadAsync(@NonNull ResourceDescriptor<PlayerSkin, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerSkin(descriptor.getUri());
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<PlayerSkin> loadSync(@NonNull ResourceDescriptor<PlayerSkin, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getPlayerSkin(descriptor.getUri()));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private PlayerSkin getPlayerSkin(URI skinUri) throws IOException {
        String skinUrl = skinUri.toURL().toString();
        BufferedImage skinImage = WebUtils.getImage(skinUrl);
        skinImage.flush();

        return PlayerSkin.builder()
                .resourceUri(skinUri)
                .skinData(TextureData.of(
                        SkinUtils.bufferedImageToImageData(skinImage),
                        skinImage.getWidth(),
                        skinImage.getHeight()))
                .build();
    }
}
