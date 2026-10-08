package com.dowwrey.beds.storage;

/** Small vanilla-inspired palette used for the saved-bed accent line. */
public final class BedColors {
    public static final int DEFAULT = 0xFF6E9FCF;

    public static final int[] PALETTE = {
            0xFFD0B66A, // warm gold
            0xFF7FA35A, // moss green
            0xFF6E9FCF, // sky blue
            0xFF5F79B8, // blue
            0xFF9A75B7, // amethyst
            0xFFC36B5F, // terracotta red
            0xFFD0D0D0, // light stone
            0xFF7C868A  // slate
    };

    private BedColors() {}

    public static boolean isAllowed(int color) {
        for (int paletteColor : PALETTE) {
            if (paletteColor == color) {
                return true;
            }
        }
        return false;
    }
}
