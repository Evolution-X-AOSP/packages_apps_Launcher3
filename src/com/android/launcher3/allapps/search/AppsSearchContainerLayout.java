/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.allapps.search;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import static com.android.launcher3.Utilities.prefixTextWithIcon;
import static com.android.launcher3.icons.IconNormalizer.ICON_VISIBLE_AREA_FACTOR;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.Rect;
import android.text.Selection;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.MarginLayoutParams;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Insettable;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.search.SearchCallback;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;

/**
 * Layout to contain the All-apps search UI.
 */
public class AppsSearchContainerLayout extends ExtendedEditText
        implements SearchUiManager, SearchCallback<AdapterItem>,
        AllAppsStore.OnUpdateListener, Insettable {

    private final ActivityContext mLauncher;
    private final AllAppsSearchBarController mSearchBarController;
    private final SpannableStringBuilder mSearchQueryBuilder;

    private ActivityAllAppsContainerView<?> mAppsView;

    // The amount of pixels to shift down and overlap with the rest of the content.
    private final int mContentOverlap;

    private final int searchtopMargin;
    private final int searchSideMargin;

    public AppsSearchContainerLayout(Context context) {
        this(context, null);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLauncher = ActivityContext.lookupContext(context);
        mSearchBarController = new AllAppsSearchBarController();

        mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(mSearchQueryBuilder, 0);

        mContentOverlap =
                getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_content_overlap);
        searchtopMargin =
                getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_margin_top);
        searchSideMargin =
                getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_margin_side);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(mAppsView != null)
            mAppsView.getAppsStore().addUpdateListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mAppsView != null)
            mAppsView.getAppsStore().removeUpdateListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Update the width to match the grid padding
        DeviceProfile dp = mLauncher.getDeviceProfile();
        int myRequestedWidth = getSize(widthMeasureSpec);
        int rowWidth = myRequestedWidth - mAppsView.getActiveRecyclerView().getPaddingLeft()
                - mAppsView.getActiveRecyclerView().getPaddingRight();

        int cellWidth = DeviceProfile.calculateCellWidth(rowWidth,
                dp.cellLayoutBorderSpacePx.x, dp.numShownHotseatIcons);
        int iconVisibleSize = Math.round(ICON_VISIBLE_AREA_FACTOR * dp.iconSizePx);
        int iconPadding = cellWidth - iconVisibleSize;

        int myWidth = rowWidth - iconPadding + getPaddingLeft() + getPaddingRight();
        super.onMeasure(makeMeasureSpec(myWidth, EXACTLY), heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        Drawable gIcon = getContext().getDrawable(R.drawable.ic_super_g_color);
        Drawable gIconThemed = getContext().getDrawable(R.drawable.ic_super_g_themed);
        Drawable sIcon = getContext().getDrawable(R.drawable.ic_allapps_search);
        Drawable lens = getContext().getDrawable(R.drawable.ic_lens_color);
        Drawable lensThemed = getContext().getDrawable(R.drawable.ic_lens_themed);
        
        // Shift the widget horizontally so that its centered in the parent (b/63428078)
        View parent = (View) getParent();
        int availableWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        int myWidth = right - left;
        int expectedLeft = parent.getPaddingLeft() + (availableWidth - myWidth) / 2;
        int shift = expectedLeft - left;
        setTranslationX(shift);

        if (Utilities.showQSB(getContext()) && !Utilities.isThemedIconsEnabled(getContext())) {
          setCompoundDrawablesRelativeWithIntrinsicBounds(gIcon, null, lens, null);
        } else if (Utilities.showQSB(getContext()) && Utilities.isThemedIconsEnabled(getContext())) {
          setCompoundDrawablesRelativeWithIntrinsicBounds(gIconThemed, null, lensThemed, null);
        } else {
          setCompoundDrawablesRelativeWithIntrinsicBounds(sIcon, null, lens, null);
        }

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float touchX = event.getRawX();
                    float touchY = event.getRawY();
                    int rightDrawableWidth = getCompoundDrawables()[2].getBounds().width();
                    int leftDrawableWidth = getCompoundDrawables()[0].getBounds().width();
                    int paddingEnd = getPaddingEnd();
                    int paddingStart = getPaddingStart();

                    // Check if the touch is outside the bounds of the right drawable
                    if (touchX >= (getRight() - rightDrawableWidth - paddingEnd)) {
                        // Handle touch on the right drawable (lens icon)
                        // launch lens app
                        Intent lensIntent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("caller_package", Utilities.GSA_PACKAGE);
                        bundle.putLong("start_activity_time_nanos", SystemClock.elapsedRealtimeNanos());
                        lensIntent.setComponent(new ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .setPackage(Utilities.GSA_PACKAGE)
                                .setData(Uri.parse(Utilities.LENS_URI))
                                .putExtra("lens_activity_params", bundle);
                        getContext().startActivity(lensIntent);
                        return true;
                    }
                    // Check if the touch is outside the bounds of the left drawable
                    else if (touchX <= (leftDrawableWidth + paddingStart + searchSideMargin)) {
                        // Handle touch on the left drawable (Google icon)
                        // launch google app
                        Intent gIntent = getContext().getPackageManager().getLaunchIntentForPackage(Utilities.GSA_PACKAGE);
                        gIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        getContext().startActivity(gIntent);
                        return true;
                    }
                    // Check if the touch is in the middle part of the Search bar
                    else if (touchX > (leftDrawableWidth + paddingStart) && touchX < (getRight() - rightDrawableWidth - paddingEnd)) {
                        // Launch Pixel search directly if installed 
                        // to produce a similar search experience like pixel launcher
                        Intent pixelSearchIntent = getContext().getPackageManager().getLaunchIntentForPackage("rk.android.app.pixelsearch");
                        if (pixelSearchIntent != null) {
                            // The app is installed, launch it
                            pixelSearchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            getContext().startActivity(pixelSearchIntent);
                            return true;
                        } else {
                            // Use normal behaviour if pixel search is not installed.
                            return false;
                        }
                    }
                }
                return false;
            }
        });

        offsetTopAndBottom(mContentOverlap);

        setUpBackground();
    }

    private void setUpBackground() {
        Context context = getContext();
        float cornerRadius = getCornerRadius(context);
        int color = Themes.getAttrColor(context, R.attr.qsbFillColor);
        if (Utilities.isThemedIconsEnabled(context))
            color = Themes.getAttrColor(context, R.attr.qsbFillColorThemed);
        PaintDrawable pd = new PaintDrawable(color);
        pd.setCornerRadius(cornerRadius);
        setClipToOutline(cornerRadius > 0);
        setBackground(pd);
    }

    private float getCornerRadius(Context context) {
        Resources res = context.getResources();
        float qsbWidgetHeight = res.getDimension(R.dimen.qsb_widget_height);
        float qsbWidgetPadding = res.getDimension(R.dimen.qsb_widget_vertical_padding);
        float innerHeight = qsbWidgetHeight - 2 * qsbWidgetPadding;
        return (innerHeight / 2) * ((float)Utilities.getCornerRadius(context) / 100f);
    }

    @Override
    public void initializeSearch(ActivityAllAppsContainerView<?> appsView) {
        mAppsView = appsView;
        mSearchBarController.initialize(
                new DefaultAppSearchAlgorithm(getContext(), true),
                this, mLauncher, this);
    }

    @Override
    public void onAppsUpdated() {
        mSearchBarController.refreshSearchResult();
    }

    @Override
    public void resetSearch() {
        mSearchBarController.reset();
    }

    @Override
    public void preDispatchKeyEvent(KeyEvent event) {
        // Determine if the key event was actual text, if so, focus the search bar and then dispatch
        // the key normally so that it can process this key event
        if (!mSearchBarController.isSearchFieldFocused() &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
            final int unicodeChar = event.getUnicodeChar();
            final boolean isKeyNotWhitespace = unicodeChar > 0 &&
                    !Character.isWhitespace(unicodeChar) && !Character.isSpaceChar(unicodeChar);
            if (isKeyNotWhitespace) {
                boolean gotKey = TextKeyListener.getInstance().onKeyDown(this, mSearchQueryBuilder,
                        event.getKeyCode(), event);
                if (gotKey && mSearchQueryBuilder.length() > 0) {
                    mSearchBarController.focusSearchField();
                }
            }
        }
    }

    @Override
    public void onSearchResult(String query, ArrayList<AdapterItem> items) {
        if (items != null) {
            mAppsView.setSearchResults(items);
        }
    }

    @Override
    public void clearSearchResult() {
        // Clear the search query
        mSearchQueryBuilder.clear();
        mSearchQueryBuilder.clearSpans();
        Selection.setSelection(mSearchQueryBuilder, 0);
        mAppsView.onClearSearchResult();
    }

    @Override
    public void setInsets(Rect insets) {
        MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
        mlp.topMargin = searchtopMargin;
        requestLayout();
    }

    @Override
    public ExtendedEditText getEditText() {
        return this;
    }
}
