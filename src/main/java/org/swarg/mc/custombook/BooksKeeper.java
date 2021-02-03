package org.swarg.mc.custombook;

import java.util.Map;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import noppes.npcs.Server;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerDialogData;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.DialogCategory;
import noppes.npcs.controllers.DialogOption;
import noppes.npcs.controllers.Dialog;


/**
 * 01-02-21
 * @author Swarg
 */
public class BooksKeeper {

    /*Define prefix of the Titles of DialogCategories that will be associated
    with meta of the books */
    private static final String BOOK_DIALOG_CATEGORY_NAME = "BOOK_";

    private static BooksKeeper instance;

    /*Dummy npc for open dialog associated with books*/
    private final EntityDialogNpc bookKeeper;
    /*StartMain Pages of Books - Dialogs(FirstPage in book) associated with
      book metadata( itemDamage ) */
    private final Map<Integer, Dialog> metaToDialog = new HashMap<Integer, Dialog>();


    public static BooksKeeper instance() {
        if (instance == null) {
            instance = new BooksKeeper();
        }
        return instance;
    }

    public BooksKeeper() {
        bookKeeper = new EntityDialogNpc(null);
        bookKeeper.display.name = "";
    }


    /**
     * Open LifeBook GUI (CustomNPC Dialog for book meta of ItemStack)
     * @param player
     * @param book
     */
    public void openBookDialog(EntityPlayer player, ItemStack book ) {
        if (player != null && player.worldObj != null && !player.worldObj.isRemote && book != null) {
            final int meta = book.getItemDamage();

            bookKeeper.worldObj = player.worldObj;//++ null safe; not impact to work

            Dialog dialog = getStartDialogForBook(meta);
            //OpenDialog in Player Client
            if (dialog != null && PlayerDataController.instance != null && DialogController.instance != null) {
                //Dialog dialog = (Dialog) DialogController.instance.dialogs.get( this.dialogIds[meta] );
                
                debugOpMsg(player, "SendData Meta: %s DIALOG Title: '%s' id: %s ", meta, dialog.title, dialog.id);

                ///*Full:*/ NoppesUtilServer.openDialog(player, keeper, dialog);

                //Eco:
                Server.sendData((EntityPlayerMP)player, EnumPacketClient.DIALOG_DUMMY, new Object[]{ bookKeeper.getCommandSenderName(), dialog.writeToNBT(new NBTTagCompound())});
                //Server.sendData((EntityPlayerMP)player, EnumPacketClient.DIALOG, new Object[]{ Integer.valueOf(keeper.getEntityId()), dialog.writeToNBT(new NBTTagCompound()) });
                // without this package, the dialog options don't open for some reason
                //NoppesUtilServer.setEditingNpc(player, bookKeeper);
            }
            else {
                //itembook meta corresponds DialogCategoryIndex in exists dialogs BOOK_<Slot>
                player.addChatMessage(new ChatComponentText("Locked"));
                debugOpMsg(player, "No DialogId for meta: " + meta);
            }            
        }
    }

    /**
     * Search start-main-page-dialog for book by item:meta(Damage)
     * @param meta
     * @return
     */
    public Dialog getStartDialogForBook(int meta) {
        boolean DEBUG = 0==0;
        Integer key = Integer.valueOf(meta);
        Dialog dialog = metaToDialog.get(key);

        /*if the dialog was already searched for, it was not found - the
          value will be null. In this case, do not search again */
        if (dialog == null && !metaToDialog.containsKey(key) &&
            DialogController.instance != null && !DialogController.instance.categories.isEmpty())
        {

            HashMap<Integer, DialogCategory> dca = DialogController.instance.categories;

            for (DialogCategory dc : dca.values()) {
                if (dc != null) {

                    //Category Title "BOOK_N" where N corresponds meta in BookItem //(slot of dialog in npc)
                    if (dc.title != null && dc.title.startsWith(BOOK_DIALOG_CATEGORY_NAME)) {
                        final int bookN = getSlotFromCategoryTitle(dc.title);
                        if (bookN == meta) {
                            //Autosearch first dialog in category "BOOK_META" "main page of book"
                            Integer dId = getLowestDialogId(dc.dialogs);
                            if (dId != null ) {
                                dialog = dc.dialogs.get(dId);
                                this.metaToDialog.put(meta, dialog);

                                if (dialog != null) {
                                    /*DEBUG*/if (DEBUG) { System.out.println("Found Dialog for meta: " + meta + " dialogId:" + dialog.id);}
                                    //DialogOption dOption = new DialogOption();dOption.dialogId = dialog.id;dOption.title = dialog.title;bookKeeper.dialogs.put( mi, dOption);
                                } else {
                                    /*DEBUG*/if (DEBUG) {System.out.println("Dialog for meta "+meta+" is null!");}
                                }
                                return dialog;
                            }
                            else {
                                final int sz = dc.dialogs == null ? 0 : dc.dialogs.size();
                                /*DEBUG*/if (DEBUG) {System.out.println("Not found lowestDialogId in variants counts: " + sz + " for meta: "+meta);}
                            }

                        }
                    }
                }
            }

            //mark as not exist. 'use command custombook reload' if added new dialogs and categories
            //for search only once
            this.metaToDialog.put(meta, null);
        }

        return dialog;
    }


