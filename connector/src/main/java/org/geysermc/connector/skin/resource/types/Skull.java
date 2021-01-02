package org.geysermc.connector.skin.resource.types;

import lombok.Builder;
import lombok.Data;

import java.net.URI;

@Data
@Builder
public class Skull implements Resource {
    private final URI resourceUri;
    private final String skullId;
    private final TextureData skullData;
}
