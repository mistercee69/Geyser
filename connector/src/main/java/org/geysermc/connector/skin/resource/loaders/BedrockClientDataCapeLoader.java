package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Cape;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.UUIDUtils;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BedrockClientDataCapeLoader implements ResourceLoader<Cape, Void> {

    // URI form bedrockClientCape:UUID

    @Override
    public CompletableFuture<Cape> loadAsync(@NonNull ResourceDescriptor<Cape, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getCape(descriptor.getUri());
            } catch (Throwable e) {
                e.printStackTrace();
                GeyserConnector.getInstance().getLogger().debug("Problem getting cape: " + e.getMessage());
                throw new ResourceLoadFailureException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Cape> loadSync(@NonNull ResourceDescriptor<Cape, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getCape(descriptor.getUri()));
        } catch (Throwable e) {
            e.printStackTrace();
            GeyserConnector.getInstance().getLogger().debug("Problem getting cape: " + e.getMessage());
            return CompletableFuture.supplyAsync(() -> { throw new ResourceLoadFailureException(e); });
        }
    }

    private Cape getCape(URI uri) throws IOException {
        UUID playerUuid = UUID.fromString(UUIDUtils.toDashedUUID(uri.getSchemeSpecificPart()));
        GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerUuid);
        BedrockClientData clientData = session.getClientData();

        return Cape.builder()
                .resourceUri(uri)
                .capeId(clientData.getCapeId())
                .capeData(TextureData.of(
                        clientData.getCapeData(),
                        clientData.getCapeImageWidth(),
                        clientData.getCapeImageHeight()))
                .build();

    }
}