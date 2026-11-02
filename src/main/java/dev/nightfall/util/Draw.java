package dev.nightfall.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import org.joml.Matrix3x2fStack;

public final class Draw {
    /** Oberkante/Mitte der zuletzt gezeichneten Box (fuer Labels). */
    public static float topX, topY, botY;
    private static final double[][] C = new double[8][2];
    private static final boolean[] OK = new boolean[8];

    private Draw() {}

    public static void line(DrawContext ctx, float x1, float y1, float x2, float y2, int argb, int th) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f || len > 20000f) return;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x1, y1);
        m.rotate((float) Math.atan2(dy, dx));
        int half = th / 2;
        ctx.fill(0, -half, (int) Math.ceil(len), th - half, argb);
        m.popMatrix();
    }

    /** Zeichnet eine Box als 3D-Kanten oder als 2D-Rahmen. @return true wenn etwas sichtbar war. */
    public static boolean box(DrawContext ctx, Box b, int argb, int th, boolean threeD) {
        int any = 0;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean all = true;
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? b.minX : b.maxX;
            double y = (i & 2) == 0 ? b.minY : b.maxY;
            double z = (i & 4) == 0 ? b.minZ : b.maxZ;
            OK[i] = Projection.project(x, y, z, C[i]);
            if (OK[i]) {
                any++;
                minX = Math.min(minX, (float) C[i][0]); maxX = Math.max(maxX, (float) C[i][0]);
                minY = Math.min(minY, (float) C[i][1]); maxY = Math.max(maxY, (float) C[i][1]);
            } else all = false;
        }
        if (any == 0) return false;
        topX = (minX + maxX) / 2f; topY = minY; botY = maxY;
        if (threeD) {
            for (int i = 0; i < 8; i++) {
                if (!OK[i]) continue;
                for (int bit = 1; bit <= 4; bit <<= 1) {
                    if ((i & bit) != 0) continue;
                    int j = i | bit;
                    if (OK[j]) line(ctx, (float) C[i][0], (float) C[i][1], (float) C[j][0], (float) C[j][1], argb, th);
                }
            }
            return true;
        }
        if (!all) return false;
        int x0 = Math.round(minX), y0 = Math.round(minY), x1 = Math.round(maxX), y1 = Math.round(maxY);
        ctx.fill(x0, y0, x1, y0 + th, argb);
        ctx.fill(x0, y1 - th, x1, y1, argb);
        ctx.fill(x0, y0, x0 + th, y1, argb);
        ctx.fill(x1 - th, y0, x1, y1, argb);
        return true;
    }

    public static void centeredText(DrawContext ctx, net.minecraft.client.font.TextRenderer tr, String s, float cx, float y, int argb) {
        ctx.drawText(tr, s, Math.round(cx - tr.getWidth(s) / 2f), Math.round(y), argb, true);
    }
}
