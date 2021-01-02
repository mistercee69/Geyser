package org.geysermc.connector.skin.resource;

public class ResourceLoadFailureException extends RuntimeException {
    public ResourceLoadFailureException(String message) {
        super(message);
    }

    public ResourceLoadFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceLoadFailureException(Throwable cause) {
        super(cause);
    }
}
