package com.android.launcher3.qsb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.core.view.ViewCompat;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.qsb.QsbContainerView;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;
import android.view.View;

public class QsbLayout extends FrameLayout {

    private ImageView micIcon;
    private ImageView gIcon;
    private ImageView lensIcon;
    private Context mContext;
    private FrameLayout inner;

    public QsbLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public QsbLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        micIcon = findViewById(R.id.mic_icon);
        gIcon = findViewById(R.id.g_icon);
        lensIcon = findViewById(R.id.lens_icon);
        inner = findViewById(R.id.inner);

        setUpMainSearch();
        setUpBackground();
        clipIconRipples();

        if (Utilities.isThemedIconsEnabled(mContext)) {
            micIcon.setImageResource(R.drawable.ic_mic_themed);
            gIcon.setImageResource(R.drawable.ic_super_g_themed);
            lensIcon.setImageResource(R.drawable.ic_lens_themed);
        } else {
            micIcon.setImageResource(R.drawable.ic_mic_color);
            gIcon.setImageResource(R.drawable.ic_super_g_color);
            lensIcon.setImageResource(R.drawable.ic_lens_color);
        }
        setupLensIcon();
    }

    private void clipIconRipples() {
        float cornerRadius = getCornerRadius();
        PaintDrawable pd = new PaintDrawable(Color.TRANSPARENT);
        pd.setCornerRadius(cornerRadius);
        micIcon.setClipToOutline(cornerRadius > 0);
        micIcon.setBackground(pd);
        lensIcon.setClipToOutline(cornerRadius > 0);
        lensIcon.setBackground(pd);
    }

    private void setUpBackground() {
        float cornerRadius = getCornerRadius();
        int color = Themes.getAttrColor(mContext, R.attr.qsbFillColor);
        if (Utilities.isThemedIconsEnabled(mContext))
            color = Themes.getColorBackgroundFloating(mContext);
        PaintDrawable pd = new PaintDrawable(color);
        pd.setCornerRadius(cornerRadius);
        inner.setClipToOutline(cornerRadius > 0);
        inner.setBackground(pd);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int requestedWidth = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        DeviceProfile dp = ActivityContext.lookupContext(mContext).getDeviceProfile();
        int cellWidth = DeviceProfile.calculateCellWidth(requestedWidth, dp.cellLayoutBorderSpacePx.x, dp.numShownHotseatIcons);
        int iconSize = (int)(Math.round((dp.iconSizePx * 0.92f)));
        int widthReduction = cellWidth - iconSize;
        int width = requestedWidth - widthReduction;
        setMeasuredDimension(width, height);

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child != null) {
                measureChildWithMargins(child, widthMeasureSpec, widthReduction, heightMeasureSpec, 0);
            }
        }
    }

    private void setUpMainSearch() {
        String searchPackage = QsbContainerView.getSearchWidgetPackageName(mContext);
        setOnClickListener(view -> {
            mContext.startActivity(new Intent("android.search.action.GLOBAL_SEARCH").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK).setPackage(searchPackage));
        });
    }

    private void setupLensIcon() {
        lensIcon.setOnClickListener(view -> {
            Intent lensIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("caller_package", Utilities.GSA_PACKAGE);
            bundle.putLong("start_activity_time_nanos", SystemClock.elapsedRealtimeNanos());
            lensIntent.setComponent(new ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setPackage(Utilities.GSA_PACKAGE)
                    .setData(Uri.parse(Utilities.LENS_URI))
                    .putExtra("lens_activity_params", bundle);
            mContext.startActivity(lensIntent);
        });
    }

    private float getCornerRadius() {
        Resources res = mContext.getResources();
        float qsbWidgetHeight = res.getDimension(R.dimen.qsb_widget_height);
        float qsbWidgetPadding = res.getDimension(R.dimen.qsb_widget_vertical_padding);
        float innerHeight = qsbWidgetHeight - 2 * qsbWidgetPadding;
        return (innerHeight / 2) * ((float)Utilities.getCornerRadius(mContext) / 100f);
    }
}
