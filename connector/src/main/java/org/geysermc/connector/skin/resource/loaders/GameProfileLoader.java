package org.geysermc.connector.skin.resource.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.auth.data.GameProfile;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.GameProfileData;
import org.geysermc.connector.utils.UUIDUtils;
import org.geysermc.connector.utils.WebUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GameProfileLoader implements ResourceLoader<GameProfileData, Void> {
    @Override
    public CompletableFuture<GameProfileData> loadAsync(@NonNull ResourceDescriptor<GameProfileData, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getGameProfileData(descriptor.getUri());
            } catch (Throwable e) {
                e.printStackTrace();
                throw new ResourceLoadFailureException(e);
            }
        });
    }

    @Override
    public CompletableFuture<GameProfileData> loadSync(@NonNull ResourceDescriptor<GameProfileData, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getGameProfileData(descriptor.getUri()));
        } catch (Throwable e) {
            e.printStackTrace();
            return CompletableFuture.supplyAsync(() -> { throw new ResourceLoadFailureException(e); });
        }
    }

    private GameProfileData getGameProfileData(URI uri) throws IOException {
        String uuidToString = uri.getSchemeSpecificPart();
        JsonNode node = WebUtils.getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidToString);
        GameProfile gameProfile = new GameProfile(UUIDUtils.toDashedUUID(node.get("id").asText()), node.get("name").asText());

        List<GameProfile.Property> profileProperties = new ArrayList<>();
        JsonNode properties = node.get("properties");
        if (properties != null) {
            profileProperties.add(new GameProfile.Property("textures", node.get("properties").get(0).get("value").asText()));
            gameProfile.setProperties(profileProperties);
            return GameProfileData.builder()
                    .resourceUri(uri)
                    .gameProfile(gameProfile)
                    .build();
        }
        GeyserConnector.getInstance().getLogger().debug("No properties found in Mojang response for " + uuidToString);
        throw new ResourceLoadFailureException("No properties found in Mojang response for " + uuidToString);
    }

}