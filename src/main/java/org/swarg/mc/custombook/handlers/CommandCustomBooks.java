package org.swarg.mc.custombook.handlers;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;

import org.swarg.cmds.ArgsWrapper;
import org.swarg.mc.custombook.BooksKeeper;
import org.swarg.mc.custombook.util.NpcUtil;
import org.swarg.mc.custombook.util.BookConverter;

/**
 * 02-02-21
 * @author Swarg
 */
public class CommandCustomBooks extends CommandBase {
    private final List<String> aliases;
    /*used to free dialogsId on remove last yang Dialogs pack*/
    private boolean packCreated;

    public CommandCustomBooks() {
        this.aliases = new ArrayList<String>();
        this.aliases.add("cb");
    }
    
    @Override
    public List<String> getCommandAliases() {
        return this.aliases;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;//??
    }

    @Override
    public String getCommandName() {
        return "custombooks";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "<status/reload/display/quest-tag/jump-to-dim/convert>";
    }


    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String response = null;
        
        ArgsWrapper w = new ArgsWrapper(args);

        if (w.isHelpCmdOrNoArgs()) {
            response = getCommandUsage(sender);
        }
        else if (w.isCmd("reload")) {
            //BooksKeeper.instance().setupMappingDialogsToBooks();
            BooksKeeper.instance().reload();
            response = "done";//todo status
        }
        
        else if (w.isCmd("status", "st")) {
            response = BooksKeeper.instance().status();
        }

