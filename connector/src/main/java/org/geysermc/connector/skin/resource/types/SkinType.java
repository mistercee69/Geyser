package org.geysermc.connector.skin.resource.types;

import lombok.Getter;
import lombok.NonNull;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.loaders.BedrockClientDataSkinLoader;
import org.geysermc.connector.skin.resource.loaders.InternalSkinLoader;
import org.geysermc.connector.skin.resource.loaders.JavaEarsSkinCombiningLoader;
import org.geysermc.connector.skin.resource.loaders.JavaGameProfileSkinLoader;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

@Getter
public enum SkinType {
    DEFAULT_STEVE(
            "skin:bedrock/skin/skin_steve.png",
            "^skin:bedrock/skin/skin_steve.png$",
            TextureIdUriType.NONE,
            InternalSkinLoader.class),
    DEFAULT_ALEX(
            "skin:bedrock/skin/skin_alex.png",
            "^skin:bedrock/skin/skin_alex.png$",
            TextureIdUriType.NONE,
            InternalSkinLoader.class),
    BEDROCK_CLIENT_DATA(
            "bedrockClientSkin:%s",
            "^bedrockClientSkin:.*",
            TextureIdUriType.UUID,
            BedrockClientDataSkinLoader.class),
    JAVA_GAME_PROFILE(
            "javaClientSkin:%s",
            "^javaClientSkin:.*",
            TextureIdUriType.UUID,
            JavaGameProfileSkinLoader.class),
    JAVA_MERGED_EARS(
            "javaClientSkinEars:%s",
            "^javaClientSkinEars:.*",
            TextureIdUriType.UUID,
            JavaEarsSkinCombiningLoader.class);

    private final String uriTemplate;
    private final Pattern uriPattern;
    private final TextureIdUriType idUriType;
    private final Class<? extends ResourceLoader<?, ?>> loader;

    SkinType(@NonNull String uriTemplate, @NonNull String uriRegex, @NonNull TextureIdUriType idUriType, @NonNull Class<? extends ResourceLoader<?, ?>> loader) {
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

    public static SkinType fromUri(@NonNull URI uri) {
        for (SkinType value : SkinType.values()) {
            if (value.matches(uri))
                return value;
        }
        throw new IllegalArgumentException("Unable to map uri (" + uri.toString() + ") to player skin type");
    }

    public ResourceDescriptor<Skin, Void> getDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(getUriFor(playerEntity), Skin.class);
    }

    public <P> ResourceDescriptor<Skin, P> getDescriptorFor(@NonNull PlayerEntity playerEntity, P params) {
        return ResourceDescriptor.of(getUriFor(playerEntity), Skin.class, params);
    }

    public static ResourceDescriptor<Skin, Void> getDefaultDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(playerEntity.isSlim() ? DEFAULT_ALEX.getUriFor(playerEntity) : DEFAULT_STEVE.getUriFor(playerEntity), Skin.class);
    }
}
