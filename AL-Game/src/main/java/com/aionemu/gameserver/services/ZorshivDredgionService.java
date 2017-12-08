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
package com.aionemu.gameserver.services;

import java.util.*;
import javolution.util.FastMap;

import com.aionemu.commons.services.CronService;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.shedule.DredgionSchedule;
import com.aionemu.gameserver.configs.shedule.DredgionSchedule.Dredgion;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.zorshivdredgionspawns.*;
import com.aionemu.gameserver.model.zorshivdredgion.*;
import com.aionemu.gameserver.services.zorshivdredgionservice.*;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * @author Rinzler (Encom)
 */

public class ZorshivDredgionService
{
	private DredgionSchedule dredgionSchedule;
	private Map<Integer, ZorshivDredgionLocation> zorshivDredgion;
	private static final int duration = CustomConfig.ZORSHIV_DREDGION_DURATION;
	private final Map<Integer, ZorshivDredgion<?>> activeZorshivDredgion = new FastMap<Integer, ZorshivDredgion<?>>().shared();
	
	public void initZorshivDredgionLocations() {
		if (CustomConfig.ZORSHIV_DREDGION_ENABLED) {
			zorshivDredgion = DataManager.ZORSHIV_DREDGION_DATA.getZorshivDredgionLocations();
			for (ZorshivDredgionLocation loc: getZorshivDredgionLocations().values()) {
				spawn(loc, ZorshivDredgionStateType.PEACE);
			}
		} else {
			zorshivDredgion = Collections.emptyMap();
		}
	}
	
	public void initZorshivDredgion() {
		if (CustomConfig.ZORSHIV_DREDGION_ENABLED) {
		    dredgionSchedule = DredgionSchedule.load();
		    for (Dredgion dredgion: dredgionSchedule.getDredgionsList()) {
			    for (String zorshivTime: dredgion.getZorshivTimes()) {
				    CronService.getInstance().schedule(new DredgionStartRunnable(dredgion.getId()), zorshivTime);
			    }
			}
		}
	}
	
	public void startZorshivDredgion(final int id) {
		final ZorshivDredgion<?> zorshiv;
		synchronized (this) {
			if (activeZorshivDredgion.containsKey(id)) {
				return;
			}
			zorshiv = new Zorshiv(zorshivDredgion.get(id));
			activeZorshivDredgion.put(id, zorshiv);
		}
		zorshiv.start();
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				stopZorshivDredgion(id);
			}
		}, duration * 3600 * 1000);
	}
	
	public void stopZorshivDredgion(int id) {
		if (!isZorshivDredgionInProgress(id)) {
			return;
		}
		ZorshivDredgion<?> zorshiv;
		synchronized (this) {
			zorshiv = activeZorshivDredgion.remove(id);
		} if (zorshiv == null || zorshiv.isPeace()) {
			return;
		}
		zorshiv.stop();
	}
	
	public void spawn(ZorshivDredgionLocation loc, ZorshivDredgionStateType zstate) {
		if (zstate.equals(ZorshivDredgionStateType.LANDING)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getZorshivDredgionSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				ZorshivDredgionSpawnTemplate zorshivDredgiontemplate = (ZorshivDredgionSpawnTemplate) st;
				if (zorshivDredgiontemplate.getZStateType().equals(zstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(zorshivDredgiontemplate, 1));
				}
			}
		}
	}
	
   /**
	* Dredgion Invasion Msg.
	*/
	public boolean levinshorMsg(int id) {
        switch (id) {
            case 1:
			case 2:
                World.getInstance().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys3Message(player, "\uE050", "The <Zorshiv Dredgion> to lands at levinshor !!!");
						//The Balaur Dredgion has appeared.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_SPAWN, 120000);
						//The Dredgion has dropped Balaur Troopers.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_DROP_DRAGON, 300000);
						//The Balaur Dredgion has disappeared.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_DESPAWN, 3600000);
					}
				});
			    return true;
            default:
                return false;
        }
    }
	public boolean inggisonMsg(int id) {
        switch (id) {
            case 3:
                World.getInstance().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys3Message(player, "\uE050", "The <Zorshiv Dredgion> to lands at inggison !!!");
						//The Balaur Dredgion has appeared.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_SPAWN, 120000);
						//The Dredgion has dropped Balaur Troopers.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_DROP_DRAGON, 300000);
						//The Balaur Dredgion has disappeared.
						PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_DESPAWN, 3600000);
					}
				});
			    return true;
            default:
                return false;
        }
    }
	
	public void despawn(ZorshivDredgionLocation loc) {
		for (VisibleObject npc : loc.getSpawned()) {
			((Npc) npc).getController().cancelTask(TaskId.RESPAWN);
			npc.getController().onDelete();
		}
		loc.getSpawned().clear();
	}
	
	public boolean isZorshivDredgionInProgress(int id) {
		return activeZorshivDredgion.containsKey(id);
	}
	
	public Map<Integer, ZorshivDredgion<?>> getActiveZorshivDredgion() {
		return activeZorshivDredgion;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public ZorshivDredgionLocation getZorshivDredgionLocation(int id) {
		return zorshivDredgion.get(id);
	}
	
	public Map<Integer, ZorshivDredgionLocation> getZorshivDredgionLocations() {
		return zorshivDredgion;
	}
	
	public static ZorshivDredgionService getInstance() {
		return ZorshivDredgionServiceHolder.INSTANCE;
	}
	
	private static class ZorshivDredgionServiceHolder {
		private static final ZorshivDredgionService INSTANCE = new ZorshivDredgionService();
	}
}