package com.android.launcher3.qsb;

import android.animation.ValueAnimator;
import android.graphics.Rect;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.Workspace;
import com.android.quickstep.SystemUiProxy;
import com.android.systemui.animation.Interpolators;
import com.android.systemui.shared.system.smartspace.ILauncherUnlockAnimationController;
import com.android.systemui.shared.system.smartspace.SmartspaceState;
import com.google.android.systemui.smartspace.BcSmartspaceView;

public final class LauncherUnlockAnimationController extends ILauncherUnlockAnimationController.Stub {

    private final Launcher mLauncher;
    private BcSmartspaceView mSmartspaceView;
    private SmartspaceState mSmartspaceState = new SmartspaceState();
    private Rect mLauncherSmartspaceBounds = new Rect();
    private Rect mLockscreenSmartspaceBounds = new Rect();
    private final ValueAnimator mSmartspaceAnimator;
    private boolean mShouldAnimateSmartspace;
    private boolean mUnlockAnimationPlaying;

    public LauncherUnlockAnimationController(Launcher launcher) {
        mLauncher = launcher;
        mSmartspaceAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mSmartspaceAnimator.setInterpolator(Interpolators.EMPHASIZED);
        mSmartspaceAnimator.addUpdateListener(animation -> {
            setSmartspaceProgressToLauncherPosition(
                ((Float) animation.getAnimatedValue()).floatValue());
        });
    }

    public void setSmartspaceView(BcSmartspaceView view) {
        mSmartspaceView = view;
        updateSmartspaceState();
    }

    private void setSmartspaceProgressToLauncherPosition(float f) {
        if (mSmartspaceView != null) {
            mSmartspaceView.setTranslationX(
                (mLockscreenSmartspaceBounds.left - mLauncherSmartspaceBounds.left) * (1.0f - f));
            mSmartspaceView.setTranslationY(
                (mLockscreenSmartspaceBounds.top - mLauncherSmartspaceBounds.top) * (1.0f - f));
        }
    }

    @Override
    public void prepareForUnlock(boolean animateSmartspace,
            Rect lockscreenSmartspaceBounds, int selectedPage) {
        mShouldAnimateSmartspace = animateSmartspace;
        mLockscreenSmartspaceBounds.set(lockscreenSmartspaceBounds);

        if (mSmartspaceView == null) {
            return;
        }
        mSmartspaceView.getBoundsOnScreen(mLauncherSmartspaceBounds);
        mLauncherSmartspaceBounds.offset(0, mSmartspaceView.getCurrentCardTopPadding());
        setSmartspaceProgressToLauncherPosition(animateSmartspace ? 0f : 1f);
        setSmartspaceSelectedPage(selectedPage);

        Workspace workspace = mLauncher.getWorkspace();
        CellLayout currentPage = workspace.getScreenWithId(
                workspace.getScreenIdForPageIndex(workspace.getCurrentPage()));
        currentPage.getShortcutsAndWidgets().setClipChildren(!animateSmartspace);
    }

    @Override
    public void setUnlockAmount(float amount, boolean forceIfAnimating) {
        // we do not implement this yet
    }

    @Override
    public void playUnlockAnimation(boolean unlocked, long duration, long startDelay) {
        if (mSmartspaceView != null && mShouldAnimateSmartspace) {
            mUnlockAnimationPlaying = true;
            mSmartspaceView.post(() -> {
                mSmartspaceAnimator.setDuration(startDelay + duration);
                mSmartspaceAnimator.start();
            });
        }
    }

    @Override
    public void setSmartspaceSelectedPage(int selectedPage) {
        if (mSmartspaceView != null) {
            mSmartspaceView.post(() -> mSmartspaceView.setSelectedPage(selectedPage));
        }
    }

    @Override
    public void setSmartspaceVisibility(int visibility) {
        if (mSmartspaceView != null) {
            mSmartspaceView.post(() -> mSmartspaceView.setVisibility(visibility));
        }
    }

    @Override
    public void dispatchSmartspaceStateToSysui() {
        if (mSmartspaceView != null) {
            SystemUiProxy.INSTANCE.get(mLauncher).notifySysuiSmartspaceStateUpdated(mSmartspaceState);
        }
    }

    public final void updateSmartspaceState() {
        if (mSmartspaceView == null) {
            return;
        }
        Rect bounds = new Rect();
        mSmartspaceView.getBoundsOnScreen(bounds);
        boolean visible = !mLauncher.getWorkspace().isOverlayShown()
                && mLauncher.getWorkspace().getDestinationPage() == 0
                && !bounds.isEmpty();
        int selectedPage = mSmartspaceView.getSelectedPage();
        if (mSmartspaceState.getVisibleOnScreen() != visible
                || mSmartspaceState.getSelectedPage() != selectedPage
                || !mSmartspaceState.getBoundsOnScreen().equals(bounds)) {
            mSmartspaceState.setBoundsOnScreen(bounds);
            mSmartspaceState.setSelectedPage(selectedPage);
            mSmartspaceState.setVisibleOnScreen(visible);
            dispatchSmartspaceStateToSysui();
        }
    }
}
