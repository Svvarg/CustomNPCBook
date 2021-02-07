package org.swarg.mc.custombook;

import java.util.Map;
import java.util.HashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ChatComponentText;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import noppes.npcs.NoppesUtilServer;

import noppes.npcs.Server;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.DialogCategory;
import noppes.npcs.controllers.Dialog;

import org.swarg.mc.custombook.util.NpcUtil;


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
    //private final Map<Integer, Dialog> metaToDialog = new HashMap<Integer, Dialog>();
                    // meta    dialogId
    private final Map<Integer, Integer> metaToDialogId = new HashMap<Integer, Integer>();
    private String backTitle = "Back"; //for autogenerate for command custombook dialog #id option add-new option name one \ option two \ the best option
    public boolean debug;


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
                dialog.hideNPC = true;
                
                debugOpMsg(player, "SendData Meta: %s DIALOG id: %s Title: '%s'", meta, dialog.id, dialog.title);

                ///*Full:*/ NoppesUtilServer.openDialog(player, keeper, dialog);

                //Eco:
                Server.sendData((EntityPlayerMP)player, EnumPacketClient.DIALOG_DUMMY, new Object[]{ bookKeeper.getCommandSenderName(), dialog.writeToNBT(new NBTTagCompound())});
                //Server.sendData((EntityPlayerMP)player, EnumPacketClient.DIALOG, new Object[]{ Integer.valueOf(keeper.getEntityId()), dialog.writeToNBT(new NBTTagCompound()) });
                //without this, will not be able to activate the child dialog-options
                NoppesUtilServer.setEditingNpc(player, bookKeeper);

                if (meta == 0) { //!player.func_147099_x().hasAchievementUnlocked()
                    //counter of guide book usage
                    player.addStat(CustomBooksAchievements.aOpenGuideBook, 1);
                }
            }
            else {
                //itembook meta corresponds DialogCategoryIndex in exists dialogs BOOK_<Slot>
                player.addChatMessage(new ChatComponentTranslation("commands.CustomNPCBooks.locked"));//Sealed
                debugOpMsg(player, "No DialogId for meta: " + meta);
            }
        }
    }

    public Dialog getDialogForMeta(Integer meta) {
        //Integer key = Integer.valueOf(meta);
        Integer dialogId = metaToDialogId.get(meta);
        return (dialogId != null ) ? getDialog(dialogId) : null;
    }

    public static Dialog getDialog(Integer dialogId) {
        return DialogController.instance != null && DialogController.instance.dialogs != null
                ? DialogController.instance.dialogs.get(dialogId)
                : null;
    }

    /**
     * Search start-main-page-dialog for book by item:meta(Damage)
     * @param bookMeta
     * @return
     */
    public Dialog getStartDialogForBook(int bookMeta) {
        final Integer meta = Integer.valueOf(bookMeta);
        Dialog dialog = getDialogForMeta(meta);//metaToDialog.get(key);

        /*if the dialog was already searched for, it was not found - the
          value will be null. In this case, do not search again */
        if (dialog == null && !metaToDialogId.containsKey(meta) &&
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

                                if (dialog != null) {
                                    /*DEBUG*/if (debug) { System.out.println("Found Dialog for meta: " + meta + " dialogId:" + dialog.id);}
                                    //DialogOption dOption = new DialogOption();dOption.dialogId = dialog.id;dOption.title = dialog.title;bookKeeper.dialogs.put( mi, dOption);
                                    this.metaToDialogId.put(meta, dialog.id);
                                } else {
                                    /*DEBUG*/if (debug) {System.out.println("Dialog for meta "+meta+" is null!");}
                                    this.metaToDialogId.put(meta, null);
                                }

                                return dialog;
                            }
                            else {
                                final int sz = dc.dialogs == null ? 0 : dc.dialogs.size();
                                /*DEBUG*/if (debug) {System.out.println("Not found lowestDialogId in variants counts: " + sz + " for meta: "+meta);}
                            }

                        }
                    }
                }
            }

            //mark alredy searched but not exist. Use command 'custombook reload' if added new dialogs and categories
            //for search only once
            this.metaToDialogId.put(meta, null);
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
        this.metaToDialogId.clear();//todo
    }


    public String status() {
        StringBuilder sb = new StringBuilder();
        //sb.append("BookKeeperDummyNPC:").append( this.bookKeeper == null ? '-' : '+').append(' ');
        //int kbsz = this.bookKeeper.dialogs == null ? 0 : this.bookKeeper.dialogs.size();
        //sb.append("DialogsInKeeper: ").append( kbsz );
        if (debug) {
            sb.append("[DebugMode]");
        }

        //final int sz = this.dialogIds.length;
        final int sz = this.metaToDialogId.size();
        if (sz > 0) {
            sb.append('\n');
            for (Map.Entry<Integer, Integer> entry : metaToDialogId.entrySet()) {
                Integer meta = entry.getKey();                
                Integer dialogId = entry.getValue();//Dialog dialog = entry.getValue();

                sb.append("Meta:").append(meta);//corresponds bookitem meta and DialogCategory Name "BOOK_0"

                if (dialogId != null) {
                    sb.append(" DialogId:").append(dialogId).append(' ');
                    Dialog dialog = getDialog(dialogId);
                    if (dialog != null) {
                        if (dialog.category != null) {
                            sb.append(dialog.category.title);
                        }
                        sb.append('.').append(dialog.title);
                        if (dialogId != dialog.id) {
                            sb.append("[DeSync! Dialog.Id:").append(dialog.id).append(']');//check
                        }
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

    //used in autoadd back from dialogoption
    public String getBackTitle() {
        return backTitle;
    }

    public void setBackTitle(String backTitle) {
        this.backTitle = backTitle;
    }



    //                          --- UTILS ---
    
    
    public void debugOpMsg(EntityPlayer player, String msg, Object...args) {
        if (debug && NpcUtil.isOp(player) && msg != null) {
            try {
                final String line = args == null || args.length == 0 ? msg : String.format(msg, args);
                player.addChatMessage(new ChatComponentText( EnumChatFormatting.GOLD + "[DEBUG] "+ line));
            } catch (Exception e) {
            }
        }
    }

}

