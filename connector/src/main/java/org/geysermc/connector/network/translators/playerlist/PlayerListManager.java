package org.geysermc.connector.network.translators.playerlist;

import com.nukkitx.protocol.bedrock.data.skin.*;
import com.nukkitx.protocol.bedrock.packet.AdventureSettingsPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.entity.player.SkullPlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.skin.SkinManager;
import org.geysermc.connector.skin.resource.ResourceManager;
import org.geysermc.connector.skin.resource.types.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerListManager {

    private final GeyserSession session;

    private final Map<UUID, PlayerListInfo> playerListInfoMap = new ConcurrentHashMap<>();

    public PlayerListManager(GeyserSession session) {
        this.session = session;
    }

    public void login() {
        // look for pending entries
        HashSet<UUID> uuids = new HashSet<>(playerListInfoMap.keySet());
        for (UUID uuid : uuids) {
            playerListInfoMap.computeIfPresent(uuid, (u, playerListInfo) -> {
                // avoid concurrent modification
                PlayerListInfo newPlayerListInfo = playerListInfo.toBuilder().build();
                boolean cleanUp = false;
                if (newPlayerListInfo.pendingPlayerListPacket != null) {
                    if (newPlayerListInfo.pendingPlayerListPacket.getAction() == PlayerListPacket.Action.REMOVE) {
                        cleanUp = true;
                    }
                }

                if (newPlayerListInfo.sendPackets(session)) {
                    if (cleanUp) {
                        return null;
                    }
                }
                return newPlayerListInfo;
            });
        }
    }

    public void registerPlayer(@NonNull PlayerEntity playerEntity) {
        PlayerListInfo playerListInfo = playerListInfoMap.computeIfAbsent(playerEntity.getUuid(), (uuid) -> {
            GeyserSession owningSession;
            if (playerEntity.getUuid().equals(session.getPlayerEntity().getUuid())) {
                owningSession = session;
            } else {
                owningSession = session.getConnector().getPlayerByUuid(playerEntity.getUuid());
            }
            return PlayerListInfo.builder()
                    .owningSession(owningSession)
                    .playerEntity(playerEntity)
                    .build();
        });

        // not yet sent a packet to client
        if (playerListInfo.hasNotSentUpdate()) {
            playerListInfo.generatePackets(session, ResourceManager.get(PlayerSkinProfile.getDescriptorFor(playerEntity)));
            // try to send right now
            playerListInfo.sendPackets(session);
            if (playerListInfo.getUsedDummyProfile()) {
                SkinManager.refreshPlayerSkins(playerEntity, false);
            }
        }
    }

    public void notifyPlayerSpawnUpdate(@NonNull PlayerEntity playerEntity) {
        session.getEntityCache().cacheEntity(playerEntity);

        if (session.getEntityCache().getPlayerEntity(playerEntity.getUuid()) == null) {
            session.getConnector().getLogger().error("[PLM:notifyPlayerSpawnUpdate] PlayerEntity not in cache ignoring: " + playerEntity);
            return;
        }

        playerListInfoMap.computeIfPresent(playerEntity.getUuid(), (uuid, playerListInfo) -> {
            // avoid concurrent modification
            PlayerListInfo newPlayerListInfo = playerListInfo.toBuilder().build();
            newPlayerListInfo.generateAdventureSettingsPacket(session);
            newPlayerListInfo.sendPackets(session);
            return newPlayerListInfo;
        });
    }

    public void unregisterPlayer(@NonNull PlayerEntity playerEntity) {
        if (session.getEntityCache().getPlayerEntity(playerEntity.getUuid()) == null) {
            session.getConnector().getLogger().error("[PLM:unregisterPlayer] PlayerEntity not in cache ignoring: " + playerEntity);
            return;
        }

        playerListInfoMap.computeIfPresent(playerEntity.getUuid(), (uuid, playerListInfo) -> {
            // avoid concurrent modification
            PlayerListInfo newPlayerListInfo = playerListInfo.toBuilder().playerEntity(playerEntity).build();
            newPlayerListInfo.generatePackets(session, newPlayerListInfo.lastPlayerSkinProfile);
            if (newPlayerListInfo.sendPackets(session)) {
                return null;
            }
            return newPlayerListInfo;
        });
    }

    public void forcePlayerProfileUpdate(@NonNull PlayerEntity playerEntity) {
        if (session.getEntityCache().getPlayerEntity(playerEntity.getUuid()) == null) {
            session.getConnector().getLogger().error("[PLM:forcePlayerProfileUpdate] PlayerEntity not in cache ignoring: " + playerEntity);
            return;
        }

        playerListInfoMap.computeIfPresent(playerEntity.getUuid(), (uuid, playerListInfo) -> {
            // avoid concurrent modification
            PlayerListInfo newPlayerListInfo = playerListInfo.toBuilder().build();
            newPlayerListInfo.generatePackets(session, newPlayerListInfo.lastPlayerSkinProfile);
            newPlayerListInfo.sendPackets(session);
            return newPlayerListInfo;
        });
    }

    public void notifyPlayerProfileUpdate(@NonNull PlayerEntity playerEntity, @NonNull PlayerSkinProfile playerSkinProfile) {
        if (session.getEntityCache().getPlayerEntity(playerEntity.getUuid()) == null) {
            session.getConnector().getLogger().error("[PLM:notifyPlayerProfileUpdate] PlayerEntity not in cache ignoring: " + playerEntity);
            return;
        }

        playerListInfoMap.computeIfPresent(playerEntity.getUuid(), (uuid, playerListInfo) -> {
            if (playerListInfo.shouldUpdatePlayerList(playerEntity, playerSkinProfile)) {
                // avoid concurrent modification
                PlayerListInfo newPlayerListInfo = playerListInfo.toBuilder().playerEntity(playerEntity).build();
                newPlayerListInfo.generatePackets(session, playerSkinProfile);
                newPlayerListInfo.sendPackets(session);
                return newPlayerListInfo;
            }
            return playerListInfo;
        });
    }

    public void notifyPlayerSkullProfileUpdate(@NonNull SkullPlayerEntity skullPlayerEntity, @NonNull PlayerSkullProfile playerSkullProfile) {
        Skull skull = ResourceManager.get(playerSkullProfile.getSkullDescriptor());
        SkinGeometry geometry = ResourceManager.get(playerSkullProfile.getGeometryDescriptor());

        if (session.getUpstream().isInitialized()) {
            PlayerListPacket.Entry updatedEntry = buildSkullEntryManually(
                    skullPlayerEntity.getUuid(),
                    skullPlayerEntity.getUsername(),
                    skullPlayerEntity.getGeyserId(),
                    skull,
                    geometry
            );

            PlayerListPacket playerAddPacket = new PlayerListPacket();
            playerAddPacket.setAction(PlayerListPacket.Action.ADD);
            playerAddPacket.getEntries().add(updatedEntry);
            session.getConnector().getLogger(session).debug("[PLM:notifyPlayerSkullProfileUpdate] PlayerSkullEntry Add: " + playerAddPacket);
            session.sendUpstreamPacket(playerAddPacket);

            // It's a skull. We don't want them in the player list.
            PlayerListPacket playerRemovePacket = new PlayerListPacket();
            playerRemovePacket.setAction(PlayerListPacket.Action.REMOVE);
            // Only uuid is needed for a removal (don't bother sending serialized skin again)
            playerRemovePacket.getEntries().add(new PlayerListPacket.Entry(skullPlayerEntity.getUuid()));
            session.getConnector().getLogger(session).debug("[PLM:notifyPlayerSkullProfileUpdate] PlayerSkullEntry Remove: " + playerRemovePacket);
            session.sendUpstreamPacket(playerRemovePacket);
        }
    }

    private static boolean resourcesAvailable(PlayerSkinProfile playerSkinProfile) {
        return playerSkinProfile != null && ResourceManager.allAvailable(playerSkinProfile.getDescriptors());
    }

    private static AnimatedTextureType toAnimatedTextureType(BedrockClientData.TextureType textureType) {
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

    private static AnimationExpressionType toAnimationExpressionType(BedrockClientData.ExpressionType expressionType) {
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

    private static AnimationData toAnimationData(BedrockClientData.SkinAnimation skinAnimation) {
        return new AnimationData(ImageData.of(skinAnimation.getImageWidth(), skinAnimation.getImageHeight(),
                skinAnimation.getImageData()), toAnimatedTextureType(skinAnimation.getTextureType()),
                skinAnimation.getFrames(), toAnimationExpressionType(skinAnimation.getExpressionType()));
    }

    private static PersonaPieceData toPersonaPieceData(BedrockClientData.PersonaSkinPiece personaSkinPiece) {
        return new PersonaPieceData(personaSkinPiece.getId(), personaSkinPiece.getType(), personaSkinPiece.getPackId(), personaSkinPiece.isDefault(),
                personaSkinPiece.getProductId());
    }

    private static PersonaPieceTintData toPersonaPieceTintData(BedrockClientData.PersonaSkinPieceTintColor personaSkinPieceTintColor) {
        return new PersonaPieceTintData(personaSkinPieceTintColor.getType(), personaSkinPieceTintColor.getColors());
    }

    private static PlayerListPacket.Entry buildSkullEntryManually(UUID uuid, String username, long geyserId,
        Skull skull, SkinGeometry skullGeometry) {

            SerializedSkin serializedSkin = SerializedSkin.of("persona.skull."+uuid.toString(), skullGeometry.getResourcePatch(),
                    ImageData.of(skull.getSkullData().getData()), Collections.emptyList(), ImageData.EMPTY, skullGeometry.getData(),
                    "", true, false, false, "no-cape", "persona.skull."+uuid.toString());
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


    @Data
    @Builder(toBuilder = true)
    private static class PlayerListInfo {
        GeyserSession owningSession;
        PlayerEntity playerEntity;
        PlayerSkinProfile lastPlayerSkinProfile;
        Boolean usedDummyProfile;
        Boolean lastPlayerListState;
        PlayerListPacket pendingPlayerListPacket;
        AdventureSettingsPacket pendingAdventureSettingsPacket;

        boolean isOwnedBy(GeyserSession session) {
            return owningSession == session;
        }

        boolean isBedrockPlayer() {
            return owningSession != null;
        }

        boolean hasNotSentUpdate() {
            return lastPlayerSkinProfile == null || lastPlayerListState == null;
        }

        String getXuid() {
            if (owningSession != null) {
                return owningSession.getAuthData().getXboxUUID();
            }
            return "";
        }

        long getLocalGeyserId(GeyserSession session) {
            if (isOwnedBy(session)) {
                return session.getPlayerEntity().getGeyserId();
            }
            if (isBedrockPlayer()) {
                Entity entityByJavaId = session.getEntityCache().getEntityByJavaId(owningSession.getPlayerEntity().getEntityId());
                if (entityByJavaId != null) {
                    return entityByJavaId.getGeyserId();
                }
            }
            return -1;
        }

        boolean shouldUpdatePlayerList(@NonNull PlayerEntity playerEntity, @NonNull PlayerSkinProfile newPlayerSkinProfile) {
            return (hasNotSentUpdate() || !lastPlayerListState.equals(playerEntity.isPlayerList()) || !lastPlayerSkinProfile.equals(newPlayerSkinProfile));
        }

        void generatePackets(@NonNull GeyserSession session, PlayerSkinProfile playerSkinProfile) {
            generatePlayerListPacket(session, playerSkinProfile);
            generateAdventureSettingsPacket(session);
        }

        void generatePlayerListPacket(@NonNull GeyserSession session, PlayerSkinProfile playerSkinProfile) {
            setLastPlayerListState(playerEntity.isPlayerList());
            PlayerListPacket playerListPacket = buildPlayerListPacket(playerEntity, session, playerSkinProfile);
            setPendingPlayerListPacket(playerListPacket);
        }

        void generateAdventureSettingsPacket(@NonNull GeyserSession session) {
            AdventureSettingsPacket adventureSettingsPacket = buildAdventureSettingsPacket(playerEntity, session);
            setPendingAdventureSettingsPacket(adventureSettingsPacket);
        }

        synchronized boolean sendPackets(GeyserSession session) {
            boolean wasPacketSent = false;
            // will get triggered on session login if not initialized
            if (session.getUpstream().isInitialized()) {
                if (pendingPlayerListPacket != null) {
                    GeyserConnector.getInstance().getLogger(session).debug("[PLM:sendPackets] PlayerList Entry: " + pendingPlayerListPacket);
                    session.sendUpstreamPacket(pendingPlayerListPacket);
                    wasPacketSent = true;
                    if (pendingPlayerListPacket.getAction() == PlayerListPacket.Action.ADD) {
                        session.getEntityCache().addPlayerEntity(playerEntity);
                    }
                    if (pendingPlayerListPacket.getAction() == PlayerListPacket.Action.REMOVE) {
                        session.getEntityCache().removePlayerEntity(playerEntity.getUuid());
                    }
                    pendingPlayerListPacket = null;
                }
                if (pendingAdventureSettingsPacket != null) {
                    GeyserConnector.getInstance().getLogger(session).debug("[PLM:sendPackets] AdventureSettings: " + pendingPlayerListPacket);
                    session.sendUpstreamPacket(pendingAdventureSettingsPacket);
                    wasPacketSent = true;
                    pendingAdventureSettingsPacket = null;
                }
            }
            return wasPacketSent;
        }

        private AdventureSettingsPacket buildAdventureSettingsPacket(PlayerEntity playerEntity, GeyserSession session) {
            if (playerEntity.isPlayerList() && isBedrockPlayer()) {
                // if player is another bedrock player then also communicate their action settings
                // in order to display correct permission levels
                long localGeyserId = getLocalGeyserId(session);
                if (localGeyserId != 0) {
                    return owningSession.getAdventureSettings(localGeyserId);
                }
            }
            return null;
        }

        private PlayerListPacket buildPlayerListPacket(PlayerEntity playerEntity, GeyserSession session, PlayerSkinProfile playerSkinProfile) {
            PlayerListPacket.Entry playerEntry = createPlayerListEntry(playerEntity, session);

            PlayerListPacket playerListPacket = new PlayerListPacket();
            playerListPacket.getEntries().add(playerEntry);

            setLastPlayerListState(playerEntity.isPlayerList());
            if (playerEntity.isPlayerList()) {
                playerListPacket.setAction(PlayerListPacket.Action.ADD);
                // check if all required skins are already cached
                if (!resourcesAvailable(playerSkinProfile)) {
                    // create dummy profile so we can send ASAP to client
                    playerSkinProfile = PlayerSkinProfile.getDefaultSkinProfile(playerEntity);
                    setUsedDummyProfile(true);
                } else {
                    setUsedDummyProfile(false);
                }
                setLastPlayerSkinProfile(playerSkinProfile); // capture
                populateSkinInfo(playerEntry, playerSkinProfile);
            } else {
                // removals only need the UUID
                playerListPacket.setAction(PlayerListPacket.Action.REMOVE);
            }
            return playerListPacket;
        }

        private PlayerListPacket.Entry createPlayerListEntry(PlayerEntity playerEntity, GeyserSession session) {
            PlayerListPacket.Entry entry;
            // If we are building a PlayerListEntry for our own session we use our AuthData UUID instead of the Java UUID
            // as bedrock expects to get back its own provided uuid
            if (isOwnedBy(session)) {
                entry = new PlayerListPacket.Entry(session.getAuthData().getUUID());
            } else {
                entry = new PlayerListPacket.Entry(playerEntity.getUuid());
            }
            entry.setName(playerEntity.getUsername());
            entry.setEntityId(playerEntity.getGeyserId());
            // This attempts to find the xuid of the player so profile images show up for xbox accounts
            entry.setXuid(getXuid());
            entry.setPlatformChatId("");
            entry.setTeacher(false);
            return entry;
        }

        private void populateSkinInfo(PlayerListPacket.Entry entry, PlayerSkinProfile skinProfile) {
            Skin skin = ResourceManager.get(skinProfile.getSkinDescriptor());
            Cape cape = ResourceManager.get(skinProfile.getCapeDescriptor());
            SkinGeometry geometry = ResourceManager.get(skinProfile.getGeometryDescriptor());

            List<AnimationData> animations = Collections.emptyList();
            if (skin.getAnimations() != null) {
                animations = skin.getAnimations().stream().map(PlayerListManager::toAnimationData).collect(Collectors.toList());
            }

            List<PersonaPieceData> pieces = Collections.emptyList();
            if (skin.getPersonaPieces() != null) {
                pieces = skin.getPersonaPieces().stream().map(PlayerListManager::toPersonaPieceData).collect(Collectors.toList());
            }

            List<PersonaPieceTintData> tints = Collections.emptyList();
            if (skin.getPersonaTintColors() != null) {
                tints = skin.getPersonaTintColors().stream().map(PlayerListManager::toPersonaPieceTintData).collect(Collectors.toList());
            }

            ImageData skinImage = ImageData.of(skin.getSkinData().getWidth(), skin.getSkinData().getHeight(), skin.getSkinData().getData());
            ImageData capeImage = ImageData.of(cape.getCapeData().getWidth(), cape.getCapeData().getHeight(), cape.getCapeData().getData());

            SerializedSkin serializedSkin = SerializedSkin.of(skin.getSkinId(), geometry.getResourcePatch(), skinImage,
                    animations, capeImage, geometry.getData(), skin.getAnimationData(), skin.isPremium(), skin.isPersona(),
                    skin.isCapeOnClassic(), cape.getCapeId(), skin.getSkinId() + cape.getCapeId(),
                    skin.getArmSize(), skin.getSkinColor(), pieces, tints);

            entry.setSkin(serializedSkin);
            entry.setTrustedSkin(true);
        }
    }
}
