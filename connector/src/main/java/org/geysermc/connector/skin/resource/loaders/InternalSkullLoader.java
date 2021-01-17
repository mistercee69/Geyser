package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Skull;
import org.geysermc.connector.skin.resource.types.SkullType;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.SkinUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class InternalSkullLoader implements ResourceLoader<Skull, Void> {
    @Override
    public CompletableFuture<Skull> loadAsync(@NonNull ResourceDescriptor<Skull, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerSkull(descriptor.getUri());
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Skull> loadSync(@NonNull ResourceDescriptor<Skull, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getPlayerSkull(descriptor.getUri()));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private Skull getPlayerSkull(URI skullUri) throws IOException {
        SkullType skullType = SkullType.fromUri(skullUri);
        String skullId = skullUri.toString();
        if (skullType == SkullType.DEFAULT_ALEX) {
            skullId = "alex";
        } else if (skullType == SkullType.DEFAULT_STEVE) {
            skullId = "steve";
        }

        String location = skullUri.getSchemeSpecificPart();
        BufferedImage skullImage = ImageIO.read(FileUtils.getResource(location));
        byte[] data = SkinUtils.bufferedImageToImageData(skullImage);
        int width = skullImage.getWidth();
        int height = skullImage.getHeight();
        skullImage.flush();

        return Skull.builder()
                .resourceUri(skullUri)
                .skullId(skullId)
                .skullData(TextureData.of(
                        data,
                        width,
                        height))
                .build();
    }
}