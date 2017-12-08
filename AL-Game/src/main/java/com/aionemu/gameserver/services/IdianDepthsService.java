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
import com.aionemu.gameserver.configs.main.*;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.idiandepthsspawns.*;
import com.aionemu.gameserver.model.idiandepths.IdianDepthsLocation;
import com.aionemu.gameserver.model.idiandepths.IdianDepthsStateType;
import com.aionemu.gameserver.services.idiandepthsservice.*;
import com.aionemu.gameserver.services.teleport.*;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.*;
import com.aionemu.gameserver.world.*;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * @author Rinzler (Encom)
 */

public class IdianDepthsService
{
	private Map<Integer, IdianDepthsLocation> idianDepths;
	private static final int duration = CustomConfig.IDIAN_DEPTHS_DURATION;
	private final Map<Integer, IdianDepths<?>> activeIdianDepths = new FastMap<Integer, IdianDepths<?>>().shared();
	
	public void initIdianDepthsLocations() {
		if (CustomConfig.IDIAN_DEPTHS_ENABLED) {
			idianDepths = DataManager.IDIAN_DEPTHS_DATA.getIdianDepthsLocations();
			for (IdianDepthsLocation loc: getIdianDepthsLocations().values()) {
				spawn(loc, IdianDepthsStateType.CLOSED);
			}
			CronService.getInstance().schedule(new Runnable() {
				@Override
				public void run() {
					for (IdianDepthsLocation loc: getIdianDepthsLocations().values()) {
				        startIdianDepths(loc.getId());
					}
					World.getInstance().doOnAllPlayers(new Visitor<Player>() {
					    @Override
					    public void visit(Player player) {
							sendRequest(player);
						    PacketSendUtility.sendSys3Message(player, "\uE0AA", "<Idian Depths> open !!!");
					    }
					});
				}
			}, CustomConfig.IDIAN_DEPTHS_SCHEDULE);
		} else {
			idianDepths = Collections.emptyMap();
		}
	}
	
	public void startIdianDepths(final int id) {
		final IdianDepths<?> idian;
		synchronized (this) {
			if (activeIdianDepths.containsKey(id)) {
				return;
			}
			idian = new Idian(idianDepths.get(id));
			activeIdianDepths.put(id, idian);
		}
		idian.start();
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				stopIdianDepths(id);
			}
		}, duration * 3600 * 1000);
	}
	
	public void stopIdianDepths(int id) {
		if (!isIdianDepthsInProgress(id)) {
			return;
		}
		IdianDepths<?> idian;
		synchronized (this) {
			idian = activeIdianDepths.remove(id);
		} if (idian == null || idian.isClosed()) {
			return;
		}
		idian.stop();
	}
	
	public void spawn(IdianDepthsLocation loc, IdianDepthsStateType istate) {
		if (istate.equals(IdianDepthsStateType.OPEN)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getIdianDepthsSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				IdianDepthsSpawnTemplate idianDepthsttemplate = (IdianDepthsSpawnTemplate) st;
				if (idianDepthsttemplate.getIStateType().equals(istate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(idianDepthsttemplate, 1));
				}
			}
		}
	}
	
	public void sendRequest(final Player player) {
        String message = "Idian Depths is now opened. Do you want to enter ?";
        RequestResponseHandler responseHandler = new RequestResponseHandler(player) {
            @Override
            public void acceptRequest(Creature requester, Player responder) {
				switch (responder.getRace()) {
					case ELYOS:
						//Levinshor <Aetherbrak>
						TeleportService2.teleportTo(responder, 600100000, 717.06506f, 268.116f, 291.07382f, (byte) 61);
					break;
					case ASMODIANS:
						//Enshar <The Blood Grains>
						TeleportService2.teleportTo(responder, 220080000, 1764.2205f, 557.8873f, 207.97919f, (byte) 95);
					break;
				}
            }
            @Override
            public void denyRequest(Creature requester, Player responder) {
            }
        };
        boolean requested = player.getResponseRequester().putRequest(902247, responseHandler);
        if (requested) {
            PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(902247, 0, 0, message));
        }
    }
	
	public void despawn(IdianDepthsLocation loc) {
		for (VisibleObject npc : loc.getSpawned()) {
			((Npc) npc).getController().cancelTask(TaskId.RESPAWN);
			npc.getController().onDelete();
		}
		loc.getSpawned().clear();
	}
	
	public boolean isIdianDepthsInProgress(int id) {
		return activeIdianDepths.containsKey(id);
	}
	
	public Map<Integer, IdianDepths<?>> getActiveIdianDepths() {
		return activeIdianDepths;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public IdianDepthsLocation getIdianDepthsLocation(int id) {
		return idianDepths.get(id);
	}
	
	public Map<Integer, IdianDepthsLocation> getIdianDepthsLocations() {
		return idianDepths;
	}
	
	public static IdianDepthsService getInstance() {
		return IdianDepthsServiceHolder.INSTANCE;
	}
	
	private static class IdianDepthsServiceHolder {
		private static final IdianDepthsService INSTANCE = new IdianDepthsService();
	}
}