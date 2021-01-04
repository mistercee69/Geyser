package org.geysermc.connector.skin.resource.types;

import lombok.Data;
import lombok.ToString;

@Data(staticConstructor = "of")
public class TextureData {
    @ToString.Exclude
    private final byte[] data;
    private final int width;
    private final int height;
}
