package org.geysermc.connector.skin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SkinProfile {
    private UUID playerId;
    private boolean bedrockSkinLoaded;
    private String skinId;
    private String capeId;
    private String geometryId;
    private boolean isPersona;
    private boolean isPremium;
}