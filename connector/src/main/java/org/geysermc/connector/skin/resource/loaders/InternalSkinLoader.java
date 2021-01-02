package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.PlayerSkin;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.SkinUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class InternalSkinLoader implements ResourceLoader<PlayerSkin, Void> {
    @Override
    public CompletableFuture<PlayerSkin> loadAsync(@NonNull ResourceDescriptor<PlayerSkin, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerSkin(descriptor.getUri());
            } catch (Throwable e) {
                e.printStackTrace();
                throw new ResourceLoadFailureException(e);
            }
        });
    }

    @Override
    public CompletableFuture<PlayerSkin> loadSync(@NonNull ResourceDescriptor<PlayerSkin, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getPlayerSkin(descriptor.getUri()));
        } catch (Throwable e) {
            e.printStackTrace();
            return CompletableFuture.supplyAsync(() -> { throw new ResourceLoadFailureException(e); });
        }
    }

    private PlayerSkin getPlayerSkin(URI skinUri) throws IOException {
        String location = skinUri.getSchemeSpecificPart();
        BufferedImage skinImage = ImageIO.read(FileUtils.getResource(location));
        byte[] data = SkinUtils.bufferedImageToImageData(skinImage);
        int width = skinImage.getWidth();
        int height = skinImage.getHeight();
        skinImage.flush();

        return PlayerSkin.builder()
                .resourceUri(skinUri)
                .skinData(TextureData.of(
                        data,
                        width,
                        height))
                .build();
    }
}