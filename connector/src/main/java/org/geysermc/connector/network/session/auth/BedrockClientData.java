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

package org.geysermc.connector.network.session.auth;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.geysermc.floodgate.util.DeviceOS;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class BedrockClientData {
    @JsonProperty(value = "GameVersion")
    private String gameVersion;
    @JsonProperty(value = "ServerAddress")
    private String serverAddress;
    @JsonProperty(value = "ThirdPartyName")
    private String username;
    @JsonProperty(value = "LanguageCode")
    private String languageCode;

    @JsonProperty(value = "SkinId")
    private String skinId;
    @JsonProperty(value = "SkinData")
    private byte[] skinData;
    @JsonProperty(value = "SkinImageHeight")
    private int skinImageHeight;
    @JsonProperty(value = "SkinImageWidth")
    private int skinImageWidth;
    @JsonProperty(value = "AnimatedImageData")
    private List<SkinAnimation> animations;
    @JsonProperty(value = "CapeId")
    private String capeId;
    @JsonProperty(value = "CapeData")
    private byte[] capeData;
    @JsonProperty(value = "CapeImageHeight")
    private int capeImageHeight;
    @JsonProperty(value = "CapeImageWidth")
    private int capeImageWidth;
    @JsonProperty(value = "CapeOnClassicSkin")
    private boolean capeOnClassicSkin;
    @JsonProperty(value = "SkinResourcePatch")
    private byte[] resourcePatch;
    @JsonProperty(value = "SkinGeometryData")
    private byte[] geometryData;
    @JsonProperty(value = "PersonaSkin")
    private boolean personaSkin;
    @JsonProperty(value = "PremiumSkin")
    private boolean premiumSkin;
    @JsonProperty(value = "ArmSize")
    private String armSize;
    @JsonProperty(value = "SkinAnimationData")
    private String skinAnimationData;
    @JsonProperty(value = "SkinColor")
    private String skinColor;
    @JsonProperty(value = "PersonaPieces")
    private List<PersonaSkinPiece> personaSkinPieces;
    @JsonProperty(value = "PieceTintColors")
    private List<PersonaSkinPieceTintColor> personaTintColors;

    @JsonProperty(value = "DeviceId")
    private String deviceId;
    @JsonProperty(value = "DeviceModel")
    private String deviceModel;
    @JsonProperty(value = "DeviceOS")
    private DeviceOS deviceOS;
    @JsonProperty(value = "UIProfile")
    private UIProfile uiProfile;
    @JsonProperty(value = "GuiScale")
    private int guiScale;
    @JsonProperty(value = "CurrentInputMode")
    private InputMode currentInputMode;
    @JsonProperty(value = "DefaultInputMode")
    private InputMode defaultInputMode;
    @JsonProperty("PlatformOnlineId")
    private String platformOnlineId;
    @JsonProperty(value = "PlatformOfflineId")
    private String platformOfflineId;
    @JsonProperty(value = "SelfSignedId")
    private UUID selfSignedId;
    @JsonProperty(value = "ClientRandomId")
    private long clientRandomId;

    @JsonProperty(value = "ThirdPartyNameOnly")
    private boolean thirdPartyNameOnly;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkinAnimation {
        @JsonProperty(value = "Image")
        private byte[] imageData;
        @JsonProperty(value = "ImageHeight")
        private int imageHeight;
        @JsonProperty(value = "ImageWidth")
        private int imageWidth;
        @JsonProperty(value = "Type")
        private TextureType textureType;
        @JsonProperty(value = "Frames")
        private float frames;
        @JsonProperty(value = "AnimationExpression")
        private ExpressionType expressionType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonaSkinPiece {
        @JsonProperty(value = "PieceId")
        private String id;
        @JsonProperty(value = "PieceType")
        private String type;
        @JsonProperty(value = "PackId")
        private String packId;
        @JsonProperty(value = "IsDefault")
        private boolean isDefault;
        @JsonProperty(value = "ProductId")
        private String productId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonaSkinPieceTintColor {
        @JsonProperty(value = "PieceType")
        private String type;
        @JsonProperty(value = "Colors")
        private List<String> colors;
    }

    public enum UIProfile {
        @JsonEnumDefaultValue
        CLASSIC,
        POCKET
    }

    public enum InputMode {
        @JsonEnumDefaultValue
        UNKNOWN,
        KEYBOARD_MOUSE,
        TOUCH, // I guess Touch?
        CONTROLLER,
        VR
    }

    public enum TextureType {
        @JsonEnumDefaultValue
        NONE,
        FACE,
        BODY_32X32,
        BODY_128X128
    }

    public enum ExpressionType {
        @JsonEnumDefaultValue
        LINEAR,
        BLINKING
    }
}
