package org.geysermc.connector.skin.resource.types;

import com.github.steveice10.mc.auth.data.GameProfile;
import lombok.Builder;
import lombok.Data;

import java.net.URI;

@Data
@Builder
public class GameProfileData implements Resource {
    private final URI resourceUri;
    private final GameProfile gameProfile;
}