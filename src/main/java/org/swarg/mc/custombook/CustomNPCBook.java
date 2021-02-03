package org.swarg.mc.custombook;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import org.swarg.mc.custombook.handlers.CommandCustomBook;
import org.swarg.mc.custombook.handlers.CommandNamedBroadcast;

/**
 * 01-02-21
 * @author Swarg
 */
@Mod(modid = CustomNPCBook.MODID, version = CustomNPCBook.VERSION)
public class CustomNPCBook {
    public static final String MODID = "CustomNPCBook";
    public static final String VERSION = "0.3";

    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandNamedBroadcast());
        event.registerServerCommand(new CommandCustomBook());
    }


    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ItemCustomBook.customBook = new ItemCustomBook()
                .setHasSubtypes(true)
                .setUnlocalizedName(MODID+".book")
                .setMaxStackSize(4)
                .setCreativeTab(CreativeTabs.tabMisc);
        GameRegistry.registerItem(ItemCustomBook.customBook, "CustomBook");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    }
}
