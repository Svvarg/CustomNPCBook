package org.swarg.mc.custombook.handlers;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Iterator;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.DataScript;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.scripted.ScriptEntity;
import noppes.npcs.scripted.ScriptNpc;

import org.swarg.cmds.ArgsWrapper;
import org.swarg.mc.custombook.util.NpcUtil;

/**
 * 08-02-21
 * @author Swarg
 */
public class CommandCustomScript {

    private static CommandCustomScript INSTANCE;
    public static CommandCustomScript instance() {
        if (INSTANCE == null) {
            INSTANCE = new CommandCustomScript();
        }
        return INSTANCE;
    }

    //========================================================================\\
    //                           S C R I P T
    //========================================================================\\
    /**
     *
     * @param w
     * @param sender
     * @return
     * cx script
     */
    public String cmdScript(ArgsWrapper w, ICommandSender sender) {
        final String usage = "<npc/>";
        if (w.isHelpCmd()) {
            return usage;
        } else if (w.isCmd("npc")) {
            EntityNPCInterface npc = null;

            //Getting NPC Instance
            //nearest to the player
            if (w.isCmd("nearest", "n") && sender instanceof EntityPlayer) {
                npc = (EntityNPCInterface) NpcUtil.getFirsNearestEntity((EntityPlayer)sender, EntityNPCInterface.class);
            }
            //by Id
            else {
                int id = w.argI(w.ai++,-1);
                if (id < 0) {
                    return "(EntityId) | <nearest>";
                }
                Object ent = sender.getEntityWorld().getEntityByID(id);
                if (ent instanceof EntityNPCInterface) {
                    npc = (EntityNPCInterface)ent;
                }
            }

            //Actions with NPC
            if (npc != null) {
                final String usageNPC = "<status/temp-data/stored-data>";//script
                if (w.isHelpCmdOrNoArgs()) {
                    return usageNPC;
                }
                //cx script npc nearest status
                else if (w.isCmd("status", "st")) {
                    return formatNpcStatus(npc);
                }
                //--------------
                //one session data
                else if (w.isCmd("temp-data", "td")) {
                    final String usageNpcTempData = "<list/>";
                    if (w.isHelpCmdOrNoArgs()) {
                        return usageNpcTempData;
                    }
                    else if (w.isCmd("list", "l")) {
                        return getViewScrNpcAllTempData(npc);
                    }
                    else if (w.isCmd("removed", "rm")) {
                        String key = w.arg(w.ai++);
                        boolean was = npc.script.dummyNpc.hasTempData(key);
                        npc.script.dummyNpc.removeTempData(key);
                        //boolean has = npc.script.dummyNpc.hasTempData(key);
                        return "[TempData] "+(!was ? "Not Found" : "Removed") + " '"+key+"'";
                    }
                    else if (w.isCmd("script", "s")) {
                        return cmdGUIScript(w, sender, npc);
                    }
                }
                //--------------
                //persistents data saved with entity to disk
                else if (w.isCmd("stored-data", "sd")) {
                    final String usageNpcTempData = "<list/remove>";
                    if (w.isHelpCmdOrNoArgs()) {
                        return usageNpcTempData;
                    }
                    else if (w.isCmd("list", "l")) {
                        return getViewScrNpcAllStoredData(npc);
                    }
                    else if (w.isCmd("remove", "rm")) {
                        NBTTagCompound stored = getScriptNpcStoreData(npc, false);
                        if (stored != null) {
                            String key = w.arg(w.ai++);
                            boolean was = npc.script.dummyNpc.hasStoredData(key);
                            npc.script.dummyNpc.removeStoredData(key);
                            //boolean has = npc.script.dummyNpc.hasStoredData(key);
                            return "[StoredData] "+(!was ? "Not Found" : "Removed") + " '"+key+"'";
                        }
                    }
                }
            }
        }//
        return usage;
    }

    
    private String formatNpcStatus(EntityNPCInterface npc) {
        if (npc != null) {
            StringBuilder sb = new StringBuilder();
            sb.append('#').append( npc.getEntityId() ).append(' ').append( npc.getCommandSenderName() ).append('\n');
            DataScript ds = npc.script;
            if (ds != null) {
                sb.append("Script Enabled:").append( ds.enabled).append(" Lang:").append(ds.scriptLanguage)
                  .append(" Scripts:").append( ds.scripts.size()).append('\n');
                //----
                if (ds.scripts != null && !ds.scripts.isEmpty()) {
                    //=0= Init  =1= update  =2= init  =3= dialog  =4= damage  =5= killed  =6= atack  =7= target  =8= collide  =9= kills  =10 dialog Closed
                    final String[] titles = {"init","update","interact","dialog","damaged","killed","attack","target","collide","kills","dialogclosed"};
                    for (Map.Entry<Integer, ScriptContainer> e : ds.scripts.entrySet()) {
                        final Integer index = e.getKey();//script type
                        sb.append('#').append( e.getKey() );
                        if (index != null && index > -1 && index < titles.length) {
                            sb.append('(').append(titles[index]).append(')');
                        }
                        sb.append(' ');
                        ScriptContainer sc = e.getValue();
                        //the full script is a combination of all the scripts from
                        //TextArea in the GUI + all the contents of the files specified
                        //in the list sc.scripts (contains file-names)
                        final int fssz = sc.fullscript == null ? -1 : sc.fullscript.length();
                        //it contants of TextArea GUI
                        final int ssz = sc.script == null ? -1 : sc.script.length();

                        if (sc.scripts != null && sc.scripts.size() > 0) {
                            for (int i = 0; i < sc.scripts.size(); i++) {
                                String s = sc.scripts.get(i); //название файла скрипта!
                                if (i > 0) {
                                    sb.append(';');
                                }
                                sb.append(s);
                            }
                        } else {
                            sb.append("No scripts loaded from files");
                        }
                        sb.append(" FullScriptSz:").append(fssz).append(" Script(Gui)Sz:").append(ssz);
                        //error output for each scripts?
                        //sb.append("Console: ").append(sc.console).append('\n');
                    }
                } else {
                    sb.append("no scripts");
                }
                //-----
                ScriptNpc snpc = ds.dummyNpc;
                if (snpc != null) {
                    Entity entity = snpc.getMCEntity();
                    //check is one instance or not
                    sb.append("\nScriptNpc.MCEntity ").append(entity == npc ? '=' : '!').append("= Npc\n");
                    //npc.say(npc.getMCEntity().func_70005_c_())//func_70005_c_,getCommandSenderName
                } else {
                    sb.append("\nNo ScriptNpc in DataScript");
                }
            } else {
                sb.append("DataScript is null");
            }
            return sb.toString();
        }
        else return "npc not found";
    }


