package app.minihome.tv;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

final class Prefs {
    static final int MAX_FAVS = 6;
    // Standard-dock. Ändras i appen genom att hålla in OK på en app i "Alla appar".
    static final String[] DEFAULT_FAVS = {
        "com.netflix.ninja",
        "io.gh.reisxd.tizentube.cobalt",
        "com.sportzx.live",
        "com.esaba.downloader",
        "com.phlox.tvwebbrowser"
    };

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("mini", Context.MODE_PRIVATE);
    }

    static List<String> getFavs(Context c) {
        String s = sp(c).getString("favs", null);
        List<String> out = new ArrayList<>();
        if (s == null) {
            for (String d : DEFAULT_FAVS) out.add(d);
            return out;
        }
        for (String p : s.split(",")) if (!p.isEmpty()) out.add(p);
        return out;
    }

    static void setFavs(Context c, List<String> favs) {
        StringBuilder b = new StringBuilder();
        for (String f : favs) {
            if (b.length() > 0) b.append(',');
            b.append(f);
        }
        sp(c).edit().putString("favs", b.toString()).apply();
    }

    /** Returnerar true om appen lades till, false om den togs bort. */
    static boolean toggle(Context c, String pkg) {
        List<String> f = getFavs(c);
        boolean added;
        if (f.contains(pkg)) { f.remove(pkg); added = false; }
        else { f.add(pkg); added = true; }
        setFavs(c, f);
        return added;
    }

    /** 1 = tillagd, 0 = borttagen, -1 = dockan full. */
    static int toggleWithLimit(Context c, String pkg) {
        List<String> f = getFavs(c);
        if (f.contains(pkg)) { f.remove(pkg); setFavs(c, f); return 0; }
        if (f.size() >= MAX_FAVS) return -1;
        f.add(pkg);
        setFavs(c, f);
        return 1;
    }
}
