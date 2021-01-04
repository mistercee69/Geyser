package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Cape;
import org.geysermc.connector.skin.resource.types.Ears;
import org.geysermc.connector.skin.resource.types.GameProfileData;
import org.geysermc.connector.skin.resource.types.TextureData;

import java.util.concurrent.CompletableFuture;

public class NoopLoader implements ResourceLoader<Object, Object> {
    @Override
    public CompletableFuture<Object> loadAsync(@NonNull ResourceDescriptor<Object, Object> resourceDescriptor) {
        return CompletableFuture.completedFuture(noopResource(resourceDescriptor));
    }

    @Override
    public CompletableFuture<Object> loadSync(@NonNull ResourceDescriptor<Object, Object> resourceDescriptor) throws Exception {
        return CompletableFuture.completedFuture(noopResource(resourceDescriptor));
    }

    private Object noopResource(ResourceDescriptor<Object, Object> resourceDescriptor) {
        if (Cape.class.isAssignableFrom(resourceDescriptor.getType())) {
            return Cape.builder()
                    .resourceUri(resourceDescriptor.getUri())
                    .capeId("no-cape")
                    .capeData(TextureData.of(new byte[0], 0, 0))
                    .build();
        }
        if (Ears.class.isAssignableFrom(resourceDescriptor.getType())) {
            return Ears.builder()
                    .resourceUri(resourceDescriptor.getUri())
                    .earsData(TextureData.of(new byte[0], 0, 0))
                    .build();
        }
        if (GameProfileData.class.isAssignableFrom(resourceDescriptor.getType())) {
            return GameProfileData.builder()
                    .resourceUri(resourceDescriptor.getUri())
                    .gameProfile(null)
                    .build();
        }
        return null;
    }
}
