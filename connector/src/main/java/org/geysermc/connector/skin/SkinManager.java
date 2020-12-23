/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.skin;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.nukkitx.protocol.bedrock.data.skin.*;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.common.AuthType;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.utils.LanguageUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.geysermc.connector.network.session.auth.BedrockClientData.*;
import static org.geysermc.connector.skin.SkinProvider.*;

public class SkinManager {

    private static final Map<UUID, SkinProfile> cachedSkinProfile = new ConcurrentHashMap<>();


    public static UUID registerBedrockSkin(PlayerEntity playerEntity, BedrockClientData clientData) {
        GeyserConnector.getInstance().getLogger().info(LanguageUtils.getLocaleStringLog("geyser.skin.bedrock.register", playerEntity.getUsername(), playerEntity.getUuid()));

        SkinProfile.SkinProfileBuilder builder = SkinProfile.builder();

        try {
            byte[] skinBytes = clientData.getSkinData();
            byte[] capeBytes = clientData.getCapeData();

            byte[] geometryNameBytes = Base64.getDecoder().decode(clientData.getGeometryName().getBytes(StandardCharsets.UTF_8));
            byte[] geometryBytes = Base64.getDecoder().decode(clientData.getGeometryData().getBytes(StandardCharsets.UTF_8));

            GeyserConnector.getInstance().getLogger().info("Player '" + playerEntity.getUsername() + "' skin is: " + clientData.getSkinImageWidth() + "x" + clientData.getSkinImageHeight() +
                    " isPersona: " + clientData.isPersonaSkin() + " isPremium: " + clientData.isPremiumSkin() + " id: " + clientData.getSkinId());

            if (skinBytes.length <= (128 * 128 * 4) && !clientData.isPersonaSkin() && !clientData.isPremiumSkin()) {
                Skin skin = Skin.builder()
                        .skinOwner(playerEntity.getUuid())
                        .textureUrl(clientData.getSkinId())
                        .skinData(skinBytes)
                        .build();

                builder.skinId(storeBedrockSkin(skin));
                builder.geometryId(storeBedrockGeometry(playerEntity.getUuid(), geometryNameBytes, geometryBytes));
            } else if (skinBytes.length >= (64 * 32 * 4)) {
                Skin skin = Skin.builder()
                        .skinOwner(playerEntity.getUuid())
                        .textureUrl(clientData.getSkinId())
                        .skinWidth(clientData.getSkinImageWidth())
                        .skinHeight(clientData.getSkinImageHeight())
                        .skinData(skinBytes)
                        .animations(clientData.getAnimations() != null ? clientData.getAnimations() : Collections.emptyList())
                        .animationData(clientData.getSkinAnimationData())
                        .premium(clientData.isPremiumSkin())
                        .persona(clientData.isPersonaSkin())
                        .armSize(clientData.getArmSize())
                        .skinColor(clientData.getSkinColor())
                        .personaPieces(clientData.getPersonaSkinPieces() != null ? clientData.getPersonaSkinPieces() : Collections.emptyList())
                        .personaTintColors(clientData.getPersonaTintColors() != null ? clientData.getPersonaTintColors() : Collections.emptyList())
                        .build();

                builder.isPersona(clientData.isPersonaSkin());
                builder.isPremium(clientData.isPremiumSkin());
                builder.skinId(storeBedrockSkin(skin));
                builder.geometryId(storeBedrockGeometry(playerEntity.getUuid(), geometryNameBytes, geometryBytes));
            } else {
                GeyserConnector.getInstance().getLogger().info(LanguageUtils.getLocaleStringLog("geyser.skin.bedrock.fail", playerEntity.getUsername()));
                GeyserConnector.getInstance().getLogger().debug("The size of '" + playerEntity.getUsername() + "' skin is: " + clientData.getSkinImageWidth() + "x" + clientData.getSkinImageHeight());
            }

            if (!clientData.getCapeId().equals("")) {
                builder.capeId(storeBedrockCape(playerEntity.getUuid(), capeBytes, clientData.getCapeImageWidth(), clientData.getCapeImageHeight()));
            }

            builder.bedrockSkinLoaded(true);
            SkinProfile skinProfile = builder.build();
            cachedSkinProfile.put(playerEntity.getUuid(), skinProfile);
            return playerEntity.getUuid();
        } catch (Exception e) {
            throw new AssertionError("Failed to cache skin for bedrock user (" + playerEntity.getUsername() + "): ", e);
        }
    }

