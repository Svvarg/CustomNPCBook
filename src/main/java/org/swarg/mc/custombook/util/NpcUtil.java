package org.swarg.mc.custombook.util;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.lang.reflect.Field;
import java.util.Iterator;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.constants.EnumOptionType;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.DialogCategory;
import noppes.npcs.controllers.QuestCategory;
import noppes.npcs.controllers.DialogOption;
import noppes.npcs.controllers.Dialog;
import noppes.npcs.controllers.Quest;

import org.swarg.mc.custombook.BooksKeeper;
import static net.minecraft.util.StringUtils.isNullOrEmpty;
import noppes.npcs.controllers.QuestController;

/**
 * 04-02-21
 * @author Swarg
 */
public class NpcUtil {

    public static boolean isServerSide(Entity e) {
        return e != null && e.worldObj != null && !e.worldObj.isRemote;
    }

    public static void toSender(ICommandSender sender, String response) {
        if (response != null) {
            if (sender instanceof EntityPlayer && response.contains("\n")) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    sender.addChatMessage(new ChatComponentText(line));
                }
            }
            else {
                sender.addChatMessage(new ChatComponentText(response));
            }
        }
    }
    /**
     * Message to all players
     * @param name
     * @param message
     */
    public static void sendGlobalMessage(String name, String message) {
        ServerConfigurationManager csm = MinecraftServer.getServer().getConfigurationManager();
        if (csm != null && message != null) {
            ChatComponentStyle msg;
            if (name == null || name.isEmpty()) {
                try {
                    msg = new ChatComponentTranslation(message);
                }
                catch (Exception e) {
                    msg = new ChatComponentText(message);
                }
            }
            // <name> message
            else {
                StringBuilder sb = new StringBuilder();
                sb.append(EnumChatFormatting.WHITE)
                  .append('<')
                  .append(EnumChatFormatting.GREEN).append(name)
                  .append(EnumChatFormatting.WHITE)
                  .append('>').append(' ')
                  .append(EnumChatFormatting.GOLD)
                  .append(message);
                msg = new ChatComponentText(sb.toString());
            }
            csm.sendChatMsg(msg);
        }
    }

    public static boolean isOp (EntityPlayer p) {
        return p != null && MinecraftServer.getServer().getConfigurationManager().func_152596_g( p.getGameProfile());
    }


    //------------------------------------------------------------------------\\
    //                      Custom NPC tools
    //------------------------------------------------------------------------\\

    public static Dialog newDialog(int categoryId, String title, String text) {
        if (DialogController.instance != null) {
            Dialog dialog = new Dialog();
            dialog.category = DialogController.instance.categories.get(categoryId);
            dialog.title = title;
            dialog.text = text;
            return DialogController.instance.saveDialog(categoryId, dialog);
        }
        return null;
    }

    public static boolean saveDialog(Dialog dialog) {
        if (dialog != null && dialog.category != null) {
            DialogController.instance.saveDialog(dialog.category.id, dialog);
            return true;
        }
        return false;
    }


    public static int getLastDialogID(Boolean trim) {
        if (DialogController.instance != null) {
            try {
                Field field = DialogController.class.getDeclaredField("lastUsedDialogID");
                if (field != null) {
                    field.setAccessible(true);
                    int lastUsedDialogID = field.getInt(DialogController.instance);

                    /*to save space for dialog file names when frequently
                    recreating dialogs and deleting them
                    TODO check old dialog for reading by players*/
                    if (trim && lastUsedDialogID > 0 && DialogController.instance.dialogs != null) {
                        int max = 0;
                        for(Integer id : DialogController.instance.dialogs.keySet()) {
                            if (id > max) {
                                max = id;
                            }
                        }
                        if (max < lastUsedDialogID) {
                            lastUsedDialogID = max;
                            field.setInt(DialogController.instance, lastUsedDialogID);
                        }
                    }
                    return lastUsedDialogID;
                }
            }
            catch (Exception e) {
            }
        }
        return -1;//error
    }

    /**
     * Trim - для случаев когда создаются квесты по командам и квест создался неправильно
     * для того чтобы можно было экономно использовать идишники квестов (иначе
     * после удаления квеста идишник будет не использован) Но это только для горячих
     * исправлений. Иначе может полуится так что идишник будет забит у негоко игрока
     * (AutoTrim автоматически происходит при рестарте сервера и CustomNPC)
     * @param trim
     * @return
     */
    public static int getLastQuestID(Boolean trim) {
        if (QuestController.instance != null) {
            try {
                Field field = QuestController.class.getDeclaredField("lastUsedQuestID");
                if (field != null) {
                    field.setAccessible(true);
                    int lastUsedQuestID = field.getInt(QuestController.instance);

                    if (trim && lastUsedQuestID > 0 && QuestController.instance.quests != null) {
                        int max = 0;
                        for(Integer id : QuestController.instance.quests.keySet()) {
                            if (id > max) {
                                max = id;
                            }
                        }
                        if (max < lastUsedQuestID) {
                            lastUsedQuestID = max;
                            field.setInt(QuestController.instance, lastUsedQuestID);
                        }
                    }
                    return lastUsedQuestID;
                }
            }
            catch (Exception e) {
            }
        }
        return -1;//error
    }

    /**
     * Guard to endit CustomNPC
     * Only for op-player has permission customnpcs.npc.gui 
     * @param sender
     * @param needTool Only with held custom-npc-edit-tool
     * @param verbose
     * @return
     */
    public static boolean canPlayerEditNpc(ICommandSender sender, boolean needTool, boolean verbose) {
        if (sender instanceof EntityPlayerMP ) {
            EntityPlayerMP player = (EntityPlayerMP)sender;

            //guard to endit
            if (isOp(player) && CustomNpcsPermissions.hasPermissionString(player, "customnpcs.npc.gui")) {
                if (needTool) {
                    ItemStack stack = player.getHeldItem();
                    Item it = stack == null ? null : stack.getItem();
                    boolean hasEditTool =  it == CustomItems.wand || it == CustomItems.scripter || it == CustomItems.cloner || it == CustomItems.moving;
                    if (!hasEditTool && verbose) {
                        sender.addChatMessage(new ChatComponentText("Take the tool in your hands"));
                    }
                    return hasEditTool;
                } else {
                    return true;
                }
            }
            // no permision
            else if (verbose) {
                ChatComponentTranslation noPermissionMsg = new ChatComponentTranslation("commands.generic.permission");
                noPermissionMsg.getChatStyle().setColor(EnumChatFormatting.RED);
                sender.addChatMessage(noPermissionMsg);
            }
        }
        return false;
    }

    public static StringBuilder appendDialog(StringBuilder sb, Dialog dialog) {
        if (sb != null && dialog != null) {
            sb.append("#").append(dialog.id).append(" '").append(dialog.title).append("' options: ").append(safe(dialog.options).size());
        }
        return sb;
    }

    public static int getCategoryId(Dialog dialog) {
        return dialog == null || dialog.category == null ? -1 : dialog.category.id;
    }
    
    public static int getCategoryId(Quest quest) {
        return quest == null || quest.category == null ? -1 : quest.category.id;
    }

    public static StringBuilder appendCategory(StringBuilder sb, DialogCategory cat) {
        if (sb != null && cat != null) {
            sb.append("#").append(cat.id).append(" '").append(cat.title).append("' size: ").append(safe(cat.dialogs).size());
        }
        return sb;
    }
    
    public static StringBuilder appendCategory(StringBuilder sb, QuestCategory cat) {
        if (sb != null && cat != null) {
            sb.append("#").append(cat.id).append(" '").append(cat.title).append("' size: ").append(safe(cat.quests).size());
        }
        return sb;
    }

    public static String getHexColor(int color) {
        StringBuilder sb = new StringBuilder(6);
        sb.append(Integer.toHexString(color));
        int rem = 6 - sb.length();
        while (rem > 0) {
            sb.insert(0, '0');
            rem--;
        }
        return sb.toString();
    }

    public static int getColorFromHexStr(String hex, int def) {
        if (!isNullOrEmpty(hex)) {
            try {
                return Integer.parseInt(hex, 16);
            }
            catch (Exception e) {
            }
        }
        return def;
    }

    public static boolean addDialogOptionSwitchTo(Dialog dialog, int slot, int toDialogId, String title) {
        if (dialog != null && toDialogId > -1 && slot > -1) {
            //add only on exists dialogs Id
            if (DialogController.instance.dialogs != null && DialogController.instance.dialogs.containsKey(toDialogId)) {
                if (!isNullOrEmpty(title) && BooksKeeper.getDialog(toDialogId) != null && toDialogId != dialog.id) {
                    DialogOption backOption = new DialogOption();
                    backOption.optionType = EnumOptionType.DialogOption;
                    backOption.dialogId = toDialogId;
                    backOption.title = title;
                    dialog.options.put(slot, backOption);
                    NpcUtil.saveDialog(dialog);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean addQuitDialogOption(Dialog dialog, int slot, String quitTitle) {
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

    //------------------------------------------------------------------------\\

    public static Map safe(Map map) {
        return (map == null) ? Collections.EMPTY_MAP : map;
    }

    //------------------------------------------------------------------------\\
    //                      MC ItemStack NBT tools
    //------------------------------------------------------------------------\\

    public static boolean hasDisplayTag(ItemStack is) {
        return (is != null && is.stackTagCompound != null && is.stackTagCompound.hasKey("display", 10));
    }

    /**
     * Get Exist Display Tag or Create New if Need
     * @param is
     * @param canCreateNewIfNotExist
     * @return
     */
    public static NBTTagCompound getDisplayTag(ItemStack is, boolean canCreateNewIfNotExist) {
        if (is != null) {
            if (!is.hasTagCompound()) {
                if (!canCreateNewIfNotExist) {//can not create new tag
                    return null;
                }
                is.stackTagCompound = new NBTTagCompound();
            }
            if (is.stackTagCompound != null) {
                NBTTagCompound nbtDisplay = null;
                if (is.stackTagCompound.hasKey("display", 10)) {
                    nbtDisplay = is.stackTagCompound.getCompoundTag("display");
                    if (nbtDisplay != null) {
                        return nbtDisplay;
                    }
                }

                //create new nbt
                if (canCreateNewIfNotExist) {
                    is.stackTagCompound.setTag("display", nbtDisplay = new NBTTagCompound());
                    return nbtDisplay;
                }
            }
        }
        return null;
    }

    public static NBTTagList getLoreTag(NBTTagCompound nbt) {
        if ( nbt != null && nbt.hasKey("display")) {
            NBTTagCompound nbtDisplay = nbt.getCompoundTag("display");
            if (nbtDisplay != null) {
                return nbtDisplay.getTagList("Lore", 8);
            }
        }
        return null;
    }
    
    /**
     * Temporary waitfor mcCoreLib dependency
     * Look up entity only in same chunk!
     * @param player
     * @param clazz
     * @return
     */
    public static Entity getFirsNearestEntity(EntityPlayer player, Class clazz) {
        if (player != null && player.worldObj != null) {
            try {
                //ChunkProviderServer cps = ((WorldServer)player.worldObj).theChunkProviderServer;
                IChunkProvider cp = player.worldObj.getChunkProvider();

                if (cp != null && cp.chunkExists(player.chunkCoordX, player.chunkCoordZ)) {
                    Chunk chunk = cp.provideChunk(player.chunkCoordX, player.chunkCoordZ);
                    final int k = player.chunkCoordY;
                    if (chunk != null && k > -1 && k < chunk.entityLists.length) {
                        List list = chunk.entityLists[k];

                        for (int i = 0; i < list.size(); i++) {
                            Object e = list.get(i);
                            ///*DEBUG*/System.out.println(e);
                            if (e != null && clazz.isAssignableFrom(e.getClass())) {
                                return (Entity) e;
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
            }
        }
        return null;
    }

    //alib
    public static String exceptionTraceAsString(Throwable cause) {
        if (cause != null) {
            java.io.StringWriter sw = new java.io.StringWriter();
            cause.printStackTrace(new java.io.PrintWriter(sw));
            return sw.toString();
        } else {
            return null;
        }
    }


    public static int getFirstFreeDialogOptionIndex(Dialog d, int max) {
        if (d != null && d.options != null) {
            final int sz = d.options.size();
            if (sz == 0) {
                return 0;
            } else {
                //Iterator<Integer> iter = d.options.keySet().iterator();
                //int remain = 0;
                //int last = -1;
                for (int i = 0; i < max; i++) {
                    if (!d.options.containsKey(i)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
    /**
     * '1.2 DialogTitle'  Prefix is '1.2'
     * @param dialog
     * @return
     */
    public static String getDialotTitlePrefix(Dialog dialog) {
        if (dialog != null && !isNullOrEmpty(dialog.title)) {
            final int s = dialog.title.indexOf(' ');
            if (s > 0) {
                return dialog.title.substring(0, s);
            }
        }
        return "";
    }
}
