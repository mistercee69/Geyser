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
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.protocol.bedrock.data.skin.*;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadResult;
import org.geysermc.connector.skin.resource.ResourceManager;
import org.geysermc.connector.skin.resource.loaders.GameProfileSkinParams;
import org.geysermc.connector.skin.resource.loaders.JavaEarsSkinParams;
import org.geysermc.connector.skin.resource.types.*;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.connector.utils.UUIDUtils;
import org.geysermc.connector.utils.WebUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.geysermc.connector.network.session.auth.BedrockClientData.*;

public class SkinManager {
    public static final boolean ALLOW_THIRD_PARTY_CAPES = GeyserConnector.getInstance().getConfig().isAllowThirdPartyCapes();
    public static final boolean ALLOW_THIRD_PARTY_EARS = GeyserConnector.getInstance().getConfig().isAllowThirdPartyEars();

    static {

        // Schedule Daily Image Expiry if we are caching them
        if (GeyserConnector.getInstance().getConfig().getCacheImages() > 0) {
            GeyserConnector.getInstance().getGeneralThreadPool().scheduleAtFixedRate(() -> {
                File cacheFolder = GeyserConnector.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("images").toFile();
                if (!cacheFolder.exists()) {
                    return;
                }

                int count = 0;
                final long expireTime = ((long)GeyserConnector.getInstance().getConfig().getCacheImages()) * ((long)1000 * 60 * 60 * 24);
                for (File imageFile : Objects.requireNonNull(cacheFolder.listFiles())) {
                    if (imageFile.lastModified() < System.currentTimeMillis() - expireTime) {
                        //noinspection ResultOfMethodCallIgnored
                        imageFile.delete();
                        count++;
                    }
                }

                if (count > 0) {
                    GeyserConnector.getInstance().getLogger().debug(String.format("Removed %d cached image files as they have expired", count));
                }
            }, 10, 1440, TimeUnit.MINUTES);
        }
    }

    public static void updateBedrockSkin(PlayerEntity playerEntity, GeyserSession session, SerializedSkin skin) {
        GeyserConnector.getInstance().getLogger().info(LanguageUtils.getLocaleStringLog("geyser.skin.bedrock.update", playerEntity.getUsername(), playerEntity.getUuid()));

        session.getClientData().setSkinId(skin.getSkinId());
        session.getClientData().setSkinData(skin.getSkinData().getImage());
        session.getClientData().setSkinImageHeight(skin.getSkinData().getHeight());
        session.getClientData().setSkinImageWidth(skin.getSkinData().getWidth());

        if (skin.getAnimations() != null) {
            session.getClientData().setAnimations(skin.getAnimations().stream().map(SkinManager::toSkinAnimation).collect(Collectors.toList()));
        }

        session.getClientData().setCapeId(skin.getCapeId());
        session.getClientData().setCapeData(skin.getCapeData().getImage());
        session.getClientData().setCapeImageHeight(skin.getCapeData().getHeight());
        session.getClientData().setCapeImageWidth(skin.getCapeData().getWidth());
        session.getClientData().setCapeOnClassicSkin(skin.isCapeOnClassic());
        session.getClientData().setGeometryName(Base64.getEncoder().encodeToString(skin.getSkinResourcePatch().getBytes(StandardCharsets.UTF_8)));
        session.getClientData().setGeometryData(Base64.getEncoder().encodeToString(skin.getGeometryData().getBytes(StandardCharsets.UTF_8)));
        session.getClientData().setPersonaSkin(skin.isPersona());
        session.getClientData().setPremiumSkin(skin.isPremium());
        session.getClientData().setArmSize(skin.getArmSize());
        session.getClientData().setSkinAnimationData(skin.getAnimationData());
        session.getClientData().setSkinColor(skin.getSkinColor());

        if (skin.getPersonaPieces() != null) {
            session.getClientData().setPersonaSkinPieces(skin.getPersonaPieces().stream().map(SkinManager::toPersonaPieceData).collect(Collectors.toList()));
        }
        if (skin.getTintColors() != null) {
            session.getClientData().setPersonaTintColors(skin.getTintColors().stream().map(SkinManager::toPersonaSkinPieceTintColor).collect(Collectors.toList()));
        }

        registerBedrockSkin(playerEntity, session);
    }

