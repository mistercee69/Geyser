package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Ears;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.SkinUtils;
import org.geysermc.connector.utils.WebUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class StdUrlEarsLoader implements ResourceLoader<Ears, Void> {
    @Override
    public CompletableFuture<Ears> loadAsync(@NonNull ResourceDescriptor<Ears, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getEars(descriptor.getUri());
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Ears> loadSync(@NonNull ResourceDescriptor<Ears, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getEars(descriptor.getUri()));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private Ears getEars(URI earsUri) throws IOException {
        String earsUrl = earsUri.toURL().toString();
        BufferedImage earsImage = WebUtils.getImage(earsUrl);
        byte[] earsData = SkinUtils.bufferedImageToImageData(earsImage);
        int width = earsImage.getWidth();
        int height = earsImage.getHeight();
        earsImage.flush();

        return Ears.builder()
                .resourceUri(earsUri)
                .earsData(TextureData.of(earsData, width, height))
                .build();
    }
}