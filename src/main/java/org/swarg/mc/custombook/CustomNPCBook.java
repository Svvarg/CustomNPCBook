package org.swarg.mc.custombook;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.creativetab.CreativeTabs;

import org.swarg.mc.custombook.handlers.CommandCustomBooks;
import org.swarg.mc.custombook.handlers.CommandCustomExtension;
import org.swarg.mc.custombook.handlers.CommandNamedBroadcast;
import org.swarg.mc.custombook.util.Fixes;

/**
 * 01-02-21
 * @author Swarg
 */
@Mod(modid = CustomNPCBook.MODID, version = CustomNPCBook.VERSION)
public class CustomNPCBook {
    public static final String MODID = "CustomNPCBooks";
    public static final String VERSION = "0.3";

    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandCustomBooks());
        event.registerServerCommand(new CommandCustomExtension());
        event.registerServerCommand(new CommandNamedBroadcast());
    }


    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ItemCustomBook.customBook = new ItemCustomBook()
                .setHasSubtypes(true)
                .setUnlocalizedName(MODID+".book")
                .setMaxStackSize(4)
                .setCreativeTab(CreativeTabs.tabMisc);
        GameRegistry.registerItem(ItemCustomBook.customBook, CustomNPCBook.MODID);//"CustomBooks"

        CustomBooksAchievements.register();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @EventHandler
    public void postInit(cpw.mods.fml.common.event.FMLPostInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            Fixes.fixGuiStatListItemBased(net.minecraft.stats.StatList.objectMineStats);
            //Fixes.fixStatListItemBased(net.minecraft.stats.StatList.itemStats);
        }
    }
}
