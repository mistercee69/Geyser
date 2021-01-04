package org.geysermc.connector.skin.resource.types;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.skin.resource.ResourceDescriptor;

import java.net.URI;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class PlayerSkinProfile implements Resource {
    private final URI resourceUri;
    private final UUID playerId;
    private boolean bedrockSkinLoaded;
    private ResourceDescriptor<PlayerSkin, ?> skinDescriptor;
    private ResourceDescriptor<Cape, ?> capeDescriptor;
    private ResourceDescriptor<SkinGeometry, ?> geometryDescriptor;

    public static URI getUriFor(@NonNull PlayerEntity player) {
        return URI.create("skinProfile:" + player.getUuid());
    }

    public static ResourceDescriptor<PlayerSkinProfile, Void> getDescriptorFor(@NonNull PlayerEntity player) {
        return ResourceDescriptor.of(getUriFor(player), PlayerSkinProfile.class);
    }

    public static ResourceDescriptor<PlayerSkinProfile, Void> getDescriptorFor(@NonNull PlayerSkinProfile playerSkinProfile) {
        return ResourceDescriptor.of(playerSkinProfile.getResourceUri(), PlayerSkinProfile.class);
    }

    public static PlayerSkinProfile getDefaultSkinProfile(PlayerEntity playerEntity) {
        return PlayerSkinProfile.builder()
                .resourceUri(getUriFor(playerEntity))
                .playerId(playerEntity.getUuid())
                .bedrockSkinLoaded(false)
                .skinDescriptor(PlayerSkinType.getDefaultDescriptorFor(playerEntity))
                .capeDescriptor(CapeType.getDefaultDescriptorFor(playerEntity))
                .geometryDescriptor(SkinGeometryType.getDefaultDescriptorFor(playerEntity))
                .build();
    }
}