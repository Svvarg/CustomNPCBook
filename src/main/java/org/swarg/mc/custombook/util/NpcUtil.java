package org.swarg.mc.custombook.util;

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.lang.reflect.Field;

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
import noppes.npcs.controllers.DialogOption;
import noppes.npcs.controllers.Dialog;

import org.swarg.mc.custombook.BooksKeeper;
import static net.minecraft.util.StringUtils.isNullOrEmpty;

/**
 * 04-02-21
 * @author Swarg
 */
public class NpcUtil {
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

    public static boolean addBackDialogOptionTo(Dialog dialog, int slot, int toDialogId, String title) {
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

}
