package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.SkinGeometry;
import org.geysermc.connector.skin.resource.types.SkinGeometryType;
import org.geysermc.connector.utils.FileUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class InternalSkinGeometryLoader implements ResourceLoader<SkinGeometry, Void> {

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
    public CompletableFuture<SkinGeometry> loadSync(@NonNull ResourceDescriptor<SkinGeometry, Void> descriptor) throws IOException {
        try {
            return CompletableFuture.completedFuture(getGeometry(descriptor.getUri()));
        } catch (Throwable e) {

            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private SkinGeometry getGeometry(@NonNull URI uri) {
        SkinGeometryType skinGeometryTypes = SkinGeometryType.fromUri(uri);
        if (skinGeometryTypes != null) {
            String geometryName = uri.getSchemeSpecificPart();
            switch (skinGeometryTypes) {
                case LEGACY:
                case LEGACY_SLIM:
                    return SkinGeometry.builder()
                            .resourceUri(uri)
                            .name(geometryName)
                            .data("")
                            .build();
                case EARS:
                case EARS_SLIM:
                case CUSTOM_SKULL:
                    return SkinGeometry.builder()
                            .resourceUri(uri)
                            .name(geometryName)
                            .data(new String(FileUtils.readAllBytes(FileUtils.getResource("bedrock/skin/"+geometryName+".json")), StandardCharsets.UTF_8))
                            .build();
            }
        }

        throw new IllegalArgumentException("Unknown skin geometry uri " + uri.toString());
    }
}
