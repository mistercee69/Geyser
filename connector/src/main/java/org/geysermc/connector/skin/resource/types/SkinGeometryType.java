package org.geysermc.connector.skin.resource.types;

import lombok.Getter;
import lombok.NonNull;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.loaders.BedrockClientDataGeometryLoader;
import org.geysermc.connector.skin.resource.loaders.InternalSkinGeometryLoader;
import org.geysermc.connector.skin.resource.loaders.JavaGameProfileGeometryLoader;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

@Getter
public enum SkinGeometryType {
    BEDROCK_CLIENT_DATA(
            "bedrockClientGeom:%s",
            "^bedrockClientGeom:.*",
            TextureIdUriType.UUID,
            BedrockClientDataGeometryLoader.class),
    JAVA_GAME_PROFILE(
            "javaClientGeom:%s",
            "^javaClientGeom:.*",
            TextureIdUriType.UUID,
            JavaGameProfileGeometryLoader.class),
    LEGACY(
            "geom:geometry.humanoid.custom",
            "geom:geometry.humanoid.custom",
            TextureIdUriType.NONE,
            InternalSkinGeometryLoader.class),
    LEGACY_SLIM(
            "geom:geometry.humanoid.customSlim",
            "geom:geometry.humanoid.customSlim",
            TextureIdUriType.NONE,
            InternalSkinGeometryLoader.class),
    EARS(
            "geom:geometry.humanoid.ears",
            "geom:geometry.humanoid.ears",
            TextureIdUriType.NONE,
            InternalSkinGeometryLoader.class),
    EARS_SLIM(
            "geom:geometry.humanoid.earsSlim",
            "geom:geometry.humanoid.earsSlim",
            TextureIdUriType.NONE,
            InternalSkinGeometryLoader.class),
    CUSTOM_SKULL(
            "geom:geometry.humanoid.customskull",
            "geom:geometry.humanoid.customskull",
            TextureIdUriType.NONE,
            InternalSkinGeometryLoader.class);

    private final String uriTemplate;
    private final Pattern uriPattern;
    private final TextureIdUriType idUriType;
    private final Class<? extends ResourceLoader<?, ?>> loader;

    SkinGeometryType(@NonNull String uriTemplate, @NonNull String uriRegex, @NonNull TextureIdUriType idUriType, @NonNull Class<? extends ResourceLoader<?, ?>> loader) {
        this.uriTemplate = uriTemplate;
        this.uriPattern = Pattern.compile(uriRegex);
        this.idUriType = idUriType;
        this.loader = loader;
    }

    private boolean matches(URI uri) {
        return uriPattern.matcher(uri.toString()).matches();
    }

    public URI getUriFor(PlayerEntity playerEntity) {
        return getUriFor(toRequestedType(idUriType, playerEntity.getUuid(), playerEntity.getUsername()));
    }

    private URI getUriFor(String type) {
        return this.idUriType == TextureIdUriType.NONE ? URI.create(uriTemplate) : URI.create(String.format(uriTemplate, type));
    }

    private static String toRequestedType(TextureIdUriType type, UUID uuid, String username) {
        switch (type) {
            case NONE: return "";
            case UUID: return uuid.toString().replace("-", "");
            case UUID_DASHED: return uuid.toString();
            default: return username;
        }
    }

    public static SkinGeometryType fromUri(@NonNull URI uri) {
        for (SkinGeometryType value : SkinGeometryType.values()) {
            if (value.matches(uri))
                return value;
        }
        throw new IllegalArgumentException("Unable to map uri (" + uri.toString() + ") to skin geometry type");
    }

    public ResourceDescriptor<SkinGeometry, Void> getDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(getUriFor(playerEntity), SkinGeometry.class);
    }

    public <P> ResourceDescriptor<SkinGeometry, P> getDescriptorFor(@NonNull PlayerEntity playerEntity, P params) {
        return ResourceDescriptor.of(getUriFor(playerEntity), SkinGeometry.class, params);
    }

    public static ResourceDescriptor<SkinGeometry, Void> getDefaultDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(playerEntity.isSlim() ? LEGACY_SLIM.getUriFor(playerEntity) : LEGACY.getUriFor(playerEntity), SkinGeometry.class);
    }
}
