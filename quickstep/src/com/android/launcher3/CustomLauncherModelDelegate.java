package com.android.launcher3;

import android.app.smartspace.SmartspaceConfig;
import android.app.smartspace.SmartspaceManager;
import android.app.smartspace.SmartspaceSession;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.Context;
import android.util.Log;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.model.AllAppsList;
import com.android.launcher3.model.BaseModelUpdateTask;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.model.QuickstepModelDelegate;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.util.Executors;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CustomLauncherModelDelegate extends QuickstepModelDelegate
    implements SmartspaceSession.OnTargetsAvailableListener {

    public static final String TAG = "CustomLauncherModelDelegate";

    public final Context mContext;
    public final Deque mSmartspaceTargets = new LinkedList();

    public SmartspaceSession mSmartspaceSession;

    public CustomLauncherModelDelegate(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void destroy() {
        super.destroy();
        destroySmartspaceSession();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println(prefix + "Recent BC Smartspace Targets (most recent first)");
        if (mSmartspaceTargets.size() == 0) {
            writer.println(prefix + "   No data\n");
            return;
        }
        mSmartspaceTargets.descendingIterator().forEachRemaining((x) -> {
            writer.println(prefix + "   Number of targets: " + mSmartspaceTargets.size());
            Iterator it = mSmartspaceTargets.iterator();
            while (it.hasNext()) {
                writer.println(prefix + "      " + ((SmartspaceTarget) it.next()));
            }
            writer.println();
        });
    }

    public final void destroySmartspaceSession() {
        if (mSmartspaceSession != null) {
            mSmartspaceSession.close();
            mSmartspaceSession = null;
        }
    }

    public void onTargetsAvailable(List<SmartspaceTarget> targets) {
        List<SmartspaceTarget> list = targets.stream()
                                             .filter(t -> t.getFeatureType() != 34)
                                             .collect(Collectors.toList());
        mSmartspaceTargets.offerLast(list);
        if (mSmartspaceTargets.size() > 5) {
            mSmartspaceTargets.pollFirst();
        }
        mApp.getModel().enqueueModelUpdateTask(new BaseModelUpdateTask() {
            @Override
            public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
                BgDataModel.FixedContainerItems container = new BgDataModel.FixedContainerItems(-110);
                for (SmartspaceTarget target : list) {
                    SmartspaceItem item = new SmartspaceItem();
                    item.setSmartspaceTarget(target);
                    item.container = container.containerId;
                    item.itemType = 8;
                    container.items.add(item);
                }
                bindExtraContainerItems(container);
            }
        });
    }

    public void notifySmartspaceEvent(SmartspaceTargetEvent event) {
        mApp.getModel().enqueueModelUpdateTask(new BaseModelUpdateTask() {
            @Override
            public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
                if (mSmartspaceSession != null) {
                    mSmartspaceSession.notifySmartspaceEvent(event);
                }
            }
        });
    }

    @Override
    public void validateData() {
        super.validateData();
        if (mSmartspaceSession != null) {
            mSmartspaceSession.requestSmartspaceUpdate();
        }
    }

    @Override
    public void workspaceLoadComplete() {
        super.workspaceLoadComplete();
        destroySmartspaceSession();
        if (!mActive) {
            return;
        }
        Log.d(TAG, "Starting smartspace session for home");
        mSmartspaceSession = ((SmartspaceManager) mContext.getSystemService(SmartspaceManager.class))
                                    .createSmartspaceSession(new SmartspaceConfig.Builder(mContext, "home").build());
        mSmartspaceSession.addOnTargetsAvailableListener(Executors.MODEL_EXECUTOR, this);
        mSmartspaceSession.requestSmartspaceUpdate();
    }

    public class SmartspaceItem extends ItemInfo {
        public SmartspaceTarget mSmartspaceTarget;

        public SmartspaceTarget getSmartspaceTarget() {
            return mSmartspaceTarget;
        }

        public void setSmartspaceTarget(SmartspaceTarget target) {
            mSmartspaceTarget = target;
        }
    }
}
