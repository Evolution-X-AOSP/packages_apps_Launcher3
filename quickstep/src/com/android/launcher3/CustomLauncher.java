/*
 * Copyright (C) 2019 Paranoid Android
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

package com.android.launcher3;

import android.app.smartspace.SmartspaceTarget;
import android.os.Bundle;

import com.android.launcher3.CustomLauncherModelDelegate.SmartspaceItem;

import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.qsb.LauncherUnlockAnimationController;
import com.android.launcher3.uioverrides.QuickstepLauncher;
import com.android.quickstep.SystemUiProxy;

import com.google.android.systemui.smartspace.BcSmartspaceDataProvider;

import java.util.List;
import java.util.stream.Collectors;

public class CustomLauncher extends QuickstepLauncher {
    private BcSmartspaceDataProvider mSmartspacePlugin = new BcSmartspaceDataProvider();
    private LauncherUnlockAnimationController mUnlockAnimationController =
            new LauncherUnlockAnimationController(this);

    public BcSmartspaceDataProvider getSmartspacePlugin() {
        return mSmartspacePlugin;
    }

    public LauncherUnlockAnimationController getLauncherUnlockAnimationController() {
        return mUnlockAnimationController;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        SystemUiProxy.INSTANCE.get(this).setLauncherUnlockAnimationController(mUnlockAnimationController);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SystemUiProxy.INSTANCE.get(this).setLauncherUnlockAnimationController(null);
    }

    @Override
    public void onOverlayVisibilityChanged(boolean visible) {
        super.onOverlayVisibilityChanged(visible);
        mUnlockAnimationController.updateSmartspaceState();
    }

    @Override
    public void onPageEndTransition() {
        super.onPageEndTransition();
        mUnlockAnimationController.updateSmartspaceState();
    }

    @Override
    public void bindExtraContainerItems(BgDataModel.FixedContainerItems container) {
        if (container.containerId == -110) {
            List<SmartspaceTarget> targets = container.items.stream()
                                                            .map(item -> ((SmartspaceItem) item).getSmartspaceTarget())
                                                            .collect(Collectors.toList());
            mSmartspacePlugin.onTargetsAvailable(targets);
        }
        super.bindExtraContainerItems(container);
    }

}
