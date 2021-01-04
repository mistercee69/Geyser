package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.SkinGeometry;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.UUIDUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BedrockClientDataGeometryLoader implements ResourceLoader<SkinGeometry, Void> {
    public static final boolean ALLOW_BEDROCK_CHARACTER_CREATOR_SKINS = GeyserConnector.getInstance().getConfig().isAllowBedrockCharacterCreatorSkins();

    // URI form bedrockClientGeom:UUID <-- player's UUID

    @Override
    public CompletableFuture<SkinGeometry> loadAsync(@NonNull ResourceDescriptor<SkinGeometry, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getGeometry(descriptor.getUri());
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<SkinGeometry> loadSync(@NonNull ResourceDescriptor<SkinGeometry, Void> descriptor) {
        try {
            return CompletableFuture.completedFuture(getGeometry(descriptor.getUri()));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private SkinGeometry getGeometry(@NonNull URI uri) {
        UUID playerUuid = UUID.fromString(UUIDUtils.toDashedUUID(uri.getSchemeSpecificPart()));
        GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerUuid);
        BedrockClientData clientData = session.getClientData();

        if (!ALLOW_BEDROCK_CHARACTER_CREATOR_SKINS) {
            if (clientData.getSkinData().length > (128 * 128 * 4) || clientData.isPersonaSkin())   {
                throw new ResourceLoadFailureException(LanguageUtils.getLocaleStringLog("geyser.skin.bedrock.fail", playerUuid));
            }
        }

        byte[] geometryNameBytes = Base64.getDecoder().decode(clientData.getGeometryName().getBytes(StandardCharsets.UTF_8));
        byte[] geometryBytes = Base64.getDecoder().decode(clientData.getGeometryData().getBytes(StandardCharsets.UTF_8));

        return SkinGeometry.builder()
                .resourceUri(uri)
                .resourcePatch(new String(geometryNameBytes))
                .data(new String(geometryBytes))
                .build();
    }
}