    public static void registerBedrockSkin(PlayerEntity playerEntity, GeyserSession session) {
        GeyserConnector.getInstance().getLogger(session).info(LanguageUtils.getLocaleStringLog("geyser.skin.bedrock.register",
                playerEntity.getUsername(), playerEntity.getUuid()));

        final ResourceDescriptor<PlayerSkin, Void> bedrockSkin = PlayerSkinType.BEDROCK_CLIENT_DATA.getDescriptorFor(playerEntity);
        final ResourceDescriptor<SkinGeometry, Void> bedrockGeom = SkinGeometryType.BEDROCK_CLIENT_DATA.getDescriptorFor(playerEntity);
        final ResourceDescriptor<Cape, Void> bedrockCape = CapeType.BEDROCK_CLIENT_DATA.getDescriptorFor(playerEntity);

        ResourceManager.loadAsync(bedrockSkin, bedrockGeom, bedrockCape)
                .whenComplete((resultMap, throwable) -> {
                    boolean skinFailed=false, geometryFailed=false, capeFailed=false;

                    ResourceDescriptor<PlayerSkin, ?> finalSkin = bedrockSkin;
                    ResourceDescriptor<SkinGeometry, ?> finalGeom = bedrockGeom;
                    ResourceDescriptor<Cape, ?> finalCape = bedrockCape;

                    if (resultMap.get(bedrockSkin).isFailed()) {
                        finalSkin = PlayerSkinType.getDefaultDescriptorFor(playerEntity);
                        skinFailed = true;
                    }

                    if (resultMap.get(bedrockGeom).isFailed()) {
                        finalGeom = SkinGeometryType.getDefaultDescriptorFor(playerEntity);
                        geometryFailed = true;
                    }

                    if (resultMap.get(bedrockCape).isFailed()) {
                        finalCape = CapeType.getDefaultDescriptorFor(playerEntity);
                        capeFailed = true;
                    }

                    PlayerSkinProfile skinProfile = PlayerSkinProfile.builder()
                            .resourceUri(PlayerSkinProfile.getUriFor(playerEntity))
                            .playerId(playerEntity.getUuid())
                            .bedrockSkinLoaded(!skinFailed || !geometryFailed || !capeFailed)
                            .skinDescriptor(finalSkin)
                            .geometryDescriptor(finalGeom)
                            .capeDescriptor(finalCape)
                            .build();

                    ResourceManager.add(PlayerSkinProfile.getDescriptorFor(skinProfile), skinProfile);
                    updatePlayerList(session, playerEntity);
                });
    }

