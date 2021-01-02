package org.geysermc.connector.skin.resource.loaders;

import com.github.steveice10.mc.auth.data.GameProfile;
import lombok.Value;

@Value(staticConstructor = "of")
public class GameProfileSkinParams {
    GameProfile gameProfile;
}
