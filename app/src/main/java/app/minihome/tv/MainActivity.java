package app.minihome.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final long ROTATE_MS = 10 * 60 * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private WallpaperView wall;
    private WallpaperSource source;
    private LinearLayout dock;
    private TextView clock, date;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 15000);
        }
    };
    private final Runnable rotate = new Runnable() {
        @Override public void run() {
            loadNextWallpaper();
            handler.postDelayed(this, ROTATE_MS);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        source = new WallpaperSource(this);

        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);

        wall = new WallpaperView(this);
        root.addView(wall, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, 0xB0000000}));
        root.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(this, 320), Gravity.BOTTOM));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56);
        date = new TextView(this);
        date.setTextColor(0xCCFFFFFF);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        top.addView(clock);
        top.addView(date);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        topLp.setMargins(Util.dp(this, 64), Util.dp(this, 36), 0, 0);
        root.addView(top, topLp);

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        int pad = Util.dp(this, 14);
        dock.setPadding(pad, pad, pad, pad);
        GradientDrawable dockBg = new GradientDrawable();
        dockBg.setColor(0x66000000);
        dockBg.setCornerRadius(Util.dp(this, 32));
        dock.setBackground(dockBg);
        FrameLayout.LayoutParams dockLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dockLp.setMargins(0, 0, 0, Util.dp(this, 48));
        root.addView(dock, dockLp);

        setContentView(root);
        loadNextWallpaper();
    }

    @Override protected void onResume() {
        super.onResume();
        rebuildDock();
        handler.removeCallbacks(clockTick);
        handler.removeCallbacks(rotate);
        handler.post(clockTick);
        handler.postDelayed(rotate, ROTATE_MS);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockTick);
        handler.removeCallbacks(rotate);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        bg.shutdownNow();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Hem-knappen trycktes: tillbaka till dockan och ny bakgrund.
        if (dock.getChildCount() > 0) dock.getChildAt(0).requestFocus();
        loadNextWallpaper();
    }

    @Override public void onBackPressed() {
        // Startskärmen ska inte kunna stängas.
    }

    private void updateClock() {
        Date now = new Date();
        clock.setText(android.text.format.DateFormat.getTimeFormat(this).format(now));
        String d = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now);
        if (!d.isEmpty()) d = Character.toUpperCase(d.charAt(0)) + d.substring(1);
        date.setText(d);
    }

    private void loadNextWallpaper() {
        try {
            bg.execute(() -> {
                final android.graphics.Bitmap bmp = source.next();
                runOnUiThread(() -> wall.show(bmp));
            });
        } catch (Exception ignored) { }
    }

    private LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = Util.dp(this, 6);
        lp.setMargins(m, 0, m, 0);
        return lp;
    }

    private void rebuildDock() {
        PackageManager pm = getPackageManager();
        int focusIdx = 0;
        View f = getCurrentFocus();
        if (f != null && f.getParent() == dock) focusIdx = dock.indexOfChild(f);
        dock.removeAllViews();

        for (final String pkg : Prefs.getFavs(this)) {
            if (Util.launchIntent(pm, pkg) == null) continue; // ej installerad
            Drawable icon;
            String label;
            try {
                icon = pm.getApplicationIcon(pkg);
                label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            Tile t = new Tile(this, icon, label);
            t.setOnClickListener(v -> Util.launch(this, pkg));
            t.setOnLongClickListener(v -> {
                Prefs.toggle(this, pkg);
                Toast.makeText(this, "Borttagen från dockan", Toast.LENGTH_SHORT).show();
                rebuildDock();
                return true;
            });
            dock.addView(t, tileLp());
        }

        Tile all = new Tile(this, new BitmapDrawable(getResources(), Util.allAppsIcon()), "Alla appar");
        all.setOnClickListener(v -> startActivity(new Intent(this, AllAppsActivity.class)));
        dock.addView(all, tileLp());

        int idx = Math.max(0, Math.min(focusIdx, dock.getChildCount() - 1));
        dock.getChildAt(idx).requestFocus();
    }
}
