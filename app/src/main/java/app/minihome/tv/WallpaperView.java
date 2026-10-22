package app.minihome.tv;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/** Visar en bakgrund (center-crop) och tonar mjukt över till nästa. */
public class WallpaperView extends View {
    private Bitmap cur, nxt;
    private float fade = 1f;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect src = new Rect();
    private final Rect dst = new Rect();
    private ValueAnimator anim;

    public WallpaperView(Context c) { super(c); }

    void show(Bitmap b) {
        if (b == null) return;
        if (cur == null) { cur = b; invalidate(); return; }
        if (anim != null) anim.cancel();
        if (nxt != null) { cur = nxt; nxt = null; }
        nxt = b;
        fade = 0f;
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1800);
        anim.addUpdateListener(a -> { fade = (float) a.getAnimatedValue(); invalidate(); });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                if (nxt != null && fade >= 1f) { cur = nxt; nxt = null; invalidate(); }
            }
        });
        anim.start();
    }

    @Override protected void onDraw(Canvas c) {
        if (cur != null) drawCover(c, cur, 255);
        if (nxt != null) drawCover(c, nxt, (int) (fade * 255));
    }

    private void drawCover(Canvas c, Bitmap b, int alpha) {
        int vw = getWidth(), vh = getHeight();
        if (vw == 0 || vh == 0) return;
        float s = Math.max(vw / (float) b.getWidth(), vh / (float) b.getHeight());
        int sw = Math.round(vw / s), sh = Math.round(vh / s);
        int sx = (b.getWidth() - sw) / 2, sy = (b.getHeight() - sh) / 2;
        src.set(sx, sy, sx + sw, sy + sh);
        dst.set(0, 0, vw, vh);
        paint.setAlpha(alpha);
        c.drawBitmap(b, src, dst, paint);
    }
}