    public static void registerJavaSkin(PlayerEntity playerEntity, GeyserSession session) {
        PlayerSkinProfile playerSkinProfile = getSkinProfile(playerEntity);
        if (playerSkinProfile != null && playerSkinProfile.isBedrockSkinLoaded()) {
            return;
        }

        // check if actually a bedrock player
        boolean isBedrock = GeyserConnector.getInstance().getPlayerByUuid(playerEntity.getUuid()) != null;

        final GameProfileSkinParams gameProfileSkinParams = GameProfileSkinParams.of(playerEntity.getProfile());
        final ResourceDescriptor<PlayerSkin, GameProfileSkinParams> javaSkin = PlayerSkinType.JAVA_GAME_PROFILE.getDescriptorFor(playerEntity, gameProfileSkinParams);
        final ResourceDescriptor<SkinGeometry, GameProfileSkinParams> javaGeom = SkinGeometryType.JAVA_GAME_PROFILE.getDescriptorFor(playerEntity, gameProfileSkinParams);
        final ResourceDescriptor<Cape, GameProfileSkinParams> javaCape = CapeType.JAVA_GAME_PROFILE.getDescriptorFor(playerEntity, gameProfileSkinParams);

        ResourceManager.loadAsync(javaSkin, javaGeom, javaCape)
                .whenComplete((resultMap, throwable) -> {

                    boolean isEars = false, usingBedrockSkin = false;
                    ResourceDescriptor<PlayerSkin, ?> finalSkin = javaSkin;
                    ResourceDescriptor<SkinGeometry, ?> finalGeom = javaGeom;
                    ResourceDescriptor<Ears, ?> finalEars = EarsType.NONE.getDescriptorFor(playerEntity);
                    ResourceDescriptor<Cape, ?> finalCape = javaCape;

                    // no custom skin was specified
                    if (resultMap.get(javaSkin).isFailed()) {
                        if (isBedrock) {
                            finalSkin = PlayerSkinType.BEDROCK_CLIENT_DATA.getDescriptorFor(playerEntity);
                            ResourceLoadResult bedrockSkinResult = ResourceManager.loadAsync(finalSkin).join();

                            if (bedrockSkinResult.isFailed()) {
                                finalSkin = PlayerSkinType.getDefaultDescriptorFor(playerEntity);
                                ResourceManager.loadAsync(finalSkin).join();
                            } else {
                                usingBedrockSkin = true;
                            }
                        } else {
                            // java player (they are stuck with default skins)
                            finalSkin = PlayerSkinType.getDefaultDescriptorFor(playerEntity);
                            ResourceManager.loadAsync(finalSkin).join();
                        }
                    }

                    // Not a bedrock player, check for ears
                    if (!isBedrock && ALLOW_THIRD_PARTY_EARS) {
                        // Its deadmau5, gotta support his skin :)
                        if (playerEntity.getUuid().toString().equals("1e18d5ff-643d-45c8-b509-43b8461d8614")) {
                            finalEars = EarsType.DEADMAU5.getDescriptorFor(playerEntity);
                            isEars = true;
                        } else {
                            // Get the ears texture for the player
                            for (EarsType earsType : EarsType.values(EnumSet.of(TextureIdUriType.UUID, TextureIdUriType.UUID_DASHED, TextureIdUriType.USERNAME))) {
                                ResourceDescriptor<Ears, Void> earDescriptor = earsType.getDescriptorFor(playerEntity);
                                ResourceLoadResult earResult = ResourceManager.loadAsync(earDescriptor).join();
                                if (!earResult.isFailed()) {
                                    isEars = true;
                                    finalEars = earDescriptor;
                                    break;
                                }
                            }
                        }
                    }

                    if (resultMap.get(javaGeom).isFailed()) {
                        if (isBedrock && usingBedrockSkin) {
                            finalGeom = SkinGeometryType.BEDROCK_CLIENT_DATA.getDescriptorFor(playerEntity);
                            ResourceLoadResult bedrockGeomResult = ResourceManager.loadAsync(finalGeom).join();

                            if (bedrockGeomResult.isFailed()) {
                                finalGeom = SkinGeometryType.getDefaultDescriptorFor(playerEntity);
                                ResourceManager.loadAsync(finalGeom).join();
                            }
                        } else {
                            // java player (they are stuck with default skins)
                            finalGeom = SkinGeometryType.getDefaultDescriptorFor(playerEntity);
                            ResourceManager.loadAsync(finalGeom).join();
                        }
                    }

                    if (!isBedrock && isEars) {
                        if (playerEntity.isSlimByDefault()) {
                            finalGeom = SkinGeometryType.EARS_SLIM.getDescriptorFor(playerEntity);
                        } else {
                            finalGeom = SkinGeometryType.EARS.getDescriptorFor(playerEntity);
                        }
                        ResourceManager.loadAsync(finalGeom).join();

                        // get merged ears
                        ResourceDescriptor<PlayerSkin, JavaEarsSkinParams> mergedSkinDescriptor = PlayerSkinType.JAVA_MERGED_EARS
                                .getDescriptorFor(playerEntity, JavaEarsSkinParams.of(finalSkin, finalEars));
                        ResourceLoadResult mergedSkinResult = ResourceManager.loadAsync(mergedSkinDescriptor).join();
                        if (!mergedSkinResult.isFailed()) {
                            finalSkin = mergedSkinDescriptor;
                        }
                    }

                    if (resultMap.get(javaCape).isFailed()) {
                        boolean foundCape = false;
                        if (ALLOW_THIRD_PARTY_CAPES) {
                            for (CapeType capeType : CapeType.values(EnumSet.of(TextureIdUriType.UUID, TextureIdUriType.UUID_DASHED, TextureIdUriType.USERNAME))) {
                                if (capeType == CapeType.BEDROCK_CLIENT_DATA && !isBedrock || (capeType == CapeType.JAVA_GAME_PROFILE)) {
                                    continue;
                                }
                                ResourceDescriptor<Cape, Void> capeDescriptor = capeType.getDescriptorFor(playerEntity);
                                ResourceLoadResult capeResult = ResourceManager.loadAsync(capeDescriptor).join();
                                if (!capeResult.isFailed()) {
                                    finalCape = capeDescriptor;
                                    foundCape = true;
                                    break;
                                }
                            }
                        }
                        if (!foundCape) {
                            finalCape = CapeType.getDefaultDescriptorFor(playerEntity);
                        }
                    }

                    PlayerSkinProfile skinProfile = PlayerSkinProfile.builder()
                            .resourceUri(PlayerSkinProfile.getUriFor(playerEntity))
                            .playerId(playerEntity.getUuid())
                            .bedrockSkinLoaded(false)
                            .skinDescriptor(finalSkin)
                            .geometryDescriptor(finalGeom)
                            .capeDescriptor(finalCape)
                            .build();

                    ResourceManager.add(PlayerSkinProfile.getDescriptorFor(skinProfile), skinProfile);
                    updatePlayerList(session, playerEntity);
                });
    }

