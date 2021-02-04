package org.swarg.mc.custombook.handlers;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import org.swarg.mc.custombook.util.NpcUtil;

/**
 * 02-02-21
 * @author Swarg
 */
public class CommandNamedBroadcast extends CommandBase {

    public CommandNamedBroadcast() {
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;//??
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public String getCommandName() {
        return "nsay";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "(name) message";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (sender != null && args != null) {
            if (args.length < 2) {
                sender.addChatMessage( new ChatComponentText( getCommandUsage(sender) ));
                //throw new WrongUsageException("commands.say.usage", new Object[0]);
            }
            else {
                ///*DEBUG*/System.out.println(sender.getClass()+" "+sender);
                String name = args[0];
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    String arg = args[i];
                    sb.append(arg).append(' ');
                }
                NpcUtil.sendGlobalMessage(name, sb.toString());
            }
        }
    }

}
