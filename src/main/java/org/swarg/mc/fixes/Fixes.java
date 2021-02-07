package org.swarg.mc.fixes;

import java.util.List;
import java.util.Iterator;
import java.lang.reflect.Field;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatList;
import net.minecraft.item.Item;

/**
 * 05-02-21
 * @author Swarg
 */
public class Fixes {

    //-------------------------   EXPERIMENTAL   ----------------------------\\
    //public static int guiCounter = 0;//to check the response frequency
    /*Client Side:
    at org.swarg.mc.custombook.handlers.EventsClientHandler.onClientGUI(EventsClientHandler.java:28)
    at cpw.mods.fml.common.eventhandler.ASMEventHandler_288_EventsClientHandler_onClientGUI_GuiOpenEvent.invoke(.dynamic)
    at cpw.mods.fml.common.eventhandler.ASMEventHandler.invoke(ASMEventHandler.java:54)
    at cpw.mods.fml.common.eventhandler.EventBus.post(EventBus.java:140)
    at net.minecraft.client.Minecraft.func_147108_a(Minecraft.java:786)                | displayGuiScreen
    at net.minecraft.client.gui.GuiIngameMenu.func_146284_a(GuiIngameMenu.java:67)     | actionPerformed
    at net.minecraft.client.gui.GuiScreen.func_73864_a(GuiScreen.java:225)             | mouseClicked
    at net.minecraft.client.gui.GuiScreen.func_146274_d(GuiScreen.java:296)            | handleMouseInput
    at net.minecraft.client.gui.GuiScreen.func_146269_k(GuiScreen.java:268)            | handleInput
    at net.minecraft.client.Minecraft.func_71407_l(Minecraft.java:1640)                | runTick
    at net.minecraft.client.Minecraft.func_71411_J(Minecraft.java:973)                 | runGameLoop
    at net.minecraft.client.Minecraft.func_99999_d(Minecraft.java:898)                 | run
    at net.minecraft.client.main.Main.main(SourceFile:148)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
    at java.lang.reflect.Method.invoke(Unknown Source)
    at net.minecraft.launchwrapper.Launch.launch(Launch.java:135)
    at net.minecraft.launchwrapper.Launch.main(Launch.java:28)*/
    /**
     * Statistics Gui fix portal 90 -1 if of
     * find the best way to fix
     * net.minecraft.network.play.server.S37PacketStatistics
     * @param event
     * MinecraftForge.EVENT_BUS.register
     */
    @SubscribeEvent
    public void onClientGUI(net.minecraftforge.client.event.GuiOpenEvent event) {
        //guiCounter++;
        if (event.gui != null) {
            if (event.gui.getClass() == net.minecraft.client.gui.achievement.GuiStats.class) {
                //CustomNPCBook.logger.log("GUI-FIX:" + event.gui.getClass().getName());
                //clientPrintChatMessage(NpcUtil.exceptionTraceAsString(new Throwable()));
                Fixes.fixGuiStatListItemBased(net.minecraft.stats.StatList.objectMineStats);
            }
        }
    }
    //------------------------------------------------------------------------\\

    
    /**
     * objectMineStats
     * Prevent and Fix Crash on ClientSide then click to Statistics button
     * but it not dix Blocks button inside Statistics!
     * java.lang.ArrayIndexOutOfBoundsException: -1
     *  at net.minecraft.client.gui.achievement.GuiStats$StatsBlock.[init](SourceFile:548)
     *  at net.minecraft.client.gui.achievement.GuiStats.func_146509_g(SourceFile:123)
     *  at net.minecraft.client.network.NetHandlerPlayClient.func_147293_a(NetHandlerPlayClient.java:1328)
     *
     * Fixes.fixStatListItemBased(net.minecraft.stats.StatList.objectMineStats);
     * Fixes.fixStatListItemBased(net.minecraft.stats.StatList.itemStats);
     * net.minecraft.stats.StatCrafting
     */
    @SideOnly(Side.CLIENT)
    public static int fixGuiStatListItemBased(List stats) {
        int count = 0;
        if (stats != null && stats.size() > 0 ) {
            final int max = StatList.objectUseStats.length;
            Iterator iter = stats.iterator();
            while (iter.hasNext()) {
                Object e = iter.next();
                if (e instanceof StatCrafting) {
                    Item item = ((StatCrafting)e).func_150959_a();//ClientSideOnly! //"field_150960_a"
                    final int id = Item.getIdFromItem(item);
                    if (id < 0 || id >= max ) {
                        iter.remove();
                        count++;
                    }
                }
            }
            //String msg = "###[Fix] StatList Removed not Registered Items: " + count;
            //CustomNPCBook.logger.log(msg);
            ///*DEBUG*/System.out.println(msg);
        }
        return count;
    }

