package app.minihome.tv;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import java.util.Random;

/** Ritar naturbilder (berg, himmel, sol/måne) i koden. Inga bildfiler, inga upphovsrättsfrågor. */
final class WallpaperGenerator {
    // skyTop, skyBottom, sun, mountainDark, haze
    private static final int[][] T = {
        {0xFF2B2D5B, 0xFFF59A6B, 0xFFFFE2B0, 0xFF1B1830, 0xFFE9A07F}, // gryning
        {0xFF1E5AA8, 0xFFBFE3F6, 0xFFFFFFFF, 0xFF1F3D4D, 0xFFCDE7F2}, // alpin dag
        {0xFF2A1B4D, 0xFFE0627A, 0xFFFFC38A, 0xFF1A1030, 0xFFB86A9A}, // skymning
        {0xFF050818, 0xFF1B2A55, 0xFFE8EEFF, 0xFF05070F, 0xFF28386B}, // natt
        {0xFF4C6A5E, 0xFFC9D7CC, 0xFFF2F2E0, 0xFF1D2F27, 0xFFB5C6BA}, // skogsdimma
        {0xFF0F3E5A, 0xFFFFB36B, 0xFFFFE9B8, 0xFF0A1F2E, 0xFF6FA3A8}, // havssolnedgang
    };
    private static final int NIGHT = 3;
    private static final int N = 2048;

    static Bitmap make(int index, int w, int h) {
        int theme = Math.floorMod(index, T.length);
        int[] t = T[theme];
        Random r = new Random(1000L + index * 7919L);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setShader(new LinearGradient(0, 0, 0, h * 0.75f, t[0], t[1], Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        if (theme == NIGHT) {
            p.setColor(Color.WHITE);
            for (int i = 0; i < 220; i++) {
                p.setAlpha(60 + r.nextInt(170));
                c.drawCircle(r.nextFloat() * w, r.nextFloat() * h * 0.55f, 0.8f + r.nextFloat() * 1.6f, p);
            }
            p.setAlpha(255);
        }

        float sx = w * (0.2f + 0.6f * r.nextFloat());
        float sy = h * (theme == NIGHT ? 0.2f : 0.38f);
        float sr = h * (theme == NIGHT ? 0.05f : 0.09f);
        int rgb = t[2] & 0x00FFFFFF;
        p.setShader(new RadialGradient(sx, sy, sr * 5f, 0x66000000 | rgb, rgb, Shader.TileMode.CLAMP));
        c.drawCircle(sx, sy, sr * 5f, p);
        p.setShader(null);
        p.setColor(t[2]);
        c.drawCircle(sx, sy, sr, p);

        int layers = 5;
        for (int L = 0; L < layers; L++) {
            float[] ridge = ridge(r, N, 0.55f);
            float f = L / (float) (layers - 1);
            float baseY = h * (0.52f + 0.11f * L);
            float amp = h * (0.30f - 0.035f * L);
            int col = mix(t[4], t[3], 0.15f + 0.85f * f);
            int bottom = mix(col, t[3], 0.55f);
            Path path = new Path();
            path.moveTo(0, h);
            for (int x = 0; x <= w; x += 4) {
                int idx = Math.min(N, Math.round(x * (float) N / w));
                path.lineTo(x, baseY - ridge[idx] * amp);
            }
            path.lineTo(w, h);
            path.close();
            p.setShader(new LinearGradient(0, baseY - amp, 0, h, col, bottom, Shader.TileMode.CLAMP));
            c.drawPath(path, p);
        }
        p.setShader(null);
        return bmp;
    }

    /** Midpoint displacement, normaliserad till 0..1. */
    private static float[] ridge(Random r, int n, float rough) {
        float[] a = new float[n + 1];
        a[0] = r.nextFloat();
        a[n] = r.nextFloat();
        float disp = 1f;
        for (int step = n; step > 1; step /= 2) {
            for (int i = 0; i + step <= n; i += step) {
                int mid = i + step / 2;
                a[mid] = (a[i] + a[i + step]) / 2f + (r.nextFloat() - 0.5f) * disp;
            }
            disp *= rough;
        }
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (float v : a) { if (v < min) min = v; if (v > max) max = v; }
        float range = Math.max(0.0001f, max - min);
        for (int i = 0; i < a.length; i++) a[i] = (a[i] - min) / range;
        return a;
    }

    private static int mix(int a, int b, float t) {
        int rr = (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int gg = (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bb = (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.argb(255, rr, gg, bb);
    }
}
