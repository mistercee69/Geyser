package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Skull;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.SkinUtils;
import org.geysermc.connector.utils.WebUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class StdUrlSkullLoader implements ResourceLoader<Skull, Void> {
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
        String skullUrl = skullUri.toURL().toString();
        BufferedImage skullImage = WebUtils.getImage(skullUrl);
        skullImage.flush();

        return Skull.builder()
                .resourceUri(skullUri)
                // Prevents https://cdn.discordapp.com/attachments/613194828359925800/779458146191147008/unknown.png
                .skullId(skullUrl.toString()+"_skull")
                .skullData(TextureData.of(
                        SkinUtils.bufferedImageToImageData(skullImage),
                        skullImage.getWidth(),
                        skullImage.getHeight()))
                .build();
    }
}
