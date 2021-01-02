package org.geysermc.connector.skin.resource.types;


import lombok.Getter;
import lombok.NonNull;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.loaders.NoopLoader;
import org.geysermc.connector.skin.resource.loaders.StdUrlEarsLoader;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import java.util.regex.Pattern;

@Getter
public enum EarsType {
    NONE(
            "ears:none",
            "ears:none",
            TextureIdUriType.NONE,
            NoopLoader.class),
    MINECRAFTCAPES(
            "https://minecraftcapes.net/profile/%s/ears",
            "https://minecraftcapes.net/profile/.*?/ears",
            TextureIdUriType.UUID,
            StdUrlEarsLoader.class),
    DEADMAU5(
            "ears:deadmau5",
            "ears:deadmau5",
            TextureIdUriType.NONE,
            NoopLoader.class);

    private final String uriTemplate;
    private final Pattern uriPattern;
    private final TextureIdUriType type;
    private final Class<? extends ResourceLoader<?, ?>> loader;

    EarsType(@NonNull String uriTemplate, @NonNull String uriRegex, @NonNull TextureIdUriType type, @NonNull Class<? extends ResourceLoader<?, ?>> loader) {
        this.uriTemplate = uriTemplate;
        this.uriPattern = Pattern.compile(uriRegex);
        this.type = type;
        this.loader = loader;
    }

    public URI getUriFor(@NonNull PlayerEntity playerEntity) {
        return getUriFor(toRequestedType(type, playerEntity.getUuid(), playerEntity.getUsername()));
    }

    private URI getUriFor(@NonNull String type) {
        return URI.create(String.format(uriTemplate, type));
    }

    public ResourceDescriptor<Ears, Void> getDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(getUriFor(playerEntity), Ears.class);
    }

    public <P> ResourceDescriptor<Ears, P> getDescriptorFor(@NonNull PlayerEntity playerEntity, P params) {
        return ResourceDescriptor.of(getUriFor(playerEntity), Ears.class, params);
    }

    private boolean matches(URI uri) {
        return uriPattern.matcher(uri.toString()).matches();
    }


    private static String toRequestedType(@NonNull TextureIdUriType type, @NonNull UUID uuid, @NonNull String username) {
        switch (type) {
            case UUID: return uuid.toString().replace("-", "");
            case UUID_DASHED: return uuid.toString();
            default: return username;
        }
    }

    public static EarsType[] values(EnumSet<TextureIdUriType> textureIdUriTypes) {
        return Arrays.stream(values()).filter(e -> textureIdUriTypes.contains(e.type)).toArray(EarsType[]::new);
    }

    public static EarsType fromUri(@NonNull URI uri) {
        for (EarsType value : EarsType.values()) {
            if (value.matches(uri)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to map uri ("+ uri.toString() + ") to ears type");
    }

    public static ResourceDescriptor<Ears, Void> getDefaultDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(NONE.getUriFor(playerEntity), Ears.class); // default is no ears
    }
}
