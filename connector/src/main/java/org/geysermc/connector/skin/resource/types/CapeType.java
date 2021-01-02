package org.geysermc.connector.skin.resource.types;

import lombok.Getter;
import lombok.NonNull;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.loaders.*;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

@Getter
public enum CapeType {
    NONE(
            "cape:none",
            "^cape:none$",
            TextureIdUriType.NONE,
            NoopLoader.class),
    BEDROCK_CLIENT_DATA(
            "bedrockClientCape:%s",
            "^bedrockClientCape:.*",
            TextureIdUriType.UUID,
            BedrockClientDataCapeLoader.class),
    JAVA_GAME_PROFILE(
            "javaClientCape:%s",
            "^javaClientCape:.*",
            TextureIdUriType.UUID,
            JavaGameProfileCapeLoader.class),
    JAVA_TEXTURE(
            "https://textures.mojang.com/texture/%s",
            "https://textures.mojang.com/texture/.*",
            TextureIdUriType.NONE,
            StdUrlCapeLoader.class),
    JAVA_LEGACY_TEXTURE(
            "https://textures.minecraft.net/texture/%s",
            "https://textures.minecraft.net/texture/.*",
            TextureIdUriType.NONE,
            StdUrlCapeLoader.class),
    OPTIFINE(
            "https://optifine.net/capes/%s.png",
            "https://optifine.net/capes/.*",
            TextureIdUriType.USERNAME,
            StdUrlCapeLoader.class),
    LABYMOD(
            "https://dl.labymod.net/capes/%s",
            "https://dl.labymod.net/capes/.*",
            TextureIdUriType.UUID_DASHED,
            StdUrlCapeLoader.class),
    FIVEZIG(
            "https://textures.5zigreborn.eu/profile/%s",
            "https://textures.5zigreborn.eu/profile/.*",
            TextureIdUriType.UUID_DASHED,
            FiveZigUrlCapeLoader.class),
    MINECRAFTCAPES(
            "https://minecraftcapes.net/profile/%s/cape",
            "https://minecraftcapes.net/profile/.*",
            TextureIdUriType.UUID,
            StdUrlCapeLoader.class);

    private final String uriTemplate;
    private final Pattern uriPattern;
    private final TextureIdUriType type;
    private final Class<? extends ResourceLoader<?, ?>> loader;

    CapeType(@NonNull String uriTemplate, @NonNull String uriRegex, @NonNull TextureIdUriType type, @NonNull Class<? extends ResourceLoader<?, ?>> loader) {
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

    public ResourceDescriptor<Cape, Void> getDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(getUriFor(playerEntity), Cape.class);
    }

    public <P> ResourceDescriptor<Cape, P> getDescriptorFor(@NonNull PlayerEntity playerEntity, P params) {
        return ResourceDescriptor.of(getUriFor(playerEntity), Cape.class, params);
    }

    private boolean matches(URI uri) {
        return uriPattern.matcher(uri.toString()).matches();
    }

    public String getCapeIdFor(@NonNull URI uri) {
        String[] urlSection = uri.toString().split("/");
        switch (this) {
            case BEDROCK_CLIENT_DATA:
            case JAVA_TEXTURE:
            case JAVA_LEGACY_TEXTURE:
            case OPTIFINE:
            case LABYMOD:
            case FIVEZIG:
                return urlSection[urlSection.length-1];
            case MINECRAFTCAPES:
                return urlSection[urlSection.length-2];
            default:
                throw new IllegalArgumentException("Unhandled cape type");
        }
    }

    public static CapeType[] values(EnumSet<TextureIdUriType> textureIdUriTypes) {
        return Arrays.stream(values()).filter(e -> textureIdUriTypes.contains(e.type)).toArray(CapeType[]::new);
    }

    private static String toRequestedType(@NonNull TextureIdUriType type, @NonNull UUID uuid, @NonNull String username) {
        switch (type) {
            case NONE:
                return "";
            case UUID: return uuid.toString().replace("-", "");
            case UUID_DASHED: return uuid.toString();
            default: return username;
        }
    }

    public static CapeType fromUri(@NonNull URI uri) {
        for (CapeType value : CapeType.values()) {
            if (value.matches(uri)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to map uri ("+ uri.toString() + ") to cape type");
    }

    public static ResourceDescriptor<Cape, Void> getDefaultDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(NONE.getUriFor(playerEntity), Cape.class); // default is no cape
    }
}