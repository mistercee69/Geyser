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
import org.geysermc.connector.skin.resource.loaders.InternalSkullLoader;
import org.geysermc.connector.skin.resource.loaders.StdUrlSkullLoader;

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

@Getter
public enum SkullType {
    DEFAULT_STEVE(
            "skin:bedrock/skin/skin_steve.png",
            "^skin:bedrock/skin/skin_steve.png$",
            TextureIdUriType.NONE,
            InternalSkullLoader.class),
    DEFAULT_ALEX(
            "skin:bedrock/skin/skin_alex.png",
            "^skin:bedrock/skin/skin_alex.png$",
            TextureIdUriType.NONE,
            InternalSkullLoader.class),
    JAVA_SERVER_GAME_PROFILE(
            "https://textures.minecraft.net/texture/%s",
            "https://textures.minecraft.net/texture/.*",
            TextureIdUriType.TEXTURE_ID,
            StdUrlSkullLoader.class);

    private final String uriTemplate;
    private final Pattern uriPattern;
    private final TextureIdUriType idUriType;
    private final Class<? extends ResourceLoader<?, ?>> loader;

    SkullType(@NonNull String uriTemplate, @NonNull String uriRegex, @NonNull TextureIdUriType idUriType, @NonNull Class<? extends ResourceLoader<?, ?>> loader) {
        this.uriTemplate = uriTemplate;
        this.uriPattern = Pattern.compile(uriRegex);
        this.idUriType = idUriType;
        this.loader = loader;
    }

    private boolean matches(URI uri) {
        return uriPattern.matcher(uri.toString()).matches();
    }

    public URI getUriFor(PlayerEntity playerEntity) {
        return getUriFor(toRequestedType(idUriType, playerEntity));
    }

    private URI getUriFor(String type) {
        return this.idUriType == TextureIdUriType.NONE ? URI.create(uriTemplate) : URI.create(String.format(uriTemplate, type));
    }

    private static String toRequestedType(TextureIdUriType type, PlayerEntity playerEntity) {
        switch (type) {
            case NONE: return "";
            case UUID: return playerEntity.getUuid().toString().replace("-", "");
            case UUID_DASHED: return playerEntity.getUuid().toString();
            case UUID_AND_SKIN_ID:
                GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerEntity.getUuid());
                return playerEntity.getUuid()+"/"+session.getClientData().getSkinId();
            case TEXTURE_ID:
                try {
                    GameProfile gameProfile = playerEntity.getProfile();
                    Map<GameProfile.TextureType, GameProfile.Texture> textures = gameProfile.getTextures(false);
                    if (textures.containsKey(GameProfile.TextureType.SKIN)) {
                        String path = URI.create(textures.get(GameProfile.TextureType.SKIN).getURL()).getPath();
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

    public static SkullType fromUri(@NonNull URI uri) {
        for (SkullType value : SkullType.values()) {
            if (value.matches(uri))
                return value;
        }
        throw new IllegalArgumentException("Unable to map uri (" + uri.toString() + ") to player skin type");
    }

    public ResourceDescriptor<Skull, Void> getDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(getUriFor(playerEntity), Skull.class);
    }

    public <P> ResourceDescriptor<Skull, P> getDescriptorFor(@NonNull PlayerEntity playerEntity, P params) {
        return ResourceDescriptor.of(getUriFor(playerEntity), Skull.class, params);
    }

    public static ResourceDescriptor<Skull, Void> getDefaultDescriptorFor(@NonNull PlayerEntity playerEntity) {
        return ResourceDescriptor.of(playerEntity.isSlim() ? DEFAULT_ALEX.getUriFor(playerEntity) : DEFAULT_STEVE.getUriFor(playerEntity), Skull.class);
    }
}

