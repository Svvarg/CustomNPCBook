package org.swarg.mc.custombook;

import java.util.Iterator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.World;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

import noppes.npcs.Server;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.DialogCategory;
import noppes.npcs.controllers.DialogOption;
import noppes.npcs.controllers.Dialog;
import noppes.npcs.controllers.PlayerDialogData;
import noppes.npcs.controllers.PlayerDataController;


/**
 * 01-02-21
 * @author Swarg
 */
public class GuideKeeper {
    /*Name of CustomNPC Entity than contains Guide Dialogs*/
    private static final String GUIDE_ENTITY_NAME = "Guide";

    private static GuideKeeper instance;
    /*EntityId of CustomNPC GuideKeeper which will be opened through the 
      GuideBook. Keeper will be located in the persists chunks (Spawn)*/
    private Integer keeperId;
    /*Entry dialog writen to nbt - firs page
      meta of book corresponds to the dialog number of the npc*/
    private NBTTagCompound[] aDialogNbt;// = new NBTTagCompound[ItemGuideBook.BOOKS.length];//max 11


    public static GuideKeeper instance() {
        if (instance == null) {
            instance = new GuideKeeper();
        }
        return instance;
    }


    /**
     * aka Open GuideBook GUI (CustomNPC Dialog via book)
     * ResearchTool /os o e cnn Guide \ dialogs \ -a index 0 \ -list
     * @param is
     * @param world
     * @param player
     */
    public void openGuideDialog(ItemStack is, World world, EntityPlayer player) {
        if (world != null && !world.isRemote && player != null && is != null) {
            final int meta = MathHelper.clamp_int(is.getItemDamage(), 0, ItemGuideBook.BOOKS.length); //11-ItemGuideBook.BOOKS.length

            EntityNPCInterface keeper = checkOrFindGuideKeeper();//update keeperId on success

            if (isValidKeeperState(keeper)) {

                if (isNoDialogs()) {
                    //init dialogNBT by search first dialog in GuideKeeper
                    setGuideDialog(keeper);
                }

                if (hasDialog(this.aDialogNbt, meta) && PlayerDataController.instance != null) {

                    //OpenDialog in Player Client
                    //dialogId dialogNbt.getInteger("DialogId");
                    Server.sendData((EntityPlayerMP)player, EnumPacketClient.DIALOG, new Object[]{ keeperId, aDialogNbt[meta]});

                    // without this package, the dialog options don't open for some reason
                    NoppesUtilServer.setEditingNpc(player, keeper);
                }
                else {
                    ///*DEBUG*/System.out.println( "hasDialogNbt:" + (dialogNbt !=null) + " hasDialogId:" + (dialogId!=null) + " hasPlayerDataController:"+(PlayerDataController.instance!=null));
                }
            }
        }
    }

    private boolean hasDialog(Object[] a, int index) {
        return  (a != null && index > -1 && index < a.length && a[index] != null);
    }

    /**
     * Cleanup dialogs for ingame "reload" via GuideKeeperEntity
     */
    private void cleanDialogs() {
        if (this.aDialogNbt != null) {
            for (int i = 0; i < aDialogNbt.length; i++) {
                this.aDialogNbt[i] = null;
            }
        } else {
            //meta of book corresponds dialog number in npc
            // for basic guidebook it index = 0
            this.aDialogNbt = new NBTTagCompound[ItemGuideBook.BOOKS.length];//max 11;
        }
    }

