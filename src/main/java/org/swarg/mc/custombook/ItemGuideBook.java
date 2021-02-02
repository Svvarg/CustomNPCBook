package org.swarg.mc.custombook;

import java.util.List;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.world.World;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;


/**
 * 01-02-21
 *
 * @author Swarg
 */
public class ItemGuideBook extends Item {
    public static final String[] BOOKS = new String[]{"base", "rare" /*reserve*/}; //max size 15
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

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



    @Override
    public String getUnlocalizedName(ItemStack is) {
        final int meta = MathHelper.clamp_int(is.getItemDamage(), 0, /*15*/BOOKS.length);

        return super.getUnlocalizedName() + "."+ BOOKS[meta];
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister reg) {
        icons = new IIcon[BOOKS.length];
        for (int i = 0; i < icons.length; i++) {
            icons[i] = reg.registerIcon(CustomNPCBook.MODID + ":" + "guideBook_" + BOOKS[i]);

        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamage(int meta) {
        if (meta > this.icons.length) {
            meta = 0;
        }
        return this.icons[meta];
    }

    @SideOnly(Side.CLIENT)
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void getSubItems(Item item, CreativeTabs ct, List list) {
        for (int i = 0; i < BOOKS.length; i++) {
            list.add(new ItemStack(this, 1, i));
        }
    }

}
