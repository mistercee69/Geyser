/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import com.google.common.collect.Maps;
import com.nukkitx.protocol.bedrock.data.skin.*;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.common.AuthType;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.entity.player.SkullPlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.skin.resource.ResourceDescriptor;
import org.geysermc.connector.skin.resource.ResourceLoadResult;
import org.geysermc.connector.skin.resource.ResourceManager;
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
import java.util.stream.Collectors;

public class SkinManager {
    private static final boolean ALLOW_THIRD_PARTY_CAPES = GeyserConnector.getInstance().getConfig().isAllowThirdPartyCapes();
    private static final boolean ALLOW_THIRD_PARTY_EARS = GeyserConnector.getInstance().getConfig().isAllowThirdPartyEars();
    private static final Map<UUID, CompletableFuture<PlayerSkinProfile>> skinRegistrationInProgress = Maps.newConcurrentMap();
    private static final Map<UUID, CompletableFuture<PlayerSkullProfile>> skullRegistrationInProgress = Maps.newConcurrentMap();

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
                    logger().debug(String.format("Removed %d cached image files as they have expired", count));
                }
            }, 10, 1440, TimeUnit.MINUTES);
        }
    }

    public static synchronized void updateBedrockSkin(PlayerEntity playerEntity, GeyserSession session, SerializedSkin skin) {
        logger().debug(String.format("Updating bedrock client skin %s %s", playerEntity.getUsername(), playerEntity.getUuid()));

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
        session.getClientData().setResourcePatch(skin.getSkinResourcePatch().getBytes(StandardCharsets.UTF_8));
        session.getClientData().setGeometryData(skin.getGeometryData().getBytes(StandardCharsets.UTF_8));
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

        refreshPlayerSkins(playerEntity, true);
    }

    public static void refreshPlayerSkull(final SkullPlayerEntity playerEntity, final GeyserSession session, final Runnable onCompletion) {
        // check if all required skins are already cached
        PlayerSkullProfile playerSkullProfile = ResourceManager.get(PlayerSkullProfile.getDescriptorFor(playerEntity));
        if (!resourcesAvailable(playerSkullProfile)) {
            skullRegistrationInProgress.computeIfAbsent(playerEntity.getUuid(), uuid -> registerSkull(playerEntity).whenCompleteAsync((skullProfile, throwable) -> {
                skullRegistrationInProgress.remove(playerEntity.getUuid());
                skullCompletion(playerEntity, session, onCompletion, skullProfile);
            }));
        } else {
            CompletableFuture.runAsync(() -> skullCompletion(playerEntity, session, onCompletion, playerSkullProfile));
        }
    }

    private static void skullCompletion(SkullPlayerEntity playerEntity, GeyserSession session, Runnable onCompletion, PlayerSkullProfile skullProfile) {
        if (skullProfile != null) {
            session.getPlayerListManager().notifyPlayerSkullProfileUpdate(playerEntity, skullProfile);
            if (onCompletion != null) {
                try {
                    onCompletion.run();
                } catch (Throwable t) {
                    logger().error("refreshPlayerSkull onCompletion failed", t);
                }
            }
        }
    }

    public static void refreshPlayerSkins(PlayerEntity playerEntity, boolean force) {
        // check if all required skins are already cached
        PlayerSkinProfile playerSkinProfile = ResourceManager.get(PlayerSkinProfile.getDescriptorFor(playerEntity));
        if (!resourcesAvailable(playerSkinProfile)) {
            skinRegistrationInProgress.computeIfAbsent(playerEntity.getUuid(), uuid -> registerWithFuture(playerEntity, force).whenCompleteAsync((skinProfile, throwable) -> {
                skinRegistrationInProgress.remove(playerEntity.getUuid());
                if (skinProfile != null) {
                    GeyserConnector.getInstance().getPlayers().forEach(session -> session.getPlayerListManager().notifyPlayerProfileUpdate(playerEntity, skinProfile));
                }
            }));
        } else {
            CompletableFuture.runAsync(() -> GeyserConnector.getInstance().getPlayers().forEach(session -> session.getPlayerListManager().notifyPlayerProfileUpdate(playerEntity, playerSkinProfile)));
        }
    }

    private static CompletableFuture<PlayerSkinProfile> registerWithFuture(PlayerEntity playerEntity, boolean force) {
        // Use java skins if:
        //    (1) server is using online auth
        //    (2) player entity is a java user (no GeyserSession)
        boolean isJavaPlayer = GeyserConnector.getInstance().getPlayerByUuid(playerEntity.getUuid()) == null;
        if (GeyserConnector.getInstance().getAuthType() == AuthType.ONLINE || isJavaPlayer) {
            return registerJavaSkin(playerEntity, force);
        } else {
            return registerBedrockSkin(playerEntity, force);
        }
    }

    private static boolean resourcesAvailable(PlayerSkinProfile playerSkinProfile) {
        return playerSkinProfile != null && ResourceManager.allAvailable(playerSkinProfile.getDescriptors());
    }

    private static boolean resourcesAvailable(PlayerSkullProfile playerSkullProfile) {
        return playerSkullProfile != null && ResourceManager.allAvailable(playerSkullProfile.getDescriptors());
    }

    private static CompletableFuture<PlayerSkinProfile> registerBedrockSkin(PlayerEntity playerEntity, boolean force) {
        logger().info(LanguageUtils.getLocaleStringLog("geyser.skin.bedrock.register",
                playerEntity.getUsername(), playerEntity.getUuid()));

        final ResourceDescriptor<Skin, Void> bedrockSkin = SkinType.BEDROCK_CLIENT_DATA.getDescriptorFor(playerEntity);
        final ResourceDescriptor<SkinGeometry, Void> bedrockGeom = SkinGeometryType.BEDROCK_CLIENT_DATA.getDescriptorFor(playerEntity);
        final ResourceDescriptor<Cape, Void> bedrockCape = CapeType.BEDROCK_CLIENT_DATA.getDescriptorFor(playerEntity);

        CompletableFuture<PlayerSkinProfile> skinProfileFuture = new CompletableFuture<>();
        ResourceManager.loadAsync(force, bedrockSkin, bedrockGeom, bedrockCape)
                .whenComplete((resultMap, throwable) -> {
                    try {
                        ResourceDescriptor<Skin, ?> finalSkin = bedrockSkin;
                        ResourceDescriptor<SkinGeometry, ?> finalGeom = bedrockGeom;
                        ResourceDescriptor<Cape, ?> finalCape = bedrockCape;

                        if (resultMap.get(bedrockSkin).isFailed()) {
                            finalSkin = SkinType.getDefaultDescriptorFor(playerEntity);
                            ResourceManager.loadAsync(finalSkin).join();
                        }

                        if (resultMap.get(bedrockGeom).isFailed()) {
                            finalGeom = SkinGeometryType.getDefaultDescriptorFor(playerEntity);
                            ResourceManager.loadAsync(finalGeom).join();
                        }

                        if (resultMap.get(bedrockCape).isFailed()) {
                            finalCape = CapeType.getDefaultDescriptorFor(playerEntity);
                            ResourceManager.loadAsync(finalCape).join();
                        }

                        PlayerSkinProfile skinProfile = PlayerSkinProfile.builder()
                                .resourceUri(PlayerSkinProfile.getUriFor(playerEntity))
                                .playerId(playerEntity.getUuid())
                                .bedrockSkinLoaded(true)
                                .skinDescriptor(finalSkin)
                                .geometryDescriptor(finalGeom)
                                .capeDescriptor(finalCape)
                                .build();

                        ResourceManager.add(PlayerSkinProfile.getDescriptorFor(skinProfile), skinProfile);
                        skinProfileFuture.complete(skinProfile);
                    } catch (Throwable t) {
                        logger().error("registerBedrockSkin.whenComplete failed", t);
                        skinProfileFuture.completeExceptionally(t);
                    }
                });
        return skinProfileFuture;
    }

    private static CompletableFuture<PlayerSkinProfile> registerJavaSkin(PlayerEntity playerEntity, boolean force) {
        logger().info(String.format("Registering java skin %s %s",playerEntity.getUsername(), playerEntity.getUuid()));

        // check if actually a bedrock player
        final boolean isBedrock = GeyserConnector.getInstance().getPlayerByUuid(playerEntity.getUuid()) != null;

        final ResourceDescriptor<Skin, Void> javaSkin = SkinType.JAVA_SERVER_GAME_PROFILE.getDescriptorFor(playerEntity);
        final ResourceDescriptor<SkinGeometry, Void> javaGeom = SkinGeometryType.getDefaultDescriptorFor(playerEntity);
        final ResourceDescriptor<Cape, Void> javaCape = CapeType.JAVA_SERVER_GAME_PROFILE.getDescriptorFor(playerEntity);

        CompletableFuture<PlayerSkinProfile> skinProfileFuture = new CompletableFuture<>();
        ResourceManager.loadAsync(force, javaSkin, javaGeom, javaCape)
                .whenComplete((resultMap, throwable) -> {
                    try {
                        boolean isEars = false, usingBedrockSkin = false;
                        ResourceDescriptor<Skin, ?> finalSkin = javaSkin;
                        ResourceDescriptor<SkinGeometry, ?> finalGeom = javaGeom;
                        ResourceDescriptor<Ears, ?> finalEars = EarsType.NONE.getDescriptorFor(playerEntity);
                        ResourceDescriptor<Cape, ?> finalCape = javaCape;

                        // no custom skin was specified
                        if (resultMap.get(javaSkin).isFailed()) {
                            if (isBedrock) {
                                finalSkin = SkinType.BEDROCK_CLIENT_DATA.getDescriptorFor(playerEntity);
                                ResourceLoadResult bedrockSkinResult = ResourceManager.loadAsync(finalSkin).join();

                                if (bedrockSkinResult.isFailed()) {
                                    finalSkin = SkinType.getDefaultDescriptorFor(playerEntity);
                                    ResourceManager.loadAsync(finalSkin).join();
                                } else {
                                    usingBedrockSkin = true;
                                }
                            } else {
                                // java player (they are stuck with default skins)
                                finalSkin = SkinType.getDefaultDescriptorFor(playerEntity);
                                ResourceManager.loadAsync(finalSkin).join();
                            }
                        }

                        // Not a bedrock player, check for ears
                        if (!isBedrock && ALLOW_THIRD_PARTY_EARS) {
                            // Its deadmau5, gotta support his skin :)
                            if (playerEntity.getUuid().toString().equals("1e18d5ff-643d-45c8-b509-43b8461d8614")) {
                                finalEars = EarsType.DEADMAU5.getDescriptorFor(playerEntity);
                                ResourceManager.loadAsync(finalEars).join();
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
                            if (playerEntity.isSlim()) {
                                finalGeom = SkinGeometryType.EARS_SLIM.getDescriptorFor(playerEntity);
                            } else {
                                finalGeom = SkinGeometryType.EARS.getDescriptorFor(playerEntity);
                            }
                            ResourceManager.loadAsync(finalGeom).join();

                            // get merged ears
                            ResourceDescriptor<Skin, JavaEarsSkinParams> mergedSkinDescriptor = SkinType.JAVA_MERGED_EARS
                                    .getDescriptorFor(playerEntity, JavaEarsSkinParams.of(finalSkin, finalEars));
                            ResourceLoadResult mergedSkinResult = ResourceManager.loadAsync(mergedSkinDescriptor).join();
                            if (!mergedSkinResult.isFailed()) {
                                finalSkin = mergedSkinDescriptor;
                            }
                        }

                        if (resultMap.get(javaCape).isFailed()) {
                            boolean foundCape = false;
                            if (ALLOW_THIRD_PARTY_CAPES) {
                                for (CapeType capeType : CapeType.values(TextureIdUriType.setExcluding(TextureIdUriType.NONE))) {
                                    if (capeType == CapeType.BEDROCK_CLIENT_DATA && !isBedrock || (capeType == CapeType.JAVA_SERVER_GAME_PROFILE)) {
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
                                ResourceManager.loadAsync(finalCape).join();
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
                        skinProfileFuture.complete(skinProfile);
                    } catch (Throwable t) {
                        logger().error("registerJavaSkin.whenComplete failed", t);
                        skinProfileFuture.completeExceptionally(t);
                    }
                });
        return skinProfileFuture;
    }


    private static CompletableFuture<PlayerSkullProfile> registerSkull(PlayerEntity playerEntity) {
        logger().debug(String.format("Registering java skull %s %s",playerEntity.getUsername(), playerEntity.getUuid()));

        final ResourceDescriptor<Skull, Void> javaSkull = SkullType.JAVA_SERVER_GAME_PROFILE.getDescriptorFor(playerEntity);
        final ResourceDescriptor<SkinGeometry, Void> skullGeom = SkinGeometryType.CUSTOM_SKULL.getDescriptorFor(playerEntity);
        CompletableFuture<PlayerSkullProfile> skullProfileFuture = new CompletableFuture<>();

        ResourceManager.loadAsync(javaSkull, skullGeom)
                .whenComplete((resultMap, throwable) -> {
                    try {
                        ResourceDescriptor<Skull, ?> finalSkull = javaSkull;

                        boolean skullFailed = resultMap.get(javaSkull).isFailed();
                        boolean geomFailed = resultMap.get(skullGeom).isFailed();

                        if (skullFailed) {
                            finalSkull = SkullType.getDefaultDescriptorFor(playerEntity);
                            ResourceLoadResult loadResult = ResourceManager.loadAsync(finalSkull).join();
                            skullFailed = loadResult.isFailed();
                        }

                        if (!skullFailed && !geomFailed) {
                            PlayerSkullProfile skullProfile = PlayerSkullProfile.builder()
                                    .resourceUri(PlayerSkullProfile.getUriFor(playerEntity))
                                    .playerId(playerEntity.getUuid())
                                    .skullDescriptor(finalSkull)
                                    .geometryDescriptor(skullGeom)
                                    .build();

                            logger().debug("SkullProfile: " + skullProfile);
                            ResourceManager.add(PlayerSkullProfile.getDescriptorFor(skullProfile), skullProfile);
                            skullProfileFuture.complete(skullProfile);
                        } else {
                            throw new RuntimeException("Loading skull failed");
                        }
                    } catch (Throwable t) {
                        logger().error("registerBedrockSkin.whenComplete failed", t);
                        skullProfileFuture.completeExceptionally(t);
                    }
                });
        return skullProfileFuture;
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
                    logger().debug("No UUID found in Mojang response for " + skullOwner.get("Name").getValue());
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

    public static CompletableFuture<GameProfile> refreshGameProfile(UUID playerUuid) {
        if (playerUuid != null) {
            ResourceDescriptor<GameProfileData, Void> gameProfileDescriptor = ResourceDescriptor.of(
                    GameProfileDataType.MINECRAFT.getUriFor(playerUuid),
                    GameProfileData.class);

            CompletableFuture<GameProfile> future = new CompletableFuture<>();

            ResourceManager.loadAsync(gameProfileDescriptor)
                    .whenComplete((result, throwable) -> {
                        if (result != null && throwable == null) {
                            GameProfileData data = (GameProfileData) result.getResource();
                            future.complete(data.getGameProfile());
                        } else {
                            future.completeExceptionally(throwable);
                        }
                    });

            return future;
        }
        return CompletableFuture.completedFuture(null);
    }

    private static BedrockClientData.ExpressionType toExpressionType(AnimationExpressionType animationExpressionType) {
        if (animationExpressionType != null) {
            switch (animationExpressionType) {
                case BLINKING:
                    return BedrockClientData.ExpressionType.BLINKING;
                case LINEAR:
                default:
                    return BedrockClientData.ExpressionType.LINEAR;
            }
        }
        return BedrockClientData.ExpressionType.LINEAR;
    }

    private static BedrockClientData.SkinAnimation toSkinAnimation(AnimationData animationData) {
        ImageData image = animationData.getImage();
        return new BedrockClientData.SkinAnimation(image.getImage(), image.getHeight(), image.getWidth(), toTextureType(animationData.getTextureType()),
                animationData.getFrames(), toExpressionType(animationData.getExpressionType()));
    }

    private static BedrockClientData.PersonaSkinPieceTintColor toPersonaSkinPieceTintColor(PersonaPieceTintData personaPieceTintData) {
        return new BedrockClientData.PersonaSkinPieceTintColor(personaPieceTintData.getType(), personaPieceTintData.getColors());
    }

    private static BedrockClientData.PersonaSkinPiece toPersonaPieceData(PersonaPieceData personaPieceData) {
        return new BedrockClientData.PersonaSkinPiece(personaPieceData.getId(), personaPieceData.getType(), personaPieceData.getPackId(), personaPieceData.isDefault(),
                personaPieceData.getProductId());
    }

    private static BedrockClientData.TextureType toTextureType(AnimatedTextureType animatedTextureType) {
        if (animatedTextureType != null) {
            switch (animatedTextureType) {
                case FACE:
                    return BedrockClientData.TextureType.FACE;
                case BODY_32X32:
                    return BedrockClientData.TextureType.BODY_32X32;
                case BODY_128X128:
                    return BedrockClientData.TextureType.BODY_128X128;
                case NONE:
                default:
                    return BedrockClientData.TextureType.NONE;
            }
        }
        return BedrockClientData.TextureType.NONE;
    }

    private static GeyserLogger logger() {
        return GeyserConnector.getInstance().getLogger();
    }
}
