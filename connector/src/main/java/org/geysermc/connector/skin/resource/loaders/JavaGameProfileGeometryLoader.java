package org.geysermc.connector.skin.resource.loaders;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.property.PropertyException;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.SkinGeometry;
import org.geysermc.connector.skin.resource.types.SkinGeometryType;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class JavaGameProfileGeometryLoader  implements ResourceLoader<SkinGeometry, GameProfileSkinParams> {

    private InternalSkinGeometryLoader internalLoader = new InternalSkinGeometryLoader();

    // URI form javaClientGeom:UUID <-- player's UUID

    @Override
    public CompletableFuture<SkinGeometry> loadAsync(@NonNull ResourceDescriptor<SkinGeometry, GameProfileSkinParams> descriptor) {
        try {
            ResourceDescriptor<SkinGeometry, Void> internalDescriptor = getInternalGeometry(descriptor);
            if (internalDescriptor != null) {
                return internalLoader.loadAsync(internalDescriptor);
            }
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
        return CompletableFuture.supplyAsync(() -> { throw new ResourceLoadFailureException("Game profile is missing skin uri"); });
    }

    @Override
    public CompletableFuture<SkinGeometry> loadSync(@NonNull ResourceDescriptor<SkinGeometry, GameProfileSkinParams> descriptor) throws ResourceLoadFailureException {
        try {
            ResourceDescriptor<SkinGeometry, Void> internalDescriptor = getInternalGeometry(descriptor);
            if (internalDescriptor != null) {
                return internalLoader.loadSync(internalDescriptor);
            }
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
        return CompletableFuture.supplyAsync(() -> { throw new ResourceLoadFailureException("Game profile is missing skin uri"); });
    }

    private ResourceDescriptor<SkinGeometry, Void> getInternalGeometry(@NonNull ResourceDescriptor<SkinGeometry, GameProfileSkinParams> descriptor) throws PropertyException {
        GameProfile gameProfile = getGameProfile(descriptor);
        Map<GameProfile.TextureType, GameProfile.Texture> textures = gameProfile.getTextures(false);

        if (hasSkin(textures)) {
            GameProfile.TextureModel skinModel = getSkinModel(textures);
            ResourceDescriptor<SkinGeometry, Void> internalDescriptor;
            if (skinModel == GameProfile.TextureModel.SLIM) {
                internalDescriptor = ResourceDescriptor.of(URI.create(SkinGeometryType.LEGACY_SLIM.getUriTemplate()), SkinGeometry.class);
            } else {
                internalDescriptor = ResourceDescriptor.of(URI.create(SkinGeometryType.LEGACY.getUriTemplate()), SkinGeometry.class);
            }
            return internalDescriptor;
        }
        return null;
    }

    private UUID getUuidFromUri(URI uri) {
        return UUID.fromString(uri.getSchemeSpecificPart());
    }

    private GameProfile getGameProfile(@NonNull ResourceDescriptor<SkinGeometry, GameProfileSkinParams> descriptor) {
        if (descriptor.getParams().getGameProfile() != null) {
            return descriptor.getParams().getGameProfile();
        }

        UUID playerUuid = getUuidFromUri(descriptor.getUri());
        GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerUuid);
        return session.getPlayerEntity().getProfile();
    }

    private boolean hasSkin(Map<GameProfile.TextureType, GameProfile.Texture> textureMap) {
        return textureMap.containsKey(GameProfile.TextureType.SKIN);
    }

    private GameProfile.TextureModel getSkinModel(Map<GameProfile.TextureType, GameProfile.Texture> textureMap) {
        if (textureMap.containsKey(GameProfile.TextureType.SKIN)) {
            return textureMap.get(GameProfile.TextureType.SKIN).getModel();
        }
        return null;
    }
}