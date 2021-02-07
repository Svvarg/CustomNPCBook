package org.swarg.mc.custombook;

import net.minecraft.init.Items;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

/**
 * 02-02-21
 * @author Swarg
 */
public class CustomBooksAchievements {

    public static Achievement aOpenGuideBook;
    public static Achievement aQuitFromSpawn;

    //used for fix client side crash
    //public static final Achievement BLOCKED = new Achievement("achievement.blocked", "?", 0, 0, Items.bone, null);

    public static void register () {
        aOpenGuideBook = new Achievement("achievement.OpenGuideBook", "OpenGuideBook", 0, -1, ItemCustomBook.customBook, null).registerStat();
        aQuitFromSpawn = new Achievement("achievement.QuitFromSpawn", "QuitFromSpawn", -1, 0, Items.leather_boots, null).registerStat();//todo sync with "multiverse-core" plugin dimension tp
        
	//pageBiome = new AchievementPage("TerraFirmaCraft", achievementsTFC);
	//AchievementPage.registerAchievementPage(pageBiome);
        //net.minecraft.client.gui.achievement.GuiStats
    }
}
