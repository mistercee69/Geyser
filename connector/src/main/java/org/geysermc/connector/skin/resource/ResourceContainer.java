package org.geysermc.connector.skin.resource;

import lombok.Data;
import lombok.NonNull;

@Data
public class ResourceContainer {
    long requestedOn = System.currentTimeMillis();
    boolean updated = false;
    Object resource;

    public ResourceContainer(@NonNull Object resource) {
        this.resource = resource;
    }

    public static ResourceContainer of(@NonNull Object resource) {
        return new ResourceContainer(resource);
    }
}