    public static void registerSkinAsync(PlayerEntity entity, Consumer<UUID> skinRegistrationConsumer) {
        SkinProfile skinProfile = cachedSkinProfile.get(entity.getUuid());
        if (!skinProfile.isBedrockSkinLoaded()) {
            GameProfileData data = GameProfileData.from(entity.getProfile());
            requestSkinAndCape(entity.getUuid(), data.getSkinUrl(), data.getCapeUrl())
                    .whenCompleteAsync((skinAndCape, throwable) -> {
                                try {

                                    SkinProfile.SkinProfileBuilder skinProfileBuilder = SkinProfile.builder();
                                    skinProfileBuilder.playerId(entity.getUuid());
                                    skinProfileBuilder.bedrockSkinLoaded(false);

                                    Skin skin = skinAndCape.getSkin();
                                    skinProfileBuilder.skinId(skin.getTextureUrl());

                                    Cape cape = skinAndCape.getCape();

                                    if (!cape.isFailed()) {
                                        skinProfileBuilder.capeId(cape.getTextureUrl());
                                    } else {
                                        cape = getOrDefault(requestBedrockCape(entity.getUuid()),
                                                EMPTY_CAPE, 3);

                                        if (!cape.isFailed()) {
                                            skinProfileBuilder.capeId(cape.getTextureUrl());
                                        } else if (ALLOW_THIRD_PARTY_CAPES) {

                                            cape = getOrDefault(requestUnofficialCape(
                                                    cape, entity.getUuid(),
                                                    entity.getUsername(), false
                                            ), EMPTY_CAPE, CapeProvider.VALUES.length * 3);

                                            if (!cape.isFailed()) {
                                                skinProfileBuilder.capeId(cape.getTextureUrl());
                                            }
                                        }
                                    }

                                    SkinGeometry geometry = SkinGeometry.getLegacy(data.isAlex());
                                    skinProfileBuilder.geometryId(data.isAlex() ? "alex" : "steve");

                                    geometry = getOrDefault(requestBedrockGeometry(
                                            geometry, entity.getUuid()
                                    ), geometry, 3);

                                    if (!geometry.isFailed()) {
                                        skinProfileBuilder.geometryId(entity.getUuid().toString());
                                    } else if (ALLOW_THIRD_PARTY_EARS) {
                                        // Not a bedrock player check for ears
                                        boolean isEars;

                                        // Its deadmau5, gotta support his skin :)
                                        if (entity.getUuid().toString().equals("1e18d5ff-643d-45c8-b509-43b8461d8614")) {
                                            isEars = true;
                                        } else {
                                            // Get the ears texture for the player
                                            skin = getOrDefault(requestUnofficialEars(
                                                    skin, entity.getUuid(), entity.getUsername(), false
                                            ), skin, 3);

                                            isEars = skin.isEars();
                                        }

                                        // Does the skin have an ears texture
                                        if (isEars) {
                                            // Get the new geometry
                                            geometry = SkinGeometry.getEars(data.isAlex());

                                            // Store the skin and geometry for the ears
                                            skinProfileBuilder.skinId(storeEarSkin(entity.getUuid(), skin));
                                            skinProfileBuilder.geometryId(storeEarGeometry(entity.getUuid(), data.isAlex()));
                                        }
                                    }

                                    cachedSkinProfile.put(entity.getUuid(), skinProfileBuilder.build());
                                    if (skinRegistrationConsumer != null) {
                                        skinRegistrationConsumer.accept(entity.getUuid());
                                    }
                                } catch (Exception e) {
                                    GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.skin.fail", entity.getUuid()), e);
                                }
                            }
                    );
        } else {
            // trigger immediately (not loading aysnc skins)
            if (skinRegistrationConsumer != null) {
                skinRegistrationConsumer.accept(entity.getUuid());
            }
        }
    }

    @AllArgsConstructor
    @Getter
    public static class GameProfileData {
        private final String skinUrl;
        private final String capeUrl;
        private final boolean alex;

