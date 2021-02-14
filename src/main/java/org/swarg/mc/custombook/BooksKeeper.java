package org.swarg.mc.custombook;

import java.util.Map;
import java.util.HashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ChatComponentText;
import net.minecraft.nbt.NBTTagCompound;

import noppes.npcs.Server;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityDialogNpc;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.DialogCategory;
import noppes.npcs.controllers.Dialog;

import org.apache.logging.log4j.Level;

import org.swarg.mc.custombook.util.NpcUtil;
import static org.swarg.mc.custombook.CustomNPCBook.LOG;
import static org.swarg.mc.custombook.util.NpcUtil.isServerSide;


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
    public boolean debug = false;


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
     * Send Package to Player for Open Custom Book
     * Open Dialog GUI (CustomNPC Dialog for book by specific meta of ItemStack)
     * @param player
     * @param book
     */
    public void openBookDialog(EntityPlayer player, ItemStack book) {
        if (book != null && isServerSide(player)) {
            final int meta = book.getItemDamage();

            bookKeeper.worldObj = player.worldObj;//++ null safe; not impact to work

            Dialog dialog = getStartDialogForBook(meta);
            //OpenDialog in Player Client
            if (dialog != null && PlayerDataController.instance != null && DialogController.instance != null) {
                if (dialog.category == null) {
                    logD("No Category in DialogId: {} [CANCELED]", dialog.id);
                    return;
                }
                dialog.hideNPC = true;
                
                logD("Meta: {} DialogId: {} CategoryId: {} [SendData:DIALOG_DUMMY]", meta, dialog.id, dialog.category.id);

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
                logD("No DialogId for meta: {}", meta);//debugOpMsg(player, "No DialogId for meta: " + meta);
            }
        }
    }

    public Dialog getMappedDialogForMeta(Integer meta) {
        Integer dialogId = metaToDialogId.get(meta);
        /*DEBUG*/if (debug) { LOG.log(Level.INFO, "[getDialogForMeta] Meta: {} > DialogId: {}", meta, dialogId);}
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
        Dialog dialog = getMappedDialogForMeta(meta);//metaToDialog.get(key);

        /*if the dialog was already searched for, it was not found - the
          value will be null. In this case, do not search again */
        if (dialog == null && !metaToDialogId.containsKey(meta) &&
            DialogController.instance != null && !DialogController.instance.categories.isEmpty())
        {
            HashMap<Integer, DialogCategory> categories = DialogController.instance.categories;

            for (DialogCategory dcat : categories.values()) {
                if (dcat != null) {

                    //Category Title "BOOK_N" where N corresponds meta in BookItem //(slot of dialog in npc)
                    if (dcat.title != null && dcat.title.startsWith(BOOK_DIALOG_CATEGORY_NAME)) {
                        final int bookN = getMetaFromCategoryTitle(dcat.title); // BOOK_0 -> 0
                        if (bookN == meta) {
                            //Autosearch first dialog in category "BOOK_META" "main page of book"
                            Integer lowestDialogId = getLowestDialogId(dcat.dialogs);
                            if (lowestDialogId != null ) {
                                dialog = dcat.dialogs.get(lowestDialogId);

                                if (dialog != null) {
                                    logD("For Meta:{} in CategoryId:{} Found DialogId:{}", meta, dcat.id, dialog.id);
                                    //DialogOption dOption = new DialogOption();dOption.dialogId = dialog.id;dOption.title = dialog.title;bookKeeper.dialogs.put( mi, dOption);
                                    this.metaToDialogId.put(meta, dialog.id);
                                } else {
                                    //lowestDialogId = -1 "Not Found"
                                    logD("For Meta:{} in CategoryId:{} LowestDialogId:{} return Null Dialog", meta, dcat.id, lowestDialogId);//NotFound
                                    this.metaToDialogId.put(meta, null);//in order not to search again after the first search
                                }

                                return dialog;
                            }
                            else {
                                final int sz = dcat.dialogs == null ? -1 : dcat.dialogs.size();
                                logD("Not Found lowestDialogId for Meta: {} in CategoryId: {} DialogsInCategory: {}", meta, dcat.id, sz);
                            }
                        }
                    }
                }
            }
            /*if no dialog is found for the specified meta put a stub in the map
              In order not to search for the dialog again for this meta
              For remove stub and remapping use command 'custombooks reload'  */
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
    public int getMetaFromCategoryTitle(String title) {
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
                    if (debug) {
                        LOG.log(Level.WARN, "Illegal Meta in Category Title: '{}'", title);
                    }
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

    //waitfor MLog
    //---------------------------- DEBUG LOG ---------------------------------\\
    public void logD(String message, int i) {
        if (debug) {
            LOG.log(Level.INFO, message, i);
        }
    }
    public void logD(String message, int a, int b, int c) {
        if (debug) {
            LOG.log(Level.INFO, message, a, b, c);
        }
    }

}

