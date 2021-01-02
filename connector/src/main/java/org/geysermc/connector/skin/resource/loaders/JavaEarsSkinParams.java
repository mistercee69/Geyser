package org.geysermc.connector.skin.resource.loaders;

import lombok.Value;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.types.Ears;
import org.geysermc.connector.skin.resource.types.PlayerSkin;

@Value(staticConstructor = "of")
public class JavaEarsSkinParams {
    ResourceDescriptor<PlayerSkin, ?> skin;
    ResourceDescriptor<Ears, ?> ears;
}
