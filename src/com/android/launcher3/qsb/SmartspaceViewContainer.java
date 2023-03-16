package com.android.launcher3.qsb;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.android.launcher3.CustomLauncher;
import com.android.launcher3.CustomLauncherModelDelegate;

import com.android.launcher3.celllayout.CellLayoutLayoutParams;;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.icons.GraphicsUtils;
import com.android.launcher3.uioverrides.plugins.PluginManagerWrapper;
import com.android.launcher3.views.ActivityContext;
import com.android.quickstep.SystemUiProxy;

import com.android.systemui.plugins.BcSmartspaceDataPlugin;
import com.android.systemui.plugins.PluginListener;

import com.google.android.systemui.smartspace.BcSmartspaceDataProvider;
import com.google.android.systemui.smartspace.BcSmartspaceView;

import java.util.ArrayList;
import java.util.List;

public class SmartspaceViewContainer extends FrameLayout implements PluginListener<BcSmartspaceDataPlugin> {

    public BcSmartspaceView mView;

    public SmartspaceViewContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mView = (BcSmartspaceView) inflate(context, R.layout.smartspace_enhanced, null);
        mView.setPrimaryTextColor(GraphicsUtils.getAttrColor(context, R.attr.workspaceTextColor));
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layoutParams.setMarginStart(getResources().getDimensionPixelSize(R.dimen.enhanced_smartspace_margin_start_launcher));
        addView(mView, layoutParams);

        CustomLauncher launcher = (CustomLauncher) ActivityContext.lookupContext(context);
        launcher.getLauncherUnlockAnimationController().setSmartspaceView(mView);

        CustomLauncherModelDelegate delegate = (CustomLauncherModelDelegate) launcher.getModel().getModelDelegate();
        BcSmartspaceDataProvider plugin = launcher.getSmartspacePlugin();
        plugin.registerSmartspaceEventNotifier(event -> delegate.notifySmartspaceEvent(event));
        mView.registerDataProvider(plugin);
    }

    @Override
    public void onPluginConnected(BcSmartspaceDataPlugin plugin, Context context) {
        mView.registerDataProvider(plugin);
    }

    @Override
    public void onPluginDisconnected(BcSmartspaceDataPlugin plugin) {
        CustomLauncher launcher = (CustomLauncher) ActivityContext.lookupContext(getContext());
        mView.registerDataProvider(launcher.getSmartspacePlugin());
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        PluginManagerWrapper.INSTANCE.get(getContext()).addPluginListener(this, BcSmartspaceDataPlugin.class);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        PluginManagerWrapper.INSTANCE.get(getContext()).removePluginListener(this);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) getLayoutParams();
        lp.setMargins(left, top, right, bottom);
        setLayoutParams(lp);
    }
}
