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
public class PlayerSkullProfile implements Resource {
    private final URI resourceUri;
    private final UUID playerId;
    private ResourceDescriptor<Skull, ?> skullDescriptor;
    private ResourceDescriptor<SkinGeometry, ?> geometryDescriptor;

    public static URI getUriFor(@NonNull PlayerEntity player) {
        return URI.create("skullProfile:" + player.getUuid());
    }

    public static ResourceDescriptor<PlayerSkullProfile, Void> getDescriptorFor(@NonNull PlayerEntity player) {
        return ResourceDescriptor.of(getUriFor(player), PlayerSkullProfile.class);
    }

    public static ResourceDescriptor<PlayerSkullProfile, Void> getDescriptorFor(@NonNull PlayerSkullProfile playerSkullProfile) {
        return ResourceDescriptor.of(playerSkullProfile.getResourceUri(), PlayerSkullProfile.class);
    }

    public ResourceDescriptor<?,?>[] getDescriptors() {
        return new ResourceDescriptor<?,?>[]{skullDescriptor, geometryDescriptor};
    }

    public static PlayerSkullProfile getDefaultSkullProfile(PlayerEntity playerEntity) {
        return PlayerSkullProfile.builder()
                .resourceUri(getUriFor(playerEntity))
                .playerId(playerEntity.getUuid())
                .skullDescriptor(SkullType.getDefaultDescriptorFor(playerEntity))
                .geometryDescriptor(SkinGeometryType.getDefaultDescriptorFor(playerEntity))
                .build();
    }
}
