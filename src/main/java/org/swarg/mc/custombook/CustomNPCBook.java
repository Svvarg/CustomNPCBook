package org.swarg.mc.custombook;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import org.swarg.mc.custombook.handlers.CommandGuideBook;
import org.swarg.mc.custombook.handlers.CommandNamedBroadcast;

/**
 * 01-02-21
 * @author Swarg
 */
@Mod(modid = CustomNPCBook.MODID, version = CustomNPCBook.VERSION)
public class CustomNPCBook {
    public static final String MODID = "CustomNPCBook";
    public static final String VERSION = "0.1";

    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandNamedBroadcast());
        event.registerServerCommand(new CommandGuideBook());
    }


    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ItemGuideBook.guideBook = new ItemGuideBook()
                .setHasSubtypes(true)
                .setUnlocalizedName(MODID+".guideBook")
                //.setTextureName(MODID+":"+"guideBook")
                .setMaxStackSize(4)
                .setCreativeTab(CreativeTabs.tabMisc);
        GameRegistry.registerItem(ItemGuideBook.guideBook, "GuideBook");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    }
}
