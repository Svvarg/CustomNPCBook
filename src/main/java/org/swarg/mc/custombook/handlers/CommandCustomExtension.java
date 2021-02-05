package org.swarg.mc.custombook.handlers;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.time.Instant;
import java.time.ZoneId;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import noppes.npcs.Server;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumOptionType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.controllers.DialogCategory;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.DialogOption;
import noppes.npcs.controllers.Dialog;
import noppes.npcs.controllers.PlayerData;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerDialogData;
import noppes.npcs.controllers.PlayerQuestData;
import noppes.npcs.controllers.QuestCategory;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.QuestData;
import noppes.npcs.controllers.Quest;

import org.swarg.cmds.ArgsWrapper;
import org.swarg.mc.custombook.BooksKeeper;
import org.swarg.mc.custombook.util.NpcUtil;
import static org.swarg.mc.custombook.util.NpcUtil.safe;


/**
 * 05-02-21
 * @author Swarg
 */
public class CommandCustomExtension extends CommandBase {

    public static final java.time.format.DateTimeFormatter DT_FORMAT = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yy");
    
    private final List<String> aliases;
    /*used to free dialogsId on remove last yang Dialogs pack*/
    private boolean packCreated;

    public CommandCustomExtension() {
        this.aliases = new ArrayList<String>();
        this.aliases.add("cx");//custom-eXtension
    }

