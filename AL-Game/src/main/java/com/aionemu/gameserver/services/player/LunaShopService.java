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
package com.aionemu.gameserver.services.player;

import static java.lang.System.out;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.PlayerLunaShop;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dao.PlayerCPDAO;
import com.aionemu.gameserver.dao.PlayerWardrobeDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.luna.LunaConsumeRewardsTemplate;
import com.aionemu.gameserver.model.templates.recipe.LunaComponent;
import com.aionemu.gameserver.model.templates.recipe.LunaComponentElement;
import com.aionemu.gameserver.model.templates.recipe.LunaTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * @author Ranastic
 */

public class LunaShopService
{
	private Logger log = LoggerFactory.getLogger(LunaShopService.class);
	PlayerWardrobeDAO wDAO = DAOManager.getDAO(PlayerWardrobeDAO.class);
	private boolean freeBoxCraft = true;
	private boolean dailyGenerated = true;
	private boolean specialGenerated = true;
	private List<Integer> DailyCraft = new ArrayList<Integer>();
	private List<Integer> SpecialCraft = new ArrayList<Integer>();
	private List<Integer> armors = new ArrayList<Integer>();
	private List<Integer> pants = new ArrayList<Integer>();
	private List<Integer> shoes = new ArrayList<Integer>();
	private List<Integer> gloves = new ArrayList<Integer>();
	private List<Integer> shoulders = new ArrayList<Integer>();
	private List<Integer> weapons = new ArrayList<Integer>();
	public void init() {
		log.info("Luna Reset");
		String daily = "0 0 9 1/1 * ? *";
		String weekly = "0 0 9 ? * WED *";
		if (DailyCraft.size() == 0) {
			generateDailyCraft();
		} if (SpecialCraft.size() == 0) {
			generateSpecialCraft();
		} 
		//if (freeBoxCraft) {
			//craftBox(null);
		//}
		CronService.getInstance().schedule(new Runnable() {
			public void run() {
				dailyGenerated = false;
				generateDailyCraft();
				resetFreeLuna();
			}
		}, daily);
		/*CronService.getInstance().schedule(new Runnable() {
			public void run() {
				freeBoxCraft = false;
				craftBox(null);
			}
		}, daily);*/
		CronService.getInstance().schedule(new Runnable() {
			public void run() {
				specialGenerated = false;
				generateSpecialCraft();
			}
		}, weekly);
	}
	
	public void generateSpecialCraft() {
		if (SpecialCraft.size() > 0) {
			SpecialCraft.clear();
		}
		armors.add(10029);
		armors.add(10031);
		armors.add(10033);
		armors.add(10035);
		pants.add(10037);
		pants.add(10039);
		pants.add(10041);
		pants.add(10043);
		shoes.add(10061);
		shoes.add(10063);
		shoes.add(10065);
		shoes.add(10067);
		gloves.add(10053);
		gloves.add(10055);
		gloves.add(10057);
		gloves.add(10059);
		shoulders.add(10045);
		shoulders.add(10047);
		shoulders.add(10049);
		shoulders.add(10051);
		weapons.add(10021);
		weapons.add(10017);
		weapons.add(10025);
		weapons.add(10005);
		weapons.add(10011);
		weapons.add(10023);
		weapons.add(10003);
		weapons.add(10007);
		weapons.add(10019);
		weapons.add(10013);
		weapons.add(10027);
		weapons.add(10009);
		weapons.add(10015);
		weapons.add(10001);
		int rnd = Rnd.get(1, 6);
		switch (rnd) {
			case 1 :
				SpecialCraft.addAll(weapons);
			break;
			case 2:
				SpecialCraft.addAll(armors);
			break;
			case 3 :
				SpecialCraft.addAll(pants);
			break;
			case 4:
				SpecialCraft.addAll(shoes);
			break;
			case 5 :
				SpecialCraft.addAll(gloves);
			break;
			case 6:
				SpecialCraft.addAll(shoulders);
			break;
		} if (!specialGenerated) {
			updateSpecialCraft();
		}
	}
	
	public void resetFreeLuna(){
		DAOManager.getDAO(PlayerLunaShopDAO.class).delete();
		updateFreeLuna();
	}
	
