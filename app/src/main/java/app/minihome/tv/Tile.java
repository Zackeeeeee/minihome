package app.minihome.tv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Bred rundad bricka i tvOS-stil: växer, får vit ram och visar namn när den har fokus. */
final class Tile extends LinearLayout {
    private final FrameLayout card;
    private final TextView label;
    private final GradientDrawable ring;
    private Runnable onFocused;

    Tile(Context c, int w, int h, Drawable banner, Drawable icon, String title, String sub) {
        super(c);
        setOrientation(VERTICAL);
        setFocusable(true);
        setClickable(true);
        setClipChildren(false);
        setClipToPadding(false);

        final int r = Math.round(h * 0.14f);
        card = new FrameLayout(c);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        card.setClipToOutline(true);
        card.setElevation(Util.dp(c, 3));

        if (banner != null) {
            ImageView iv = new ImageView(c);
            iv.setImageDrawable(banner);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            card.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF3A4150, 0xFF1B1F2A}));
            if (icon != null) {
                ImageView iv = new ImageView(c);
                iv.setImageDrawable(icon);
                int s = Math.round(h * 0.58f);
                card.addView(iv, new FrameLayout.LayoutParams(s, s, Gravity.CENTER));
            } else {
                TextView t = new TextView(c);
                t.setText(title);
                t.setTextColor(Color.WHITE);
                t.setTypeface(Typeface.DEFAULT_BOLD);
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                t.setGravity(Gravity.CENTER);
                card.addView(t, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }

        ring = new GradientDrawable();
        ring.setColor(Color.TRANSPARENT);
        ring.setCornerRadius(r);
        ring.setStroke(0, Color.WHITE);
        card.setForeground(ring);
        addView(card, new LinearLayout.LayoutParams(w, h));

        label = new TextView(c);
        label.setText(banner == null && icon == null ? (sub == null ? "" : sub) : title);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                w, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Util.dp(c, 8);
        addView(label, lp);

        setOnFocusChangeListener((v, has) -> {
            float s = has ? 1.1f : 1f;
            card.animate().scaleX(s).scaleY(s)
                    .translationZ(has ? Util.dp(c, 18) : 0).setDuration(140).start();
            label.animate().alpha(has ? 1f : 0f).setDuration(140).start();
            ring.setStroke(has ? Math.max(2, h / 40) : 0, Color.WHITE);
            if (has && onFocused != null) onFocused.run();
        });
    }

    void setOnTileFocus(Runnable r) { onFocused = r; }
}
