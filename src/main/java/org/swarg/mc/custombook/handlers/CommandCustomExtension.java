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
import net.minecraft.util.ChatComponentText;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;

import noppes.npcs.Server;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumQuestType;
import noppes.npcs.constants.EnumOptionType;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumAvailabilityQuest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.controllers.Availability;
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
import noppes.npcs.quests.QuestItem;

import org.swarg.cmds.CmdUtil;
import org.swarg.cmds.ArgsWrapper;
import org.swarg.mc.custombook.BooksKeeper;
import org.swarg.mc.custombook.util.NpcUtil;
import static org.swarg.mc.custombook.util.NpcUtil.safe;
import static net.minecraft.util.StringUtils.isNullOrEmpty;
import static org.swarg.mc.custombook.handlers.CommandCustomBooks.QUESTTAG;


/**
 * Additional commands for CustomNPCs mod
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

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        final String s = args == null || args.length < 2 ? "" : args[args.length - 2];
        final String[] pname = {"player-data", "pd", "player-stat", "ps"};
        if (CmdUtil.IndexOfArgEquals(pname, s) > 0) {
            return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        }
        return Collections.emptyList();
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
        return "<category/dialog/quest/script/player-data/player-stat/run>";
    }


    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String response = null;

        ArgsWrapper w = new ArgsWrapper(args);

        if (w.isHelpCmdOrNoArgs()) {
            response = getCommandUsage(sender);
        }

        //commands only for op-player
        else if (NpcUtil.canPlayerEditNpc(sender, false, true) || sender instanceof MinecraftServer) {

            if (w.isCmd("category", "c")) {//dialogCategory
                response = cmdCategory(w, sender);
            }
            else if (w.isCmd("dialog", "d")) {
                response = cmdDialog(w, sender);
            }
            else if (w.isCmd("quest", "q")) {
                response = cmdQuest(w, sender);
            }
            else if (w.isCmd("script", "s")) {
                response = CommandCustomScript.instance().cmdScript(w, sender);
            }
            else if (w.isCmd("player-data", "pd")) { //custom npc data (dialog & quests)
                response = cmdPlayerData(w, sender);
            }
            else if (w.isCmd("player-stat", "ps")) {//achievement
                response = cmdPlayerStat(w, sender);
            }
            //[DEBUG] Execute console-command via CustomNPCs
            else if (w.isCmd("run")) {//achievement
                if (sender instanceof EntityPlayer && NpcUtil.isOp((EntityPlayer)sender)) {
                    EntityPlayer player = ((EntityPlayer)sender);
                    String command = w.join(w.ai);
                    NoppesUtilServer.runCommand(player, "Manual", command, player);
                    response = "Done";
                } else {
                    response = "Only for op-player";
                }
            }
            else {
                response = "UKNOWN";
            }
        }

        NpcUtil.toSender(sender, response);
    }

    //========================================================================\\
    //                    CUSTOM-NPC DialogCategories
    //========================================================================\\
    //DialogCategories
    private String cmdCategory(ArgsWrapper w, ICommandSender sender) {
        final String usage = "<info/list>";
        if (w.isHelpCmdOrNoArgs()) {
            return usage;
        }
        else if (w.isCmd("info", "i")) {
            int cid = w.argI(w.ai++);
            DialogCategory cat = DialogController.instance.categories.get(cid);
            return cat != null
                    ? NpcUtil.appendCategory(new StringBuilder(), cat).toString()
                    : "Not Found";
        }
        else if (w.isCmd("list", "l")) {
            HashMap<Integer, DialogCategory> cats = DialogController.instance.categories;
            StringBuilder sb = new StringBuilder("--- DialogCategories ---\n");
            for (DialogCategory cat: cats.values()) {
                NpcUtil.appendCategory(sb, cat).append('\n');
            }
            return sb.toString();
        }
        else {
            return usage;
        }
    }

    //========================================================================\\
    //                       CUSTOM-NPC Dialogs                               \\
    //========================================================================\\

    /**
     *
     * @param w
     * @param sender
     * @return
     */
    private String cmdDialog(ArgsWrapper w, ICommandSender sender) {
        final String usage = "dialog (id) <status/edit/text/gui/options/script> | dialog <last-dialog-id [-trim] / set-back-title (title)>";
        if (w.isHelpCmdOrNoArgs()) {
            return usage;
        }
        String response = "?";

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
                if (!isNullOrEmpty(response)) {
                    BooksKeeper.instance().setBackTitle(response);
                }
            }
        }

        int dialogId = w.argI(w.ai++, -1);
        Dialog dialog = (Dialog) DialogController.instance.dialogs.get( dialogId );
        if (dialog == null) {
            return "Not Found Dialog for Id: " + dialogId;
        }

        //cx dialog new-options op0 op2 op3 .. op5
        if (w.noArgs() || w.isCmd("status", "st")) {
            response = cmdDialogStatus(dialog, w);
        }
        //cx dialog #id edit
        else if (w.isCmd("edit", "e")) {
            response = cmdDialogEdit(dialog, w);
        }

        else if (w.isCmd("text", "t")) {
            response = dialog.text;
        }

        //show dialog in gui
        else if (w.isCmd("gui", "g") && sender instanceof EntityPlayerMP) {
            sendOpenDialogGui(sender, dialog);
        }

        //DialogOptions - possible responses to the current dialog
        // cx dialog #id options add dialog option0-title o1-title o2-title... n
        else if (w.isCmd("options", "o")) {
            response = cmdDialogOption(w, sender, dialog);
        }

        //cx dialog #dId script to-file (script-name)
        //save content of dialog.text to script-file to WordName/customnpc/scripts/
        else if (w.isCmd("script")) {
            return cmdDialogScript(sender, w, dialog);
        }
        else
            response = usage;//"UKNOWN: "+ w.arg(w.ai++);

        return response;
    }

    //todo how Send Packet to OpenSpecified Dialog?
    //now opened only DialogCategory
    public static boolean sendOpenDialogGui(ICommandSender sender, Dialog dialog) {
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
            //response = "Gui Opened";
            return true;
        } else {
            sender.addChatMessage(new ChatComponentText("You need to stand in the same chunk with any CustomNpc"));
        }
        return false;
    }


    public String cmdDialogStatus(Dialog dialog, ArgsWrapper w) {
        StringBuilder sb = new StringBuilder();
        if (dialog.category != null) {
            sb.append("CategoryId: ").append(dialog.category.id).append(" '").append(dialog.category.title).append("'").append(" size:").append(safe(dialog.category.dialogs).size()).append('\n');
        }
        sb.append("DialogId: ").append(dialog.id).append(" '").append(dialog.title).append("'\n");
        final int optSz = dialog.options == null ? 0 : dialog.options.size();
        sb.append("Options[").append( optSz ).append("]: ");
        if (optSz > 0) {
            sb.append("\nSlot #DId  #Q SltOptionType  Color  SlotTitle\n");
            for (Map.Entry<Integer, DialogOption> entry : dialog.options.entrySet()) {
                Integer slot = entry.getKey();
                DialogOption dop = entry.getValue();

                //slot: 0 - 5 varioans for this dialog
                if (dop != null) {
                    Dialog d = BooksKeeper.getDialog(dop.dialogId);
                    final int qId = d == null ? -8 : d.quest; //-1 no quest; -2 no dialog -error
                    //todo color
                    sb.append( String.format("% ,2d  % ,4d  % ,3d [%12s] %06X %s\n",
                            slot, dop.dialogId, qId, dop.optionType, dop.optionColor, dop.title) );
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

        response = "#" + dialog.id + " '" + dialog.title + "' Q:" + dialog.quest + " Options: " + dialog.options.size() +
                " ShowWheel:" + dialog.showWheel + " Sound:'" + dialog.sound + "' CategoryId:" + NpcUtil.getCategoryId(dialog);
        return response;
    }



    /**
     * cx dialog #id script lang filename (save|load|delete)
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

            String lang = w.arg(w.ai++);    // "scala", "ecmascript"
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
            ///cx d 15 script scala Hello.scala load
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
     * Edit DialogOptions - variants for choise
     * @param w
     * @param sender
     * @param dialog
     * @return
     */
    public String cmdDialogOption(ArgsWrapper w, ICommandSender sender, Dialog dialog) {
        String response = "?";
        if (w.isHelpCmdOrNoArgs()) {
            return "<add/remove/clean/view/edit/move>";
        }
        if (dialog.options == null) {
            dialog.options = new HashMap();
        }
        //add add new options-dialogs to (Map)dialog.options of current dialog
        // use \ for separate title names
        if (w.isCmd("add", "a")) {
            response = cmdDialogOptionAdd(w, dialog, sender);
        }//add
        //---------
        //remove dialog option from current dialog.options and delete the dialog-option from disk
        else if (w.isCmd("remove", "rm")) {
            response = cmdDialogOptionRemove(w, dialog);
        }

        //remove all DialogOption from current dialog, and dialogs by DialogOption.dialogId form mem and disk
        else if (w.isCmd("clean", "clear")) {
            response = cmdDialogOptionClean(dialog, w);
        }
        //for the ability to edit dialog-options in slots above 5
        else if (w.isCmd("view", "v")) {
            final int slot = w.argI(w.ai++, -1);
            DialogOption dop = dialog.options.get(slot);
            response = dop != null 
                    ? viewDialogOption(new StringBuilder(), slot, dop, true).toString()
                    : "Not Found dialog-option for slot:" + slot;
        }
        //for the ability to edit dialog-options in slots above 5
        else if (w.isCmd("edit", "e")) {
            response = cmdDialogOptionEdit(dialog, w);
        }
        //move DialogOptin to specific slot (swap places)
        else if (w.isCmd("move", "m")) {
            response = cmdDialogOptionMove(dialog, w);
        }
        else {
            response = "UKNOWN";
        }
        return response;
    }

    private String cmdDialogOptionRemove(ArgsWrapper w, Dialog dialog) {
        if (dialog == null) {
            return "no dialog";
        }
        final String usage = "[WARN] Remove DialogOption from dialog and DialogEntry corresponding DialogOption.dialogIds form mem and disk. For confirm input:\n(OptSlot) (DialogId)";
        //
        int removedCount = 0;
        StringBuilder sb = new StringBuilder();

        while (w.hasArg()) { // optSlot ConfirmDialogId optSlot ConfirmDID ... N
            //remove one specific dialog-option from dialog.options by his slotIndex
            Integer slot = w.argI(w.ai++, -1);
            int confirmDialogId = w.argI(w.ai++, -1);
            DialogOption roDialog = dialog.options.get(slot);
            if (roDialog != null) {
                //Need confirm
                if (confirmDialogId != roDialog.dialogId) {
                    return usage;
                    //если это не первый слот и были удаленные - то не произойдет сохранение диалога!
                }
                Dialog rDialog = DialogController.instance.dialogs.get(roDialog.dialogId);
                if (rDialog != null) {
                    //remove DialogEntry from maps(mem) and disk
                    DialogController.instance.removeDialog(rDialog); //todo deep? - options inside this dialog
                }
                //case then no dialog but has dialog-option slot
                DialogOption removed = dialog.options.remove(slot);
                sb.append("Slot:").append(slot);
                if (removed == null) {
                    sb.append(" Empty");
                } else {
                    sb.append(" - Cleaned. Removed Dialog: ");
                    NpcUtil.appendDialog(sb, rDialog);
                    removedCount++;
                }
                sb.append('\n');
            }
            else {
                sb.append("Not Found dialog-option for slot: ").append(w.arg(w.ai - 1)).append("\n"); //-2?
            }
        }

        if (removedCount > 0) {
            //save changes
            NpcUtil.saveDialog(dialog);
            sb.append("Removed Options: ").append(removedCount).append(" from DialogId: #").append(dialog.id).append(' ').append(dialog.title);
        }

        return sb.toString();
    }


    public String cmdDialogOptionClean(Dialog dialog, ArgsWrapper w) {
        final String usage = "[WARN] Remove all DialogOptions from dialog and all dialog entries corresponding DialogOption.dialogIds form mem and disk. For confirm input:\n(DialogOptionCount)";
        if (w.isHelpCmd()) {
             return usage;
        }
        int toRemove = w.argI(w.ai++, 0);
        String response;
        int counter = 0;
        final int dosz = safe(dialog.options).size();
        if (dosz > 0) {
            //onfirm
            if (toRemove != dosz) {
                return usage;
            }
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
        return response;
    }


    /**
     * cx dialog # option add (action)
     * @param w
     * @param dialog
     * @param sender
     * @return
     */
    private String cmdDialogOptionAdd(ArgsWrapper w, Dialog dialog, ICommandSender sender) {
        final String usageAdd = "<dialog/switch/back/quit/command>";
        if (w.isHelpCmdOrNoArgs()) {
            return usageAdd;
        }
        String response;
        //new one ot more option-dialogs with ceating new sub dialog mapped to dialog-option
        if (w.isCmd("dialog", "d")) {//dialog-option with dialogEntry
            response = cmdDialogOptionsAddNew(dialog, w);
        }
        //switching to another exists DialogId
        else if (w.isCmd("switch", "s")) {
            if (w.isHelpCmdOrNoArgs()) {
                response = "(#slot) (#toDialogId) [title]";
            } else {
                int slot = w.argI(w.ai++, -1);
                int toDialogId = w.argI(w.ai++, -1);//jump to this dialogId
                String jumpTitle = w.hasArg() ? w.join(w.ai) : "Next";
                boolean flag = NpcUtil.addDialogOptionSwitchTo(dialog, slot, toDialogId, jumpTitle);
                response = "jump Added: "+flag+" from DialogId: " + dialog.id + " Slot:" + slot + " to DialogId:" + toDialogId;
            }
        }
        // cx d #id add-back #toDialogId
        //add back from current dialog to specified dialogId
        else if (w.isCmd("back", "b")) {
            if (w.isHelpCmdOrNoArgs()) {
                response = "(#toDialogId) [BackTitle]";
            } else {
                //slot will be #99
                int toDialogId = w.argI(w.ai++, -1);//"back"-move to this dialog
                String backTitle = w.hasArg() ? w.join(w.ai) : BooksKeeper.instance().getBackTitle();
                boolean flag = NpcUtil.addDialogOptionSwitchTo(dialog, 99, toDialogId, backTitle);
                response = "Back Added: " + flag + " to DialogId:" + toDialogId +" from DialogId: " + dialog.id;
            }
        }
        //---------
        else if (w.isCmd("quit", "q")) {
            if (w.isHelpCmdOrNoArgs()) {
                response = "(#slot) [QuitTitle]";
            } else {
                int slot = w.argI(w.ai++, -1);
                String quitTitle = w.hasArg() ? w.join(w.ai) : "Quit";
                boolean flag = NpcUtil.addQuitDialogOption(dialog, slot, quitTitle);
                response = "Quit Added: " + flag + " to DialogId: " + dialog.id + " to DialogOptionSlot: " + slot;
            }
        }
        //---------
        //add option-dialog with specified command
        else if (w.isCmd("command", "c")) {
            final String UsageAC = "(#slot) (title) (command)";
            if (w.isHelpCmdOrNoArgs()) {
                response = UsageAC;
            } else {
                int slot = w.argI(w.ai++, -1);
                String title = w.arg(w.ai++);
                String command = w.join(w.ai);
                if (slot > -1 && !isNullOrEmpty(title) && !isNullOrEmpty(command)) {
                    boolean flag = addCommandDialogOption(sender, dialog, slot, title, command);
                    response = "Command Added: " + flag + " to DialogId: " + dialog.id + " to DialogOptionSlot: " + slot;
                } else {
                    response = UsageAC;
                }
            }
        } else {
            response = usageAdd;
        }
        return response;
    }

    public boolean addCommandDialogOption(ICommandSender sender, Dialog dialog, int slot, String title, String command) {
        //checking access
        if (NpcUtil.canPlayerEditNpc(sender, false, true) && dialog != null) {
            //add only on exists dialogs Id
            if (DialogController.instance.dialogs != null && title != null && !title.isEmpty() && slot >-1) {
                if (dialog.options.containsKey(slot)) {
                    // do not replace exists dialog-option slot
                    sender.addChatMessage(new ChatComponentText("Slot: " + slot + "Already Exists"));
                } else {
                    DialogOption backOption = new DialogOption();
                    backOption.optionType = EnumOptionType.CommandBlock;
                    backOption.dialogId = -1;
                    backOption.title = title;
                    dialog.options.put(slot, backOption);
                    //save
                    NpcUtil.saveDialog(dialog);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Edit Dialog option
     * @param dialog
     * @param w
     * @return
     * cx dialog #d options edit #slot (action) [params]
     */
    public String cmdDialogOptionEdit(Dialog dialog, ArgsWrapper w) {
        final String USAGE = "(slot) <title/dialog-id/type/command/color>";
        if (w.isHelpCmdOrNoArgs()) {
            return USAGE;
        }
        String response;
        Integer slot = w.argI(w.ai++, -1);
        boolean mod = false;
        DialogOption dop = dialog.options.get(slot);
        if (dop == null) {
            response = "Not Found dialog-option for slot:" + w.arg(w.ai - 1);
        } else {
            if (w.isHelpCmd()) {
                return USAGE;
            }
            else if (w.isCmd("dialog-id", "di")) {
                int last = dop.dialogId;
                dop.dialogId = w.argI(w.ai++, -1);
                mod = last != dop.dialogId;
            }
            else if (w.isCmd("title", "tt")) {
                String last = dop.command;
                dop.title = w.join(w.ai);
                mod = !dop.title.equals(last);
            }
            else if (w.isCmd("command", "cmd")) {
                String last = dop.command;
                dop.command = w.join(w.ai);
                mod = !dop.command.equals(last);
            }
            else if (w.isCmd("color", "cl")) {
                int last = dop.optionColor;
                dop.optionColor = NpcUtil.getColorFromHexStr(w.arg(w.ai++), dop.optionColor);
                //todo from hex
                mod = last != dop.optionColor;
            }
            else if (w.isCmd("type", "tp")) {
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
            } else {
                return "UKNOWN "+ w.arg(w.ai-1);
            }

            StringBuilder sb = new StringBuilder();
            if (mod) {
                sb.append("[Modified] ");
                //save changes
                NpcUtil.saveDialog(dialog);
            }
            viewDialogOption(sb, slot, dop, true);
            response = sb.toString();
        }
        return response;
    }

    public StringBuilder viewDialogOption(StringBuilder sb, int slot, DialogOption dop, boolean newLineForCommand) {
        sb.append(" Slot: ").append(slot)
          //indicates the transition of this option to another dialog
          .append(" [").append(dop.optionType).append(']');
        if (dop.optionType == EnumOptionType.DialogOption) {
            sb.append(" DialogId: ").append(dop.dialogId);
        }
        sb.append(" Color: #").append(NpcUtil.getHexColor(dop.optionColor))
          .append(" Title: '").append(dop.title).append("' ");
        if (!isNullOrEmpty(dop.command)) {
            sb.append( newLineForCommand ? '\n' : ' ')
              .append("Command: '") .append(dop.command).append('\'');
        }
        return sb;
    }
    /**
     * Create multiple dialogs with empty content but with the specified titles
     * and add them all as response options to the given dialog
     * @param dialog
     * @param w
     * @return
     */
    public String cmdDialogOptionsAddNew(Dialog dialog, ArgsWrapper w) {
        String response = "?";
        if (w==null || w.isHelpCmdOrNoArgs()) {
            response = "<add-new> (dialog option title name 1) \\ (name 2) \\ (name N)";
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
                            String text = String.valueOf( slot ); //blank  dialog.text
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
                    if (mod) {
                        NpcUtil.saveDialog(dialog);
                    }
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

    //========================================================================\\
    //                       CUSTOM-NPC Quests
    //========================================================================\\

    private String cmdQuest(ArgsWrapper w, ICommandSender sender) {
        final String usage = "quest (id) <status/edit/text> | quest <categories/new/last-quest-id>";
        if (w.isHelpCmdOrNoArgs()) {
            return usage;
        }
        if (w.isCmd("categories", "c")) {
            return cmdQuestCategories(w, sender);
        }
        else if (w.isCmd("new", "n")) {
            return cmdQuestNew(sender, w);
        }
        else if (w.isCmd("last-quest-id", "lqi")) {
            boolean trim = w.hasOpt("-trim");
            return "" + NpcUtil.getLastQuestID(trim);
        }


        int questId = w.argI(w.ai++, -1);
        Quest quest = (Quest) QuestController.instance.quests.get( questId );
        if (quest == null) {
            return "Not Found Dialog for Id: " + questId;
        }

        //cx dialog new-options op0 op2 op3 .. op5
        if (w.noArgs() || w.isCmd("status", "st")) { //view
            return cmdQuestStatus(quest, w);
        }
        //cx dialog #id edit
        else if (w.isCmd("edit", "e")) {
            return cmdQuestEdit(quest, w);
        }
        else if (w.isCmd("text", "t")) {
            final String usageText = "<log/complite>";
            if (w.isHelpCmdOrNoArgs()) {
                return usageText;
            } 
            //текстовое описание квеста
            else if (w.isCmd("log","l")) {
                return quest.logText;
            } 
            //текст завершения
            else if (w.isCmd("complete","c")) {
                return quest.completeText;
            }
            return quest.logText;
        }
        else return usage;
    }


    private String cmdQuestCategories(ArgsWrapper w, ICommandSender sender) {
        final String usage = "<info/list/>";
        String response = usage;
        if (w.isHelpCmdOrNoArgs()) {
            return usage;
        }
        else if (w.isCmd("info", "i")) {
            int cid = w.argI(w.ai++);
            boolean verbose = w.hasOpt("-verbose", "-v");
            QuestCategory cat = QuestController.instance.categories.get(cid);
            if (cat == null) {
                response = "Not Found";
            } else {
                StringBuilder sb = new StringBuilder();
                NpcUtil.appendCategory(sb, cat);
                
                if (verbose) {
                    sb.append('\n');
                    final int sz = safe(cat.quests).size();
                    if (sz > 0) {
                        for(Quest q : cat.quests.values()) {
                            sb.append(" Q#").append(q.id).append(' ').append(q.type).append(' ').append(q.title).append('\n');
                        }
                    }
                }
                response = sb.toString();
            }

        }
        else if (w.isCmd("list", "l")) {
            HashMap<Integer, QuestCategory> cats = QuestController.instance.categories;
            StringBuilder sb = new StringBuilder("--- DialogCategories ---\n");
            for (QuestCategory cat: cats.values()) {
                NpcUtil.appendCategory(sb, cat).append('\n');
            }
            return sb.toString();
        }
        return response;
    }

    private String cmdQuestStatus(Quest quest, ArgsWrapper w) {
        StringBuilder sb = new StringBuilder();
        if (quest.category != null) {
            sb.append("CategoryId: ").append(quest.category.id).append(" '").append(quest.category.title).append("'").append(" size:").append(safe(quest.category.quests).size()).append('\n');
        }
        sb.append("QuestId: ").append(quest.id).append(" '").append(quest.title).append("'\n");
        if (quest.nextQuestid > -1) {
            sb.append("NextQuestId: ").append(quest.nextQuestid).append(" '").append(quest.nextQuestTitle).append("'\n");
        }
        sb.append("Type: ").append(quest.type).append(' ');
        if (quest.questInterface != null) {
            sb.append(quest.questInterface.getClass().getSimpleName()).append('\n');
            if (quest.questInterface instanceof QuestItem) {
                appendQuestItem(sb, (QuestItem) quest.questInterface);
            }
        } else {
            sb.append('\n');
        }

        if (quest.completion == EnumQuestCompletion.Npc) {
            sb.append("CompleterNpc: '").append(quest.completerNpc).append("'\n");
        } else {
            sb.append("Completion: Instant (WithoutNPC)\n");
        }
        if (!isNullOrEmpty(quest.command)) {
            sb.append("Command: '").append(quest.command).append("'\n");
        }
        sb.append("Repeat: ").append( quest.repeat ).append('\n');
        sb.append("RewardExp: ").append( quest.rewardExp ).append('\n');
        if (quest.rewardItems != null && safe(quest.rewardItems.items).size() > 0) {
            sb.append("Rewards: ");
            for (ItemStack stack: quest.rewardItems.items.values()) {
                if (stack != null && stack.getItem() != null) {
                    sb.append(stack.toString()).append(';');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static StringBuilder appendQuestItem(StringBuilder sb, QuestItem qi) {
        if (sb == null) {
            return sb;
        }
        if (qi.items != null && safe(qi.items.items).size() > 0) {
            /*GuiNpcQuestTypeItem: quest.takeitems  gui.yes gui.no  leaveItems ? 1 : 0   QuestItem.handleComplete()*/
            sb.append(" leaveItems: ")  .append(qi.leaveItems).append('\n'); // on false the quest takes items defined in quest slot   // the quest leaves then item with the player 
            sb.append(" IgnoreDamage: ").append(qi.ignoreDamage).append('\n');
            sb.append(" IgnoreNBT: ")   .append(qi.ignoreNBT).append('\n');
            sb.append(" Items:\n");
            for (ItemStack stack : qi.items.items.values()) {
                //return this.stackSize + "x" + this.field_151002_e.getUnlocalizedName() + "@" + this.itemDamage;
                if (stack.getItem() != null) {
                    //.getgetUnlocalizedName()
                    sb.append("  id #").append(Item.getIdFromItem(stack.getItem())).append(':').append(stack.getItemDamage()).append(" (").append(stack.getUnlocalizedName()).append(')');
                    if (stack.stackSize > 1) {
                        sb.append(' ').append('x').append(stack.stackSize);
                    }

                    if (stack.hasTagCompound()) {
                        sb.append(' ');
                        final int rootNbtTags = stack.stackTagCompound.func_150296_c().size();
                        if (rootNbtTags > 1) {
                            sb.append("[NBT:").append(rootNbtTags).append(']');
                        }
                        boolean hasQuestTag = stack.stackTagCompound.hasKey(QUESTTAG);
                        if (hasQuestTag) {
                            sb.append("[QuestTag]");//unique
                        }
                        if (stack.stackTagCompound.hasKey(QuestCraftHandler.QUESTBOOK_CRAFT_ITEM)) {
                            sb.append("[QUESTBOOK_CRAFT_ITEM]:")
                              .append(stack.stackTagCompound.getTag(QuestCraftHandler.QUESTBOOK_CRAFT_ITEM));
                        }
                    }
                    sb.append('\n');
                }
            }
        } else {
            sb.append(" Empty");
        }
        return sb;
    }


    //cx quest new
    private String cmdQuestNew(ICommandSender sender, ArgsWrapper w) {
        final String usage = "<craft-item>";
        if (w.isHelpCmdOrNoArgs()) {
            return usage;
        } 
        else if (w.isCmd("craft-item", "ci")) {
            if (w.isHelpCmdOrNoArgs()) {
                //return "(QCategoryId) [-quest-tag] []";
                return "(QCategoryId) [-items N] [-reward-exp N] [-to-dialog|-next-for-quest (ID)]  (quest title)";//добавляет предмет из 0го слота оператора если задано число то дабавит от 0 до заданного чиста из инвентаря оператора
                //QCategoryId - в какую категорию добавлять квест (по сути определяет папку-категориии-квестов на диске)
                //-dialog - диалог содержащий список квестов для выбора
                //-parent-quest - идишник родительского квеста продолжением которого станет текущий
            }
            int catId = w.argI(w.ai++, -1);
            if (catId == -1) {
                return "(QuestCategoryId)";
            }
            QuestCategory qc = null;
            if ((qc = QuestController.instance.categories.get(catId)) == null) {
                return "Not Exists QuestCategory: " + catId;
            }
            int additems = (int) w.optValueLongOrDef(1, "-items", "-i");
            final int rewardExp = (int) w.optValueLongOrDef(10, "-reward-exp", "-re");
            final int dialogId = (int) w.optValueLongOrDef(-1, "-to-dialog", "-td", "-d");
            final int parentQuestId = (int) w.optValueLongOrDef(-1, "-next-for-quest", "-nfq", "-q");
            //пропускаю опции для корректного названия квеста/ иначе они попадают в названия
            for (int i = w.argsCount() - 1; i > 0; i--) {
                String s = w.arg(i);
                if (s != null && s.startsWith("-")) {
                    w.ai = i + 2;
                    break;
                }
            }// /cx q n ci 1 -i 4 -re 88 -d 6 Лезвие топора
            Dialog dialog = null; //диалог содержащий список квестов на выполнение
            Quest parentQuest = null; //базовый квест подолжением которого будет текущий создаваемый
            //либо в диалог - либо продолжением другого квеста
            if (dialogId > 0) {
                dialog = (Dialog) DialogController.instance.dialogs.get( dialogId );
                if (dialog == null) {
                    return "Not Found dialogId " + dialogId;
                }
            } 
            //создаваемый квест добавить продолжением родительскому квесту
            else if (parentQuestId > 0) {
                parentQuest = QuestController.instance.quests.get(parentQuestId);
            }

            EntityPlayer p = sender instanceof EntityPlayer ? (EntityPlayer)sender : null;
            if (p == null) {
                return "only for op players"; //todo возможность задать предмет по описанию из консоли?
            }
            int current = p.inventory.currentItem;
            //такое ограничения для исключения ошибок при автосоздании квеста через команду
            //т.к. первая добавляемая вещь будет браться из 0го слота
            //и если указано то до count слота в инвентаре оператора
            if (current != 0) {
                return "activate first slot in inv-quickbar";
            }
            ItemStack stack = p.inventory.getStackInSlot(current);
            if (stack == null || stack.getItem() == null) {
                return "no item in 0 slot of inventory";
            }
            //boolean uQuestTag = w.hasOpt("-quest-tag", "-qt") && w.ai++ > 0;
            //todo к указанному диалогу в который добавить вариант на данный квест (dialogOption) -> Сам диалог описания квеста с выбором брать или нет ->
            //rewardExp
            if (qc != null) {
                Quest q = new Quest();
                q.title = w.join(w.ai);//название квеста
                q.rewardExp = rewardExp;
                if (isNullOrEmpty(q.title)) {
                    q.title = "Craft " + stack.getUnlocalizedName();
                }
                q.type = EnumQuestType.Item;
                //заготовка текста квеста
                q.logText = "Craft item  id #" + Item.getIdFromItem(stack.getItem()) + (stack.getHasSubtypes() ? ":"+stack.getItemDamage() : "");
                q.completeText = "Done";
                q.completion = EnumQuestCompletion.Instant;//without npc
                QuestItem qi = new QuestItem();
                ItemStack qs = stack.copy();
                //этот так обязателен он страхует от автовыполнения квеста при наличии предметов в инвентаре а не в момент создания
                QuestCraftHandler.setQuestBookCraftItemTag(qs, QuestCraftHandler.ANY_OF_ITEM);
                //if (uQuestTag) {setQuestTag(qs, 0);}
                qi.items.items.put(0, qs);
                qi.leaveItems = true; //leave quest item with player

                if (additems > 0) {
                    additems = Math.min(additems, 16);//p.inventory.getSizeInventory()
                    for (int i = 1; i < additems; i++) {
                        qs = p.inventory.getStackInSlot(i);
                        if (qs == null) {
                            return "CANCELED: no item in slot: " + i;
                        }
                        //спец-таг настройки квеста навешивается только на первый требуемый предмет остальные как есть
                        qi.items.items.put(i, qs.copy());
                    }
                }
                q.questInterface = qi;
                //обновление для того чтобы в хуке onItemCraft была проверка данного игрока при крафте вещей
                //промазал это команда по завершению квеста
                //q.command = "noppescooldown craft-item-quest set @dp true";
                QuestController.instance.saveQuest(catId, q);
                StringBuilder sb = new StringBuilder();

                if (parentQuest != null && parentQuest.category != null) {
                    //добавить созданный квест продолжением другого указанного(родительского)
                    sb.append("[Next-for-Quest] #").append(parentQuest.id).append(' ').append(parentQuest.title).append('\n');
                    parentQuest.nextQuestid = q.id;
                    parentQuest.nextQuestTitle = q.title;
                    QuestController.instance.saveQuest(parentQuest.category.id, parentQuest);
                }
                //добавить для вновь созданного квеста диалог его взятия и опцию в родительском диалоге для выбора данного диалога
                else if (dialog != null && dialog.category != null) {
                    sb.append("[Dialog] ");
                    int freeOptSlot = NpcUtil.getFirstFreeDialogOptionIndex(dialog, 16);
                    if (freeOptSlot < 0) {
                        sb.append("Not found Free Slot in dialogId:").append(dialogId).append(" DialogOfQuest not added\n");
                    }
                    else {
                        final String odTitle = NpcUtil.getDialotTitlePrefix(dialog) + "." + freeOptSlot + "-Craft " + q.title;
                        Dialog odialog = NpcUtil.newDialog(dialog.category.id, odTitle,  q.logText);
                        if (odialog != null) {
                            DialogOption dop = new DialogOption();
                            dop.dialogId = odialog.id;
                            dop.title = q.title;
                            dop.optionType = EnumOptionType.DialogOption;
                            dialog.options.put(freeOptSlot, dop);
                            //Диалог выбора квеста
                            sb.append("Added QuestSelectionDialog(#").append(odialog.id).append(") to ParentDialog #")
                                    .append(dialog.id).append(" '").append(dialog.title)
                                    .append("' in OptSlot:").append(freeOptSlot).append('\n');
                            odialog.quest = q.id;
                            /*Для обновления хука на крафты для данного игрока чтобы во время последующих крафтов его проверяло на крафт-квест*/
                            odialog.command = "noppescooldown craft-item-quest set @dp true";//временное решение для снятие нагрузки на хук onIntemCraft в QuestCraftHandler
                            //Чтобы диалог был доступен только 1 раз до взятия квеста.
                            odialog.availability = new Availability();
                            odialog.availability.questId = q.id;
                            odialog.availability.questAvailable = EnumAvailabilityQuest.Before;
                            
                            NpcUtil.saveDialog(dialog);//parent
                            NpcUtil.saveDialog(odialog);//child
                        }
                    }
                }
                sb.append("Added new CraftItemQuest #").append(q.id).append(" '").append(q.title).append("' to Category #").append(qc.id).append(" [").append(qc.title).append("]  ReqItemsVariants: ").append(qi.items.items.size()) ;//+(uQuestTag?"[UniqueQuestTag]":"")
                return sb.toString();
            }
        }
        return usage;
    }

    
    private String cmdQuestEdit(Quest quest, ArgsWrapper w) {
        return "TODO";
    }

    //========================================================================\\
    //                       CUSTOM-NPC PlayerData
    //========================================================================\\

    /**
     *
     * @param w
     * @param sender
     * @return
     * cx pd quest
     */
    private String cmdPlayerData(ArgsWrapper w, ICommandSender sender) {
        final String usage = "(player-name) <help/status/quest/dialogs>";
        if (w.isHelpCmdOrNoArgs()) {
            return usage;
        }

        String playerName = w.arg(w.ai++);
        PlayerData data = null;
        EntityPlayerMP player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(playerName);//getPlayer(sender, playerName); //can throw
        if (player == null) {
            data = PlayerDataController.instance.getDataFromUsername(playerName);
            if (data == null) {
                return "Not Found Data of Offline Player " + playerName;
            }
        } else {
            playerName = player.getCommandSenderName();//страховка
        }
        String response = "?";

        if (data == null && player != null) {
            //вять данные из онлайн игрока
            data = PlayerDataController.instance.getPlayerData(player);
            if (data == null) {
                return "Not Found Data of Online Player " + playerName;
            }
        }
        

        //=============
        if (w.noArgs() || w.isCmd("status", "st")) {
            NBTTagCompound nbt = new NBTTagCompound();
            StringBuilder sb = new StringBuilder(" = Status ").append(playerName).append(" =\n");
            data.factionData.saveNBTData(nbt);
            sb.append(nbt.toString());
            sb.append("\nQuest")
              .append(" Active: ")      .append(data.questData.activeQuests.size())
              .append(" Finished: ")    .append(data.questData.finishedQuests.size()).append('\n')
              .append("DialogsReaded: ").append(data.dialogData.dialogsRead.size()).append('\n');
            response = sb.toString();
        }
        else if (w.isHelpCmd()) {
            response = usage;
        }
        //=============
        else if (w.isCmd("quest", "q")) {
            response = cmdPlayerDataQuest(w, sender, data);
        }
        //=============
        else if (w.isCmd("dialogs", "d")) {
            response = cmdPlayerDataDialog(w, sender, data);
        }
        return response;
    }

    /**
     *
     * @param w
     * @param sender
     * @param data
     * @return
     */
    public String cmdPlayerDataQuest(ArgsWrapper w, ICommandSender sender, PlayerData data) {
        String response = "?";
        if (w.isHelpCmdOrNoArgs()) {
            return "categories | active [catId] | finished [catId] [-repeated-only] | is-finished (questId) | remove qId";
            //catId - filter
        }
        PlayerQuestData qd = data.questData;
        String playerName = data.playername;

        //show short infos about all quests categories,  more in 'cx quest categories' cmd
        if (w.isCmd("categories", "c")) {
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
                        if (q.repeat == EnumQuestRepeat.RLDAILY || q.repeat == EnumQuestRepeat.RLWEEKLY) {
                            Instant instant = Instant.ofEpochMilli(completeTime);
                            time = instant.atZone(ZoneId.systemDefault()).format(DT_FORMAT);
                        } else {
                            time = "TotalWordTime: " + completeTime; //ticks?
                        }
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
            Integer catId = w.argI(w.ai++, -1);//filter
            if (safe(qd.activeQuests).size() > 0) {
                StringBuilder sb = new StringBuilder("=Active Quests [").append(playerName).append("] =\n");
                if (catId > -1) {
                    QuestCategory qc = QuestController.instance.categories.get(catId);
                    if (qc == null) {
                        return "Not Exists QuestCategoryId: " + catId;
                    } else {
                        sb.append("Only for QuestCategory: ").append(qc.title).append(" [").append( catId ).append("]\n");
                    }
                }

                Iterator<QuestData> iter = qd.activeQuests.values().iterator();
                while(iter.hasNext()) {
                    QuestData q = iter.next();
                    if (q != null && q.quest != null) {
                        if (catId > -1 && catId != q.quest.category.id) { //filter by QuestCateg)
                            continue;
                        }
                        appendQuestLine(sb, q.quest).append('\n');//qId type Repeat Categ:QuestTitle
                    }
                }
                response = sb.toString();
            } else {
                response = "["+playerName+"] No Active Quests";
            }
        }
        //---------
        //quest.repeat (EnumQuestRepeat.RLDAILY || EnumQuestRepeat.RLWEEKLY) -> systemcurrentMillis   Else   player.worldObj.getTotalWorldTime()
        else if (w.isCmd("finished", "f")) {
            boolean onlyRepeated = w.hasOpt("-repeated-only", "-r");
            Integer catId = w.argI(w.ai++, -1);//filter
            //todo show world time then quest will be complited
            if (safe(qd.finishedQuests).size() > 0) {
                StringBuilder sb = new StringBuilder("= Finished Quests [").append(playerName).append("] =\n");
                if (catId > -1) {
                    QuestCategory qc = QuestController.instance.categories.get(catId);
                    if (qc == null) {
                        return "Not Exists QuestCategoryId: " + catId;
                    } else {
                        sb.append("Only for QuestCategory: ").append(qc.title).append(" [").append( catId ).append("]\n");
                    }
                }

                Iterator<Integer> iter = qd.finishedQuests.keySet().iterator();
                while (iter.hasNext()) {
                    Integer qId = iter.next();
                    Quest quest = QuestController.instance.quests.get(qId);
                    if (quest != null ) {
                        if (catId > -1 && catId != quest.category.id //filter by QuestCat
                            || onlyRepeated && quest.repeat == EnumQuestRepeat.NONE) {
                            continue;
                        }
                        appendQuestLine(sb, quest).append('\n');//qId type Repeat Categ:QuestTitle
                    }
                }
                response = sb.toString();
            } else {
                response = "["+playerName+"] No Finished Quests";
            }
        }
        //---------
        //remove quest autoseach in active and finished player quests
        else if (w.isCmd("remove", "rm")) {
            if (w.isHelpCmdOrNoArgs()) {
                return "(#QuestId2Remove)";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(playerName).append("]\n");
            int removedQuests = 0;

            while (w.hasArg()) {
                Integer qId = w.argI(w.ai++, -1);//QuestId to remove
                boolean removed = false;
                if (qId > -1) {
                    if (safe(qd.activeQuests).size() > 0) {
                        removed = (qd.activeQuests.remove(qId) != null);
                    }
                    if (safe(qd.finishedQuests).size() > 0) {
                        removed = (qd.finishedQuests.remove(qId) != null) || removed;
                    }

                    if (removed) {
                        removedQuests++;
                        sb.append(" Removed: ");
                        Quest quest = QuestController.instance.quests.get(qId);
                        if (quest != null) {
                            appendQuestLine(sb, quest).toString();
                        } else {
                            sb.append("#").append(qId).append(" QuestId");
                        }
                        //noppes.npcs.controllers.PlayerDataController.instance.savePlayerData(data);
                    }
                    else {
                        sb.append('#').append(qId).append(" QuestId").append(" [Not Found]");
                    }
                }
                else {
                    sb.append("illegal QuestId: ").append(w.arg(w.ai - 1));
                }
                sb.append('\n');
            }
            //save to disk
            if (removedQuests > 0) {
                noppes.npcs.controllers.PlayerDataController.instance.savePlayerData(data);
            }
            response = sb.toString();
        }

        return response;
    }


    public StringBuilder appendQuestLine(StringBuilder sb, Quest quest) {
        if (sb != null) {
            if (quest != null) {
                String categTitle = quest.category == null ? "" : quest.category.title;
                                      //  qId type Repeat CategTitle:QuestTitle
                sb.append( String.format("% ,3d [%8s] [%10s] %s:%s",
                        quest.id, quest.type, quest.repeat,  categTitle, quest.title));
            } else {
                sb.append("null");
            }
        }
        return sb;
    }


    public String cmdPlayerDataDialog(ArgsWrapper w, ICommandSender sender, PlayerData data) {
        String response = "?";
        if (w.isHelpCmdOrNoArgs()) {
            return "is-read (dialogId) | readed-in-category (catId) | categories | total-readed";
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
        else if (w.isCmd("categories", "c")) {
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
        return response;
    }

    //========================================================================\\
    //                       MC Player Statistics                             \\
    //========================================================================\\

    /**
     * For the ability to view the frequency of use of the book
     * Player Achievement | Stat
     * @param w
     * @param sender
     * @return
     * cx player-stat
     */
    private String cmdPlayerStat(ArgsWrapper w, ICommandSender sender) {
        if (w.isHelpCmdOrNoArgs()) {
            return "(player-name) (statId|achievementId)";
        }
        String playerName = w.arg(w.ai++);
        EntityPlayerMP player = getPlayer(sender, playerName); //can throw
        if (player == null) {
            return "Not Found Player " + playerName;
        }
        StatBase stat = StatList.func_151177_a(w.arg(w.ai++,"achievement.OpenGuideBook"));
        String response;
        if (stat != null) {
            boolean achiev = stat.isAchievement();
            response = (achiev ? "[Ach] " : "[Stat] ") + stat.statId + " Count: " + player.func_147099_x().writeStat(stat);
        } else {
            response = "Not Found Stat " + w.arg(w.ai-1);
        }
        return response;
    }


}
