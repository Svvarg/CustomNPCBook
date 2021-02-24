package org.swarg.mc.custombook.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.server.MinecraftServer;
import noppes.npcs.Server;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumQuestType;
import noppes.npcs.controllers.PlayerData;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestData;
import noppes.npcs.controllers.Quest;
import noppes.npcs.quests.QuestItem;

import org.swarg.mc.custombook.util.NpcUtil;
import static org.swarg.mc.custombook.util.NpcUtil.safe;
import static net.minecraft.util.StringUtils.isNullOrEmpty;

/**
 * 13-02-21
 * for own custombooks quests for crafting items
 * @author Swarg
 */
public class QuestCraftHandler {
    public int bookQuestCatId = 0;
    //private boolean debug = false;
    /*данные о том, имеет ли игрок активные квесты на крафты из книги
      чтобы уменьшить нагрузку при хуке на крафт предмета (иначе перебор-поиск
    будет идти при каждом крафте при наличии других активных квестов)*/
    private Map<String, Boolean> activeCraftQuest;

    private static QuestCraftHandler INSTANCE;
    public static QuestCraftHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new QuestCraftHandler();
        }
        return INSTANCE;
    }
    
    public QuestCraftHandler() {
        //if (SERVER_SIDE)
            activeCraftQuest = new HashMap<String, Boolean>();
    }


    //ServerSide
    /**
     * Хук на событие крафта, для автоматического выполнения крафт-квестов из кастом-книги
     * @param event
     */
    @SubscribeEvent
    public void onItemCraftEvent(PlayerEvent.ItemCraftedEvent event) {
        try {
            if (NpcUtil.isServerSide(event.player) && PlayerDataController.instance != null && event.crafting != null)
            {
                Boolean has = activeCraftQuest.get(event.player.getCommandSenderName());
                /*инициирущая проверка например после перезапуска сервера установка флага должна
                быть в момент взятия квеста(ну или при открытии книги) Для исключения
                излишних полных проверок на наличе крафт-квестов в данном событии
                команду обновления наличия крафт-квестов "руками" добавлять в commands внутри 
                кастома иначе даже и не будет проверять в данном событии*/
                if (has == null) {
                    has = updatePlayerActiveItemCraftQuestsStatus(event.player);
                }
                if (has == Boolean.TRUE) {
                    ///*DEBUG*/System.out.println("### HasCraftItemQuest ["+has+"] for " + event.player.getCommandSenderName());

                    PlayerData pData = PlayerDataController.instance.getPlayerData(event.player);
                    if (pData != null) {
                        HashMap<Integer, QuestData> activeQuests = pData.questData.activeQuests;
                        final int aqCount = activeQuests == null ? 0 : activeQuests.size();
                        if (aqCount > 0) {
                            /*тонкий момент если здесь будет выполнение более одного крафт-квеста за раз
                            нужно будет исп-ть итератор и ручное удаление т.к. forceCompleteQuestFor вызывает
                            удаление квеста по идишнику в setQuestFinished и будет ошибка канкарент доступа */
                            for (QuestData qdata : activeQuests.values()) {
                                Map<Integer, ItemStack> reqItems = getRequiredCraftQuestItems(qdata);
                                if (reqItems != null && reqItems.size() > 0) {
                                    final int sz = reqItems.size();
                                    //ANY_OF_ITEM например для тфк где по 4 вида каменных инструментов или просто в одном квесте для зачёта нужно скарфтить любой однин предмет из списка
                                    for (int i = 0; i < sz; i++) {
                                        ItemStack is = reqItems.get(i);
                                        if (isMatchedItemStack(is, event.crafting, true)) {// permutation qi.isCompleted()
                                            ///*DEBUG*/System.out.println("[######]  Found Equals in quest item!");

                                            if (forceCompleteQuestFor(event.player, qdata.quest, false)) {
                                                qdata.isCompleted = true;
                                                PlayerDataController.instance.savePlayerData(pData);
                                                /*если был активный только один квест - то после выполнение сразу могут дать
                                                следующий поэтому нужна полная проверка если текущий выполненный квест не имеет продолжения*/
                                                //чтобы не проверять его в данном событии при каждом крафте
                                                if (aqCount == 1) {
                                                    if (qdata.quest != null && qdata.quest.nextQuestid > 0) {
                                                        //полная проверка т.к. могли дать следующий крафт-квест
                                                        updatePlayerActiveItemCraftQuestsStatus(event.player);
                                                    } else {
                                                        activeCraftQuest.put(event.player.getCommandSenderName(), Boolean.FALSE);
                                                    }
                                                }                                                
                                                return;//can only craft one thing at a time
                                            }
                                        }
                                    }
                                }
                            }
                        } else {                 
                            activeCraftQuest.put(event.player.getCommandSenderName(), Boolean.FALSE);
                        }
                    }
                }
            }
        }
        catch (Throwable e) {
        }
    }

    public boolean updatePlayerActiveItemCraftQuestsStatus(EntityPlayer player) {
        if (player != null) {
            PlayerData pData = PlayerDataController.instance.getPlayerData(player);
            if (pData != null) {
                if (pData != null) {
                    HashMap<Integer, QuestData> activeQuests = pData.questData.activeQuests;
                    if (activeQuests != null && !activeQuests.isEmpty()) {
                        for (QuestData qdata : activeQuests.values()) {
                            Map<Integer, ItemStack> reqItems = getRequiredCraftQuestItems(qdata);
                            if (reqItems != null && reqItems.size() > 0) {
                                this.activeCraftQuest.put(player.getCommandSenderName(), Boolean.TRUE);
                                return true;
                            }
                        }
                    }
                }
            }
            this.activeCraftQuest.put(player.getCommandSenderName(), Boolean.FALSE);//?
        }
        return false;
    }

    //метка квеста из книги на создание предмета
    public static final String QUESTBOOK_CRAFT_ITEM = "qbci";// NBTByte!
    //защитывается крафт любого из предметов
    public static final byte ANY_OF_ITEM = 1;
    /**
     * Мапа предеметов - нужныйх для выполнения квеста
     * @param qdata
     * @return 
     */
    public Map<Integer, ItemStack> getRequiredCraftQuestItems(QuestData qdata) {
        if (qdata != null && !qdata.isCompleted && qdata.quest != null
                && qdata.quest.completion == EnumQuestCompletion.Instant  //without NPC
                && qdata.quest.type == EnumQuestType.Item && qdata.quest.questInterface instanceof QuestItem //itemQuest + OneItem slot0!
            )
        {
            QuestItem qi = (QuestItem) qdata.quest.questInterface;
            /*Если квест оставляет предметы у игрока и заданы предметы-нужные для выполнения */
            if (/*!!*/qi.leaveItems/*!!*/ && qi.items != null && qi.items.items != null && qi.items.items.size() > 0) {
                ItemStack is0 = qi.items.items.get(0);
                /*первый предмет должен содержать нбт-таг подтверждающий что это квест на создание предметов через кастом-книгу
                и тип квеста должен быть - любой предмет из списка требуемых*/
                if (is0 != null && is0.hasTagCompound()) {
                    NBTBase base = is0.stackTagCompound.getTag(QUESTBOOK_CRAFT_ITEM);
                    if (base != null && base.getId() == 1) { //byte
                        byte type = ((NBTBase.NBTPrimitive)base).func_150290_f();
                        if (type == ANY_OF_ITEM) {
                            return qi.items.items;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static byte setQuestBookCraftItemTag(ItemStack is, int type) {
        if (is != null) {
            if (!is.hasTagCompound()) {
                is.stackTagCompound = new NBTTagCompound();
            }
            byte btype = (byte)type;
            is.stackTagCompound.setByte(QUESTBOOK_CRAFT_ITEM, btype);
            return btype;
        }
        return -1;//error
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

                else if (has.stackTagCompound == null && 
                        qIs.stackTagCompound != null && !qIs.stackTagCompound.hasNoTags() && ignoreQuestTagInQuestSlot) {
                    //has only QuestTag in nbt
                    final Set topNbtKeys = qIs.stackTagCompound.func_150296_c();
                    final int sz = topNbtKeys.size();
                    if (sz == 1 && topNbtKeys.contains(QUESTBOOK_CRAFT_ITEM)) {
                    //тогда второй таг QUESTTAG который ранее холет использовать как защиту от автовыполнение квеста при наличии предметов в инвентаре - не нужен
                    //эту защиту будет осуществвлять в данном случае обязательный для крафт-квестов через кастом-книгу таг QUESTBOOK_CRAFT_ITEM
                    //if (qIs.stackTagCompound.hasKey(QUESTTAG) && qIs.stackTagCompound.func_150296_c().size() == 1) {
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
            //Command onQuestDone
            if (!isNullOrEmpty(quest.command)) {
                NoppesUtilServer.runCommand(player, "QuestCompletion", quest.command);
            }

            //((PlayerQuestData)data).activeQuests.remove(Integer.valueOf(quest.id));
            PlayerQuestController.setQuestFinished(quest, player);
            if (quest.hasNewQuest()) {
                PlayerQuestController.addActiveQuest(quest.getNextQuest(), player);
            }

            //PlayerQuestData.checkQuestCompletion()
            //for ItemCraft Quest not show  иначе закрывает гуи инвентаря и сбивает с толку
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

    public void setHasPlayerCraftItemQuest(String playername, boolean has) {
        if (!this.activeCraftQuest.containsKey(playername)) {
            EntityPlayer player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(playername);
            if (player == null) {
                return;
            }
            playername = player.getCommandSenderName();
        }
        activeCraftQuest.put(playername, has);
    }

    public Boolean hasPlayerCraftItemQuest(String playername) {
        return this.activeCraftQuest.get(playername);
    }
    
    //debug
    public String CraftItemHandlerStatus() {
        if (!this.activeCraftQuest.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Boolean> e : activeCraftQuest.entrySet()) {
                String key = e.getKey();
                Boolean value = e.getValue();
                sb.append(key).append(' ').append(value).append('\n');
            }
            return sb.toString();
        } else {
            return "empty";
        }
    }
}
