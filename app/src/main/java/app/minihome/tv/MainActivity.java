package app.minihome.tv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final long ROTATE_MS = 10 * 60 * 1000L;
    private static final int COLS = 6;

    /** ScrollView som låter oss styra scrollen själva (som tvOS). */
    static final class HomeScroll extends ScrollView {
        HomeScroll(Context c) { super(c); }
        @Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) { return 0; }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private WallpaperSource source;
    private WallpaperView wall;
    private View dim;
    private HomeScroll scroll;
    private LinearLayout content, dockRow;
    private TextView pill, caption;
    private int sw, sh, gap, marginX, tileW, tileH;
    private String lastFocusKey;
    private long lastBuild;
    private boolean dirty = true;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            pill.setText(android.text.format.DateFormat.getTimeFormat(MainActivity.this)
                    .format(new java.util.Date()));
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

        DisplayMetrics dm = getResources().getDisplayMetrics();
        sw = dm.widthPixels;
        sh = dm.heightPixels;
        marginX = Math.round(sw * 0.055f);
        gap = Math.round(sw * 0.012f);
        tileW = (sw - 2 * marginX - (COLS - 1) * gap) / COLS;
        tileH = Math.round(tileW * 9f / 16f);

        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);

        wall = new WallpaperView(this);
        root.addView(wall, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, 0xCC000000}));
        root.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(sh * 0.62f), Gravity.BOTTOM));

        dim = new View(this);
        dim.setBackgroundColor(Color.BLACK);
        dim.setAlpha(0f);
        root.addView(dim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        scroll = new HomeScroll(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        caption = new TextView(this);
        caption.setTextColor(0xE6FFFFFF);
        caption.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        caption.setLetterSpacing(0.3f);
        FrameLayout.LayoutParams capLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        capLp.setMargins(marginX, Util.dp(this, 36), 0, 0);
        root.addView(caption, capLp);

        pill = new TextView(this);
        pill.setTextColor(Color.WHITE);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        int ph = Util.dp(this, 16), pv = Util.dp(this, 6);
        pill.setPadding(ph, pv, ph, pv);
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(0x66000000);
        pillBg.setCornerRadius(Util.dp(this, 40));
        pill.setBackground(pillBg);
        FrameLayout.LayoutParams pillLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        pillLp.setMargins(0, Util.dp(this, 32), marginX, 0);
        root.addView(pill, pillLp);

        scroll.setOnScrollChangeListener((v, x, y, ox, oy) -> {
            float t = Math.min(1f, y / (sh * 0.35f));
            dim.setAlpha(0.7f * t);
            caption.setAlpha(1f - Math.min(1f, y / (sh * 0.2f)));
        });

        setContentView(root);
        loadNextWallpaper();
    }

    @Override protected void onResume() {
        super.onResume();
        if (dirty || System.currentTimeMillis() - lastBuild > 60000) rebuild();
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
        // Hem-knappen: tillbaka till dockan, högst upp, med ny bakgrund.
        lastFocusKey = null;
        scroll.scrollTo(0, 0);
        restoreFocus();
        loadNextWallpaper();
    }

    @Override public void onBackPressed() { /* startskärmen stängs inte */ }

    private void loadNextWallpaper() {
        try {
            bg.execute(() -> {
                final WallpaperSource.Wall w = source.next();
                runOnUiThread(() -> {
                    wall.show(w.bmp);
                    caption.setText(w.name.toUpperCase(Locale.getDefault()));
                });
            });
        } catch (Exception ignored) { }
    }

    private Map<String, ResolveInfo> queryApps(PackageManager pm) {
        Map<String, ResolveInfo> m = new LinkedHashMap<>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String p = ri.activityInfo.packageName;
                if (!p.equals(getPackageName()) && !m.containsKey(p)) m.put(p, ri);
            }
        }
        return m;
    }

    private List<TvInputInfo> hdmiInputs() {
        List<TvInputInfo> out = new ArrayList<>();
        try {
            TvInputManager tm = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (tm != null) {
                for (TvInputInfo i : tm.getTvInputList()) {
                    if (i.getType() == TvInputInfo.TYPE_HDMI && i.isPassthroughInput()) out.add(i);
                }
            }
        } catch (Throwable ignored) { }
        return out;
    }

    private LinearLayout newRow(boolean isDock) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (isDock) {
            row.setPadding(gap, gap, gap, gap);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x73202228);
            bg.setCornerRadius(tileH * 0.4f);
            bg.setStroke(Math.max(1, sw / 1920), 0x26FFFFFF);
            row.setBackground(bg);
            lp.setMargins(marginX - gap, 0, marginX - gap, 0);
        } else {
            lp.setMargins(marginX, gap, marginX, 0);
        }
        content.addView(row, lp);
        return row;
    }

    private LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, gap, 0);
        return lp;
    }

    private Tile appTile(PackageManager pm, ResolveInfo ri) {
        final String pkg = ri.activityInfo.packageName;
        Drawable banner = null;
        try { banner = ri.activityInfo.loadBanner(pm); } catch (Throwable ignored) { }
        Drawable icon = banner == null ? ri.loadIcon(pm) : null;
        final Tile t = new Tile(this, tileW, tileH, banner, icon, ri.loadLabel(pm).toString(), null);
        t.setTag(pkg);
        t.setOnClickListener(v -> Util.launch(this, pkg));
        t.setOnLongClickListener(v -> { toggleFav(pkg); return true; });
        t.setOnTileFocus(() -> onTileFocus(t));
        return t;
    }

    private Tile inputTile(final TvInputInfo info) {
        CharSequence name = null;
        try { name = info.loadCustomLabel(this); } catch (Throwable ignored) { }
        if (name == null || name.length() == 0) name = info.loadLabel(this);
        final Tile t = new Tile(this, tileW, tileH, null, null, String.valueOf(name), "Ingång");
        t.setTag("input:" + info.getId());
        t.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        TvContract.buildChannelUriForPassthroughInput(info.getId()))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Toast.makeText(this, "Använd Källa-knappen på fjärrkontrollen", Toast.LENGTH_LONG).show();
            }
        });
        t.setOnTileFocus(() -> onTileFocus(t));
        return t;
    }

    private void onTileFocus(Tile t) {
        Object k = t.getTag();
        lastFocusKey = k == null ? null : k.toString();
        View row = (View) t.getParent();
        int target = row == dockRow ? 0 : Math.max(0, row.getTop() - Math.round(sh * 0.16f));
        scroll.smoothScrollTo(0, target);
    }

    private void toggleFav(String pkg) {
        int r = Prefs.toggleWithLimit(this, pkg);
        if (r < 0) {
            Toast.makeText(this, "Dockan är full (max " + Prefs.MAX_FAVS + ")", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, r == 1 ? "Tillagd i dockan" : "Borttagen från dockan",
                Toast.LENGTH_SHORT).show();
        lastFocusKey = pkg;
        rebuild();
    }

    private void restoreFocus() {
        View target = null;
        if (lastFocusKey != null) target = content.findViewWithTag(lastFocusKey);
        if (target == null && dockRow != null && dockRow.getChildCount() > 0) target = dockRow.getChildAt(0);
        if (target != null) target.requestFocus();
        else content.requestFocus(View.FOCUS_DOWN);
    }

    private void rebuild() {
        dirty = false;
        lastBuild = System.currentTimeMillis();
        final PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> map = queryApps(pm);
        content.removeAllViews();

        content.addView(new View(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(sh * 0.50f)));

        dockRow = newRow(true);
        List<String> favs = new ArrayList<>();
        for (String p : Prefs.getFavs(this)) {
            if (map.containsKey(p) && favs.size() < Prefs.MAX_FAVS) favs.add(p);
        }
        for (String p : favs) dockRow.addView(appTile(pm, map.get(p)), tileLp());

        List<TvInputInfo> inputs = hdmiInputs();
        if (!inputs.isEmpty()) {
            LinearLayout inRow = newRow(false);
            for (TvInputInfo ti : inputs) inRow.addView(inputTile(ti), tileLp());
        }

        List<ResolveInfo> others = new ArrayList<>();
        for (Map.Entry<String, ResolveInfo> e : map.entrySet()) {
            if (!favs.contains(e.getKey())) others.add(e.getValue());
        }
        Collections.sort(others, (a, c) ->
                a.loadLabel(pm).toString().compareToIgnoreCase(c.loadLabel(pm).toString()));
        LinearLayout row = null;
        for (int i = 0; i < others.size(); i++) {
            if (i % COLS == 0) row = newRow(false);
            row.addView(appTile(pm, others.get(i)), tileLp());
        }

        content.addView(new View(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(sh * 0.45f)));
        scroll.post(this::restoreFocus);
    }
}
