package dev.pvpplus.client;

import net.minecraft.client.gui.DrawContext;

/** Оформление меню в стиле тёмной фиолетовой панели. */
public final class Ui {
    public static final int PANEL = 0xE0141226;
    public static final int ROW = 0xFF1C1A32;
    public static final int ROW_SEL = 0xFF2A2650;
    public static final int ACCENT = 0xFF6C5CE7;
    public static final int TEXT = 0xFFE8E8F4;
    public static final int DIM = 0xFF8C8AA6;
    public static final int OFF = 0xFF3A3852;
    public static final int EDGE = 0x40A898FF;

    public static void rrect(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col);
        c.fill(x, y + 1, x + w, y + h - 1, col);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
    }

    public static void panel(DrawContext c, int x, int y, int w, int h) {
        rrect(c, x - 1, y - 1, w + 2, h + 2, EDGE);
        rrect(c, x, y, w, h, PANEL);
    }

    public static void toggle(DrawContext c, int x, int y, boolean on) {
        rrect(c, x, y, 20, 10, on ? ACCENT : OFF);
        rrect(c, on ? x + 11 : x + 1, y + 1, 8, 8, 0xFFFFFFFF);
    }

    /** Значок "пульс" рядом с названием. */
    public static void logo(DrawContext c, int x, int y, int color) {
        int[][] pts = {{0, 6}, {5, 6}, {7, 1}, {10, 11}, {13, 3}, {15, 6}, {20, 6}};
        for (int i = 0; i + 1 < pts.length; i++) {
            Gfx.line(c, x + pts[i][0], y + pts[i][1], x + pts[i + 1][0], y + pts[i + 1][1], color);
            Gfx.line(c, x + pts[i][0], y + pts[i][1] + 1, x + pts[i + 1][0], y + pts[i + 1][1] + 1, color);
        }
    }
}
