package com.dowwrey.beds.storage;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Small, stable icon catalog for saved beds. The ids are persisted instead of enum ordinals. */
public final class BedIcons {
    public static final int DEFAULT = 0;

    private static final Entry[] ENTRIES = {
            new Entry("house", Items.OAK_DOOR),
            new Entry("mine", Items.IRON_PICKAXE),
            new Entry("village", Items.EMERALD),
            new Entry("castle", Items.STONE_BRICKS),
            new Entry("farm", Items.WHEAT),
            new Entry("camp", Items.CAMPFIRE),
            new Entry("portal", Items.OBSIDIAN),
            new Entry("tree", Items.OAK_SAPLING),
            new Entry("nether", Items.NETHER_BRICKS),
            new Entry("end", Items.ENDER_EYE)
    };

    public record Entry(String id, net.minecraft.world.item.Item item) {}

    private BedIcons() {}

    public static int count() { return ENTRIES.length; }

    public static boolean isAllowed(int icon) { return icon >= 0 && icon < ENTRIES.length; }

    public static int sanitize(int icon) { return isAllowed(icon) ? icon : DEFAULT; }

    public static String id(int icon) { return ENTRIES[sanitize(icon)].id(); }

    public static ItemStack itemStack(int icon) { return new ItemStack(ENTRIES[sanitize(icon)].item()); }

    public static String translationKey(int icon) { return "dowwrey_beds.icon." + id(icon); }
}
