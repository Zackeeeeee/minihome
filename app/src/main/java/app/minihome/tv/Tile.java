package app.minihome.tv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** En dock-ikon som växer och visar namn när den har fokus. */
final class Tile extends LinearLayout {
    private final TextView label;

    Tile(Context c, Drawable icon, String text) {
        super(c);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setFocusable(true);
        setClickable(true);
        setClipChildren(false);
        int pad = Util.dp(c, 8);
        setPadding(pad, pad, pad, pad);

        ImageView iv = new ImageView(c);
        iv.setImageDrawable(icon);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(iv, new LinearLayout.LayoutParams(Util.dp(c, 96), Util.dp(c, 96)));

        label = new TextView(c);
        label.setText(text);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                Util.dp(c, 110), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Util.dp(c, 4);
        addView(label, lp);

        final GradientDrawable focusBg = new GradientDrawable();
        focusBg.setColor(0x33FFFFFF);
        focusBg.setCornerRadius(Util.dp(c, 20));

        setOnFocusChangeListener((v, has) -> {
            float s = has ? 1.2f : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(140).start();
            label.animate().alpha(has ? 1f : 0f).setDuration(140).start();
            v.setBackground(has ? focusBg : null);
        });
    }
}
