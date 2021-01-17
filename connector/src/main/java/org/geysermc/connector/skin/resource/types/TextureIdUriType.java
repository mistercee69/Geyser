package org.geysermc.connector.skin.resource.types;

import lombok.NonNull;

import java.util.Collections;
import java.util.EnumSet;

public enum TextureIdUriType {
    NONE,
    USERNAME,
    UUID,
    UUID_DASHED,
    UUID_AND_SKIN_ID,
    TEXTURE_ID;

    public static EnumSet<TextureIdUriType> setExcluding(@NonNull TextureIdUriType... excludedValues) {
        EnumSet<TextureIdUriType> exclusions = EnumSet.noneOf(TextureIdUriType.class);
        Collections.addAll(exclusions, excludedValues);
        return EnumSet.complementOf(exclusions);
    }
}