    public static CompletableFuture<GameProfile> refreshGameProfile(@NonNull UUID playerUuid) {
        ResourceDescriptor<GameProfileData, Void> gameProfileDescriptor = ResourceDescriptor.of(
                GameProfileDataType.MINECRAFT.getUriFor(playerUuid),
                GameProfileData.class);

        CompletableFuture<GameProfile> future = new CompletableFuture<>();

        ResourceManager.loadAsync(gameProfileDescriptor)
                .whenComplete((result, throwable) -> {
                    if (result != null && throwable == null) {
                        GameProfileData data = (GameProfileData)result.getResource();
                        future.complete(data.getGameProfile());
                    } else {
                        future.completeExceptionally(throwable);
                    }
                });

        return future;
    }


    public static void registerSkull(PlayerEntity playerEntity, GeyserSession session,
                                     Consumer<Skull> skinConsumer) {

        final GameProfileSkinParams gameProfileSkinParams = GameProfileSkinParams.of(playerEntity.getProfile());
        final ResourceDescriptor<Skull, GameProfileSkinParams> javaSkull = SkullType.MINECRAFT.getDescriptorFor(playerEntity, gameProfileSkinParams);
        final ResourceDescriptor<SkinGeometry, Void> skullGeom = SkinGeometryType.CUSTOM_SKULL.getDescriptorFor(playerEntity);

        ResourceManager.loadAsync(javaSkull, skullGeom)
                .whenComplete((resultMap, throwable) -> {
                    Skull skull = null;
                    if (!resultMap.get(javaSkull).isFailed() && !resultMap.get(skullGeom).isFailed()) {
                        skull = (Skull)resultMap.get(javaSkull).getResource();
                        if (session.getUpstream().isInitialized()) {

                            SkinGeometry skinGeometry = (SkinGeometry)resultMap.get(skullGeom).getResource();
                            PlayerListPacket.Entry updatedEntry = buildSkullEntryManually(
                                    playerEntity.getUuid(),
                                    playerEntity.getUsername(),
                                    playerEntity.getGeyserId(),
                                    skull,
                                    skinGeometry
                            );

                            PlayerListPacket playerAddPacket = new PlayerListPacket();
                            playerAddPacket.setAction(PlayerListPacket.Action.ADD);
                            playerAddPacket.getEntries().add(updatedEntry);
                            session.sendUpstreamPacket(playerAddPacket);

                            // It's a skull. We don't want them in the player list.
                            PlayerListPacket playerRemovePacket = new PlayerListPacket();
                            playerRemovePacket.setAction(PlayerListPacket.Action.REMOVE);
                            playerRemovePacket.getEntries().add(updatedEntry);
                            session.sendUpstreamPacket(playerRemovePacket);
                        }
                    }
                    if (skinConsumer != null) {
                        skinConsumer.accept(skull);
                    }
                });


    }

