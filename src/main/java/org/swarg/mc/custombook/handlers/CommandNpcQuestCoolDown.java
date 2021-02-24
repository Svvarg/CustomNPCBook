package org.swarg.mc.custombook.handlers;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.PlayerData;
import noppes.npcs.controllers.Quest;

import org.swarg.cmds.ArgsWrapper;
import static org.swarg.mc.custombook.util.NpcUtil.toSender;
import static net.minecraft.util.StringUtils.isNullOrEmpty;

/**
 * 14-02-21
 * Отдельно от CommandCustomExtension для возможной последующей настройки доступа
 * команды из данного обработчика по сути безопасны и не несут каких-либо серьёзных
 * последствий в отличии от CommandCustomExtension (где нужно знать что делаешь)
 * @author Swarg
 */
public class CommandNpcQuestCoolDown extends CommandBase {
    private static final long MC_DAY_LENGTH = 24000L;
    private static final long MC_WEEK_LENGTH = 168000L;


    @Override
    public String getCommandName() {
        return "noppescooldown";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "<quest/craft-item-quest>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String response = null;
        ArgsWrapper w = new ArgsWrapper(args);
        if (w.isHelpCmdOrNoArgs()) {
            response = getCommandUsage(sender);
        }
        //определить и показать кулдаун повторяемого квеста
        else if (w.isCmd("quest", "q")) {
            int id = w.argI(w.ai++,-1);
            String name = w.arg(w.ai++);
            if (id < 0 || isNullOrEmpty(name)) {
                response = "(questId) (player)";
            } else {
                Quest quest = QuestController.instance.quests.get(id);
                if ( quest == null) {
                    response = "Not Found QuestId: "+id;
                } else {
                    //only for online players!
                    EntityPlayer player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);//getPlayer(sender, name);
                    if (player == null) {
                        response = "Not Found Player: " + name;
                    } else {
                        response = getQuestCoolDown(player, quest);
                        toSender(player, response);
                        return;

                    }
                }
            }
        }
        else if (w.isCmd("craft-item-quest", "ciq")) {
            if (w.isHelpCmdOrNoArgs()) {
                response = "<set/get/update>";
            }
            //добавить игрока в списки имеющих активные задания на крафты предметов
            else if (w.isCmd("status", "st")) {
                response = QuestCraftHandler.instance().CraftItemHandlerStatus();
            }
            //используется для активации проверки игрока в хуке на крафт
            else if (w.isCmd("set", "s")) {
                final String playername = w.arg(w.ai++);
                final boolean has = !w.hasArg() || w.argB(w.ai++);//def - true;
                QuestCraftHandler.instance().setHasPlayerCraftItemQuest(playername, has);
                response = playername + ' ' + has;
            }
            //[DEBUG]
            else if (w.isCmd("get", "g")) {
                final String playername = w.arg(w.ai++);
                Boolean has = QuestCraftHandler.instance().hasPlayerCraftItemQuest(playername);
                response = playername + ' ' + has;
            }
            //[DEBUG]автоматическая проверка по количеству квестов у игрока по его имени
            else if (w.isCmd("update", "u")) {
                final String playername = w.arg(w.ai++);
                EntityPlayer player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(playername);
                Boolean has = QuestCraftHandler.instance().updatePlayerActiveItemCraftQuestsStatus(player);
                response = playername + ' ' + has;
            }
        }
        //ЕЩметка для предмета для исключения повторного использования этого же предмета в квестах

//        else if (w.isCmd("complite-quest-tag", "ct")) {
//            respone = "todo";
//        }
        toSender(sender, response);
    }


    public static String getQuestCoolDown(EntityPlayer player, Quest quest) {
        if (quest != null && player != null) {
            final int id = quest.id;
            if (PlayerQuestController.canQuestBeAccepted(quest, player)) {
                return "Can Accept Quest";
            } else {
                PlayerData pd = PlayerDataController.instance.getPlayerData(player);
                if (pd != null) {
                    Long compliteTime = pd.questData.finishedQuests.get(id);
                    if (compliteTime != null) {
                        //Realy time
                        if (quest.repeat == EnumQuestRepeat.RLDAILY || quest.repeat == EnumQuestRepeat.RLWEEKLY) {
                            final long remain = System.currentTimeMillis() - compliteTime;
                            double cooldown = 0D;
                            if (quest.repeat == EnumQuestRepeat.RLDAILY) {
                                //PlayerQuestController.canQuestBeAccepted()
                                if (remain < 86400000L) {
                                    cooldown = (86400000L - remain) / 3600000D;
                                }
                            } else {
                                if (remain < 604800000L) {
                                    cooldown = (604800000L - remain) / 3600000D;
                                }
                            }
                            return "CoolDown " + cooldown + " (RL) Hours";
                        }
                        //MC ingame time
                        else {
                            if (quest.repeat == EnumQuestRepeat.MCDAILY || quest.repeat == EnumQuestRepeat.MCWEEKLY) {
                                final long inGameTime = player.worldObj.getTotalWorldTime();
                                final long passed = inGameTime - compliteTime;
                                double cooldown = 0;
                                /*DEBUG*/System.out.println("Passed: " + passed+" inGame-Now:"+inGameTime + " QuestTime:" + compliteTime);
                                if (quest.repeat == EnumQuestRepeat.MCDAILY) {
                                    if (passed < MC_DAY_LENGTH) {
                                        cooldown =  ((MC_DAY_LENGTH - passed) * 24D) / MC_DAY_LENGTH;
                                    }
                                }
                                else {
                                    if (passed < MC_WEEK_LENGTH) {
                                        cooldown =  ((MC_WEEK_LENGTH - passed) * 24D) / MC_DAY_LENGTH;
                                    }
                                }
                                return "CoolDown " + cooldown + " (Game) Hours";
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
}
