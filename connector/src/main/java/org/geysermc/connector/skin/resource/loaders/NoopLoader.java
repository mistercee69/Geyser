package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoader;

import java.util.concurrent.CompletableFuture;

public class NoopLoader implements ResourceLoader<Object, Object> {
    @Override
    public CompletableFuture<Object> loadAsync(@NonNull ResourceDescriptor<Object, Object> resourceDescriptor) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Object> loadSync(@NonNull ResourceDescriptor<Object, Object> resourceDescriptor) throws Exception {
        return CompletableFuture.completedFuture(null);
    }
}
