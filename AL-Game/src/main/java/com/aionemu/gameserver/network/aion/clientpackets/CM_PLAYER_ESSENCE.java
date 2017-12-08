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

import com.aionemu.gameserver.services.player.CreativityPanel.CreativitySkillService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityStatsService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityTransfoService;
import org.slf4j.*;

import com.aionemu.gameserver.services.player.CreativityPanel.*;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;

/**
 * @author Ranastic (Encom)
 */

public class CM_PLAYER_ESSENCE extends AionClientPacket
{
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(CM_PLAYER_ESSENCE.class);
	
	private Player activePlayer;
	private int type;
	private int plusSize;
	private int id;
	private int point;
	
	public CM_PLAYER_ESSENCE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	
	@Override
	protected void readImpl() {
		activePlayer = getConnection().getActivePlayer();
		type = readC();
		switch (type) {
			case 0: //Apply
				plusSize = readH();
				for (int i = 0; i < plusSize; i++) {
					id = readD();
					point = readH();
					if (id >= 1 && id <= 6) {
						if (point <= 255) {
							CreativityStatsService.getInstance().onEssenceApply(activePlayer, type, plusSize, id, point);
						} else if (point > 255) {
							PacketSendUtility.sendBrightYellowMessageOnCenter(activePlayer, "Essence bug detected... Please reset points or relog for solv this issue!");
						}
					} else if (id >= 7 && id <= 14 || id >= 401 && id <= 408) {
						CreativityTransfoService.getInstance().onTransfoApply(activePlayer, type, plusSize, id, point);
					} else if (id >= 15 && id <= 372 || id >= 409 && id <= 456 || id >= 373 && id <= 400) {
						CreativitySkillService.getInstance().onSkillApply(activePlayer, type, plusSize, id, point);
					}
				}
				PacketSendUtility.sendPacket(activePlayer, new SM_STATS_INFO(activePlayer));
			break;
			case 1: //Reset
				plusSize = readH();
			break;
			default:
			break;
		}
	}
	
	@Override
	protected void runImpl() {
		if (activePlayer == null) {
			return;
		} if (activePlayer.getLifeStats().isAlreadyDead()) {
			return;
		} if (type == 1) {
			CreativityEssenceService.getInstance().onResetEssence(activePlayer, plusSize);
		}
	}
}