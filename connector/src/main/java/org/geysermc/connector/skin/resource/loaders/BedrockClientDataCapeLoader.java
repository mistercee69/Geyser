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

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BedrockClientDataCapeLoader implements ResourceLoader<Cape, Void> {
    public static final boolean ALLOW_BEDROCK_CHARACTER_CREATOR_SKINS = GeyserConnector.getInstance().getConfig().isAllowBedrockCharacterCreatorSkins();

    // URI form bedrockClientCape:UUID

    @Override
    public CompletableFuture<Cape> loadAsync(@NonNull ResourceDescriptor<Cape, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getCape(descriptor.getUri());
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Cape> loadSync(@NonNull ResourceDescriptor<Cape, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getCape(descriptor.getUri()));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private Cape getCape(URI uri) {
        UUID playerUuid = UUID.fromString(UUIDUtils.toDashedUUID(uri.getSchemeSpecificPart()));
        GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerUuid);
        BedrockClientData clientData = session.getClientData();

        if (!ALLOW_BEDROCK_CHARACTER_CREATOR_SKINS && !clientData.isCapeOnClassicSkin()) {
            throw new ResourceLoadFailureException("No bedrock client cape available");
        }

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