    private Map getScriptNPCTempData(EntityNPCInterface npc) {
        if (npc != null) {
            DataScript ds = npc.script;
            if (ds != null) {
                ScriptNpc snpc = ds.dummyNpc;
                if (snpc != null) {
                    try {
                        Field field = ScriptEntity.class.getDeclaredField("tempData");
                        field.setAccessible(true);
                        return (Map) field.get(snpc);
                    }
                    catch (Throwable t) {
                    }
                }
            }
        }
        return null;
    }

    public static NBTTagCompound getScriptNpcStoreData(EntityNPCInterface npc, boolean canCreate) {
        NBTTagCompound data = npc.getEntityData();//nbt provided by forge
        if (data != null) {
            NBTTagCompound nbt = npc.getEntityData().getCompoundTag("CNPCStoredData");
            if (nbt == null && canCreate) {
                data.setTag("CNPCStoredData", nbt = new NBTTagCompound());
            }
            return nbt;
        }
        return null;
    }

    private String getViewScrNpcAllTempData(EntityNPCInterface npc) {
        Map tempData = getScriptNPCTempData(npc);
        if (tempData != null) {
            if (tempData.isEmpty()) {
                return "empty";
            }
            Iterator<Map.Entry> iter = tempData.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (iter.hasNext()) {
                Map.Entry e = iter.next();
                sb.append( e.getKey() ).append('=').append( e.getValue() ).append('\n');
            }
            return sb.toString();
        }
        return "not found tempData";
    }

    
    private String getViewScrNpcAllStoredData(EntityNPCInterface npc) {
        NBTTagCompound nbt = getScriptNpcStoreData(npc, false);
        return nbt == null ? "Not Found StoreData" : nbt.toString();
    }

    /**
     * todo
     * @param npc
     * @return
     */
    private String cmdGUIScript(ArgsWrapper w, ICommandSender sender, EntityNPCInterface npc) {
        if (sender instanceof EntityPlayer && NpcUtil.isOp((EntityPlayer)sender)) {
            final String usage = "<todo>";
            if(w.isHelpCmdOrNoArgs()) {
                return usage;
            } 
        }
        return "?";
    }

}
