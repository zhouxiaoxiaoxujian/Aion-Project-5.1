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
package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.HousingService;

import java.util.List;

public class SM_LEGION_MEMBERLIST extends AionServerPacket {

	private static final int OFFLINE = 0x00;
	private static final int ONLINE = 0x01;
	private boolean isFirst;
	private List<LegionMemberEx> legionMembers;
	
	public SM_LEGION_MEMBERLIST(List<LegionMemberEx> legionMembers, boolean isFirst) {
		this.legionMembers = legionMembers;
		this.isFirst = isFirst;
	}
	
	@Override
	protected void writeImpl(AionConnection con) {
		int size = legionMembers.size();
		int x = 1;
		writeC(isFirst ? 1 : 0);
		writeH((65536 - size));
		for (LegionMemberEx legionMember : legionMembers) {
			if (x > size)
				break;
			writeD(legionMember.getObjectId());
			writeS(legionMember.getName());
			writeC(legionMember.getPlayerClass().getClassId());
			writeD(legionMember.getLevel());
			writeC(legionMember.getRank().getRankId());
			writeD(legionMember.getWorldId());
			writeC(legionMember.isOnline() ? ONLINE : OFFLINE);
			writeS(legionMember.getSelfIntro());
			writeS(legionMember.getNickname());
			writeD(legionMember.getLastOnline());
			int address = HousingService.getInstance().getPlayerAddress(legionMember.getObjectId());
            if (address > 0) {
                House house = HousingService.getInstance().getPlayerStudio(legionMember.getObjectId());
                if (house == null) {
                    house = HousingService.getInstance().getHouseByAddress(address);
                }
                writeD(address);
                writeD(house.getDoorState().getPacketValue());
            } else {
                writeD(0);
                writeD(0);
            }
			writeC(1);
			writeC(0);
			writeC(0);
			writeC(0);
			x++;
		}
	}
}