        /**
         * Generate the GameProfileData from the given GameProfile
         *
         * @param profile GameProfile to build the GameProfileData from
         * @return The built GameProfileData
         */
        public static GameProfileData from(GameProfile profile) {
            // Fallback to the offline mode of working it out
            boolean isAlex = (Math.abs(profile.getId().hashCode() % 2) == 1);

            try {
                GameProfile.Property skinProperty = profile.getProperty("textures");

                // TODO: Remove try/catch here
                JsonNode skinObject = GeyserConnector.JSON_MAPPER.readTree(new String(Base64.getDecoder().decode(skinProperty.getValue()), StandardCharsets.UTF_8));
                JsonNode textures = skinObject.get("textures");

                JsonNode skinTexture = textures.get("SKIN");
                String skinUrl = skinTexture.get("url").asText().replace("http://", "https://");

                isAlex = skinTexture.has("metadata");

                String capeUrl = null;
                if (textures.has("CAPE")) {
                    JsonNode capeTexture = textures.get("CAPE");
                    capeUrl = capeTexture.get("url").asText().replace("http://", "https://");
                }

                return new GameProfileData(skinUrl, capeUrl, isAlex);
            } catch (Exception exception) {
                if (GeyserConnector.getInstance().getAuthType() != AuthType.OFFLINE) {
                    GeyserConnector.getInstance().getLogger().debug("Got invalid texture data for " + profile.getName() + " " + exception.getMessage());
                }
                // return default skin with default cape when texture data is invalid
                String skinUrl = isAlex ? SkinProvider.EMPTY_SKIN_ALEX.getTextureUrl() : SkinProvider.EMPTY_SKIN.getTextureUrl();
                if ("steve".equals(skinUrl) || "alex".equals(skinUrl)) {
                    GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(profile.getId());

                    if (session != null) {
                        skinUrl = session.getClientData().getSkinId();
                    }
                }
                return new GameProfileData(skinUrl, SkinProvider.EMPTY_CAPE.getTextureUrl(), isAlex);
            }
        }
    }

    public static PlayerListPacket.Entry buildCachedEntry(GeyserSession session, PlayerEntity playerEntity) {

        SkinProfile skinProfile = cachedSkinProfile.get(playerEntity.getUuid());
        Skin skin = getCachedSkin(skinProfile.getSkinId());
        Cape cape = getCachedCape(skinProfile.getCapeId());
        SkinGeometry skinGeometry = skinProfile.isBedrockSkinLoaded() ? getCachedSkinGeometry(playerEntity.getUuid()) :
                SkinGeometry.getLegacy("alex".equals(skinProfile.getGeometryId()));

        return buildEntryManually(
                session,
                playerEntity.getUuid(),
                playerEntity.getUsername(),
                playerEntity.getGeyserId(),
                skin,
                cape,
                skinGeometry);
    }

    public static void updatePlayerList(GeyserSession session, PlayerEntity entity) {
        SkinManager.registerSkinAsync(entity, uuid -> {
            if (session.getUpstream().isInitialized()) {
                PlayerListPacket.Entry updatedEntry = SkinManager.buildCachedEntry(session, entity);
                PlayerListPacket playerAddPacket = new PlayerListPacket();
                playerAddPacket.setAction(PlayerListPacket.Action.ADD);
                playerAddPacket.getEntries().add(updatedEntry);
                session.sendUpstreamPacket(playerAddPacket);

                if (!entity.isPlayerList()) {
                    PlayerListPacket playerRemovePacket = new PlayerListPacket();
                    playerRemovePacket.setAction(PlayerListPacket.Action.REMOVE);
                    playerRemovePacket.getEntries().add(updatedEntry);
                    session.sendUpstreamPacket(playerRemovePacket);
                }
                GeyserConnector.getInstance().getLogger().debug("Loaded Local Bedrock Java Skin Data");
            }
        });
    }

