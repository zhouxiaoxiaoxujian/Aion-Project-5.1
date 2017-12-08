package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.assemblednpc.AssembledNpc;
import com.aionemu.gameserver.model.assemblednpc.AssembledNpcPart;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

public class SM_NPC_ASSEMBLER extends AionServerPacket
{
	private AssembledNpc assembledNpc;
	private int routeId;
	private long timeOnMap;
	
	public SM_NPC_ASSEMBLER(AssembledNpc assembledNpc) {
		this.assembledNpc = assembledNpc;
		this.routeId = assembledNpc.getRouteId();
		timeOnMap = assembledNpc.getTimeOnMap();
	}
	
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(assembledNpc.getAssembledParts().size());
		for (AssembledNpcPart npc : assembledNpc.getAssembledParts()) {
			writeD(routeId);
			writeD(npc.getObject());
			writeD(npc.getNpcId());
			writeD(npc.getEntityId());
			writeQ(timeOnMap);
		}
	}
}