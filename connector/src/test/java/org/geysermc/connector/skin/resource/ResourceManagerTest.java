package org.geysermc.connector.skin.resource;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.skin.resource.types.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class ResourceManagerTest {

    @Test
    public void capeMatchers() {
        PlayerEntity playerEntity = new PlayerEntity(new GameProfile(UUID.randomUUID(), "johnsmith"), 1, 1, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ZERO);

        for (CapeType capeType : CapeType.values()) {
            ResourceDescriptor<Cape, Void> capeDescriptor = capeType.getDescriptorFor(playerEntity);
            System.out.println("CapeType: " + capeType + " Descriptor: " + capeDescriptor);
            Assert.assertTrue("Failed for " + capeType.name(),
                    capeType.getUriPattern().matcher(capeDescriptor.getUri().toString()).matches());
            for (CapeType capeType2 : CapeType.values()) {
                if (capeType2 == capeType) {
                    continue;
                }
                ResourceDescriptor<Cape, Void> capeDescriptor2 = capeType2.getDescriptorFor(playerEntity);
                Assert.assertFalse("Failed for " + capeType.name() + " vs. " + capeType2.name(),
                        capeType.getUriPattern().matcher(capeDescriptor2.getUri().toString()).matches());
            }
        }
    }

    @Test
    public void earsMatchers() {
        PlayerEntity playerEntity = new PlayerEntity(new GameProfile(UUID.randomUUID(), "johnsmith"), 1, 1, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ZERO);

        for (EarsType earsType : EarsType.values()) {
            ResourceDescriptor<Ears, Void> earsDescriptor = earsType.getDescriptorFor(playerEntity);
            System.out.println("EarsType: " + earsType + " Descriptor: " + earsDescriptor);
            Assert.assertTrue("Failed for " + earsType.name(),
                    earsType.getUriPattern().matcher(earsDescriptor.getUri().toString()).matches());
            for (EarsType earsType2 : EarsType.values()) {
                if (earsType2 == earsType) {
                    continue;
                }
                ResourceDescriptor<Ears, Void> earsDescriptor2 = earsType2.getDescriptorFor(playerEntity);
                Assert.assertFalse("Failed for " + earsType.name() + " vs. " + earsType2.name(),
                        earsType.getUriPattern().matcher(earsDescriptor2.getUri().toString()).matches());
            }
        }
    }

    @Test
    public void gameProfileMatchers() {
        PlayerEntity playerEntity = new PlayerEntity(new GameProfile(UUID.randomUUID(), "johnsmith"), 1, 1, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ZERO);

        for (GameProfileDataType gameProfileType : GameProfileDataType.values()) {
            ResourceDescriptor<GameProfileData, Void> gameProfileDescriptor = gameProfileType.getDescriptorFor(playerEntity);
            System.out.println("GameProfileDataType: " + gameProfileType + " Descriptor: " + gameProfileDescriptor);
            Assert.assertTrue("Failed for " + gameProfileType.name(),
                    gameProfileType.getUriPattern().matcher(gameProfileDescriptor.getUri().toString()).matches());
            for (GameProfileDataType gameProfileType2 : GameProfileDataType.values()) {
                if (gameProfileType2 == gameProfileType) {
                    continue;
                }
                ResourceDescriptor<GameProfileData, Void> gameProfileDescriptor2 = gameProfileType2.getDescriptorFor(playerEntity);
                Assert.assertFalse("Failed for " + gameProfileType.name() + " vs. " + gameProfileType2.name(),
                        gameProfileType.getUriPattern().matcher(gameProfileDescriptor2.getUri().toString()).matches());
            }
        }
    }

    @Test
    public void playerSkinMatchers() {
        PlayerEntity playerEntity = new PlayerEntity(new GameProfile(UUID.randomUUID(), "johnsmith"), 1, 1, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ZERO);

        for (SkinType skinType : SkinType.values()) {
            ResourceDescriptor<Skin, Void> playerSkinDescriptor = skinType.getDescriptorFor(playerEntity);
            System.out.println("SkinGeometryType: " + skinType + " Descriptor: " + playerSkinDescriptor);
            Assert.assertTrue("Failed for " + skinType.name(),
                    skinType.getUriPattern().matcher(playerSkinDescriptor.getUri().toString()).matches());
            for (SkinType skinType2 : SkinType.values()) {
                if (skinType2 == skinType) {
                    continue;
                }
                ResourceDescriptor<Skin, Void> playerSkinDescriptor2 = skinType2.getDescriptorFor(playerEntity);
                Assert.assertFalse("Failed for " + skinType.name() + " vs. " + skinType2.name(),
                        skinType.getUriPattern().matcher(playerSkinDescriptor2.getUri().toString()).matches());
            }
        }
    }

    @Test
    public void skinGeometryMatchers() {
        PlayerEntity playerEntity = new PlayerEntity(new GameProfile(UUID.randomUUID(), "johnsmith"), 1, 1, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ZERO);

        for (SkinGeometryType skinGeometryType : SkinGeometryType.values()) {
            ResourceDescriptor<SkinGeometry, Void> skinGeometryDescriptor = skinGeometryType.getDescriptorFor(playerEntity);
            System.out.println("SkinGeometryType: " + skinGeometryType + " Descriptor: " + skinGeometryDescriptor);
            Assert.assertTrue("Failed for " + skinGeometryType.name(),
                    skinGeometryType.getUriPattern().matcher(skinGeometryDescriptor.getUri().toString()).matches());
            for (SkinGeometryType skinGeometryType2 : SkinGeometryType.values()) {
                if (skinGeometryType2 == skinGeometryType) {
                    continue;
                }
                ResourceDescriptor<SkinGeometry, Void> skinGeometryDescriptor2 = skinGeometryType2.getDescriptorFor(playerEntity);
                Assert.assertFalse("Failed for " + skinGeometryType.name() + " vs. " + skinGeometryType2.name(),
                        skinGeometryType.getUriPattern().matcher(skinGeometryDescriptor2.getUri().toString()).matches());
            }
        }
    }

    @Test
    public void skullMatchers() {
        PlayerEntity playerEntity = new PlayerEntity(new GameProfile(UUID.randomUUID(), "johnsmith"), 1, 1, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ZERO);

        for (SkullType skullType : SkullType.values()) {
            ResourceDescriptor<Skull, Void> skullDescriptor = skullType.getDescriptorFor(playerEntity);
            System.out.println("SkullType: " + skullType + " Descriptor: " + skullDescriptor);
            Assert.assertTrue("Failed for " + skullType.name(),
                    skullType.getUriPattern().matcher(skullDescriptor.getUri().toString()).matches());
            for (SkullType skullType2 : SkullType.values()) {
                if (skullType2 == skullType) {
                    continue;
                }
                ResourceDescriptor<Skull, Void> skullDescriptor2 = skullType2.getDescriptorFor(playerEntity);
                Assert.assertFalse("Failed for " + skullType.name() + " vs. " + skullType2.name(),
                        skullType.getUriPattern().matcher(skullDescriptor2.getUri().toString()).matches());
            }
        }
    }


}