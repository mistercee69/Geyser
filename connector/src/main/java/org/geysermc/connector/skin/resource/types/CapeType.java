package org.geysermc.connector.skin.resource.types;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.property.PropertyException;
import lombok.Getter;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.loaders.BedrockClientDataCapeLoader;
import org.geysermc.connector.skin.resource.loaders.FiveZigUrlCapeLoader;
import org.geysermc.connector.skin.resource.loaders.NoopLoader;
import org.geysermc.connector.skin.resource.loaders.StdUrlCapeLoader;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
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
            TextureIdUriType.UUID_AND_SKIN_ID,
            BedrockClientDataCapeLoader.class),
    JAVA_SERVER_GAME_PROFILE(
            "https://textures.minecraft.net/texture/%s",
            "https://textures.minecraft.net/texture/.*",
            TextureIdUriType.TEXTURE_ID,
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
        try {
            return getUriFor(toRequestedType(type, playerEntity));
        } catch (PropertyException e) {
            // return NONE type instead
            GeyserConnector.getInstance().getLogger().error("Cape URI creation failed", e);
            return NONE.getUriFor(playerEntity);
        }
    }

    private URI getUriFor(@NonNull String type) {
        try {
            return URI.create(String.format(uriTemplate, type));
        } catch (Exception e) {
            // return NONE type instead
            GeyserConnector.getInstance().getLogger().error("Cape URI creation failed", e);
            return NONE.getUriFor(type);
        }
    }

    private String toRequestedType(@NonNull TextureIdUriType type, @NonNull PlayerEntity playerEntity) throws PropertyException {
        switch (type) {
            case NONE:
                return "";
            case UUID: return playerEntity.getUuid().toString().replace("-", "");
            case UUID_DASHED: return playerEntity.getUuid().toString();
            case UUID_AND_SKIN_ID:
                    GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerEntity.getUuid());
                    return playerEntity.getUuid() + "/" + session.getClientData().getCapeId();
            case TEXTURE_ID:
                try {
                    GameProfile gameProfile = playerEntity.getProfile();
                    Map<GameProfile.TextureType, GameProfile.Texture> textures = gameProfile.getTextures(false);
                    if (textures.containsKey(GameProfile.TextureType.CAPE)) {
                        String path = URI.create(textures.get(GameProfile.TextureType.CAPE).getURL()).getPath();
                        while (path.endsWith("/"))
                            path = path.substring(0, path.length() - 1); // strip any trailing '/'
                        return path.substring(path.lastIndexOf('/') + 1);
                    }
                } catch (PropertyException e) {
                    // probably should throw
                    return null;
                }
            default: return playerEntity.getUsername();
        }
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
            case JAVA_SERVER_GAME_PROFILE:
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