        //commands only for op-player
        else if (NpcUtil.canPlayerEditNpc(sender, false, true)) {
            response = "UKNOWN";

            //debug
            if (w.isCmd("jump-to-dim", "j2d")) {
                response = cmdJumpToDim(w, sender);
            }


            //-------------------------- tools -----------------------------------\\

            //add to ItemStack unique nbt taq (For use in Quests)
            else if (w.isCmd("quest-tag", "qt") && sender instanceof EntityPlayer) {
                response = cmdQuestTag(w, sender);
            }

            //set display name|lore of ItemStack Display  or remove it
            else if (w.isCmd("display", "di") && sender instanceof EntityPlayer) {
                response = cmdDisplayTag(w, sender);
            }


            else if (w.isCmd("convert") && sender instanceof EntityPlayer) {
                ItemStack is = ((EntityPlayer)sender).getHeldItem();

                if (is == null || !is.hasTagCompound()) {
                    response = "Not found Valid book";
                }
                else {
                    String text = new BookConverter().convertBigBookToText(is);
                    if (text != null) {
                        //todo limit for too big books
                        response = text;
                    }
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
    

    //Debug
    private String cmdJumpToDim(ArgsWrapper w, ICommandSender sender) {
        String response = "?";
        if (w.isHelpCmdOrNoArgs()) {
            response = "dimId x y z";
        }
        else if (sender instanceof EntityPlayerMP && NoppesUtilServer.isOp((EntityPlayerMP)sender)) {
            int dim = w.argI(w.ai++, Integer.MIN_VALUE);
            int x   = w.argI(w.ai++, 0);
            int y   = w.argI(w.ai++, 170);
            int z   = w.argI(w.ai++, 0);
            if (dim > Integer.MIN_VALUE) {
                NoppesUtilPlayer.teleportPlayer( (EntityPlayerMP)sender, x, y, z, dim);
                response = "Teleported to Dimension: " + dim;
            }
        }
        return response;
    }


    /**
     * edit display data of itemstack
     * @param w
     * @param sender
     * @return
     */
    private String cmdDisplayTag(ArgsWrapper w, ICommandSender sender) {
        String response = "?";
        if (w.isHelpCmdOrNoArgs()) {
            response = "display <set-name/set-lore/set-color/remove>";
        }
        else {
            ItemStack is = ((EntityPlayer)sender).getHeldItem();

            if (is == null) {
                response = "Take the item in your hands";
            }
            else {
                //cb display set-name (nameWord1) (nameWord2) (nameWordN)
                if (w.isCmd("set-name", "sn")) {
                    String name = w.join(w.ai);
                    if (name != null && !name.isEmpty()) {
                        is.setStackDisplayName( name );
                        response = "Changed";
                    }
                    else {
                        response = "illegal empty name";
                    }
                }
                // set-lore word1 w2 w2 \ w3  | set-lore -append
                // "\" - sepatate to new line
                else if (w.isCmd("set-lore", "sl")) {
                    if (w.isHelpCmdOrNoArgs()) {
                        response = "set-lore word1 w2 w2 \\ w3  | set-lore -append word1 w2 \\ w3";
                    }
                    else  {
                        NBTTagCompound nbtDisplay = NpcUtil.getDisplayTag(is, true);
                        
                        NBTTagList lore = null;
                        //append to exist lore
                        if (w.isCmd("-append", "-a")) {
                            lore = NpcUtil.getLoreTag(is.stackTagCompound);
                        }
                        if (lore == null) {
                            lore = new NBTTagList();
                        }
                        StringBuilder sb = new StringBuilder();

                        while (w.argsRemain() > 0) {
                            String word = w.arg(w.ai++);
                            if ("\\".equals(word)) {
                                lore.appendTag(new NBTTagString( sb.toString() ));
                                sb.setLength(0);
                            } else {
                                sb.append(word).append(' ');
                            }
                        }

                        if (sb.length() > 0) {
                            lore.appendTag(new NBTTagString(sb.toString()));
                        }
                        if (!is.hasTagCompound()) {
                            is.stackTagCompound = new NBTTagCompound();

                        }
                        
                        if (nbtDisplay != null) {
                            nbtDisplay.setTag("Lore", lore);
                            response = "Done. +Lore";
                        }
                    }
                }

                else if (w.isCmd("set-color", "sc")) {
                    int color = w.argI(w.ai++, -1);
                    if (color > -1) {
                        NBTTagCompound nbtDisplay = NpcUtil.getDisplayTag(is, true);
                        nbtDisplay.setInteger("color", color);
                        response = "Done";
                    }
                }

                //todo tfc data of creating...

                //remove display tags
                else if (w.isCmd("remove", "r")) {
                    if (is.hasTagCompound() && is.stackTagCompound.hasKey("display") ) {
                        is.stackTagCompound.removeTag("display");
                        if (is.stackTagCompound.hasNoTags()) {
                            is.stackTagCompound = null;
                            response = "Removed NBT";
                        } else {
                            response = "Removed Display Tag Only";
                        }
                    } else {
                        response = "No Dispay Tag";
                    }
                }
            }
        }
        return response;
    }


    public static final String QUESTTAG = "uQuestTag";
    /**
     * make itemstack unique
     * @param w
     * @param sender
     * @return
     */
    private String cmdQuestTag(ArgsWrapper w, ICommandSender sender) {
        String response = "?";
        ItemStack is = ((EntityPlayer)sender).getHeldItem();
        if (is == null) {
            response = "Take the item in your hands";
        }
        else {
            if (w.isHelpCmdOrNoArgs()) {
                response = "<add/remove/show>";
            } else {
                if (w.isCmd("show", "s")) {
                    if (is.hasTagCompound() && is.stackTagCompound.hasKey(QUESTTAG)) {
                        response = String.valueOf( is.stackTagCompound.getInteger(QUESTTAG) );
                    } else {
                        response = "Tag not found";
                    }
                }
                else if (w.isCmd("add", "a")) {
                    if (!is.hasTagCompound()) {
                        is.stackTagCompound = new NBTTagCompound();
                    }
                    int tag = w.argI(w.ai++, (int) (System.currentTimeMillis() / 1000L) );
                    is.stackTagCompound.setInteger(QUESTTAG, tag);
                    response = QUESTTAG + ":" + tag;
                }
                else if (w.isCmd("remove", "r")) {
                    if (is.hasTagCompound() && is.stackTagCompound.hasKey(QUESTTAG) ) {
                        is.stackTagCompound.removeTag(QUESTTAG);
                        if (is.stackTagCompound.hasNoTags()) {
                            is.stackTagCompound = null;
                        }
                    } else {
                        response = "No " + QUESTTAG;
                    }
                }
            }
        }
        return response;
    }


}