    /**
     * Dialog with Min id in category considers as main book page
     * For empty dialogs return null
     * @param dialogs
     * @return
     */
    public Integer getLowestDialogId(Map<Integer, Dialog> dialogs) {
        Integer min = null;
        if (dialogs != null && !dialogs.isEmpty()) {
            for (Integer id : dialogs.keySet()) {
                if (min == null || id != null && id < min) {
                    min = id;
                }
            }
        }
        return min;
    }

    /**
     * BOOK_1 -> 1
     * book meta corresponde index at this this.dialogIds
     * @param title
     * @return
     */
    public int getSlotFromCategoryTitle(String title) {
        if (title != null) {

            final int sz = title.length();
            final int prefSz = BOOK_DIALOG_CATEGORY_NAME.length();
            if (sz == prefSz) {//"BOOK_"
                return 0;
            } 
            // "BOOK_1"
            else if (sz > prefSz) {
                try {
                    String num = title.substring(prefSz);
                    ///*DEBUG*/System.out.println("num:" + num);
                    return Integer.parseInt(num);
                }
                catch (Exception e) {
                }
            }
        }
        return -1;
    }

    public void reload () {
        this.metaToDialog.clear();//todo
    }


    public String status() {
        StringBuilder sb = new StringBuilder();
        sb.append("BookKeeperDummyNPC:").append( this.bookKeeper == null ? '-' : '+').append(' ');
        int kbsz = this.bookKeeper.dialogs==null ? 0 : this.bookKeeper.dialogs.size();
        sb.append("DialogsInKeeper: ").append( kbsz );

        //final int sz = this.dialogIds.length;
        final int sz = this.metaToDialog.size();
        if (sz > 0) {
            sb.append('\n');
            //m - book meta in BookKeeper is DialogSlot
            for (Map.Entry<Integer, Dialog> entry : metaToDialog.entrySet()) {
                Integer meta = entry.getKey();
                Dialog dialog = entry.getValue();

                sb.append("Meta:").append(meta);//corresponds bookitem meta and DialogCategory Name "BOOK_0"

                if (dialog != null) {
                    sb.append(" DialogId:").append(dialog.id).append(' ');
                    if (dialog != null) {
                        if (dialog.category != null) {
                            sb.append(dialog.category.title);
                        }
                        sb.append('.').append(dialog.title);
                    } else {
                        sb.append("[NOT_FOUND]");
                    }
                } else {
                    sb.append(" [Empty]");
                }
                sb.append('\n');
            }
        }

        //mapping occurs dynamically when a book is used
        else {
            sb.append(" MetaToDialogMap isEmpty");
        }
        return sb.toString();
    }


    //                          --- UTILS ---
    
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
    
    public static void debugOpMsg(EntityPlayer p, String msg, Object...args) {
        if (isOp(p) && msg != null) {
            try {
                final String line = args == null || args.length == 0 ? msg : String.format(msg, args);
                p.addChatMessage(new ChatComponentText( EnumChatFormatting.GOLD + "[DEBUG] "+ line));
            } catch (Exception e) {
            }
        }
    }

}

