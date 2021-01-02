package org.geysermc.connector.skin.resource.types;

import lombok.Getter;
import lombok.NonNull;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.loaders.GameProfileLoader;
import org.geysermc.connector.skin.resource.loaders.NoopLoader;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import java.util.regex.Pattern;

@Getter
public enum GameProfileDataType {
    NONE(
            "gameProfile:none",
            "gameProfile:none",
            TextureIdUriType.NONE,
            NoopLoader.class),
    MINECRAFT(
            "gameProfile:%s",
            "gameProfile:(?!(?:none)).*",
            TextureIdUriType.UUID,
            GameProfileLoader.class);
    
    private final String uriTemplate;
    private final Pattern uriPattern;
    private final TextureIdUriType type;
    private final Class<? extends ResourceLoader<?, ?>> loader;

    GameProfileDataType(@NonNull String uriTemplate, @NonNull String uriRegex, @NonNull TextureIdUriType type, @NonNull Class<? extends ResourceLoader<?, ?>> loader) {
        this.uriTemplate = uriTemplate;
        this.uriPattern = Pattern.compile(uriRegex);
        this.type = type;
        this.loader = loader;
    }

    public URI getUriFor(@NonNull PlayerEntity playerEntity) {
        return getUriFor(toRequestedType(type, playerEntity.getUuid(), playerEntity.getUsername()));
    }

    public URI getUriFor(@NonNull UUID playerUUID) {
        return getUriFor(toRequestedType(type, playerUUID, ""));
    }

    private URI getUriFor(@NonNull String type) {
        return URI.create(String.format(uriTemplate, type));
    }

    public ResourceDescriptor<GameProfileData, Void> getDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(getUriFor(playerEntity), GameProfileData.class);
    }

    public <P> ResourceDescriptor<GameProfileData, P> getDescriptorFor(@NonNull PlayerEntity playerEntity, P params) {
        return ResourceDescriptor.of(getUriFor(playerEntity), GameProfileData.class, params);
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

    public static GameProfileDataType[] values(EnumSet<TextureIdUriType> textureIdUriTypes) {
        return Arrays.stream(values()).filter(e -> textureIdUriTypes.contains(e.type)).toArray(GameProfileDataType[]::new);
    }

    public static GameProfileDataType fromUri(@NonNull URI uri) {
        for (GameProfileDataType value : GameProfileDataType.values()) {
            if (value.matches(uri)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to map uri ("+ uri.toString() + ") to game profile data type");
    }

    public static ResourceDescriptor<GameProfileData, Void> getDefaultDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(NONE.getUriFor(playerEntity), GameProfileData.class); // default is no game profile
    }
}
