package org.geysermc.connector.skin.resource.loaders;

import lombok.Value;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.types.Ears;
import org.geysermc.connector.skin.resource.types.Skin;

@Value(staticConstructor = "of")
public class JavaEarsSkinParams {
    ResourceDescriptor<Skin, ?> skin;
    ResourceDescriptor<Ears, ?> ears;
}