    public static PlayerListPacket.Entry buildSkullEntryManually(UUID uuid, String username, long geyserId,
                                                                 Skull skull, SkinGeometry skullGeometry) {

        SerializedSkin serializedSkin = SerializedSkin.of(skull.getSkullId(), skullGeometry.getResourcePatch(),
                ImageData.of(skull.getSkullData().getData()), Collections.emptyList(), null, skullGeometry.getData(),
                "", true, false, false, null, skull.getSkullId());

        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(uuid);
        entry.setName(username);
        entry.setEntityId(geyserId);
        entry.setSkin(serializedSkin);
        entry.setXuid("");
        entry.setPlatformChatId("");
        entry.setTeacher(false);
        entry.setTrustedSkin(true);
        return entry;
    }

    public static UUID getUUIDForSkullOwner(CompoundTag skullOwner) {
        Tag uuidTag = skullOwner.get("Id");
        String uuidToString = null;
        JsonNode node;
        boolean retrieveUuidFromInternet = !(uuidTag instanceof IntArrayTag); // also covers null check

        if (!retrieveUuidFromInternet) {
            int[] uuidAsArray = ((IntArrayTag) uuidTag).getValue();
            // thank u viaversion
            UUID uuid = new UUID((long) uuidAsArray[0] << 32 | ((long) uuidAsArray[1] & 0xFFFFFFFFL),
                    (long) uuidAsArray[2] << 32 | ((long) uuidAsArray[3] & 0xFFFFFFFFL));
            retrieveUuidFromInternet = uuid.version() != 4;
            uuidToString = UUIDUtils.toMinifiedUUID(uuid.toString());
        }

        if (retrieveUuidFromInternet) {
            try {
                // Offline skin, or no present UUID
                node = WebUtils.getJson("https://api.mojang.com/users/profiles/minecraft/" + skullOwner.get("Name").getValue());
                JsonNode id = node.get("id");
                if (id == null) {
                    GeyserConnector.getInstance().getLogger().debug("No UUID found in Mojang response for " + skullOwner.get("Name").getValue());
                    return null;
                }
                uuidToString = id.asText();
            } catch (Exception e) {
                if (GeyserConnector.getInstance().getConfig().isDebugMode()) {
                    e.printStackTrace();
                }
            }
        }

        if (uuidToString != null) {
            return UUID.fromString(UUIDUtils.toDashedUUID(uuidToString));
        }
        return null;
    }

    public static void updatePlayerList(GeyserSession session, PlayerEntity entity) {
        if (session.getUpstream().isInitialized()) {
            PlayerListPacket.Entry updatedEntry = buildCachedEntry(session, entity);

            PlayerListPacket playerRemovePacket = new PlayerListPacket();
            playerRemovePacket.setAction(PlayerListPacket.Action.REMOVE);
            playerRemovePacket.getEntries().add(updatedEntry);
            session.sendUpstreamPacket(playerRemovePacket);

            PlayerListPacket playerAddPacket = new PlayerListPacket();
            playerAddPacket.setAction(PlayerListPacket.Action.ADD);
            playerAddPacket.getEntries().add(updatedEntry);
            session.sendUpstreamPacket(playerAddPacket);

            if (!entity.isPlayerList()) {
                playerRemovePacket = new PlayerListPacket();
                playerRemovePacket.setAction(PlayerListPacket.Action.REMOVE);
                playerRemovePacket.getEntries().add(updatedEntry);
                session.sendUpstreamPacket(playerRemovePacket);
            }
        }
    }

    public static PlayerListPacket.Entry buildCachedEntry(GeyserSession session, PlayerEntity playerEntity) {
        PlayerSkinProfile skinProfile = getSkinProfile(playerEntity);

        PlayerSkin skin = ResourceManager.get(skinProfile.getSkinDescriptor());
        Cape cape = ResourceManager.get(skinProfile.getCapeDescriptor());
        SkinGeometry skinGeometry = ResourceManager.get(skinProfile.getGeometryDescriptor());

        GeyserConnector.getInstance().getLogger().info("SkinProfile: "+ skinProfile);

        return buildEntryManually(
                session,
                playerEntity.getUuid(),
                playerEntity.getUsername(),
                playerEntity.getGeyserId(),
                skin,
                cape,
                skinGeometry);
    }

