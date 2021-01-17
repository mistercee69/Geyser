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

package org.geysermc.connector.network.translators.java.entity.player;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = ServerPlayerListEntryPacket.class)
public class JavaPlayerListEntryTranslator extends PacketTranslator<ServerPlayerListEntryPacket> {
    @Override
    public void translate(ServerPlayerListEntryPacket packet, GeyserSession session) {
        if (packet.getAction() != PlayerListEntryAction.ADD_PLAYER && packet.getAction() != PlayerListEntryAction.REMOVE_PLAYER)
            return;

        for (PlayerListEntry entry : packet.getEntries()) {
            GeyserConnector.getInstance().getLogger(session).debug("Java PlayerListEntry: " + entry);
            switch (packet.getAction()) {
                case ADD_PLAYER:
                    PlayerEntity playerEntity;
                    boolean self = entry.getProfile().getId().equals(session.getPlayerEntity().getUuid());

                    if (self) {
                        // Entity is ourself
                        playerEntity = session.getPlayerEntity();
                    } else {
                        playerEntity = session.getEntityCache().getPlayerEntity(entry.getProfile().getId());
                        if (playerEntity == null) {
                            // It's a new player
                            playerEntity = new PlayerEntity(
                                    entry.getProfile(),
                                    -1,
                                    session.getEntityCache().getNextEntityId().incrementAndGet(),
                                    Vector3f.ZERO,
                                    Vector3f.ZERO,
                                    Vector3f.ZERO
                            );
                        }
                    }

                    playerEntity.setProfile(entry.getProfile());
                    playerEntity.setPlayerList(true);
                    playerEntity.setValid(true);

                    session.getPlayerListManager().registerPlayer(playerEntity);
                    break;
                case REMOVE_PLAYER:
                    PlayerEntity entity = session.getEntityCache().getPlayerEntity(entry.getProfile().getId());
                    if (entity != null) {
                        // Just remove the entity's player list status
                        // Don't despawn the entity - the Java server will also take care of that.
                        entity.setPlayerList(false);
                        session.getPlayerListManager().unregisterPlayer(entity);
                    }
                    break;
            }
        }
    }
}
