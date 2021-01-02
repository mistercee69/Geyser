package org.geysermc.connector.skin.resource;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.net.URI;

@Value
public class ResourceDescriptor<T, P> {
    URI uri;
    Class<T> type;
    @EqualsAndHashCode.Exclude
    P params;

    public ResourceDescriptor(URI uri, Class<T> type) {
        this(uri, type, null);
    }

    public ResourceDescriptor(URI uri, Class<T> type, P params) {
        this.uri = uri;
        this.type = type;
        this.params = params;
    }

    public boolean isNull() {
        return uri == null || uri.toString().trim().isEmpty();
    }

    public static <T, P> ResourceDescriptor<T, P> of(URI uri, Class<T> type) {
        return new ResourceDescriptor<>(uri, type);
    }

    public static <T, P> ResourceDescriptor<T, P> of(URI uri, Class<T> type, P params) {
        return new ResourceDescriptor<>(uri, type, params);
    }
}
