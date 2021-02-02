package org.swarg.mc.custombook.handlers;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import org.swarg.mc.custombook.BookConverter;

/**
 * 02-02-21
 * @author Swarg
 */
public class CommandGuideBook extends CommandBase {
    private final List<String> aliases;

    public CommandGuideBook() {
        this.aliases = new ArrayList<String>();
        this.aliases.add("gb");
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
        return "guidebook";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "<convert>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0) {

            if ("convert".equals(args[0]) && sender instanceof EntityPlayer) {
                ItemStack is = ((EntityPlayer)sender).getHeldItem();
                
                if (is == null || is.stackTagCompound == null) {
                    sender.addChatMessage(new ChatComponentText("Not found valid book"));
                }
                else {
                    String text = new BookConverter().convertBigBookToText(is);
                    if (text != null) {
                        //todo limit for too big books
                        sender.addChatMessage(new ChatComponentText(text));
                    }
                }
            }

        } else {
            sender.addChatMessage(new ChatComponentText(this.getCommandUsage(sender)));
        }
    }

}
