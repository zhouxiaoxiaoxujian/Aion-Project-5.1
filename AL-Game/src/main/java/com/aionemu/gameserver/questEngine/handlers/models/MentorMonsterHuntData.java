package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.*;
import javolution.util.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.MentorMonsterHunt;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MentorMonsterHuntData")
public class MentorMonsterHuntData extends MonsterHuntData
{
	@XmlAttribute(name = "min_mente_level")
	protected int minMenteLevel = 1;
	
	@XmlAttribute(name = "max_mente_level")
	protected int maxMenteLevel = 99;
	
	public int getMinMenteLevel() {
		return minMenteLevel;
	}
	
	public int getMaxMenteLevel() {
		return maxMenteLevel;
	}
	
	@Override
    public void register(QuestEngine questEngine) {
        FastMap<Monster, Set<Integer>> monsterNpcs = new FastMap<Monster, Set<Integer>>();
        for (Monster m : monster) {
            monsterNpcs.put(m, new HashSet<Integer>(m.getNpcIds()));
        }
        MentorMonsterHunt template = new MentorMonsterHunt(id, startNpcIds, endNpcIds, monsterNpcs, minMenteLevel, maxMenteLevel);
        questEngine.addQuestHandler(template);
    }
}