    @Override
    public List<String> getCommandAliases() {
        return this.aliases;
    }

    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        System.out.println(args);
        return Collections.EMPTY_LIST;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;//??
    }

    @Override
    public String getCommandName() {
        return "customextension";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "<dialog/player-data>";
    }


    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String response = null;

        ArgsWrapper w = new ArgsWrapper(args);

        if (w.isHelpCmdOrNoArgs()) {
            response = getCommandUsage(sender);
        }

        //commands only for op-player
        else if (NpcUtil.canPlayerEditNpc(sender, false, true)) {
            response = "UKNOWN";

            //-------------------------- tools -----------------------------------\\
            if (w.isCmd("dialog", "d")) {
                response = cmdDialog(w, sender);
            }
            else if (w.isCmd("player-data", "pd")) {
                try {
                    response = cmdPlayerData(w, sender);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //output to op
        if (response != null) {
            // multiline
            if (sender instanceof EntityPlayer && response.contains("\n")) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    sender.addChatMessage(new ChatComponentText(line));
                }
            }
            //single line
            else {
                sender.addChatMessage(new ChatComponentText(response));
            }
        }
    }

      //  ==============------
     // Implementation

    /**
     *
     * @param w
     * @param sender
     * @return
     */
    private String cmdDialog(ArgsWrapper w, ICommandSender sender) {
        String response = "?";
        if (w.isHelpCmdOrNoArgs()) {
            return "dialog (id) <status/edit/text/gui/options/script> | dialog <last-dialog-id [-trim] / set-back-title (title)>";
        }

        if (w.isCmd("last-dialog-id", "ldi")) {
            boolean trim = w.hasOpt("-trim");
            return "" + NpcUtil.getLastDialogID(trim);
        }

        //used at dialog #id options add-new name.. to auto add backs to dialog
        //to disable set empty name
        else if (w.isCmd("set-back-title", "sbt")) {
            if (w.isHelpCmd()) {
                return "sbt (backOptionTitle)";
            } else {
                response = w.arg(w.ai++, null);
                BooksKeeper.instance().setBackTitle(response);
            }
        }


        int dialogId = w.argI(w.ai++, -1);
        Dialog dialog = (Dialog) DialogController.instance.dialogs.get( dialogId );
        if (dialog == null) {
            return "Not Found Dialog for Id: " + dialogId;
        }

        //cb dialog new-options op0 op2 op3 .. op5
        if (w.noArgs() || w.isCmd("status", "st")) {
            response = cmdDialogStatus(dialog, w);
        }
        //cb dialog #id edit
        else if (w.isCmd("edit", "e")) {
            response = cmdDialogEdit(dialog, w);
        }

        else if (w.isCmd("text")) {
            response = dialog.text;
        }

        //show dialog in gui
        else if (w.isCmd("gui", "g") && sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP)sender;
            EntityNPCInterface npc = (EntityNPCInterface) NpcUtil.getFirsNearestEntity(player, EntityNPCInterface.class);
            if (npc != null) {
                NoppesUtilServer.sendOpenGui(player, EnumGuiType.ManageDialogs, npc);//MainMenuDisplay
                //открывает категодию диалога имя категории выбирается и становится редактируемым но не видно содержимое категории - диалоги в ней
                //срабатывает только после 0 open ManageDialogs
                DialogCategory dc = dialog.category;
                if (dc != null) {
                    NBTTagCompound nbt = dc.writeNBT(new NBTTagCompound());
                    nbt.removeTag("Dialogs");
                    Server.sendData(player, EnumPacketClient.GUI_DATA, new Object[]{nbt});
                }
                //todo show dialogs in category - "open category"
                response = "Gui Opened";
            } else {
                response = "You need to stand in the same chunk with any CustomNpc";
            }
        }

        //DialogOptions - possible responses to the current dialog
        // cb dialog #id options new option0-title o1-title o2-title... n
        else if (w.isCmd("options", "o")) {
            if (w.isHelpCmdOrNoArgs()) {
                return "<add-new/add-back/add-quit/remove/clean/edit/move>";
            }
            if (dialog.options == null) {
                dialog.options = new HashMap();
            }
            //add add new options-dialogs to current dialog.options
            // use \ for separate title names
            if (w.isCmd("add-new", "an")) {
                return cmdDialogOptionsAddNew(dialog, w);
            }
            // cb d #id add-back #toDialogId
            else if (w.isCmd("add-back", "ab")) {
                if (w.isHelpCmdOrNoArgs()) {
                    response = "(#slot) [BackTitle]";
                } else {
                    int toDialogId = w.argI(w.ai++, -1);
                    String backTitle = w.hasArg() ? w.join(w.ai) : BooksKeeper.instance().getBackTitle();
                    boolean flag = addBackDialogOptionTo(dialog, toDialogId, backTitle);
                    response = "Back Added: " + flag + " to DialogId:" + toDialogId +" from DialogId: " + dialog.id;
                }
            }
            else if (w.isCmd("add-quit", "aq")) {
                if (w.isHelpCmdOrNoArgs()) {
                    response = "(#slot) [QuitTitle]";
                } else {
                    int slot = w.argI(w.ai++, -1);
                    String quitTitle = w.hasArg() ? w.join(w.ai) : "Quit";
                    boolean flag = addQuitDialogOption(dialog, slot, quitTitle);
                    response = "Quit Added: " + flag + " to DialogId: " + dialog.id + " to DialogOptionSlot: " + slot;
                }
            }

            //remove dialog option from current dialog.options and delete the dialog-option from disk
            else if (w.isCmd("remove", "rm")) {
                //remove one specific dialog-option from dialog.options by his slotIndex
                Integer slot = w.argI(w.ai++, -1);
                DialogOption roDialog = dialog.options.get(slot);
                if (roDialog != null) {
                    Dialog rDialog = DialogController.instance.dialogs.get(roDialog.dialogId);
                    if (rDialog != null) {
                        //from maps and disk
                        DialogController.instance.removeDialog(rDialog);
                    }
                    //case then no dialog but has dialog-option slot
                    Object removed = dialog.options.remove(slot);
                    response = "Slot - Cleaned.  Dialog: " + removed == null ? "The slot was Empty" : "Removed";
                    //save changes
                    NpcUtil.saveDialog(dialog);
                }
                else {
                    response = "Not Found dialog-option for slot:" + w.arg(w.ai - 1);
                }
            }

            //remove all DialogOption from current dialog, and dialogs by DialogOption.dialogId form mem and disk
            else if (w.isCmd("clean", "clear")) {
                int counter = 0;
                final int dosz = safe(dialog.options).size();
                if (dosz > 0) {
                    Iterator<Map.Entry<Integer, DialogOption>> iter = dialog.options.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<Integer, DialogOption> e = iter.next();
                        //Integer slot = e.getKey();
                        iter.remove(); //dialog.options.remove(slot);//from variants

                        DialogOption dop = e.getValue();
                        if (dop != null) {
                            //from mem and disk
                            Dialog rDialog = DialogController.instance.dialogs.get(dop.dialogId);
                            if (rDialog != null) {
                                DialogController.instance.removeDialog(rDialog);
                                counter++;
                            }
                        }
                    }
                    //save changes
                    NpcUtil.saveDialog(dialog);
                                                         // slots    actual removed dialogs
                    response = "Cleaned all dialog options["+dosz+"]: " + counter + " in DialogId:" + dialog.id;

                    //if several dialog-option were created recently - check for lastDialogId trim space
                    if (counter > 0 && this.packCreated) {
                        this.packCreated = false;
                        //trim last deleted dialogIds "save dialogId-name-space"
                        NpcUtil.getLastDialogID(true);
                    }
                }
                else {
                    response = "Empty options in dialodId: " + dialog.id;
                }
            }
            //for the ability to edit dialog-options in slots above 5
            else if (w.isCmd("edit", "e")) {
                response = cmdDialogOptionEdit(dialog, w);
            }

            //move DialogOptin to specific slot
            else if (w.isCmd("move", "m")) {
                response = cmdDialogOptionMove(dialog, w);
            }
            else {
                response = "UKNOWN";
            }

        }//options

        //cb dialog #dId script to-file (script-name)
        //save content of dialog.text to script-file to WordName/customnpc/scripts/
        else if (w.isCmd("script")) {
            return cmdDialogScript(sender, w, dialog);
        }
        else response = "UKNOWN: "+ w.arg(w.ai++);


        return response;
    }


    public String cmdDialogStatus(Dialog dialog, ArgsWrapper w) {
        StringBuilder sb = new StringBuilder();
        if (dialog.category != null) {
            sb.append("CategoryId: ").append(dialog.category.id).append(' ').append(dialog.category.title).append('\n');
        }
        sb.append("DialogId: ").append(dialog.id).append(' ').append(dialog.title).append('\n');
        sb.append("Options: ");
        if (dialog.options != null && !dialog.options.isEmpty()) {
            sb.append("\nSlot #DId  #Q :SlotOptionType :SlotTitle\n");
            for (Map.Entry<Integer, DialogOption> entry : dialog.options.entrySet()) {
                Integer slot = entry.getKey();
                DialogOption dop = entry.getValue();

                //slot: 0 - 5 varioans for this dialog
                if (dop != null) {
                    Dialog d = BooksKeeper.getDialog(dop.dialogId);
                    final int qId = d == null ? -8 : d.quest; //-1 no quest; -2 no dialog -error
                    sb.append( String.format("% ,2d  % ,4d  % ,3d [%12s] %s\n",
                            slot, dop.dialogId, qId, dop.optionType, dop.title) );
                } else {
                    sb.append(slot).append(" null\n");//check illigal state
                }

            }
        } else {
            sb.append("Empty");
        }
        return sb.toString();
    }

    /**
     *
     * @param dialog
     * @param w
     * @return
     */
    public String cmdDialogEdit(Dialog dialog, ArgsWrapper w) {
        String response = "?";
        if (w == null || w.isHelpCmdOrNoArgs()) {
            return "<quest (N) / show-wheel (bool) / sound (str) / command (multi args)>";
        }

        boolean mod = false;
        if (w.isCmd("quest", "q")) {
            int l = dialog.quest;
            dialog.quest = w.argI(w.ai++, -1);
            mod = mod || l != dialog.quest;
        }
        if (w.isCmd("show-wheel","sw")) {
            boolean l = dialog.showWheel;
            dialog.showWheel = w.argB(w.ai++);
            mod = mod || l != dialog.showWheel;
        }

        if (w.isCmd("sound", "s")) {
            String l = dialog.sound;
            dialog.sound = w.arg(w.ai++);
            mod = mod || !dialog.sound.equals(l);
        }

        if (w.isCmd("command","c")) {
            String l = dialog.command;
            dialog.command = w.join(w.ai);
            mod = mod || !dialog.command.equals(l);
        }

        if (mod) {
            NpcUtil.saveDialog(dialog);
        }
        response = "#" + dialog.id + " " + dialog.title + " Q:" + dialog.quest+" Options: " + dialog.options.size() + "ShowWheel:"+dialog.showWheel+" Sound:"+dialog.sound;
        return response;
    }



    /**
     * cb dialog #id script lang filename (save|load|delete)
     * @param sender
     * @param w
     * @param dialog
     * @return
     */
    public String cmdDialogScript(ICommandSender sender, ArgsWrapper w, Dialog dialog) {
        String response = "?";

        //check for op permision 'customnpcs.npc.gui' and edit-tool in hand
        if (dialog != null && NpcUtil.canPlayerEditNpc(sender, true, true)) {
            final String usage = "(lang) (scriptfilename) <save/load/delete>";
            if (w.isHelpCmdOrNoArgs()) {
                return usage;
            }

            String lang = w.arg(w.ai++);    // "scala" javascript??
            String filename = w.arg(w.ai++);

            //simple security barrier - do not allow change paths of default script directory
            if (lang.isEmpty() || lang.indexOf('/') >-1 || lang.indexOf('\\') > -1) {
                return "illegal lang name '"+lang+"'";
            }
            if (filename.isEmpty() || filename.indexOf('/') >-1 || filename.indexOf('\\') > -1) {
                return "illegal file name '"+filename+"'";
            }

            if (w.isCmd("save")) {
                if (dialog.text != null && !dialog.text.isEmpty()) {
                    java.io.File scriptDir = new java.io.File(noppes.npcs.CustomNpcs.getWorldSaveDirectory(), "scripts");

                        scriptDir = new java.io.File(scriptDir, lang);
                        if (!scriptDir.exists()) {
                            scriptDir.mkdirs();
                        }
                        if (!filename.isEmpty() && filename.indexOf('/') < 0 && filename.indexOf('\\') < 0) {
                            try {
                                java.io.File file = new java.io.File(scriptDir, filename);
                                org.apache.commons.io.FileUtils.write(file, dialog.text, org.apache.commons.io.Charsets.UTF_8);
                                response = "Saved content of dialogId " + dialog.id + " to " + lang + File.separator + filename + " (for update use 'noppes script reload')";
                            }
                            catch (Exception e) {
                                response = "Failure: " + e.getCause();
                            }
                        } else {
                            response = "Error: illegal scriptname";
                        }

                } else {
                    response = "Error:Empty content in dialogId: " + dialog.id;
                }
            }
            ///cb d 15 script scala Hello.scala load
            else if (w.isCmd("load")) {
                try {
                    java.io.File file = new java.io.File(noppes.npcs.CustomNpcs.getWorldSaveDirectory(), "scripts" + File.separator + lang + File.separator + filename);
                    /*DIALOG*/System.out.println(file);
                    if (file.exists()) {
                        dialog.text = org.apache.commons.io.FileUtils.readFileToString(file, org.apache.commons.io.Charsets.UTF_8);
                        NpcUtil.saveDialog(dialog);
                        response = "Script " + filename + " Loaded to DialogId:" + dialog.id + " length: "+ (dialog.text==null ? -1 : dialog.text.length());
                    } else {
                        response = "Not Found: "+ lang + "/" + filename;
                    }
                }
                catch (Exception e) {
                    response = "Fail:" + e.getMessage();
                }
            }
            else if (w.isCmd("delete")) {
                java.io.File file = new java.io.File(noppes.npcs.CustomNpcs.getWorldSaveDirectory(), "scripts" + File.separator + lang + File.separator + filename);
                response = lang + File.separator + filename;
                if (file.exists()) {
                     response += " Deleted:" + file.delete();
                } else {
                    response += " Not found";
                }
            }
            else {
                response = "UKNOWN " + w.arg(w.ai-1) + " USAGE:"+ usage;
            }
        }
        return response;
    }

    /**
     * Edit Dialog option
     * @param dialog
     * @param w
     * @return
     */
    public String cmdDialogOptionEdit(Dialog dialog, ArgsWrapper w) {
        if (w.isHelpCmdOrNoArgs()) {
            return "(slot) <dialog-id/title/type>";
        }
        String response = "?";
        Integer slot = w.argI(w.ai++, -1);
        boolean mod = false;
        DialogOption dop = dialog.options.get(slot);
        if (dop == null) {
            response = "Not Found dialog-option for slot:" + w.arg(w.ai - 1);
        } else {
            if (w.isCmd("dialog-id", "di")) {
                int last = dop.dialogId;
                dop.dialogId = w.argI(w.ai++, -1);
                mod = last != dop.dialogId;
            }
            else if (w.isCmd("title", "t")) {
                String last = dop.command;
                dop.title = w.join(w.ai);
                mod = !dop.title.equals(last);
            }
            else if (w.isCmd("command", "c")) {
                String last = dop.command;
                dop.command = w.join(w.ai);
                mod = !dop.command.equals(last);
            }
            else if (w.isCmd("type")) {
                if (w.isHelpCmdOrNoArgs()) {
                    //QuitOption, DialogOption, Disabled, RoleOption, CommandBlock
                    StringBuilder sb = new StringBuilder();
                    for (EnumOptionType t : EnumOptionType.values()) {
                        sb.append(t).append(' ');
                    }
                    return sb.toString();
                }
                else {
                    String type = w.arg(w.ai++);
                    dop.optionType = EnumOptionType.valueOf(type);//check
                    mod = true;
                }
            }
            else if (w.isCmd("color")) {
                int last = dop.optionColor;
                dop.optionColor = w.argI(w.ai++);
                mod = last != dop.optionColor;
            }

            StringBuilder sb = new StringBuilder();
            if (mod) {
              sb.append("[Modified] ");
            }
            sb.append("DialogId:").append(dop.dialogId)
              .append(' ').append(dop.optionType)
              .append(" Color: ").append(dop.optionColor)
              .append("\nTitle:\"").append(dop.title).append("\" ")
              .append("\nCommand: '") .append(dop.command).append('\'');
            response = sb.toString();

            //save changes
            NpcUtil.saveDialog(dialog);
        }
        return response;
    }


    /**
     *
     * @param dialog
     * @param w
     * @return
     */
    public String cmdDialogOptionsAddNew (Dialog dialog, ArgsWrapper w) {
        String response = "?";
        if (w==null || w.isHelpCmdOrNoArgs()) {
            response = "<add-new> dialog option title name 1 \\ name 2 \\ name N";
        }
        else {
            int sz = dialog.options.size();
            int slot = sz;
            if (sz < 24) {

                List<String> titles = new ArrayList<String>();
                StringBuilder sb = new StringBuilder();

                // opt1 word1 word2 word3 \ opt2 word \ optN
                while (w.hasArg()) {
                    String word = w.arg(w.ai++);

                    if ("\\".equals(word) && sb.length() > 1) {
                        sb.setLength(sb.length()-1); // remove ' ' in end
                        titles.add(sb.toString());
                        sb.setLength(0);
                    } else {
                        sb.append(word).append(' ');
                    }
                }
                //"last line"
                if (sb.length() > 1) {
                    sb.setLength(sb.length()-1);//remove last char - space
                    titles.add(sb.toString());
                }

                if (!titles.isEmpty()) {
                    //--- auto back to each option
                    DialogOption backOption = null;
                    String backOptionTitle = BooksKeeper.instance().getBackTitle();
                    if (backOptionTitle != null && !backOptionTitle.isEmpty()) {
                        backOption = new DialogOption();
                        backOption.optionType = EnumOptionType.DialogOption;
                        backOption.dialogId = dialog.id;
                        backOption.title = backOptionTitle;// "< Back";
                    }
                    //---
                    boolean mod = false;
                    for (int i = 0; i < titles.size(); i++) {
                        String title = titles.get(i);
                        if (title != null && !title.isEmpty()) {
                            String text = String.valueOf(slot); //blank  dialog.text
                            Dialog odialog = NpcUtil.newDialog(dialog.category.id, "." + slot + " " + title,  text);
                            if (odialog != null) {
                                DialogOption dop = new DialogOption();
                                dop.dialogId = odialog.id;
                                dop.title = title;
                                dop.optionType = EnumOptionType.DialogOption;
                                dialog.options.put(slot++, dop);
                                mod = true;
                                //back
                                if (backOption != null) {
                                    odialog.options.put(99, backOption);// max visible lines by default 30-31
                                }
                            }
                        }
                    }
                    NpcUtil.saveDialog(dialog);
                }

                response = "Done; sz:" + sz + " i: "+slot +" Added new options:"+ (slot-sz);
                //remember this opteration for case then after it will be removed and free dialogsId
                if (!packCreated) {
                    packCreated = slot-sz > 0;
                }

            } else {
                response = "Limitation of DialogOptions count " + sz;
            }
        }
        return response;
    }


    public boolean addBackDialogOptionTo(Dialog dialog, int toDialogId, String backTitle) {
        if (dialog != null && toDialogId > -1) {
            //add only on exists dialogs Id
            if (DialogController.instance.dialogs != null && DialogController.instance.dialogs.containsKey(toDialogId)) {
                //String backTitle = BooksKeeper.instance().getBackTitle();
                if (backTitle != null && !backTitle.isEmpty() && BooksKeeper.getDialog(toDialogId) != null && toDialogId != dialog.id) {
                    DialogOption backOption = new DialogOption();
                    backOption.optionType = EnumOptionType.DialogOption;
                    backOption.dialogId = toDialogId;
                    backOption.title = backTitle;//"Back"
                    dialog.options.put(99, backOption);
                    NpcUtil.saveDialog(dialog);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addQuitDialogOption(Dialog dialog, int slot, String quitTitle) {
        if (dialog != null) {
            //add only on exists dialogs Id
            if (DialogController.instance.dialogs != null &&
                quitTitle != null && !quitTitle.isEmpty() && slot >-1 && !dialog.options.containsKey(slot)) {
                DialogOption backOption = new DialogOption();
                backOption.optionType = EnumOptionType.QuitOption;
                backOption.dialogId = -1;
                backOption.title = quitTitle; //"Quit"
                dialog.options.put(slot, backOption);
                //save
                NpcUtil.saveDialog(dialog);
                return true;
            }
        }
        return false;
    }

    /**
     * Move option from Slot to new Slot if in new Slot has other option
     * other option will be moved to from Slot
     * @param dialog
     * @param w
     * @return
     */
    private String cmdDialogOptionMove(Dialog dialog, ArgsWrapper w) {
        int fromSlot = w.argI(w.ai++);
        int toSlot   = w.argI(w.ai++);
        DialogOption movedOpt = dialog.options.get(fromSlot);
        if (movedOpt == null) {
            return "Not Found Option in slot: " + fromSlot;
        } else {
            DialogOption last = dialog.options.get(toSlot);
            dialog.options.put(toSlot, movedOpt);
            if (last != null) {
                dialog.options.put(fromSlot, last);
            } else {
                dialog.options.remove(fromSlot);
            }
            NpcUtil.saveDialog(dialog);
            return "Moved option from: " + fromSlot + " to: " + toSlot;
        }
    }

    /**
     *
     * @param w
     * @param sender
     * @return
     * cb pd quest
     */
    private String cmdPlayerData(ArgsWrapper w, ICommandSender sender) {
        if (w.isHelpCmdOrNoArgs()) {
            return "(player-name) <status/quest/dialogs>";
        }

        String playerName = w.arg(w.ai++);
        EntityPlayerMP player = getPlayer(sender, playerName); //can throw
        if (player == null) {
            return "not found player " + playerName;
        }
        String response = "?";

        PlayerData data = PlayerDataController.instance.getPlayerData(player);
        if (data == null) {
            return "null data for " + playerName;
        }
        playerName = player.getCommandSenderName();

        //=============
        if (w.noArgs() || w.isCmd("status", "st")) {
            NBTTagCompound nbt = new NBTTagCompound();
            StringBuilder sb = new StringBuilder(" = Status ").append(playerName).append(" =\n");
            data.factionData.saveNBTData(nbt);
            sb.append(nbt.toString());
            sb.append("\nQuest")
              .append(" Finished: ")  .append(data.questData.finishedQuests.size())
              .append(" Active:")     .append(data.questData.finishedQuests.size()).append('\n');
            sb.append("DialogsReaded").append(data.dialogData.dialogsRead.size()).append('\n');
            response = sb.toString();
        }

        //=============
        else if (w.isCmd("quest", "q")) {
            if (w.isHelpCmdOrNoArgs()) {
                return " active | finished [-repeated-only] | is-finished (questId) | catigories";
            }
            PlayerQuestData qd = data.questData;
            if (w.isCmd("catigories", "c")) {
                Iterator<QuestCategory> iter = QuestController.instance.categories.values().iterator();
                StringBuilder sb = new StringBuilder();
                sb.append("#QCatId Title QuestCount\n");
                while(iter.hasNext()) {
                    QuestCategory qc = iter.next();
                    if (qc != null) {
                        sb.append("#").append(qc.id).append(' ').append(qc.title).append(' ').append(qc.quests.size()).append('\n');
                    }
                }
                response = sb.toString();
            }
            //---------
            else if (w.isCmd("is-finished", "if")) {
                int qId = w.argI(w.ai++, -1);
                if (qId > -1) {
                    Long completeTime = qd.finishedQuests.get(qId);
                    Quest q = QuestController.instance.quests.get(qId);
                    if (q != null) {
                        String title = q.title;
                        String time = null;
                        if (completeTime != null) {
                            Instant instant = Instant.ofEpochMilli(completeTime);
                            time = instant.atZone(ZoneId.systemDefault()).format(DT_FORMAT);
                        }
                        response = "QuestId:" + qId +" "+ title + " "+ (completeTime == null ? "Not Finished" : ("Finished at " + time +" "+ completeTime));
                    } else {
                        response = "Not found Quest fo Id:" + qId;
                        if (completeTime != null) {
                            response += "But has CompleteTime:" + completeTime;
                        }
                    }

                }
            }
            //---------
            else if (w.isCmd("active", "a")) {
                if (qd.activeQuests.size() > 0) {
                    Integer catId = w.argI(w.ai++, -1);//filter

                    Iterator<QuestData> iter = qd.activeQuests.values().iterator();
                    StringBuilder sb = new StringBuilder("=Active Quests [").append(playerName).append("]\n");

                    while(iter.hasNext()) {
                        QuestData q = iter.next();
                        if (q != null&& q.quest != null) {
                            if (catId > -1 && catId != q.quest.category.id) { //filter by QuestCateg)
                                continue;
                            }
                            sb.append( String.format("% ,3d [%8s] [%10s] %s\n",
                              q.quest.id, q.quest.type, q.quest.repeat, q.quest.title));
                        }
                        response = sb.toString();
                    }
                } else {
                    response = "No active quest in " + playerName;
                }
            }
            //---------
            else if (w.isCmd("finished", "f")) {
                boolean onlyRepeated = w.hasOpt("-repeated-only", "-r");
                Integer catId = w.argI(w.ai++, -1);//filter

                if (qd.activeQuests.size() > 0) {
                    Iterator<Integer> iter = qd.finishedQuests.keySet().iterator();
                    StringBuilder sb = new StringBuilder("= Finished Quests [").append(playerName).append("]\n");
                    while(iter.hasNext()) {
                        Integer qId = iter.next();
                        Quest quest = QuestController.instance.quests.get(qId);
                        if (quest != null ) {
                            if (catId > -1 && catId != quest.category.id //filter by QuestCat
                                || onlyRepeated && quest.repeat == EnumQuestRepeat.NONE) {
                                continue;
                            }
                            sb.append( String.format("% ,3d [%8s] [%10s] %s\n",
                              quest.id, quest.type, quest.repeat, quest.title));
                        }
                    }
                    response = sb.toString();
                } else {
                    response = "empty";
                }
            }
        }
        //=============
        else if (w.isCmd("dialogs", "d")) {
            if (w.isHelpCmdOrNoArgs()) {
                return "is-read (dialogId) | readed-in-category (catId) | catigories | total-readed";
            }
            PlayerDialogData dd = data.dialogData;
            //---------
            if (w.isCmd("is-read", "ir")) {
                int dId = w.argI(w.ai++, -1);
                if (dId > -1) {
                    boolean read = dd.dialogsRead.contains(dId);
                    Dialog d =  DialogController.instance.dialogs.get(dId);
                    String title = d == null ? "[DialogNotFound]" : d.title;
                    //may be a case when there is no dialog - "deleted" but the player has it marked as read
                    response = "DialogId:" + dId +" "+ title + " DialogId-Readed-By-Player: " + read;
                }
            }
            //---------
            else if (w.isCmd("catigories", "c")) {
                Iterator<DialogCategory> iter = DialogController.instance.categories.values().iterator();
                StringBuilder sb = new StringBuilder();
                sb.append("#DCatId Title DialogCount\n");
                while(iter.hasNext()) {
                    DialogCategory dc = iter.next();
                    if (dc != null) {
                        sb.append("#").append(dc.id).append(' ').append(dc.title).append(' ').append(dc.dialogs.size()).append('\n');
                    }
                }
                response = sb.toString();
            }
            //---------
            //show readed by specified player dialogs in defined category
            else if (w.isCmd("readed-in-category", "ric")) {
                if (w.isHelpCmd()) {
                    return "(category-id)";
                }
                int cId = w.argI(w.ai++, -1);
                DialogCategory dc = DialogController.instance.categories.get(cId);
                if (dc != null) {
                    if (dc.dialogs == null || dc.dialogs.size()==0) {
                        response = "DialogCategory is Empty";
                    } else {
                        Iterator<Dialog> iter = dc.dialogs.values().iterator();
                        StringBuilder sb = new StringBuilder();
                        sb.append("DialogCategory: '").append(dc.title).append("'\n");
                        while(iter.hasNext()) {
                            Dialog d = iter.next();
                            if (d != null) {
                                sb.append("DialogId:").append(d.id).append(' ').append(d.title); //d.availability;
                                if (d.quest >-1) {
                                    sb.append(" QuestId:").append(d.quest);
                                }
                                sb.append('\n');
                            }
                        }
                        response = sb.toString();
                    }
                }
                else {
                    response = "Not found category for id: " + w.arg(w.ai-1);
                }
            }
            //---------
            else if (w.isCmd("total-readed", "tr")) {
                response = "" + dd.dialogsRead.size();
            }
            else {
                response = "UKNOWN " + w.arg(w.ai);
            }
        }
        return response;
    }


}
