package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Skin;
import org.geysermc.connector.skin.resource.types.SkinType;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.SkinUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class InternalSkinLoader implements ResourceLoader<Skin, Void> {
    @Override
    public CompletableFuture<Skin> loadAsync(@NonNull ResourceDescriptor<Skin, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerSkin(descriptor.getUri());
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Skin> loadSync(@NonNull ResourceDescriptor<Skin, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getPlayerSkin(descriptor.getUri()));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private Skin getPlayerSkin(URI skinUri) throws IOException {
        SkinType skinType = SkinType.fromUri(skinUri);
        String skinId = skinUri.toString();
        if (skinType == SkinType.DEFAULT_ALEX) {
            skinId = "alex";
        } else if (skinType == SkinType.DEFAULT_STEVE) {
            skinId = "steve";
        }

        String location = skinUri.getSchemeSpecificPart();
        BufferedImage skinImage = ImageIO.read(FileUtils.getResource(location));
        byte[] data = SkinUtils.bufferedImageToImageData(skinImage);
        int width = skinImage.getWidth();
        int height = skinImage.getHeight();
        skinImage.flush();

        return Skin.builder()
                .resourceUri(skinUri)
                .skinId(skinId)
                .skinData(TextureData.of(
                        data,
                        width,
                        height))
                .build();
    }
}