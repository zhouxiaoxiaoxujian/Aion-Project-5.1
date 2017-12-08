package com.aionemu.gameserver.services;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.*;
import com.aionemu.gameserver.dao.*;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.landing.*;
import com.aionemu.gameserver.model.landing_special.*;
import com.aionemu.gameserver.model.templates.spawns.*;
import com.aionemu.gameserver.model.templates.spawns.landingspawns.*;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.abysslandingservice.*;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.*;
import com.aionemu.gameserver.world.*;
import com.aionemu.gameserver.world.knownlist.Visitor;

import javolution.util.FastMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AbyssLandingService
{
    private static Logger log = LoggerFactory.getLogger(AbyssLandingService.class);
    private static Map<Integer, LandingLocation> abyssLanding;
    private final Map<Integer, Landing<?>> activeLanding = new FastMap<Integer, Landing<?>>().shared();
    private final int questRate = AbyssLandingConfig.ABYSS_LANDING_QUEST_RATE;
	
    public void initLandingLocations() {
        abyssLanding = DataManager.LANDING_LOCATION_DATA.getLandingLocations();
        DAOManager.getDAO(AbyssLandingDAO.class).loadLandingLocations(abyssLanding);
        for (LandingLocation loc: getLandingLocations().values()) {
            startLanding(loc.getId());
        }
        log.info("[Abyss Landing] Loaded " + abyssLanding.size() + " Locations");
    }
	
    public void startLanding(final int id) {
        final Landing<?> land;
        synchronized (this) {
            if (activeLanding.containsKey(id)) {
                return;
            }
            land = new AbyssLanding(abyssLanding.get(id));
            activeLanding.put(id, land);
        }
        land.start(getLandingLocation(id).getLevel());
    }
	
    public void stopLanding(int id) {
        if (!activeLanding.containsKey(id)) {
            return;
        }
        Landing<?> landing;
        synchronized (this) {
            landing = activeLanding.remove(id);
        } if (landing == null) {
            return;
        }
        landing.stop();
    }
	
    public static void spawn(LandingLocation loc, LandingStateType estate) {
        if (estate.equals(estate)) {
        }
        List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getLandingSpawnsByLocId(loc.getId());
        for (SpawnGroup2 group : locSpawns) {
            for (SpawnTemplate st : group.getSpawnTemplates()) {
                LandingSpawnTemplate landingtTemplate = (LandingSpawnTemplate) st;
                if (landingtTemplate.getEStateType().equals(estate)) {
                    loc.getSpawned().add(SpawnEngine.spawnObject(landingtTemplate, 1));
                }
            }
        }
    }
	
    public static void despawn(LandingLocation loc) {
        for (VisibleObject npc : loc.getSpawned()) {
            ((Npc) npc).getController().cancelTask(TaskId.RESPAWN);
            npc.getController().onDelete();
        }
        loc.getSpawned().clear();
    }
	
    public void updateRedemptionLanding(int points, LandingPointsEnum type, boolean win) {
        LandingLocation loc = redemptionLanding();
        if (win) {
            switch (type) {
                case BASE:
                    loc.setBasePoints(loc.getBasePoints() + points);
                break;
                case SIEGE:
                    loc.setSiegePoints(loc.getSiegePoints() + points);
                break;
                case COMMANDER:
                    loc.setCommanderPoints(loc.getCommanderPoints() + points);
                break;
                case ARTIFACT:
                    loc.setArtifactPoints(loc.getArtifactPoints() + points);
                break;
                case QUEST:
                    loc.setQuestPoints(loc.getQuestPoints() + (points * questRate));
                break;
                case MONUMENT:
                    loc.setMonumentsPoints(loc.getMonumentsPoints() + points);
                break;
                case FACILITY:
                    loc.setFacilityPoints(loc.getFacilityPoints() + points);
                break;
            }
        } else {
            switch (type) {
                case BASE:
                    if (loc.getBasePoints() < points) {
                        return;
                    } else {
                        loc.setBasePoints(loc.getBasePoints() - points);
                    }
                break;
                case SIEGE:
                    if (loc.getSiegePoints() < points) {
                        return;
                    } else {
                        loc.setSiegePoints(loc.getSiegePoints() - points);
                    }
                break;
                case COMMANDER:
                    if (loc.getCommanderPoints() < points) {
                        return;
                    } else {
                        loc.setCommanderPoints(loc.getCommanderPoints() - points);
                    }
                break;
                case ARTIFACT:
                    if (loc.getArtifactPoints() < points) {
                        return;
                    } else {
                        loc.setArtifactPoints(loc.getArtifactPoints() - points);
                    }
                break;
                case QUEST:
                    if (loc.getQuestPoints() <(points * questRate)) {
                        return;
                    } else {
                        loc.setQuestPoints(loc.getQuestPoints() - (points * questRate));
                    }
                break;
                case MONUMENT:
                    if (loc.getMonumentsPoints() < points) {
                        return;
                    } else {
                        loc.setMonumentsPoints(loc.getMonumentsPoints() - points);
                    }
                break;
                case FACILITY:
                    if (loc.getFacilityPoints() < points) {
                        return;
                    } else {
                        loc.setFacilityPoints(loc.getFacilityPoints() - points);
                    }
                break;
            }
        }
        int totalScore = loc.getArtifactPoints() + loc.getCommanderPoints() + loc.getFacilityPoints() + loc.getBasePoints() + loc.getMonumentsPoints() + loc.getQuestPoints() + loc.getSiegePoints();
        loc.setPoints(totalScore);
        if (win) {
            checkRedemptionLanding(totalScore, true);
        } else {
            checkRedemptionLanding(totalScore, false);
        }
        onUpdate();
    }
	
    public void updateHarbingerLanding(int points, LandingPointsEnum type, boolean win) {
        LandingLocation loc = harbingerLanding();
        if (win) {
            switch (type) {
                case BASE:
                    loc.setBasePoints(loc.getBasePoints() + points);
                break;
                case SIEGE:
                    loc.setSiegePoints(loc.getSiegePoints() + points);
                break;
                case COMMANDER:
                    loc.setCommanderPoints(loc.getCommanderPoints() + points);
                break;
                case ARTIFACT:
                    loc.setArtifactPoints(loc.getArtifactPoints() + points);
                break;
                case QUEST:
                    loc.setQuestPoints(loc.getQuestPoints() + (points * questRate));
                break;
                case MONUMENT:
                    loc.setMonumentsPoints(loc.getMonumentsPoints() + points);
                break;
                case FACILITY:
                    loc.setFacilityPoints(loc.getFacilityPoints() + points);
                break;
            }
        } else {
            switch (type) {
                case BASE:
                    if (loc.getBasePoints() < points) {
                        return;
                    } else {
                        loc.setBasePoints(loc.getBasePoints() - points);
                    }
                break;
                case SIEGE:
                    if (loc.getSiegePoints() < points) {
                        return;
                    } else {
                        loc.setSiegePoints(loc.getSiegePoints() - points);
                    }
                break;
                case COMMANDER:
                    if (loc.getCommanderPoints() < points) {
                        return;
                    } else {
                        loc.setCommanderPoints(loc.getCommanderPoints() - points);
                    }
                break;
                case ARTIFACT:
                    if (loc.getArtifactPoints() < points) {
                        return;
                    } else {
                        loc.setArtifactPoints(loc.getArtifactPoints() - points);
                    }
                break;
                case QUEST:
                    if (loc.getQuestPoints() < (points * questRate)) {
                        return;
                    } else {
                        loc.setQuestPoints(loc.getQuestPoints() - (points * questRate));
                    }
                break;
                case MONUMENT:
                    if (loc.getMonumentsPoints() < points) {
                        return;
                    } else {
                        loc.setMonumentsPoints(loc.getMonumentsPoints() - points);
                    }
                break;
                case FACILITY:
                    if (loc.getFacilityPoints() < points) {
                        return;
                    } else {
                        loc.setFacilityPoints(loc.getFacilityPoints() - points);
                    }
                break;
            }
        }
        int totalScore = loc.getArtifactPoints() + loc.getCommanderPoints() + loc.getFacilityPoints() + loc.getBasePoints() + loc.getMonumentsPoints() + loc.getQuestPoints() + loc.getSiegePoints();
        loc.setPoints(totalScore);
        if (win) {
            checkHarbingerLanding(totalScore, true);
        } else {
            checkHarbingerLanding(totalScore, false);
        }
        onUpdate();
    }
	
    public void AnnounceToPoints(final Player pl, final DescriptionId race, final DescriptionId name, final int points, final LandingPointsEnum type) {
        World.getInstance().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                switch (type) {
					case SIEGE:
                        //%0 has occupied %0 and the Landing is now enhanced.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_OCCUPY(race, name));
                    break;
                    case BASE:
                        //%0 has occupied %1 Base and the Landing is now enhanced.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_OCCUPY_BASECAMP(race, name.toString()));
                    break;
                    case QUEST:
                        //Completed quest has contributed %0 points to the Landing.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_BUILDUP_POINT_QUEST_GAIN(points));
                        //%0's completed quest has enhanced the Landing.
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_QUEST(pl.getName()));
                    break;
                }
            }
        });
    }
	
    public void checkRedemptionLanding(int points, boolean gain) {
        int level = 0;
        if (points >= 0 && points <= 199999) {
            level = 1;
        } else if (points >= 200000 && points <= 299999) {
            level = 2;
        } else if (points >= 300000 && points <= 399999) {
            level = 3;
        } else if (points >= 400000 && points <= 499999) {
            level = 4;
        } else if (points >= 500000 && points <= 599999) {
            level = 5;
        } else if (points >= 600000 && points <= 699999) {
            level = 6;
        } else if (points >= 700000 && points <= 799999) {
            level = 7;
        } else if (points >= 800000) {
            level = 8;
        } if (gain && level != redemptionLanding().getLevel()) {
            levelUpRedemptionLanding(level);
        } if (!gain && level != redemptionLanding().getLevel()) {
            onRedemptionLandinggLevelDown(level);
        }
    }
	
    public void checkHarbingerLanding(int points, boolean gain) {
        int level = 0;
        if (points >= 0 && points <= 199999) {
            level = 1;
        } else if (points >= 200000 && points <= 299999) {
            level = 2;
        } else if (points >= 300000 && points <= 399999) {
            level = 3;
        } else if (points >= 400000 && points <= 499999) {
            level = 4;
        } else if (points >= 500000 && points <= 599999) {
            level = 5;
        } else if (points >= 600000 && points <= 699999) {
            level = 6;
        } else if (points >= 700000 && points <= 799999) {
            level = 7;
        } else if (points >= 800000) {
            level = 8;
        } if (gain && level != harbingerLanding().getLevel()) {
            levelUpHarbingerLanding(level);
        } if (!gain && level != harbingerLanding().getLevel()) {
            onHarbingerLandingLevelDown(level);
        }
    }
	
    public void levelUpRedemptionLanding(int level) {
        redemptionLanding().setLevel(level);
        stopLanding(redemptionLanding().getId());
        startLanding(redemptionLanding().getId());
        World.getInstance().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                //Landing Level Up.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ABYSS_OP_LEVEL_UP_LIGHT);
            }
        });
    }
	
    public void levelUpHarbingerLanding(int level) {
        harbingerLanding().setLevel(level);
        stopLanding(harbingerLanding().getId());
        startLanding(harbingerLanding().getId());
        World.getInstance().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                //Landing Level Up.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ABYSS_OP_LEVEL_UP_DARK);
            }
        });
    }
	
    public void onHarbingerLandingLevelDown(int level) {
        harbingerLanding().setLevel(level);
        stopLanding(harbingerLanding().getId());
        startLanding(harbingerLanding().getId());
        World.getInstance().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                //Landing Weakened.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ABYSS_OP_LEVEL_DOWN);
            }
        });
    }
	
    public void onRedemptionLandinggLevelDown(int level) {
        redemptionLanding().setLevel(level);
        stopLanding(redemptionLanding().getId());
        startLanding(redemptionLanding().getId());
        World.getInstance().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                //Landing Weakened.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ABYSS_OP_LEVEL_DOWN);
            }
        });
    }
	
   /**
	* MONUMENT
	*/
    public void onRewardMonuments(Race race, int id, int points) {
        if (race == Race.ASMODIANS) {
            AbyssLandingService.getInstance().updateHarbingerLanding(points, LandingPointsEnum.MONUMENT, true);
            AbyssLandingSpecialService.getInstance().startLanding(id);
        } else {
            AbyssLandingService.getInstance().updateRedemptionLanding(points, LandingPointsEnum.MONUMENT, true);
            AbyssLandingSpecialService.getInstance().startLanding(id);
        }
    }
    public void onDieMonuments(Race race, int id, int points) {
        if (race == Race.ELYOS) {
            AbyssLandingService.getInstance().updateRedemptionLanding(points, LandingPointsEnum.MONUMENT, true);
            AbyssLandingService.getInstance().updateHarbingerLanding(points, LandingPointsEnum.MONUMENT, false);
            AbyssLandingSpecialService.getInstance().stopLanding(id);
        } else {
            AbyssLandingService.getInstance().updateRedemptionLanding(points, LandingPointsEnum.MONUMENT, false);
            AbyssLandingService.getInstance().updateHarbingerLanding(points, LandingPointsEnum.MONUMENT, true);
            AbyssLandingSpecialService.getInstance().stopLanding(id);
        }
    }
	
   /**
	* COMMANDER
	*/
	public void onRewardCommander(Race race, int id, int points) {
        if (race == Race.ASMODIANS) {
            AbyssLandingService.getInstance().updateHarbingerLanding(points, LandingPointsEnum.COMMANDER, true);
            AbyssLandingSpecialService.getInstance().startLanding(id);
        } else {
            AbyssLandingService.getInstance().updateRedemptionLanding(points, LandingPointsEnum.COMMANDER, true);
            AbyssLandingSpecialService.getInstance().startLanding(id);
        }
    }
    public void onDieCommander(Race race, int id, int points) {
        if (race == Race.ELYOS) {
            AbyssLandingService.getInstance().updateRedemptionLanding(points, LandingPointsEnum.COMMANDER, true);
            AbyssLandingService.getInstance().updateHarbingerLanding(points, LandingPointsEnum.COMMANDER, false);
            AbyssLandingSpecialService.getInstance().stopLanding(id);
        } else {
            AbyssLandingService.getInstance().updateRedemptionLanding(points, LandingPointsEnum.COMMANDER, false);
            AbyssLandingService.getInstance().updateHarbingerLanding(points, LandingPointsEnum.COMMANDER, true);
            AbyssLandingSpecialService.getInstance().stopLanding(id);
        }
    }
	
   /**
	* FACILITY
	*/
     public void onRewardFacility(Race race, int points) {
        if (race == Race.ASMODIANS) {
            AbyssLandingService.getInstance().updateHarbingerLanding(points, LandingPointsEnum.FACILITY, true);
            AbyssLandingService.getInstance().updateRedemptionLanding(points, LandingPointsEnum.FACILITY, false);
        } else {
            AbyssLandingService.getInstance().updateRedemptionLanding(points, LandingPointsEnum.FACILITY, true);
            AbyssLandingService.getInstance().updateHarbingerLanding(points, LandingPointsEnum.FACILITY, false);
        }
    }
	
    public void onEnterWorld(Player player) {
        PacketSendUtility.sendPacket(player, new SM_ABYSS_LANDING());
    }
	
    public void onUpdate() {
        getDAO().updateLocation(getLandingLocation(redemptionLanding().getId()));
        getDAO().updateLocation(getLandingLocation(harbingerLanding().getId()));
    }
	
    private AbyssLandingDAO getDAO() {
        return DAOManager.getDAO(AbyssLandingDAO.class);
    }
	
    public void sendPacketToPlayer(Player player) {
        PacketSendUtility.sendPacket(player, new SM_ABYSS_LANDING());
    }
	
    public static AbyssLandingService getInstance() {
        return AbyssLandingService.SingletonHolder.instance;
    }
	
    private static class SingletonHolder {
        protected static final AbyssLandingService instance = new AbyssLandingService();
    }
	
    public LandingLocation getLandingLocation(int id) {
        return abyssLanding.get(id);
    }
	
    public LandingLocation redemptionLanding() {
        return abyssLanding.get(1);
    }
	
    public LandingLocation harbingerLanding() {
        return abyssLanding.get(2);
    }
	
    public static Map<Integer, LandingLocation> getLandingLocations() {
        return abyssLanding;
    }
}