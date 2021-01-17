package org.geysermc.connector.skin.resource.loaders;

import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadFailureException;
import org.geysermc.connector.skin.resource.ResourceLoader;
import org.geysermc.connector.skin.resource.types.Skin;
import org.geysermc.connector.skin.resource.types.TextureData;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.UUIDUtils;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BedrockClientDataSkinLoader implements ResourceLoader<Skin, Void> {
    public static final boolean ALLOW_BEDROCK_CHARACTER_CREATOR_SKINS = GeyserConnector.getInstance().getConfig().isAllowBedrockCharacterCreatorSkins();

    // URI form bedrockClientSkin:UUID

    @Override
    public CompletableFuture<Skin> loadAsync(@NonNull ResourceDescriptor<Skin, Void> descriptor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerSkin(descriptor.getUri());
            } catch (Throwable e) {
                throw ResourceLoadFailureException.getOrWrapException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Skin> loadSync(@NonNull ResourceDescriptor<Skin, Void> descriptor) throws ResourceLoadFailureException {
        try {
            return CompletableFuture.completedFuture(getPlayerSkin(descriptor.getUri()));
        } catch (Throwable e) {
            return CompletableFuture.supplyAsync(() -> { throw ResourceLoadFailureException.getOrWrapException(e); });
        }
    }

    private Skin getPlayerSkin(URI uri) {
        String[] split = uri.getSchemeSpecificPart().split("/");
        UUID playerUuid = UUID.fromString(UUIDUtils.toDashedUUID(split[0]));
        GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerUuid);
        BedrockClientData clientData = session.getClientData();

        if (GeyserConnector.getInstance().getLogger().isDebug()) {
            String skinContents = clientData.getSkinData() != null ? (clientData.getSkinData().length + "bytes") : "(null)";
            GeyserConnector.getInstance().getLogger().debug("[Skin Info] id: " + clientData.getSkinId() + " skinData: " + skinContents + " width: " + clientData.getSkinImageWidth() + " height: " + clientData.getSkinImageHeight() +
                        " isPersona: " + clientData.isPersonaSkin() + " isPremium: " + clientData.isPremiumSkin() + " isCapeOnClassic: " + clientData.isCapeOnClassicSkin());
        }

        if (!ALLOW_BEDROCK_CHARACTER_CREATOR_SKINS) {
            if (clientData.getSkinData().length > (128 * 128 * 4) || clientData.isPersonaSkin())   {
                throw new ResourceLoadFailureException(LanguageUtils.getLocaleStringLog("geyser.skin.bedrock.fail", playerUuid));
            }
        }

        Skin.SkinBuilder skinBuilder = Skin.builder();
        skinBuilder
                .resourceUri(uri)
                .skinId(clientData.getSkinId())
                .skinData(TextureData.of(
                        clientData.getSkinData(),
                        clientData.getSkinImageWidth(),
                        clientData.getSkinImageHeight()))
                .capeOnClassic(clientData.isCapeOnClassicSkin());

        if (ALLOW_BEDROCK_CHARACTER_CREATOR_SKINS) {
            if (clientData.getAnimations() != null) {
                skinBuilder.animations(clientData.getAnimations());
            }
            skinBuilder
                    .animationData(clientData.getSkinAnimationData())
                    .premium(clientData.isPremiumSkin())
                    .persona(clientData.isPersonaSkin())
                    .armSize(clientData.getArmSize())
                    .skinColor(clientData.getSkinColor());

            if (clientData.getPersonaSkinPieces() != null) {
                skinBuilder.personaPieces(clientData.getPersonaSkinPieces());
            }
            if (clientData.getPersonaTintColors() != null) {
                skinBuilder.personaTintColors(clientData.getPersonaTintColors());
            }
        }

        return skinBuilder.build();
    }
}