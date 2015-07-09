/*
 * Copyright (C) 2014 RoboVM AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robovm.testkit.shadow;

import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIApplicationDelegateAdapter;
import org.robovm.apple.uikit.UIApplicationLaunchOptions;
import org.robovm.apple.uikit.UIWindow;
import org.robovm.junit.server.TestServer;

import java.io.IOException;

public class ShadowUIApplicationDelegateAdapter extends UIApplicationDelegateAdapter {

    UIWindow window;

    @Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions launchOptions) {
        window = new UIWindow(org.robovm.apple.uikit.UIScreen.getMainScreen().getBounds());
        window.makeKeyAndVisible();
        Foundation.log("Loaded");
        new Thread() {
            @Override
            public void run() {
                try {
                    new TestServer().run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        return true;
    }

    @Override
    public UIWindow getWindow() {
        return window;
    }
}
