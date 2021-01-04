package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Cape;
import org.geysermc.connector.skin.resource.types.CapeType;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.SkinUtils;
import org.geysermc.connector.utils.WebUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class StdUrlCapeLoader implements ResourceLoader<Cape, Void> {
    @Override
    public CompletableFuture<Cape> loadAsync(@NonNull ResourceDescriptor<Cape, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CapeType capeType = CapeType.fromUri(descriptor.getUri());
                String capeId = capeType.getCapeIdFor(descriptor.getUri());
                return getCape(descriptor.getUri(), capeId);
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Cape> loadSync(@NonNull ResourceDescriptor<Cape, Void> descriptor) throws ResourceLoadFailureException {
        try {
            CapeType capeType = CapeType.fromUri(descriptor.getUri());
            String capeId = capeType.getCapeIdFor(descriptor.getUri());
            return CompletableFuture.completedFuture(getCape(descriptor.getUri(), capeId));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private Cape getCape(URI capeUri, String capeId) throws IOException {
        String capeUrl = capeUri.toURL().toString();
        BufferedImage capeImage = SkinUtils.scaleToWidth(WebUtils.getImage(capeUrl), 64, 32);
        byte[] capeData = SkinUtils.bufferedImageToImageData(capeImage);
        int width = capeImage.getWidth();
        int height = capeImage.getHeight();
        capeImage.flush();
        return Cape.builder()
                .resourceUri(capeUri)
                .capeId(capeId)
                .capeData(TextureData.of(capeData, width, height))
                .build();
    }
}
