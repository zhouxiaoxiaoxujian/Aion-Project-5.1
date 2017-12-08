/*
 * This file is part of Encom. **ENCOM FUCK OTHER SVN**
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.*;

import com.aionemu.commons.utils.Rnd;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.*;
import com.aionemu.gameserver.model.house.*;
import com.aionemu.gameserver.model.team.legion.*;
import com.aionemu.gameserver.model.templates.housing.*;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.*;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.*;
import com.aionemu.gameserver.world.*;

public class CM_HOUSE_TELEPORT extends AionClientPacket
{
	int actionId;
	int playerId1;
	int playerId2;
	
	public CM_HOUSE_TELEPORT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	
	@Override
	protected void readImpl() {
		actionId = readC();
		playerId1 = readD();
		playerId2 = readD();
	}
	
	@Override
    protected void runImpl() {
        Player player1 = World.getInstance().findPlayer(playerId1);
        if (player1 == null || !player1.isOnline()) {
            return;
        }
        House house = null;
        if (actionId == 1) {
            playerId2 = playerId1;
        } else if (actionId == 3) {
            List<Integer> relationIds = new ArrayList<Integer>();
            Iterator<Friend> friends = player1.getFriendList().iterator();
            int address = 0;
            while (friends.hasNext()) {
                int friendId = friends.next().getOid();
                address = HousingService.getInstance().getPlayerAddress(friendId);
                if (address != 0) {
                    house = HousingService.getInstance().getPlayerStudio(friendId);
                    if (house == null) {
                        house = HousingService.getInstance().getHouseByAddress(address);
                    } if (house.getDoorState() == HousePermissions.DOOR_CLOSED || house.getLevelRestrict() > player1.getLevel()) {
                        continue;
                    }
                    relationIds.add(friendId);
                }
            }
            Legion legion = player1.getLegion();
            if (legion != null) {
                for (int memberId : legion.getLegionMembers()) {
                    address = HousingService.getInstance().getPlayerAddress(memberId);
                    if (address != 0) {
                        house = HousingService.getInstance().getPlayerStudio(memberId);
                        if (house == null) {
                            house = HousingService.getInstance().getHouseByAddress(address);
                        } if (house.getDoorState() == HousePermissions.DOOR_CLOSED || house.getLevelRestrict() > player1.getLevel()) {
                            continue;
                        }
                        relationIds.add(memberId);
                    }
                }
            } if (relationIds.size() == 0) {
                PacketSendUtility.sendPacket(player1, SM_SYSTEM_MESSAGE.STR_MSG_NO_RELATIONSHIP_RECENTLY);
                return;
            }
            playerId2 = relationIds.get(Rnd.get(relationIds.size()));
        }
        if (playerId2 == 0) {
            return;
        }
        house = HousingService.getInstance().getPlayerStudio(playerId2);
        HouseAddress address = null;
        int instanceId = 0;
        if (house != null) {
            address = house.getAddress();
            WorldMapInstance instance = InstanceService.getPersonalInstance(address.getMapId(), playerId2);
            if (instance == null) {
                instance = InstanceService.getNextAvailableInstance(address.getMapId(), playerId2);
            }
            instanceId = instance.getInstanceId();
            InstanceService.registerPlayerWithInstance(instance, player1);
        } else {
            int addressId = HousingService.getInstance().getPlayerAddress(playerId2);
            house = HousingService.getInstance().getHouseByAddress(addressId);
            if (house == null || house.getLevelRestrict() > player1.getLevel()) {
                return;
            }
            address = house.getAddress();
            instanceId = house.getInstanceId();
        }
        VisibleObject target = player1.getTarget();
        if (target != null) {
            PacketSendUtility.sendPacket(player1, new SM_DIALOG_WINDOW(target.getObjectId(), 0));
        }
        TeleportService2.teleportTo(player1, address.getMapId(), instanceId, address.getX(), address.getY(), address.getZ(), (byte) 0, TeleportAnimation.BEAM_ANIMATION);
    }
}