package org.swarg.mc.custombook.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * 02-02-21
 *
 * @author Swarg Convert BigBook from BiblioCraft to Dialogs
 */
public class BookConverter {

    //private boolean debug = true;

    public BookConverter() {
    }


    /**
     * Converts the content of BiblioCraft BigBook to the raw text
     * nbt to text
     * @param book
     * @return
     */
    public String convertBigBookToText(ItemStack book) {
        if (book != null && book.stackTagCompound != null) {
            int       totalPages;
            int[]     chapterPageNums = null;
            boolean[] chapterBool = new boolean[8];
            String[]  chapterNames = new String[16];

            NBTTagCompound nbt = book.getTagCompound();
            if (nbt != null) {

                totalPages = nbt.getInteger("pagesTotal");
                ///*DEBUG*/if (debug) {System.out.println("totalPages:"+totalPages);}
                
                //currentPage = nbt.getInteger("pagesCurrent");
                //String signedAuthor = nbt.getString("author");
                //boolean signed = nbt.getBoolean("signed");
                //if(totalPages == 0) {totalPages = 1;}

                //CHAPTERS
                NBTTagCompound nbtChapters = nbt.getCompoundTag("chapters");
                if (nbtChapters != null) {
                    chapterPageNums = nbtChapters.getIntArray("chapPages");

                    final int[] chapBools = nbtChapters.getIntArray("chapBools");

                    for (int n = 0; n < 8; ++n) {
                        chapterBool[n] = chapBools[n] == 1;
                    }

                    for (int n = 0; n < 16; ++n) {
                        chapterNames[n] = nbtChapters.getString("chapline" + n);
                    }
                }//chapters


                //PAGES>>
                NBTTagCompound pagesTag = nbt.getCompoundTag("pages");
                if (pagesTag != null) {
                    //String[] lineTexts = new String[44];

                    StringBuilder sb = new StringBuilder();
                    boolean hasChapters = chapterPageNums != null;
                    int chi = 0;

                    //pageindex
                    for (int pi = 0; pi < totalPages && pi < 256; pi++) {
                        //chapter transfer
                        if (hasChapters && chi < 8) {
                            for (int i = 0; i < chapterPageNums.length; i++) {
                                if (chapterBool[i] && chapterPageNums[i] == pi) {

                                    chapterBool[i] = false;//perfomed
                                    final int f = i * 2;
                                    //chapter
                                    sb.append('\n').append(chapterNames[f]).append(' ').append(chapterNames[f+1]).append('\n');
                                    ///*DEBUG*/if (debug) {System.out.println("Chapter: " + i);}
                                    chi++;
                                    break;//once
                                }
                            }
                        }

                        // page line transfer
                        NBTTagList pageLines = pagesTag.getTagList("page" + pi, 8);//currentPage
                        if (pageLines != null) {
                            final int sz = pageLines.tagCount();
                            ///*DEBUG*/if (debug) {System.out.println("PageLines: " + sz);}
                            for (int i = 0; i < sz; ++i) {//44
                                String line /*lineTexts[i]*/ = pageLines.getStringTagAt(i);
                                if (line != null && !line.isEmpty()) {
                                    sb.append(line)
                                      .append(' '); // '\n'
                                }
                            }
                        }
                    }

                    return sb.toString();
                }
            }
        }
        return null;
    }

}
