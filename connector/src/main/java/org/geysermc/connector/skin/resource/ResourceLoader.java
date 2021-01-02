package org.geysermc.connector.skin.resource;

import lombok.NonNull;

import java.util.concurrent.CompletableFuture;

public interface ResourceLoader<T, P> {
    CompletableFuture<T> loadAsync(@NonNull ResourceDescriptor<T, P> resourceDescriptor);
    CompletableFuture<T> loadSync(@NonNull ResourceDescriptor<T, P> resourceDescriptor) throws Exception;
}
