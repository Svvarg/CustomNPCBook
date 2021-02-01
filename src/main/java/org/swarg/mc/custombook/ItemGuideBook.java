package org.swarg.mc.custombook;

import java.util.Iterator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 01-02-21
 *
 * @author Swarg
 */
public class ItemGuideBook extends Item {
    public static Item guideBook;
    /**
     *  Item used on block
     */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int i7, float f8, float f9, float f10) {
        GuideKeeper.instance().openGuideDialog(stack, world, player);
        return true;
    }

    /**
     * Fired if no block near
     * @return
     */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        GuideKeeper.instance().openGuideDialog(stack, world, player);
        return stack;
    }


}
