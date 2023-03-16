package com.android.launcher3;

import android.app.smartspace.SmartspaceAction;
import android.app.smartspace.SmartspaceConfig;
import android.app.smartspace.SmartspaceManager;
import android.app.smartspace.SmartspaceSession;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
    public final Deque mSmartspaceTargets = new LinkedList<List>();

    public SmartspaceSession mSmartspaceSession;

    private static final String GSA_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String GSA_WEATHER_ACTIVITY = "com.google.android.apps.search.weather.WeatherExportedActivity";
 
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
            List targets = (List) x;
            writer.println(prefix + "   Number of targets: " + targets.size());
            Iterator it = targets.iterator();
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
        List<SmartspaceTarget> list = new ArrayList<SmartspaceTarget>();
        for (SmartspaceTarget t : targets) {
            if (t.getFeatureType() == SmartspaceTarget.FEATURE_HOLIDAY_ALARM) {
                continue;
            } else if (t.getFeatureType() == SmartspaceTarget.FEATURE_WEATHER) {
                SmartspaceAction a = t.getHeaderAction();
                a = new SmartspaceAction.Builder(a.getId(), a.getTitle().toString())
                        .setIcon(a.getIcon())
                        .setSubtitle(a.getSubtitle())
                        .setContentDescription(a.getContentDescription())
                        .setPendingIntent(a.getPendingIntent())
                        .setUserHandle(a.getUserHandle())
                        .setIntent(new Intent().setComponent(
                            new ComponentName(GSA_PACKAGE, GSA_WEATHER_ACTIVITY))
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        .setExtras(a.getExtras())
                        .build();
                list.add(new SmartspaceTarget.Builder(t.getSmartspaceTargetId(),
                        t.getComponentName(), t.getUserHandle())
                        .setHeaderAction(a)
                        .setBaseAction(t.getBaseAction())
                        .setCreationTimeMillis(t.getCreationTimeMillis())
                        .setExpiryTimeMillis(t.getExpiryTimeMillis())
                        .setScore(t.getScore())
                        .setActionChips(t.getActionChips())
                        .setIconGrid(t.getIconGrid())
                        .setFeatureType(SmartspaceTarget.FEATURE_WEATHER)
                        .setSensitive(t.isSensitive())
                        .setShouldShowExpanded(t.shouldShowExpanded())
                        .setSourceNotificationKey(t.getSourceNotificationKey())
                        .setAssociatedSmartspaceTargetId(t.getAssociatedSmartspaceTargetId())
                        .setSliceUri(t.getSliceUri())
                        .setWidget(t.getWidget())
                        .setTemplateData(t.getTemplateData())
                        .build());
                continue;
            }
            list.add(t);
        }
        mSmartspaceTargets.offerLast(list);
        if (mSmartspaceTargets.size() > 5) {
            mSmartspaceTargets.pollFirst();
        }
        mApp.getModel().enqueueModelUpdateTask(new BaseModelUpdateTask() {
            @Override
            public void execute(LauncherAppState app, BgDataModel dataModel, AllAppsList apps) {
                List<ItemInfo> items = new ArrayList<>(mSmartspaceTargets.size());
                for (SmartspaceTarget target : list) {
                    SmartspaceItem item = new SmartspaceItem();
                    item.setSmartspaceTarget(target);
                    item.container = -110;
                    item.itemType = 8;
                    items.add(item);
                }
                BgDataModel.FixedContainerItems container = new BgDataModel.FixedContainerItems(-110, items);
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
