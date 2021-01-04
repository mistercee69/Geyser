package org.geysermc.connector.skin.resource;

import lombok.Data;
import lombok.NonNull;

@Data
public class ResourceContainer {
    boolean isFailed;
    Object resource;
    ResourceLoadFailureException exception;

    public ResourceContainer(Object resource) {
        this.isFailed = false;
        this.resource = resource;
    }

    public ResourceContainer(@NonNull ResourceLoadFailureException exception) {
        this.isFailed = true;
        this.exception = exception;
    }

    public static ResourceContainer of(Object resource) {
        return new ResourceContainer(resource);
    }

    public static ResourceContainer of(@NonNull Throwable throwable) {
        return new ResourceContainer(ResourceLoadFailureException.getOrWrapException(throwable));
    }
}
