package app.minihome.tv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.Toast;

final class Util {
    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static Intent launchIntent(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i != null) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    static void launch(Context c, String pkg) {
        Intent i = launchIntent(c.getPackageManager(), pkg);
        if (i == null) {
            Toast.makeText(c, "Kan inte starta appen", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            c.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(c, "Kan inte starta appen", Toast.LENGTH_SHORT).show();
        }
    }

    static Bitmap allAppsIcon() {
        int s = 192;
        Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF3A3F4B);
        cv.drawRoundRect(new RectF(0, 0, s, s), 40, 40, p);
        p.setColor(0xFFFFFFFF);
        float cell = s / 4f;
        for (int y = 1; y <= 3; y++)
            for (int x = 1; x <= 3; x++)
                cv.drawCircle(x * cell, y * cell, 11, p);
        return b;
    }
}
