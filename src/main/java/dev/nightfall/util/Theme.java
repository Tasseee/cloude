package dev.nightfall.util;

public final class Theme {
    public static final int BG_DIM      = 0x8C000000;
    public static final int PANEL       = 0xE6141418;
    public static final int HEADER_TOP  = 0xF0231E36;
    public static final int HEADER_BOT  = 0xF0171424;
    public static final int ROW_HOVER   = 0x30FFFFFF;
    public static final int ACCENT      = 0xFF8B5CF6;
    public static final int ACCENT_DIM  = 0xFF4C3290;
    public static final int ACCENT_BG   = 0x558B5CF6;
    public static final int TEXT        = 0xFFE8E8F0;
    public static final int TEXT_DIM    = 0xFF9A9AAE;
    public static final int MAP_BG      = 0x0E0E12;

    public static final int[] PALETTE = {0x8B5CF6, 0xEF4444, 0x22C55E, 0x3B82F6, 0xF59E0B, 0xEC4899, 0x14B8A6, 0xFFFFFF};

    private Theme() {}

    public static int withAlpha(int rgb, int a) { return (a << 24) | (rgb & 0xFFFFFF); }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
