package app.minihome.tv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * Bakgrundskälla. Ligger egna bilder i mappen
 *   /sdcard/Android/data/app.minihome.tv/files/walls/
 * roterar de, annars ritas inbyggda naturbilder. Anropas utanför huvudtråden.
 */
final class WallpaperSource {
    private static final int W = 1920, H = 1080;
    private final Context ctx;
    private int index = new Random().nextInt(100);

    WallpaperSource(Context c) { ctx = c.getApplicationContext(); }

    private File[] userFiles() {
        File d = ctx.getExternalFilesDir("walls");
        if (d == null) return new File[0];
        if (!d.exists()) d.mkdirs();
        File[] fs = d.listFiles((dir, name) -> {
            String n = name.toLowerCase(Locale.ROOT);
            return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp");
        });
        if (fs == null) return new File[0];
        Arrays.sort(fs);
        return fs;
    }

    Bitmap next() {
        int i = index++;
        File[] fs = userFiles();
        if (fs.length > 0) {
            Bitmap b = decode(fs[i % fs.length]);
            if (b != null) return b;
        }
        return WallpaperGenerator.make(i, W, H);
    }

    private Bitmap decode(File f) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int s = 1;
            while (o.outWidth / (s * 2) >= W && o.outHeight / (s * 2) >= H) s *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = s;
            o2.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        } catch (Throwable t) {
            return null;
        }
    }
}
