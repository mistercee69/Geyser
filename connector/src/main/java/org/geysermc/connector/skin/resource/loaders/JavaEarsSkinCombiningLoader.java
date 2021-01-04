package org.geysermc.connector.skin.resource.loaders;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.ResourceManager;
import org.geysermc.connector.skin.resource.types.*;
import org.geysermc.connector.utils.SkinUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class JavaEarsSkinCombiningLoader implements ResourceLoader<PlayerSkin, JavaEarsSkinParams> {
    @Override
    public CompletableFuture<PlayerSkin> loadAsync(@NonNull ResourceDescriptor<PlayerSkin, JavaEarsSkinParams> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerSkin(descriptor);
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<PlayerSkin> loadSync(@NonNull ResourceDescriptor<PlayerSkin, JavaEarsSkinParams> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getPlayerSkin(descriptor));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private PlayerSkin getPlayerSkin(@NonNull ResourceDescriptor<PlayerSkin, JavaEarsSkinParams> descriptor) throws IOException {
        ResourceDescriptor<PlayerSkin, ?> skinDescriptor = descriptor.getParams().getSkin();
        ResourceDescriptor<Ears, ?> earsDescriptor = descriptor.getParams().getEars();
        PlayerSkinType playerSkinType = PlayerSkinType.fromUri(skinDescriptor.getUri());
        EarsType earsType = EarsType.fromUri(earsDescriptor.getUri());
        if (playerSkinType == PlayerSkinType.JAVA_GAME_PROFILE && earsType != EarsType.NONE && earsType != EarsType.DEADMAU5) {
            PlayerSkin existingSkin = ResourceManager.get(skinDescriptor);
            Ears ears = ResourceManager.get(earsDescriptor);
            try {
                // Convert the ears data to a BufferedImage
                BufferedImage earsImage = SkinUtils.imageDataToBufferedImage(ears.getEarsData().getData(), ears.getEarsData().getWidth(), ears.getEarsData().getHeight());

                // Convert the skin data to a BufferedImage
                Preconditions.checkArgument(existingSkin.getSkinData().getWidth() == 64, "Applying ears to a skin wider than 64 (" + existingSkin.getSkinData().getWidth() + ")");
                BufferedImage skinImage = SkinUtils.imageDataToBufferedImage(existingSkin.getSkinData().getData(), existingSkin.getSkinData().getWidth(), existingSkin.getSkinData().getHeight());

                // Create a new image with the ears texture over it
                BufferedImage newSkin = new BufferedImage(skinImage.getWidth(), skinImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D) newSkin.getGraphics();
                g.drawImage(skinImage, 0, 0, null);
                g.drawImage(earsImage, 24, 0, null);

                // Turn the buffered image back into an array of bytes
                byte[] data = SkinUtils.bufferedImageToImageData(newSkin);
                int width = skinImage.getWidth();
                int height = skinImage.getHeight();
                skinImage.flush();

                // Create a new skin object with the new information
                return PlayerSkin.builder()
                        .resourceUri(descriptor.getUri())
                        .skinId(existingSkin.getSkinId())
                        .skinData(TextureData.of(data, width, height))
                        .build();
            } catch (Exception e) {
                throw new ResourceLoadFailureException("Unable to load ear skin", e);
            } // just ignore I guess
        }

        throw new ResourceLoadFailureException("Unable to load ear skin");
    }
}