package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.SkinGeometry;
import org.geysermc.connector.utils.UUIDUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BedrockClientDataGeometryLoader implements ResourceLoader<SkinGeometry, Void> {

    // URI form bedrockClientGeom:UUID <-- player's UUID

    @Override
    public CompletableFuture<SkinGeometry> loadAsync(@NonNull ResourceDescriptor<SkinGeometry, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getGeometry(descriptor.getUri());
            } catch (Throwable e) {
                e.printStackTrace();
                throw new ResourceLoadFailureException(e);
            }
        });
    }

    @Override
    public CompletableFuture<SkinGeometry> loadSync(@NonNull ResourceDescriptor<SkinGeometry, Void> descriptor) {
        try {
            return CompletableFuture.completedFuture(getGeometry(descriptor.getUri()));
        } catch (Throwable e) {
            e.printStackTrace();
            return CompletableFuture.supplyAsync(() -> { throw new ResourceLoadFailureException(e); });
        }
    }

    private SkinGeometry getGeometry(@NonNull URI uri) {
        UUID playerUuid = UUID.fromString(UUIDUtils.toDashedUUID(uri.getSchemeSpecificPart()));
        GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerUuid);
        byte[] geometryNameBytes = Base64.getDecoder().decode(session.getClientData().getGeometryName().getBytes(StandardCharsets.UTF_8));
        byte[] geometryBytes = Base64.getDecoder().decode(session.getClientData().getGeometryData().getBytes(StandardCharsets.UTF_8));

        return SkinGeometry.builder()
                .resourceUri(uri)
                .name(new String(geometryNameBytes))
                .resourcePatch(new String(geometryNameBytes))
                .data(new String(geometryBytes))
                .build();
    }
}