package org.swarg.mc.custombook.handlers;

import java.util.ArrayList;
import java.util.HashMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import noppes.npcs.Server;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumQuestType;
import noppes.npcs.controllers.PlayerData;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestData;
import noppes.npcs.controllers.Quest;
import noppes.npcs.quests.QuestItem;

import org.swarg.mc.custombook.util.NpcUtil;
import static org.swarg.mc.custombook.CustomNPCBook.LOG;
import static org.swarg.mc.custombook.handlers.CommandCustomBooks.QUESTTAG;
import static org.swarg.mc.custombook.util.NpcUtil.safe;
import static net.minecraft.util.StringUtils.isNullOrEmpty;
import noppes.npcs.constants.EnumQuestCompletion;

/**
 * 13-02-21
 * for own custombooks quests for crafting items
 * @author Swarg
 */
public class QuestCraftHandler {
    public int bookQuestCatId = 0;
    //private boolean debug = false;


    //ServerSide
    /**
     * 
     * @param event
     */
    @SubscribeEvent
    public void onItemCraftEvent(PlayerEvent.ItemCraftedEvent event) {
        ///*DEBUG*/if(debug){LOG.info("[###] Crafted:{}", event.crafting);}
        try {
            if (NpcUtil.isServerSide(event.player) && PlayerDataController.instance != null && event.crafting != null) {
                PlayerData pData = PlayerDataController.instance.getPlayerData(event.player);
                if (pData != null) {
                    HashMap<Integer, QuestData> activeQuests = pData.questData.activeQuests;
                    if (activeQuests != null && !activeQuests.isEmpty()) {
                        for (QuestData qdata : activeQuests.values()) {
                            //TODO limit qd.quest.category  ?????????????

                            if (qdata != null && !qdata.isCompleted && qdata.quest != null
                                    && qdata.quest.completion == EnumQuestCompletion.Instant  //without NPC
                                    && qdata.quest.type == EnumQuestType.Item && qdata.quest.questInterface instanceof QuestItem //itemQuest + OneItem slot0!
                                )
                            {
                                QuestItem qi = (QuestItem) qdata.quest.questInterface;
                                //only one item in QuestItem in firsSlot
                                //Process only quests for which do not need to take the item from player to complete the task
                                //only one item is required for quest complet in the first slot
                                //because at the moment there is no way to pick up the newly crafted item
                                if (/*!!*/qi.leaveItems/*!!*/ && qi.items != null && safe(qi.items.items).size() == 1) {
                                    ItemStack is0 = qi.items.items.get(0);//you can't craft more than one at a time
                                    if (is0 != null) {
                                        //NoppesUtilPlayer.compareItems() + compareItemDetails()
                                        if (isMatchedItemStack(is0, event.crafting, true)) {// permutation qi.isCompleted()
                                            ///*DEBUG*/if(debug){LOG.info("[###] Found Equals in quest item! {} isCompleted: {}", is0.getItem(), qdata.isCompleted);}

                                            //if (!qi.leaveItems) {//quest take item from player qi.handleComplete(event.player);
                                                //the quest takes the crafted item
                                                ///*DEBUG*/LOG.info("[###] Quest must take item from player isCancelable: {}", event.isCancelable());
                                                //DontWorkif (event.isCancelable()) {event.setCanceled(true);}
                                                //event.crafting.stackSize = 0;//dontWork
                                            //}
                                            if (forceCompleteQuestFor(event.player, qdata.quest, false)) {
                                                qdata.isCompleted = true;
                                                PlayerDataController.instance.savePlayerData(pData);
                                                return;//can only craft one thing at a time
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Throwable e) {
        }

    }

    /**
     * Chech is itemstack from player matched fro etalon questItemStack
     * @param qIs quest ItemStack
     * @param has item Stack from player
     * @param ignoreQuestTagInQuestSlot for case then need special behavior for unique QuestItem
     * @return
     */
    public boolean isMatchedItemStack(ItemStack qIs, ItemStack has, boolean ignoreQuestTagInQuestSlot) {
        if (qIs != null && has != null) {
            boolean b0 = qIs.isItemEqual(has) && qIs.stackSize == has.stackSize;
            if (b0) {
                if (qIs.stackTagCompound == null && has.stackTagCompound == null) {
                    return true;
                }
                //ignore QuestTag in questSlotItemStack
                //for the ability to create unique impossible in normal conditions quests for items
                //by defining an item in the necessary item questslot with a unique nbt-tag added, which cannot be obtained in the usual way in the game (command "quest-tag")

                else if (has.stackTagCompound == null && qIs.stackTagCompound != null && ignoreQuestTagInQuestSlot) {
                    //has only QuestTag in nbt
                    if (qIs.stackTagCompound.hasKey(QUESTTAG) && qIs.stackTagCompound.func_150296_c().size() == 1) {
                        return true;
                    }
                } 
                //nbt full equls
                else if (qIs.stackTagCompound != null && has.stackTagCompound != null) {
                    return qIs.stackTagCompound.equals(has.stackTagCompound);
                }
                //todo check nbt ignore questtag!
            }
        }
        return false;
    }

    /**
     * own version of NoppesUtilPlayer.questCompletion()
     * [WARN] Perform the check before calling this method!
     * [WARN] Don`t check quest condition and don`t take quest items
     * Set Quest Completed give rewards and next Quest if exists
     * Send Package to Client about the complited quest
     * @param player
     * @param quest
     * @param showCompleteTextGui
     * @return
     */
    public static boolean forceCompleteQuestFor(EntityPlayer player, Quest quest, boolean showCompleteTextGui) {
        if (player != null && quest != null) {

            //Reward Exp
            if (quest.rewardExp > 0) {
                player.worldObj.playSoundAtEntity(player, "random.orb", 0.1F, 0.5F * ((player.worldObj.rand.nextFloat() - player.worldObj.rand.nextFloat()) * 0.7F + 1.8F));
                player.addExperience(quest.rewardExp);
            }
            //Reward FactionPoints
            if (quest.factionOptions != null) {
                quest.factionOptions.addPoints(player);
            }
            //Reward Items
            if (quest.rewardItems != null && safe(quest.rewardItems.items).size() > 0) {
                //Random One Reward from list
                if (quest.randomReward ) {
                    ArrayList rndList = new ArrayList();
                    for (ItemStack reward : quest.rewardItems.items.values()) {
                        if (reward != null && reward.getItem() != null) {
                            rndList.add(reward);
                        }
                    }
                    if (!rndList.isEmpty()) {
                        NoppesUtilServer.GivePlayerItem(player, player, (ItemStack)rndList.get(player.getRNG().nextInt(rndList.size())));
                    }
                }
                //List of Rewards items
                else {
                    for (ItemStack item : quest.rewardItems.items.values()) {
                        NoppesUtilServer.GivePlayerItem(player, player, item);
                    }
                }
            }
            //
            if (!isNullOrEmpty(quest.command)) {
                NoppesUtilServer.runCommand(player, "QuestCompletion", quest.command);
            }

            PlayerQuestController.setQuestFinished(quest, player);
            if (quest.hasNewQuest()) {
                PlayerQuestController.addActiveQuest(quest.getNextQuest(), player);
            }

            //PlayerQuestData.checkQuestCompletion()
            if (showCompleteTextGui) {
                // Package for PlayerClient Gui with quest.completeText
                //part from quest.complete(player,qdata) send pkg 2player for show
                Server.sendData((EntityPlayerMP)player, EnumPacketClient.QUEST_COMPLETION, new Object[]{quest.writeToNBT(new NBTTagCompound())});
            }
            
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.MESSAGE, new Object[]{"quest.completed", quest.title});
            Server.sendData((EntityPlayerMP)player, EnumPacketClient.CHAT, new Object[]{"quest.completed", ": ", quest.title});
            //qdata.isCompleted = true;  
            //PlayerDataController.instance.savePlayerData(pData);
            return true;
        }
        return false;
    }
}
