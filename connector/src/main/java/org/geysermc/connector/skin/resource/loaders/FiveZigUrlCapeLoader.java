package org.geysermc.connector.skin.resource.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Cape;
import org.geysermc.connector.skin.resource.types.CapeType;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.SkinUtils;
import org.geysermc.connector.utils.WebUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class FiveZigUrlCapeLoader implements ResourceLoader<Cape, Void> {
    @Override
    public CompletableFuture<Cape> loadAsync(@NonNull ResourceDescriptor<Cape, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {

                return getCape(descriptor.getUri(), CapeType.FIVEZIG.getCapeIdFor(descriptor.getUri()));
            } catch (Throwable e) {
                e.printStackTrace();
                throw new ResourceLoadFailureException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Cape> loadSync(@NonNull ResourceDescriptor<Cape, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getCape(descriptor.getUri(), CapeType.FIVEZIG.getCapeIdFor(descriptor.getUri())));
        } catch (Throwable e) {
            e.printStackTrace();
            return CompletableFuture.supplyAsync(() -> { throw new ResourceLoadFailureException(e); });
        }
    }

    private Cape getCape(URI capeUri, String capeId) throws IOException {
        String capeUrl = capeUri.toURL().toString();
        BufferedImage capeImage = SkinUtils.scaleToWidth(getImage(capeUrl), 64, 32);
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

    private BufferedImage getImage(String capeUrl) throws IOException {
        JsonNode element = WebUtils.getJson(capeUrl);
        if (element != null && element.isObject()) {
            JsonNode capeElement = element.get("d");
            if (capeElement != null && !capeElement.isNull()) {
                return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(capeElement.textValue())));
            }
        }
        throw new ResourceLoadFailureException("Unexpected response from Fivezig. Got " + ((element != null) ? element.toString() : "(null)"));
    }
}