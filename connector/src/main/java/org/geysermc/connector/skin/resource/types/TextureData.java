package org.geysermc.connector.skin.resource.types;

import lombok.Data;

@Data(staticConstructor = "of")
public class TextureData {
    private final byte[] data;
    private final int width;
    private final int height;
}