    private static PlayerListPacket.Entry buildEntryManually(GeyserSession session, UUID uuid, String username, long geyserId,
                                                             PlayerSkin skin, Cape cape, SkinGeometry geometry) {
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
            skinImage = ImageData.of(skin.getSkinData().getWidth(), skin.getSkinData().getHeight(), skin.getSkinData().getData());
            capeImage = ImageData.of(cape.getCapeData().getWidth(), cape.getCapeData().getHeight(), cape.getCapeData().getData());
        } else {
            skinImage = ImageData.of(skin.getSkinData().getData());
            capeImage = ImageData.of(cape.getCapeData().getData());
        }

        SerializedSkin serializedSkin = SerializedSkin.of(skin.getSkinId(), geometry.getResourcePatch(), skinImage,
                animations, capeImage, geometry.getData(), skin.getAnimationData(), skin.isPremium(), skin.isPersona(),
                skin.isCapeOnClassic(), cape.getCapeId(), skin.getSkinId() + cape.getCapeId(),
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
        return entry;
    }


    private static PlayerSkinProfile getSkinProfile(PlayerEntity playerEntity) {
        return ResourceManager.get(PlayerSkinProfile.getDescriptorFor(playerEntity));
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

    private static TextureType toTextureType(AnimatedTextureType animatedTextureType) {
        if (animatedTextureType != null) {
            switch (animatedTextureType) {
                case FACE:
                    return TextureType.FACE;
                case BODY_32X32:
                    return TextureType.BODY_32X32;
                case BODY_128X128:
                    return TextureType.BODY_128X128;
                case NONE:
                default:
                    return TextureType.NONE;
            }
        }
        return TextureType.NONE;
    }

    private static AnimationExpressionType toAnimationExpressionType(ExpressionType expressionType) {
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

    private static ExpressionType toExpressionType(AnimationExpressionType animationExpressionType) {
        if (animationExpressionType != null) {
            switch (animationExpressionType) {
                case BLINKING:
                    return ExpressionType.BLINKING;
                case LINEAR:
                default:
                    return ExpressionType.LINEAR;
            }
        }
        return ExpressionType.LINEAR;
    }

    private static AnimationData toAnimationData(SkinAnimation skinAnimation) {
        return new AnimationData(ImageData.of(skinAnimation.getImageWidth(), skinAnimation.getImageHeight(),
                skinAnimation.getImageData()), toAnimatedTextureType(skinAnimation.getTextureType()),
                skinAnimation.getFrames(), toAnimationExpressionType(skinAnimation.getExpressionType()));
    }

    private static SkinAnimation toSkinAnimation(AnimationData animationData) {
        ImageData image = animationData.getImage();
        return new SkinAnimation(image.getImage(), image.getHeight(), image.getWidth(), toTextureType(animationData.getTextureType()),
                animationData.getFrames(), toExpressionType(animationData.getExpressionType()));
    }

    private static PersonaPieceData toPersonaPieceData(PersonaSkinPiece personaSkinPiece) {
        return new PersonaPieceData(personaSkinPiece.getId(), personaSkinPiece.getType(), personaSkinPiece.getPackId(), personaSkinPiece.isDefault(),
                personaSkinPiece.getProductId());
    }

    private static PersonaSkinPiece toPersonaPieceData(PersonaPieceData personaPieceData) {
        return new PersonaSkinPiece(personaPieceData.getId(), personaPieceData.getType(), personaPieceData.getPackId(), personaPieceData.isDefault(),
                personaPieceData.getProductId());
    }

    private static PersonaPieceTintData toPersonaPieceTintData(PersonaSkinPieceTintColor personaSkinPieceTintColor) {
        return new PersonaPieceTintData(personaSkinPieceTintColor.getType(), personaSkinPieceTintColor.getColors());
    }

    private static PersonaSkinPieceTintColor toPersonaSkinPieceTintColor(PersonaPieceTintData personaPieceTintData) {
        return new PersonaSkinPieceTintColor(personaPieceTintData.getType(), personaPieceTintData.getColors());
    }
}