    public static PlayerListPacket.Entry buildEntryManually(GeyserSession session, UUID uuid, String username, long geyserId,
                                                                 Skin skin, Cape cape, SkinGeometry geometry) {
        List<AnimationData> animations = Collections.emptyList();
        if (skin.getAnimations() != null) {
            animations = skin.getAnimations().stream().map(SkinManager::toAnimationData).collect(Collectors.toList());
        }

        List<PersonaPieceData> pieces = Collections.emptyList();
        if (skin.getPersonaPieces() != null) {
            pieces = skin.getPersonaPieces().stream().map(SkinManager::toPersonaPieceData).collect(Collectors.toList());
        }

        List<PersonaPieceTintData> tints = Collections.emptyList();
        if (skin.getPersonaTintColors() != null) {
            tints = skin.getPersonaTintColors().stream().map(SkinManager::toPersonaPieceTintData).collect(Collectors.toList());
        }

        ImageData skinImage;
        ImageData capeImage;

        if (skin.isPersona() || skin.isPremium()) {
            skinImage = ImageData.of(skin.getSkinWidth(), skin.getSkinHeight(), skin.getSkinData());
            capeImage = ImageData.of(cape.getCapeWidth(), cape.getCapeHeight(), cape.getCapeData());
        } else {
            skinImage = ImageData.of(skin.getSkinData());
            capeImage = ImageData.of(cape.getCapeData());
        }

        SerializedSkin serializedSkin = SerializedSkin.of(skin.getTextureUrl(), geometry.getGeometryName(), skinImage,
                animations, capeImage, geometry.getGeometryData(), skin.getAnimationData(), skin.isPremium(), skin.isPersona(),
                !cape.getCapeId().equals(EMPTY_CAPE.getCapeId()), cape.getCapeId(), skin.getTextureUrl() + cape.getCapeId(),
                skin.getArmSize(), skin.getSkinColor(), pieces, tints);

        // This attempts to find the xuid of the player so profile images show up for xbox accounts
        String xuid = "";
        GeyserSession player = GeyserConnector.getInstance().getPlayerByUuid(uuid);

        if (player != null) {
            xuid = player.getAuthData().getXboxUUID();
        }

        PlayerListPacket.Entry entry;

        // If we are building a PlayerListEntry for our own session we use our AuthData UUID instead of the Java UUID
        // as bedrock expects to get back its own provided uuid
        if (session.getPlayerEntity().getUuid().equals(uuid)) {
            entry = new PlayerListPacket.Entry(session.getAuthData().getUUID());
        } else {
            entry = new PlayerListPacket.Entry(uuid);
        }

        entry.setName(username);
        entry.setEntityId(geyserId);
        entry.setSkin(serializedSkin);
        entry.setXuid(xuid);
        entry.setPlatformChatId("");
        entry.setTeacher(false);
        entry.setTrustedSkin(true);
        GeyserConnector.getInstance().getLogger().info("For " + session.getPlayerEntity().getUuid() + " list update " + entry);
        return entry;
    }



    private static AnimatedTextureType toAnimatedTextureType(TextureType textureType) {
        if (textureType != null) {
            switch (textureType) {
                case FACE:
                    return AnimatedTextureType.FACE;
                case BODY_32X32:
                    return AnimatedTextureType.BODY_32X32;
                case BODY_128X128:
                    return AnimatedTextureType.BODY_128X128;
                case NONE:
                default:
                    return AnimatedTextureType.NONE;
            }
        }
        return AnimatedTextureType.NONE;
    }

    private static AnimationExpressionType toAnimationExpression(ExpressionType expressionType) {
        if (expressionType != null) {
            switch (expressionType) {
                case BLINKING:
                    return AnimationExpressionType.BLINKING;
                case LINEAR:
                default:
                    return AnimationExpressionType.LINEAR;
            }
        }
        return AnimationExpressionType.LINEAR;
    }

    private static AnimationData toAnimationData(SkinAnimation skinAnimation) {
        return new AnimationData(ImageData.of(skinAnimation.getImageData()), toAnimatedTextureType(skinAnimation.getTextureType()),
                skinAnimation.getFrames(), toAnimationExpression(skinAnimation.getExpressionType()));
    }

    private static PersonaPieceData toPersonaPieceData(PersonaSkinPiece personaSkinPiece) {
        return new PersonaPieceData(personaSkinPiece.getId(), personaSkinPiece.getType(), personaSkinPiece.getPackId(), personaSkinPiece.isDefault(),
                personaSkinPiece.getProductId());
    }

    private static PersonaPieceTintData toPersonaPieceTintData(PersonaSkinPieceTintColor personaSkinPieceTintColor) {
        return new PersonaPieceTintData(personaSkinPieceTintColor.getType(), personaSkinPieceTintColor.getColors());
    }
}
