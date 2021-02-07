package org.swarg.mc.custombook.handlers;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;

import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.client.gui.global.GuiNPCManageDialogs;

import org.swarg.mc.custombook.CustomNPCBook;
import org.swarg.mc.custombook.util.NpcUtil;
import org.swarg.mc.fixes.Fixes;

/**
 * 7-02-21
 * Experimental Commands for Client Side only
 * @author Swarg
 */
public class ClientCommandCustomBooks extends CommandBase {
    private final List<String> aliases;

    public ClientCommandCustomBooks() {
        aliases = new ArrayList<String>();
        aliases.add("cbc");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;//??
    }

    @Override
    public String getCommandName() {
        return "custombooksclient";
    }
    @Override
    public List<String> getCommandAliases() {
        return this.aliases;
    }
    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "<version/check-stat-gui/dialog>";//guic
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (sender != null && args != null) {
            String response = null;
            final int c = args.length;
            final String cmd = c > 0 ? args[0] : "";
            
            if (c == 0 || "help".equals(cmd)) {
                response = getCommandUsage(sender);
            }
            //--------------------
            else if ("v".equals(cmd) || "version".equals(cmd)) {
                response = CustomNPCBook.VERSION + "-b." + CustomNPCBook.BUILD;
            }
            //--------------------
            else if ("check-gui-stat".equals(cmd) || "cgs".equals(cmd)) {
                StringBuilder log = new StringBuilder();
                Fixes.fixItemBasedStats(log, "[CLIENT]", net.minecraft.stats.StatList.objectMineStats, true);
                response = log.toString();
            }
            //else if ("guic".equals(cmd)) {
            //    response = "GuiCounter:" + org.swarg.mc.fixes.Fixes.guiCounter;
            //}

            //--------------------
            else if (c > 1 && "dialog".equals(cmd) || "d".equals(cmd)) {
                int id = -1;
                try {
                    id = Integer.parseInt(args[1]);
                    if (id > -1) {
                        response = doOpenDialogGUI(sender, id);
                    }
                } catch (Exception e) {
                    response = e.getMessage();
                }
            }

            //output
            showChatMsg(response);
        }
    }

    /**
     * TODO
     * @param sender
     * @param id
     * @return
     */
    private String doOpenDialogGUI(ICommandSender sender, int id) {
        EntityNPCInterface npc = (EntityNPCInterface) NpcUtil.getFirsNearestEntity((EntityPlayer)sender, EntityNPCInterface.class);

        if (npc != null && sender instanceof EntityPlayer) {
            //ManageDialogs
            EntityPlayer player = (EntityPlayer)sender;
            noppes.npcs.CustomNpcs.proxy.openGui(npc, EnumGuiType.MainMenuGlobal); //client.ClientProxy. openGui
            ItemStack stack = player.getHeldItem();// new ItemStack(Items.writable_book,0);
            noppes.npcs.client.NoppesUtil.openGUI(player,
                new noppes.npcs.client.gui.player.GuiBook(player,
                     stack,//net.minecraft.item.ItemStack.loadItemStackFromNBT(noppes.npcs.Server.readNBT(null)),
                     0, 0, 0));
            //GuiNPCManageDialogs guiMd = new GuiNPCManageDialogs(npc);
            //guiMd.initGui();
            //net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(guiMd);
            return null;
        } else {
            return "NPC Not Found";
        }
    }

    /**
     * Client Side Only!
     * @param response
     */
    public static void showChatMsg(String response) {
        if (response != null) {
            GuiIngame guiInGame = Minecraft.getMinecraft().ingameGUI;//@SideOnly(Side.CLIENT)
            if (response.contains("\n")) {
                String[] a = response.split("\n");
                for (String line : a) {
                    guiInGame.getChatGUI().printChatMessage(new ChatComponentText(line));
                }
            } else {
                guiInGame.getChatGUI().printChatMessage(new ChatComponentText(response));
            }
        }
    }
}