    private boolean isNoDialogs() {
        if (this.aDialogNbt != null) {
            for (int i = 0; i < aDialogNbt.length; i++) {
                if ( aDialogNbt[i] != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
        [Alternative] how work open dialog via console command:
        '/noppes dialog show player1 <dialogId> (EntityName)'

            int diagid = Integer.parseInt(args[1]);
            Dialog dialog = (Dialog)DialogController.instance.dialogs.get(Integer.valueOf(diagid));
    
            noppes.npcs.entity.EntityDialogNpc npc = new EntityDialogNpc( playerworld );
            npc.display.name = args[2];
            EntityUtil.Copy(player, npc);
            DialogOption option = new DialogOption();
            option.dialogId = diagid;
            option.title = dialog.title;
            npc.dialogs.put(Integer.valueOf(0), option);
            NoppesUtilServer.openDialog(player, npc, dialog);
    */


    /**
     * If No KeeperId - Search In all loaded server worlds
     * Else check by exists keeperId
     * @param keeperId
     * @return
     */
    private EntityNPCInterface checkOrFindGuideKeeper() {
        this.keeperId = null;
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv != null && srv.worldServers != null) {
            final WorldServer[] aws = srv.worldServers;

            //update exist keeperId and check for changes
            if (keeperId != null && keeperId.intValue() > -1) {
                for (int i = 0; i < aws.length; i++) {
                    final WorldServer ws = aws[i];
                    final Entity e = ws.getEntityByID(keeperId.intValue());
                    if (isGuideKeeper(e)) {
                        return (EntityNPCInterface) e;
                    }
                }
            }

            //Initial Search Keeper in all loaded worlds
            //todo search only in persistens spawn area
            for (int i = 0; i < aws.length; i++) {
                WorldServer ws = aws[i];
                if (ws != null && ws.loadedEntityList != null) {
                    final Object[] es = ws.loadedEntityList.toArray();
                    for (int j = 0; j < es.length; j++) {
                        final Entity e = (Entity) es[j];
                        if (isGuideKeeper(e)) {
                            this.keeperId = e.getEntityId();//update
                            return (EntityNPCInterface) e;
                        }
                    }
                }
            }
        }
        return null;
    }


    public static boolean isGuideKeeper(Entity e) {
        return e instanceof EntityNPCInterface && GUIDE_ENTITY_NAME.equals(e.getCommandSenderName());
    }

    /**
     * Has GuideKeeper and exist Dialogs on keeper
     * @param keeper
     * @return
     */
    private boolean isValidKeeperState(EntityNPCInterface keeper) {
        //reset GuideKeeper
        if (keeper == null || keeper.isDead) {
            this.keeperId  = null;//import for reset removed from world npc-entity
            cleanDialogs();//this.aDialogNbt = null; this.dialogId  = null;
            sendGlobalMessage("DEBUG", "Not Found Guide Keeper");//player.addChatMessage(new ChatComponentText("?"));
        }
        //reset DialogNbt-workpiece
        else if (keeper.dialogs == null || keeper.dialogs.size() == 0) {
            cleanDialogs();//this.aDialogNbt = null; this.dialogId = null;
            sendGlobalMessage("DEBUG", "No Found Dialog in Guide Keeper");
        }
        //success
        else {
            this.keeperId = keeper.getEntityId();//updated 2
            return true;
        }

        return false;
    }



    /**
     * pull first dialog from keeper npc and write once it to nbt
     * update dialogNbt and dialogId
     * @param keeper
     */
    private void setGuideDialog(EntityNPCInterface keeper) {
        cleanDialogs();
        
        //this.dialogId = null;
        // i - meta of book corresponde dialog index in npc
        for (int i = 0; i < aDialogNbt.length; i++) {
            final DialogOption option = keeper.dialogs.get(Integer.valueOf(i));

            if (option != null && option.hasDialog()) {
                final Dialog dialog = option.getDialog();
                if (dialog != null) {                    
                    /* write to NPC Once for send to Player Clients
                       For reload dialog rename GuideKeeper and use GuideBook it
                       cleanup installed dialogs then, after editing the dialogs,
                       return the name to the guidekeeper npc.*/
                    this.aDialogNbt[i] = dialog.writeToNBT(new NBTTagCompound());
                    //this.dialogId  = dialog.id;//dialogNbt.getInteger("DialogId");
                    ///*DEBUG*/sendGlobalMessage("DEBUG", "Found Dialog in Guide Keeper" + i);
                }
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

}

//NoppesUtilServer.openDialog(player, keeper, dialog);
//final int dialogId = dialogNbt.getInteger("DialogId");
//final Dialog pDialog = dialog.copy(player); it transmit available dialog options for specific player. not needed here

////if not marked as read the player will not be able to activate dialogOptions
//PlayerDialogData data = PlayerDataController.instance.getPlayerData(player).dialogData;
//if (data != null && !data.dialogsRead.contains( dialogId )) {
//    sendGlobalMessage("DEBUG", "Dialog marked as read");
//    data.dialogsRead.add( dialogId );
//}
