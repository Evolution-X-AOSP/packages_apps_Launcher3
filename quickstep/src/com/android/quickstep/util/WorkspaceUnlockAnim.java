/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.quickstep.util;

import android.graphics.Point;
import android.view.View;

import com.android.launcher3.R;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Hotseat;
import com.android.launcher3.Launcher;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;

import java.util.ArrayList;
import java.util.List;

/**
 * Animation to animate in a workspace during the unlock transition.
 */
// TODO(b/219444608): use SCALE_PROPERTY_FACTORY once the scale is reset to 1.0 after unlocking.
public class WorkspaceUnlockAnim {
    /** Scale for the workspace icons at the beginning of the animation. */
    private static final float START_SCALE = 0.85f;

    /**
     * Starting translation Y values for the animation. We use a larger value if we're animating in
     * from a swipe, since there is more perceived upward movement when we unlock from a swipe.
     */
    private static final int START_TRANSLATION_DP = 20;
    private static final int START_TRANSLATION_SWIPE_DP = 60;

    private Launcher mLauncher;
    private float mUnlockAmount = 0f;

    public WorkspaceUnlockAnim(Launcher launcher) {
        mLauncher = launcher;
    }

    /**
     * Called when we're about to make the Launcher window visible and play the unlock animation.
     *
     * This is a blocking call so that System UI knows it's safe to show the Launcher window without
     * causing the Launcher contents to flicker on screen. Do not do anything expensive here.
     */
    public void prepareForUnlock() {
        Workspace workspace = mLauncher.getWorkspace();
        final Point workspaceCenter = new Point(
                workspace.getLeft() + workspace.getWidth() / 2,
                workspace.getTop() + workspace.getHeight() / 2);

        getCurrentPageCells().forEach(v -> {
            v.setAlpha(0f);
            setViewPivotTowardsPoint(v, workspaceCenter);
        });
        setViewPivotTowardsPoint(mLauncher.getHotseat(), workspaceCenter);
        mLauncher.getHotseat().setAlpha(0f);

        mUnlockAmount = 0f;
    }

    private List<View> getCurrentPageCells() {
        List<View> cells = new ArrayList<View>();
        Workspace workspace = mLauncher.getWorkspace();
        CellLayout currentPage = workspace.getScreenWithId(
                workspace.getScreenIdForPageIndex(workspace.getCurrentPage()));
        ShortcutAndWidgetContainer container = currentPage.getShortcutsAndWidgets();
        for (int i = 0; i < container.getChildCount(); i++) {
            View cell = container.getChildAt(i);
            if (cell.getId() == R.id.search_container_workspace) {
                // Smartspace is animated separately
                continue;
            }
            cells.add(cell);
        }
        return cells;
    }

    private void setViewPivotTowardsPoint(View view, Point point) {
        float centerX = view.getX() + view.getWidth() / 2;
        float centerY = view.getY() + view.getHeight() / 2;
        float offsetX = point.x - centerX;
        float offsetY = point.y - centerY;
        float clipX = Math.min(view.getWidth() / 2, Math.abs(offsetX));
        float clipY = Math.min(view.getHeight() / 2, Math.abs(offsetY));
        view.setPivotX(view.getWidth() / 2 + (clipX * (offsetX > 0 ? 1 : -1)));
        view.setPivotY(view.getHeight() / 2 + (clipY * (offsetY > 0 ? 1 : -1)));
    }

    public void setUnlockAmount(float amount, boolean fromSwipe) {
        mUnlockAmount = amount;

        final float amountInverse = 1f - amount;
        final float scale = START_SCALE + (1f - START_SCALE) * amount;

        final Hotseat hotseat = mLauncher.getHotseat();

        List<View> cells = getCurrentPageCells();

        if (fromSwipe) {
            cells.forEach(v -> v.setTranslationY(
                    Utilities.dpToPx(START_TRANSLATION_SWIPE_DP) * amountInverse));
            hotseat.setTranslationY(
                    Utilities.dpToPx(START_TRANSLATION_SWIPE_DP) * amountInverse);
        } else {
            cells.forEach(v -> v.setTranslationY(
                    Utilities.dpToPx(START_TRANSLATION_DP) * amountInverse));
            hotseat.setTranslationY(
                    Utilities.dpToPx(START_TRANSLATION_DP) * amountInverse);
        }

        cells.forEach(v -> {
            v.setScaleX(scale);
            v.setScaleY(scale);
            v.setAlpha(amount);
        });

        hotseat.setScaleX(scale);
        hotseat.setScaleY(scale);
        hotseat.setAlpha(amount);
    }
}
