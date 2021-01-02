package org.geysermc.connector.skin.resource.types;

import lombok.Builder;
import lombok.Data;
import org.geysermc.connector.network.session.auth.BedrockClientData;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Data
@Builder
public class PlayerSkin implements Resource {
    private final URI resourceUri;
    private final String skinId;
    private final TextureData skinData;
    @Builder.Default
    private final List<BedrockClientData.SkinAnimation> animations = Collections.emptyList();
    @Builder.Default
    private final String animationData= "";
    @Builder.Default
    private final boolean premium = false;
    @Builder.Default
    private final boolean persona = false;
    @Builder.Default
    private final boolean capeOnClassic = false;
    @Builder.Default
    private final String armSize = "wide";
    @Builder.Default
    private final String skinColor = "#0";
    @Builder.Default
    private final List<BedrockClientData.PersonaSkinPiece> personaPieces = Collections.emptyList();
    @Builder.Default
    private final List<BedrockClientData.PersonaSkinPieceTintColor> personaTintColors = Collections.emptyList();
}
