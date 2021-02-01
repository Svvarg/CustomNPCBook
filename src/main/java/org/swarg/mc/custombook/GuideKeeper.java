package org.swarg.mc.custombook;

import java.util.Iterator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;

import noppes.npcs.Server;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.DialogCategory;
import noppes.npcs.controllers.DialogOption;
import noppes.npcs.controllers.Dialog;


/**
 * 01-02-21
 * @author Swarg
 */
public class GuideKeeper {
    /*Name of CustomNPC Entity than contains Guide Dialogs*/
    private static final String GUIDE_ENTITY_NAME = "Guide";

    private static GuideKeeper instance;
    /*For search entity only once*/
    private static EntityNPCInterface keeper;
    //private static NBTTagCompound nbtDialog;


    public static GuideKeeper instance() {
        if (instance == null) {
            instance = new GuideKeeper();
        }
        return instance;
    }


    /**
     * ResearchTool /os o e cnn Guide \ dialogs \ -a index 0 \ -list
     * @param is
     * @param world
     * @param player
     */
    public void openGuideDialog(ItemStack is, World world, EntityPlayer player) {
        if (world != null && !world.isRemote && player != null) {

            this.keeper = findGuideKeeper();

            if (keeper == null || keeper.isDead) {
                keeper = null;//import for reset removed from world npc-entity
                /*DEBUG*/player.addChatMessage(new ChatComponentText("Not Found Guide Keeper"));
            }
            else if (keeper.dialogs == null || keeper.dialogs.size() == 0) {
                //nbtDialog = null;//для того чтобы сбросить заготовку отправляемую клиенту
                /*DEBUG*/player.addChatMessage(new ChatComponentText("No Found Dialog in Guide Keeper"));
            }
            else {
                Iterator iter = keeper.dialogs.values().iterator();

                // pull first dialog from keeper npc
                while (iter.hasNext()) {
                    DialogOption option = (DialogOption) iter.next();
                    if (option != null && option.hasDialog()) {
                        Dialog dialog = option.getDialog();
                        if (dialog != null) {
                            NoppesUtilServer.openDialog(player, keeper, dialog);
                            return;
                        }
                    }
                }

                ///*DEBUG*/player.addChatMessage(new ChatComponentText("Error No Found Dialog in Guide Keeper 2"));
            }
        }
    }



    /**
     * Serach Guide Keeper CustomNPC by name
     * @return
     */
    private EntityNPCInterface findGuideKeeper() {
        if (keeper == null) {
            MinecraftServer srv = MinecraftServer.getServer();
            if (srv != null && srv.worldServers != null) {
                final WorldServer[] a = srv.worldServers;
                for (int i = 0; a != null && i < a.length; i++) {
                    WorldServer ws = a[i];
                    if (ws != null && ws.loadedEntityList != null) {
                        for (int j = 0; j < ws.loadedEntityList.size(); j++) {
                            Entity e = (Entity)ws.loadedEntityList.get(j);
                            if (GUIDE_ENTITY_NAME.equals(e.getCommandSenderName()) && e instanceof EntityNPCInterface) {
                                return (EntityNPCInterface) e;
                            }
                        }
                    }
                }
            }
            return null;
        }
        //already founded
        else {
            return keeper;
        }
    }

}
