package org.swarg.mc.custombook.util;

import java.util.List;
import java.util.Iterator;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.stats.StatCrafting;
import net.minecraft.stats.StatList;
import net.minecraft.item.Item;

/**
 * 05-02-21
 * @author Swarg
 */
public class Fixes {

    /**
     * objectMineStats
     * Prevent and Fix Crash on ClientSide then click to Statistics button
     * but it not dix Blocks button inside Statistics!
     * java.lang.ArrayIndexOutOfBoundsException: -1
     *  at net.minecraft.client.gui.achievement.GuiStats$StatsBlock.[init](SourceFile:548)
     *  at net.minecraft.client.gui.achievement.GuiStats.func_146509_g(SourceFile:123)
     *  at net.minecraft.client.network.NetHandlerPlayClient.func_147293_a(NetHandlerPlayClient.java:1328)
     */
    @SideOnly(Side.CLIENT)
    public static void fixGuiStatListItemBased(List stats) {
        //if (world.isRemote) {
            if (stats != null && stats.size() > 0 ) {
                int count = 0;
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
                /*DEBUG*/System.out.println("###[Fix] StatList Removed not registered Items: " + count);
            }
        //}
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



    //Experimental
    //@SideOnly(Side.CLIENT)
    //@SubscribeEvent
    //public void preGUIRenderTick(GuiScreenEvent.DrawScreenEvent.Pre event) {
    //    if (event.gui instanceof GuiStats) {
    //        GuiStats gui = (GuiStats)event.gui;
    //    }
    //}

}
