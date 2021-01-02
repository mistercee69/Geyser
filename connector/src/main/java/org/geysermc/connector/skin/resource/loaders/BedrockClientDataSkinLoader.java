package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.PlayerSkin;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.UUIDUtils;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.geysermc.connector.skin.resource.types.PlayerSkin.*;


public class BedrockClientDataSkinLoader implements ResourceLoader<PlayerSkin, Void> {

    // URI form bedrockClientSkin:UUID

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

    private PlayerSkin getPlayerSkin(URI uri) throws IOException {
        UUID playerUuid = UUID.fromString(UUIDUtils.toDashedUUID(uri.getSchemeSpecificPart()));
        GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerUuid);
        BedrockClientData clientData = session.getClientData();

        PlayerSkinBuilder skinBuilder = builder();
        skinBuilder
                .resourceUri(uri)
                .skinId(clientData.getSkinId())
                .skinData(TextureData.of(
                        clientData.getSkinData(),
                        clientData.getSkinImageWidth(),
                        clientData.getSkinImageHeight()));

        if (true /*configOption*/) {
            if (clientData.getAnimations() != null) {
                skinBuilder.animations(clientData.getAnimations());
            }
            skinBuilder
                    .animationData(clientData.getSkinAnimationData())
                    .premium(clientData.isPremiumSkin())
                    .persona(clientData.isPersonaSkin())
                    .capeOnClassic(clientData.isCapeOnClassicSkin())
                    .armSize(clientData.getArmSize())
                    .skinColor(clientData.getSkinColor());

            if (clientData.getPersonaSkinPieces() != null) {
                skinBuilder.personaPieces(clientData.getPersonaSkinPieces());
            }
            if (clientData.getPersonaTintColors() != null) {
                skinBuilder.personaTintColors(clientData.getPersonaTintColors());
            }
        }

        return skinBuilder.build();
    }
}