package org.geysermc.connector.skin.resource;

import lombok.Value;


@Value(staticConstructor = "of")
public class ResourceLoadResult {
    boolean isFailed;
    ResourceDescriptor<?, ?> descriptor;
    Object resource;
    ResourceLoadFailureException exception;
}