	public void sendSpecialCraft(Player player) {
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(2, 0, SpecialCraft));
	}
	
	private void updateSpecialCraft() {
		World.getInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(2, 0, SpecialCraft));
			}
		});
	}
	
	private void updateFreeLuna() {
		World.getInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PlayerLunaShop pls = new PlayerLunaShop(true, true, true);
				pls.setPersistentState(PersistentState.UPDATE_REQUIRED);
				player.setPlayerLunaShop(pls);
				DAOManager.getDAO(PlayerLunaShopDAO.class).add(player.getObjectId(), pls.isFreeUnderpath(), pls.isFreeFactory(), pls.isFreeChest());
			}
		});
	}
	
	public void generateDailyCraft(){
		if (DailyCraft.size() > 0) {
			DailyCraft.clear();
		} for (int i = 0; i < 5; i++) {
			int templateId = Rnd.get(30000, 37016);
			DailyCraft.add(templateId);
		} if (!dailyGenerated) {
			updateDailyCraft();
		}
	}
	
	public void sendDailyCraft(Player player) {
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(DailyCraft));
	}
	
	private void updateDailyCraft() {
		World.getInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(DailyCraft));
				dailyGenerated = true;
			}
		});
	}
	
	public void lunaPointController(Player player, int point) {
		player.setLunaAccount(point);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
	}
	
	public void muniKeysController(Player player, int keys) {
		player.setMuniKeys(keys);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4));
	}
	
	public void onLogin(Player player) {
		if(player.getPlayerLunaShop() == null){
			PlayerLunaShop pls = new PlayerLunaShop(true, true, true);
			pls.setPersistentState(PersistentState.UPDATE_REQUIRED);
			player.setPlayerLunaShop(pls);
			DAOManager.getDAO(PlayerLunaShopDAO.class).add(player.getObjectId(), pls.isFreeUnderpath(), pls.isFreeFactory(), pls.isFreeChest());
		}
		sendSpecialCraft(player);
		sendDailyCraft(player);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4, player.getMuniKeys()));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(6));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(7));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
		if (!player.getPlayerLunaShop().isFreeUnderpath()) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 45));
		} if (!player.getPlayerLunaShop().isFreeFactory()) {
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 47));
		}
	}
	
	public void specialDesign(Player player, int recipeId) {
		LunaTemplate recipe = DataManager.LUNA_DATA.getLunaTemplateById(recipeId);
		int product_id = recipe.getProductid();
		int quantity = recipe.getQuantity();
		boolean isSuccess = isSuccess(player, recipeId);
		if (isSuccess) {
			for (LunaComponent lc : recipe.getLunaComponent()) {
				for (LunaComponentElement a : lc.getComponents()) {
					if (!player.getInventory().decreaseByItemId(a.getItemid(), a.getQuantity())) {
						PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(0x01, 1, product_id, quantity));
						PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(2, product_id, 0));
						return;
					}
				}
			}
			ItemService.addItem(player, product_id, quantity);
		} else {
			for (LunaComponent lc : recipe.getLunaComponent()) {
				for (LunaComponentElement a : lc.getComponents()) {
					if (!player.getInventory().decreaseByItemId(a.getItemid(), a.getQuantity())) {
						PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(0x01, 1, product_id, quantity));
						PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(2, product_id, 1));
						return;
					}
				}
			}
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(isSuccess ? 0x00 : 0x01, 1, product_id, quantity));
	}
	
	public void craftBox(Player player) {
		int itemId = 188055460;
		ItemTemplate item = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (freeBoxCraft) {
			ItemService.addItem(player, item.getTemplateId(), 1);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(3, item, 0));
			freeBoxCraft = false;
		} else {
			out.println("TODO!");
			//player.setLunaAccount(player.getLunaAccount() - 5);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(3, item, 0));
		}
	}
	
	private boolean isSuccess(Player player, int recipeId) {
		LunaTemplate recipe = DataManager.LUNA_DATA.getLunaTemplateById(recipeId);
		boolean result = false;
		float random = Rnd.get(1, 100);
		if (recipe.getRate() == 100) {
			result = true;
		} else if (recipe.getRate() < 100) {
			if (random <= recipe.getRate()) {
				result = true;
			} else {
				result = false;
			}
		}
		return result;
	}
	
	public void buyMaterials(Player player, int itemId, long count) {
		ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
		int lunaPrice = itemTemplate.getLunaPrice();
		long price = count*lunaPrice;
		ItemService.addItem(player, itemId, count);
		player.setLunaAccount(player.getLunaAccount() - price);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(4, player.getMuniKeys()));
	}
	
	public void dorinerkWardrobeLoad(Player player) {
		int size = DAOManager.getDAO(PlayerWardrobeDAO.class).getItemSize(player.getObjectId());
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(8, player.getWardrobeSlot(), size));
	}
	
	public void dorinerkWardrobeAct(Player player, int applySlot, int itemObjId) {
		int itemId = player.getInventory().getItemByObjId(itemObjId).getItemId();
		int itemOnDB = DAOManager.getDAO(PlayerWardrobeDAO.class).getWardrobeItemBySlot(player.getObjectId(), applySlot);
		if (itemOnDB != 0) {
			DAOManager.getDAO(PlayerWardrobeDAO.class).delete(player.getObjectId(), itemOnDB);
			player.setLunaAccount(player.getLunaAccount() - 10);
			player.getWardrobe().addItem(player, itemId, applySlot, 0);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
		} else {
			player.getWardrobe().addItem(player, itemId, applySlot, 0);
		}
		player.getInventory().decreaseByObjectId(itemObjId, 1);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(10, 0x00, applySlot, itemId, 1));
	}
	
	public void dorinerkWardrobeModifyAppearance(Player player, int applySlot, int itemObjId) {
		int itemId = DAOManager.getDAO(PlayerWardrobeDAO.class).getWardrobeItemBySlot(player.getObjectId(), applySlot);
		int reskinCount = DAOManager.getDAO(PlayerWardrobeDAO.class).getReskinCountBySlot(player.getObjectId(), applySlot);
		ItemTemplate it = DataManager.ITEM_DATA.getItemTemplate(itemId);
		Storage inventory = player.getInventory();
		Item keepItem = inventory.getItemByObjId(itemObjId);
		if (reskinCount != 0) {
			DAOManager.getDAO(PlayerWardrobeDAO.class).setReskinCountBySlot(player.getObjectId(), applySlot, reskinCount+1);
			player.setLunaAccount(player.getLunaAccount() - 15);
			keepItem.setItemSkinTemplate(it);
			if (!keepItem.getItemTemplate().isItemDyePermitted()) {
				keepItem.setItemColor(0);
			}
			keepItem.setLunaReskin(true);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
		} else {
			DAOManager.getDAO(PlayerWardrobeDAO.class).setReskinCountBySlot(player.getObjectId(), applySlot, reskinCount+1);
			keepItem.setItemSkinTemplate(it);
			if (!keepItem.getItemTemplate().isItemDyePermitted()) {
				keepItem.setItemColor(0);
			}
			keepItem.setLunaReskin(true);
		}
		ItemPacketService.updateItemAfterInfoChange(player, keepItem, ItemUpdateType.STATS_CHANGE);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CHANGE_ITEM_SKIN_SUCCEED(new DescriptionId(keepItem.getItemTemplate().getNameId())));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(11, applySlot));
	}
	
	public void dorinerkWardrobeExtendSlots(Player player) {
		int currentSlot = player.getWardrobeSlot();
		int size = DAOManager.getDAO(PlayerWardrobeDAO.class).getItemSize(player.getObjectId());
		player.setWardrobeSlot(currentSlot+1);
		player.setLunaAccount(player.getLunaAccount() - 10);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(8, player.getWardrobeSlot(), size));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
	}
	
	public void takiAdventure(Player player, int indun_id) {
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(14, indun_id));
	}
	
	public void takiAdventureTeleport(Player player, int indun_unk, int indun_id) {
		if (indun_id == 1) {
			if (player.getPlayerLunaShop().isFreeUnderpath()) {
				WorldMapInstance contaminatedUnderpath = InstanceService.getNextAvailableInstance(301630000);
				InstanceService.registerPlayerWithInstance(contaminatedUnderpath, player);
				TeleportService2.teleportTo(player, 301630000, contaminatedUnderpath.getInstanceId(), 230f, 169f, 164f, (byte) 60);
				player.getPlayerLunaShop().setFreeUnderpath(false);
				player.getPlayerLunaShop().setLunaShopByObjId(player.getObjectId());
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 45));
			} else {
				WorldMapInstance contaminatedUnderpath = InstanceService.getNextAvailableInstance(301630000);
				InstanceService.registerPlayerWithInstance(contaminatedUnderpath, player);
				TeleportService2.teleportTo(player, 301630000, contaminatedUnderpath.getInstanceId(), 230f, 169f, 164f, (byte) 60);
				player.setLunaAccount(player.getLunaAccount() - 89);
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 45));
			}
		} if (indun_id == 2) {
			if (player.getPlayerLunaShop().isFreeFactory()) {
				WorldMapInstance secretMunitionsFactory = InstanceService.getNextAvailableInstance(301640000);
				InstanceService.registerPlayerWithInstance(secretMunitionsFactory, player);
				TeleportService2.teleportTo(player, 301640000, secretMunitionsFactory.getInstanceId(), 400.3279f, 290.5061f, 198.64015f, (byte) 60);
				player.getPlayerLunaShop().setFreeFactory(false);
				player.getPlayerLunaShop().setLunaShopByObjId(player.getObjectId());
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 47));
			} else {
				WorldMapInstance secretMunitionsFactory = InstanceService.getNextAvailableInstance(301640000);
				InstanceService.registerPlayerWithInstance(secretMunitionsFactory, player);
				TeleportService2.teleportTo(player, 301640000, secretMunitionsFactory.getInstanceId(), 400.3279f, 290.5061f, 198.64015f, (byte) 60);
				player.setLunaAccount(player.getLunaAccount() - 59);
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(1, 1, 47));
			}
		}
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
	}
	
	public void teleport(Player player, int action, int teleportId) {
		switch (action) {
			case 6:
				PacketSendUtility.sendMessage(player, "teleportId : " + teleportId);
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(6));
			break;
			case 7:
				PacketSendUtility.sendMessage(player, "teleportId : " + teleportId);
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(7));
			break;
		}
	}
	
	public void munirunerksTreasureChamber(final Player player) {
		HashMap<Integer, Long> hm = new HashMap<Integer, Long>();
		hm.put(188054633, (long) 1); //[Event] Special Head Executor Weapon Box.
		hm.put(188054634, (long) 1); //[Event] Special Head Executor Armor Box.
		hm.put(166030013, (long) 1); //[Event] Tempering Solution.
		hm.put(166020003, (long) 1); //[Event] Omega Enchantment Stone.
		hm.put(188054122, (long) 1); //Major Stigma Bundle.
		hm.put(188055183, (long) 1); //Major Felicitous Socketing Box (Mythic).
		hm.put(188054287, (long) 1); //Greater Stigma Bundle.
		hm.put(188054462, (long) 1); //Illusion Godstone Bundle.
		hm.put(188052639, (long) 1); //[Event] Heroic Godstone Bundle.
		hm.put(169405339, (long) 10); //Pallasite Crystal.
		hm.put(164000076, (long) 10); //Greater Running Scroll.
		hm.put(164000134, (long) 10); //Greater Awakening Scroll.
		hm.put(166000196, (long) 3); //Enchantment Stone.
		hm.put(186000242, (long) 2); //Ceramium Medal.
		hm.put(186000051, (long) 2); //Major Ancient Crown.
		hm.put(188055168, (long) 10); //[Event] Blood Medal Box.
		hm.put(188054283, (long) 30); //Blood Mark Box.
		hm.put(188054463, (long) 1); //[Event] Fabled Godstone Bundle.
		hm.put(188053002, (long) 1); //[Event] Noble Composite Manastone Bundle.
		hm.put(188100335, (long) 2000); //Enchantment Stone Dust.
		hm.put(164000073, (long) 10); //Greater Courage Scroll.
		hm.put(160002497, (long) 1); //Fresh Oily Plucar Dragon Salad.
		hm.put(160002499, (long) 1); //Fresh Oily Plucar Dragon Soup.
		if (player.getMuniKeys() > 0) {
			player.setMuniKeys(player.getMuniKeys()-1);
		} else {
			player.setLunaAccount(player.getLunaAccount() - 19);
			player.setLunaConsumePoint(player.getLunaConsumePoint() + 19);
		} if (player.getLunaConsumePoint() == 25) {
			player.setLunaConsumeCount(1);
			LunaConsumeRewardsTemplate lt = DataManager.LUNA_CONSUME_REWARDS_DATA.getLunaConsumeRewardsId(player.getLunaConsumeCount());
			ItemService.addItem(player, lt.getCreateItemId(), lt.getCreateItemCount());
		} else if (player.getLunaConsumePoint() == 50) {
			player.setLunaConsumeCount(2);
			LunaConsumeRewardsTemplate lt = DataManager.LUNA_CONSUME_REWARDS_DATA.getLunaConsumeRewardsId(player.getLunaConsumeCount());
			ItemService.addItem(player, lt.getCreateItemId(), lt.getCreateItemCount());
		} else if (player.getLunaConsumePoint() == 100) {
			player.setLunaConsumeCount(3);
			LunaConsumeRewardsTemplate lt = DataManager.LUNA_CONSUME_REWARDS_DATA.getLunaConsumeRewardsId(player.getLunaConsumeCount());
			player.setMuniKeys(player.getMuniKeys() + 1);
		} else if (player.getLunaConsumePoint() == 150) {
			player.setLunaConsumeCount(4);
			LunaConsumeRewardsTemplate lt = DataManager.LUNA_CONSUME_REWARDS_DATA.getLunaConsumeRewardsId(player.getLunaConsumeCount());
			player.setMuniKeys(player.getMuniKeys() + 1);
		} else if (player.getLunaConsumePoint() == 300) {
			player.setLunaConsumeCount(5);
			LunaConsumeRewardsTemplate lt = DataManager.LUNA_CONSUME_REWARDS_DATA.getLunaConsumeRewardsId(player.getLunaConsumeCount());
			player.setMuniKeys(player.getMuniKeys() + 2);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4, player.getMuniKeys()));
		} else if (player.getLunaConsumePoint() == 500) {
			player.setLunaConsumeCount(6);
			LunaConsumeRewardsTemplate lt = DataManager.LUNA_CONSUME_REWARDS_DATA.getLunaConsumeRewardsId(player.getLunaConsumeCount());
			ItemService.addItem(player, lt.getCreateItemId(), lt.getCreateItemCount());
			player.setMuniKeys(player.getMuniKeys() + 2);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4, player.getMuniKeys()));
		} else if (player.getLunaConsumePoint() == 1000) {
			player.setLunaConsumeCount(7);
			LunaConsumeRewardsTemplate lt = DataManager.LUNA_CONSUME_REWARDS_DATA.getLunaConsumeRewardsId(player.getLunaConsumeCount());
			ItemService.addItem(player, lt.getCreateItemId(), lt.getCreateItemCount());
			player.setMuniKeys(player.getMuniKeys() + 3);
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4, player.getMuniKeys()));
		}
		final HashMap<Integer, Long> mt = new HashMap<Integer, Long>();
		for (int i=0;i<3;i++) {
			Object[] crunchifyKeys = hm.keySet().toArray();
			Object key = crunchifyKeys[new Random().nextInt(crunchifyKeys.length)];
			mt.put((int)key, (long)hm.get(key));
		}
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				for (Map.Entry<Integer, Long> e : mt.entrySet()) {
					ItemService.addItem(player, (int)e.getKey(), (long)e.getValue());
					ItemTemplate t = DataManager.ITEM_DATA.getItemTemplate((int)e.getKey());
					if (e.getValue() == 1) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LUNA_REWARD_GOTCHA_ITEM(t.getNameId()));
					} else if (e.getValue() > 1) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LUNA_REWARD_GOTCHA_ITEM_MULTI(e.getValue(), t.getNameId()));
					}
				}
				PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP(mt));
			}
		}, 1);
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(5));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(4, player.getMuniKeys()));
		PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(0, player.getLunaAccount()));
		//As you spend Luna, you can earn keys to open Munirunerks Treasure Chest. 
		//If you do not have any keys, you can spend 3 Luna to open a chest immediately. 
		//The Luna you spend on opening chests will also count towards your Luna Rewards! 
		//http://i.imgur.com/F1sikRD.jpg
	}
	
	public static LunaShopService getInstance() {
		return NewSingletonHolder.INSTANCE;
	}
	
	private static class NewSingletonHolder {
		private static final LunaShopService INSTANCE = new LunaShopService();
	}
}