package org.geysermc.connector.skin;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString
public class SkinProfile {
    private UUID playerId;
    private boolean bedrockSkinLoaded;
    private String skinId;
    private String capeId;
    private String geometryId;
    private boolean isPersona;
    private boolean isPremium;
}