    /*
    TODO crash on Client Side then click to button [Blocks] inside Statistics gui
    forge source file add check for outOfRange on
    src\main\java\net\minecraft\client\gui\achievement\GuiStats.java

    need fix snippets like:
    int k = Item.getIdFromItem(item); if item unreg then return -1;
    StatList.objectCraftStats[k], ... //Boom!


 java.lang.ArrayIndexOutOfBoundsException: -1
 	at net.minecraft.client.gui.achievement.GuiStats$StatsBlock.func_148126_a(SourceFile:628)  | drawSlot
 	at net.minecraft.client.gui.GuiSlot.func_148120_b(GuiSlot.java:433)                        | drawSelectionBox
 	at net.minecraft.client.gui.GuiSlot.func_148128_a(GuiSlot.java:306)                        | drawScreen
 	at net.minecraft.client.gui.achievement.GuiStats.func_73863_a(SourceFile:108)              | drawScreen
 	at net.minecraft.client.renderer.EntityRenderer.func_78480_b(EntityRenderer.java:1455)     | updateCameraAndRender
 	at net.minecraft.client.Minecraft.func_71411_J(Minecraft.java:1001)                        | runGameLoop
 	at net.minecraft.client.Minecraft.func_99999_d(Minecraft.java:898)                         | run
 	at net.minecraft.client.main.Main.main(SourceFile:148)
 	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
 	at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
 	at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
 	at java.lang.reflect.Method.invoke(Unknown Source)
 	at net.minecraft.launchwrapper.Launch.launch(Launch.java:135)
 	at net.minecraft.launchwrapper.Launch.main(Launch.java:28)
    */



    /**
     * Для сервера эксперементальный
     * проверял можно ли исправить проблему удалив незареганый item из objectMineStats
     * ответ отрицательный - это не даст ожидаемого результа. хотя по логике
     * сервер отсылает клиенту список вещей для отображения в gui Statistics
     *
     * если на клиенте удалять ломающие item`ы при кадом просмотре статистики
     * тогда можно не трогать сервер. и этот метод не нужен
     * @param log
     * @param pref
     * @param stats net.minecraft.stats.StatList.objectMineStats
     * @param removeWrong
     */
    
    public static void fixItemBasedStats(StringBuilder log, String pref, List stats, boolean removeWrong) {
        if (log != null && stats != null) {
            //List stats = net.minecraft.stats.StatList.objectMineStats;
            if (stats != null && stats.size() > 0 ) {
                int count = 0;
                final int max = StatList.objectUseStats.length;
                Iterator iter = stats.iterator();
                Field field = null;
                try {
                    field = StatCrafting.class.getDeclaredField("field_150960_a");
                    field.setAccessible(true);
                    while (iter.hasNext()) {
                        Object e = iter.next();
                        if (e instanceof StatCrafting) {
                            Item item = (Item) field.get((StatCrafting)e);
                            //Item item = ((StatCrafting)e).func_150959_a();//ClientSideOnly! //"field_150960_a"
                            //Determine is the item mapping are broken
                            //has Instance but not has id
                            final int id = Item.getIdFromItem(item);
                            if (id < 0 || id >= max ) {
                                log.append( pref ).append("[###] ")
                                   .append( item.getUnlocalizedName() );//<<< broken Item
                                if (removeWrong) {
                                    iter.remove();
                                    log.append(" [###] Removed\n");
                                } else {
                                    log.append(" [###] FoundWrong\n");
                                }
                                count++;
                            }
                        }
                    }
                }
                catch (Exception e) {
                    //e.printStackTrace();
                }

                /*DEBUG*/log.append(pref).append("[###][Fix] StatList Not registered Items: ").append(count).append(" ").append(removeWrong ? "[Removed]" : "[Found]").append('\n');
            } else {
                log.append("StatList.objectMineStats is empty!\n");
            }
        }
    }

/*
 java.lang.ArrayIndexOutOfBoundsException: -1
 	at net.minecraft.client.gui.achievement.GuiStats$StatsBlock.func_148126_a(SourceFile:628)
 	at net.minecraft.client.gui.GuiSlot.func_148120_b(GuiSlot.java:433)
 	at net.minecraft.client.gui.GuiSlot.func_148128_a(GuiSlot.java:306)
 	at net.minecraft.client.gui.achievement.GuiStats.func_73863_a(SourceFile:108)
 	at net.minecraft.client.renderer.EntityRenderer.func_78480_b(EntityRenderer.java:1455)
 	at net.minecraft.client.Minecraft.func_71411_J(Minecraft.java:1001)
 	at net.minecraft.client.Minecraft.func_99999_d(Minecraft.java:898)
 	at net.minecraft.client.main.Main.main(SourceFile:148)
 	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
 	at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
 	at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
 	at java.lang.reflect.Method.invoke(Unknown Source)
 	at net.minecraft.launchwrapper.Launch.launch(Launch.java:135)
 	at net.minecraft.launchwrapper.Launch.main(Launch.java:28)
*/
}
