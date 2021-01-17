package org.geysermc.connector.skin.resource.types;

import com.fasterxml.jackson.core.util.BufferRecyclers;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import org.geysermc.connector.GeyserConnector;

import java.io.IOException;
import java.net.URI;

@Data
public class SkinGeometry implements Resource {
    private final URI resourceUri;
    private final String resourcePatch;
    private final String name;
    @ToString.Exclude
    @NonNull
    private final String data;

    SkinGeometry(URI resourceUri, String resourcePatch, String name, String data) {
        this.resourceUri = resourceUri;
        this.resourcePatch = resourcePatch;
        this.name = name;
        this.data = data;
    }

    public static SkinGeometryBuilder builder() {
        return SkinGeometryBuilder.builder();
    }

    public static final class SkinGeometryBuilder {
        private URI resourceUri;
        private String resourcePatch;
        private String name;
        private String data;

        private SkinGeometryBuilder() {
        }

        public static SkinGeometryBuilder builder() {
            return new SkinGeometryBuilder();
        }

        public SkinGeometryBuilder resourceUri(URI resourceUri) {
            this.resourceUri = resourceUri;
            return this;
        }

        public SkinGeometryBuilder resourcePatch(String resourcePatch) {
            this.resourcePatch = resourcePatch;
            return this;
        }

        public SkinGeometryBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SkinGeometryBuilder data(String data) {
            this.data = data;
            return this;
        }

        public SkinGeometry build() {
            if (resourcePatch == null && name == null) {
                throw new IllegalArgumentException("ResourcePatch or Name must be provided");
            }
            if (resourcePatch == null) {
                resourcePatch = convertToResourcePatch(name);
            }
            if (name == null) {
                name = convertToGeometryName(resourcePatch);
            }
            if (data == null) {
                data = "";
            }
            return new SkinGeometry(resourceUri, resourcePatch, name, data);
        }

    }

    public static String convertToResourcePatch(String geometryName) {

        return "{\"geometry\" : {\"default\" :\"" + new String(BufferRecyclers.getJsonStringEncoder().quoteAsString(geometryName)) + "\"}}";
    }

    public static String convertToGeometryName(String resourcePatch) {
        try {
            JsonNode jsonNode = GeyserConnector.JSON_MAPPER.readTree(resourcePatch);
            if (jsonNode.isObject()) {
                JsonNode geometryNode = jsonNode.get("geometry");
                if (geometryNode.isObject()) {
                    JsonNode defaultNode = geometryNode.get("default");
                    if (defaultNode.isTextual()) {
                        return defaultNode.asText();
                    }
                }
            }
            throw new IllegalArgumentException("Invalid resourcePatch");
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid resourcePatch", e);
        }
    }

}