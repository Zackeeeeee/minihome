package app.minihome.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AllAppsActivity extends Activity {
    private final List<ResolveInfo> apps = new ArrayList<>();
    private List<String> favs;
    private PackageManager pm;
    private BaseAdapter adapter;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        pm = getPackageManager();
        getWindow().setBackgroundDrawable(new ColorDrawable(0xFF101014));
        favs = Prefs.getFavs(this);
        load();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Util.dp(this, 48);
        root.setPadding(pad, Util.dp(this, 32), pad, Util.dp(this, 16));

        TextView title = new TextView(this);
        title.setText("Alla appar");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Håll in OK på en app för att lägga till eller ta bort den i dockan (★ = finns i dockan)");
        hint.setTextColor(0x99FFFFFF);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hint.setPadding(0, Util.dp(this, 4), 0, Util.dp(this, 16));
        root.addView(hint);

        GridView grid = new GridView(this);
        grid.setNumColumns(6);
        grid.setVerticalSpacing(Util.dp(this, 8));
        grid.setHorizontalSpacing(Util.dp(this, 8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(new ColorDrawable(0x33FFFFFF));
        grid.setClipToPadding(false);

        adapter = new BaseAdapter() {
            @Override public int getCount() { return apps.size(); }
            @Override public Object getItem(int i) { return apps.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View cv, ViewGroup parent) {
                ResolveInfo ri = apps.get(i);
                LinearLayout cell = new LinearLayout(AllAppsActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Util.dp(AllAppsActivity.this, 130)));
                ImageView iv = new ImageView(AllAppsActivity.this);
                iv.setImageDrawable(ri.loadIcon(pm));
                cell.addView(iv, new LinearLayout.LayoutParams(
                        Util.dp(AllAppsActivity.this, 72), Util.dp(AllAppsActivity.this, 72)));
                TextView tv = new TextView(AllAppsActivity.this);
                String name = ri.loadLabel(pm).toString();
                boolean fav = favs.contains(ri.activityInfo.packageName);
                tv.setText((fav ? "★ " : "") + name);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                tv.setSingleLine(true);
                tv.setGravity(Gravity.CENTER);
                cell.addView(tv, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return cell;
            }
        };
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((p, v, pos, id) ->
                Util.launch(this, apps.get(pos).activityInfo.packageName));
        grid.setOnItemLongClickListener((p, v, pos, id) -> {
            String pkg = apps.get(pos).activityInfo.packageName;
            boolean added = Prefs.toggle(this, pkg);
            favs = Prefs.getFavs(this);
            Toast.makeText(this, added ? "Tillagd i dockan" : "Borttagen från dockan",
                    Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
            return true;
        });
        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void load() {
        Map<String, ResolveInfo> m = new LinkedHashMap<>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                if (!m.containsKey(pkg)) m.put(pkg, ri);
            }
        }
        apps.addAll(m.values());
        Collections.sort(apps, (a, c) ->
                a.loadLabel(pm).toString().compareToIgnoreCase(c.loadLabel(pm).toString()));
    }
}
