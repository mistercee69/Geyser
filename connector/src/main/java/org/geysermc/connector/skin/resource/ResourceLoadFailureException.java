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

    public static ResourceLoadFailureException getOrWrapException(Throwable t) {
        Throwable orig = t;
        while (t != null && !(t instanceof ResourceLoadFailureException)) {
            t = t.getCause();
        }
        if (t != null) {
            return (ResourceLoadFailureException) t;
        }
        return new ResourceLoadFailureException(orig);
    }
}
