package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.*;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.rift.*;
import com.aionemu.gameserver.model.vortex.*;
import com.aionemu.gameserver.questEngine.handlers.*;
import com.aionemu.gameserver.questEngine.model.*;
import com.aionemu.gameserver.services.*;

public class KillInWorld extends QuestHandler
{
    private final int questId;
    private final Set<Integer> startNpcs = new HashSet<Integer>();
    private final Set<Integer> endNpcs = new HashSet<Integer>();
    private final Set<Integer> worldIds = new HashSet<Integer>();
    private final int killAmount;
    private final int invasionWorldId;
	
    public KillInWorld(int questId, List<Integer> endNpcIds, List<Integer> startNpcIds, List<Integer> worldIds, int killAmount, int invasionWorld) {
        super(questId);
        if (startNpcIds != null) {
            this.startNpcs.addAll(startNpcIds);
            this.startNpcs.remove(0);
        } if (endNpcIds == null) {
            this.endNpcs.addAll(startNpcs);
        } else {
            this.endNpcs.addAll(endNpcIds);
            this.endNpcs.remove(0);
        }
        this.questId = questId;
        this.worldIds.addAll(worldIds);
        this.worldIds.remove(0);
        this.killAmount = killAmount;
        this.invasionWorldId = invasionWorld;
    }
	
    @Override
    public void register() {
        Iterator<Integer> iterator = startNpcs.iterator();
        while (iterator.hasNext()) {
            int startNpc = iterator.next();
            qe.registerQuestNpc(startNpc).addOnQuestStart(getQuestId());
            qe.registerQuestNpc(startNpc).addOnTalkEvent(getQuestId());
        }
        iterator = endNpcs.iterator();
        while (iterator.hasNext()) {
            int endNpc = iterator.next();
            qe.registerQuestNpc(endNpc).addOnTalkEvent(getQuestId());
        }
        iterator = worldIds.iterator();
        while (iterator.hasNext()) {
            int worldId = iterator.next();
            qe.registerOnKillInWorld(worldId, questId);
        } if (invasionWorldId != 0) {
            qe.registerOnEnterWorld(questId);
        }
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (startNpcs.isEmpty() || startNpcs.contains(targetId)) {
                switch (dialog) {
                    case START_DIALOG: {
                        return sendQuestDialog(env, 4762);
                    } case ACCEPT_QUEST: {
                        return sendQuestStartDialog(env);
                    } default: {
                        return sendQuestStartDialog(env);
                    }
                }
            }
        } else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
            if (endNpcs.contains(targetId)) {
                return sendQuestEndDialog(env);
            }
        }
        return false;
    }
	
    @Override
    public boolean onEnterWorldEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        VortexLocation vortexLoc = VortexService.getInstance().getLocationByWorld(invasionWorldId);
        if (player.getWorldId() == invasionWorldId) {
            if ((qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat())) {
                if ((vortexLoc != null && vortexLoc.isActive()) || (searchOpenRift())) {
                    return QuestService.startQuest(env);
                }
            }
        }
        return false;
    }
	
    private boolean searchOpenRift() {
        for (RiftLocation loc : RiftService.getInstance().getRiftLocations().values()) {
            if (loc.getWorldId() == invasionWorldId && loc.isOpened()) {
                return true;
            }
        }
        return false;
    }
	
    @Override
    public boolean onKillInWorldEvent(QuestEnv env) {
        return defaultOnKillRankedEvent(env, 0, killAmount, true);
    }
}