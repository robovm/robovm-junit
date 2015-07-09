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

package org.robovm.testkit;

import org.robovm.apple.foundation.NSArray;
import org.robovm.apple.uikit.*;

public class UITestKit {

    public static final String DEBUG = "robovm.debug";

    /**
     * Set UIApplication's root view
     * 
     * @param rootViewController
     */
    public static void setRootView(UIViewController rootViewController) {
        UIApplication application = UIApplication.getSharedApplication();
        UIApplicationDelegateAdapter adapter = (UIApplicationDelegateAdapter) application.getDelegate();
        adapter.getWindow().setRootViewController(rootViewController);
    }

    public static UIView getChildViewWithText(String text) {
        UIViewController rootViewController;

        waitForBind();

        rootViewController = UIApplication.getSharedApplication().getDelegate().getWindow().getRootViewController();

        NSArray<UIView> subviews = rootViewController.getView().getSubviews();
        for (UIView subview : subviews) {
            if (subview instanceof UITextView) {
                if (((UITextView)subview).getText().equals(text)){
                    return subview;
                }
            } else if (subview instanceof UILabel) {
                if (((UILabel)subview).getText().equals(text)){
                    return subview;
                }
            }
        }
        return null;
    }

    /**
     * Return child with specified placeholder text
     * 
     * @param placeholderText
     * @return UIView with specified placeholder text || null
     */
    public static UIView getChildViewWithPlaceholderText(String placeholderText) {
        UIViewController rootViewController;

       waitForBind();

        rootViewController = UIApplication.getSharedApplication().getDelegate().getWindow().getRootViewController();

        NSArray<UIView> subviews = rootViewController.getView().getSubviews();
        for (UIView subview : subviews) {
            if (subview instanceof UITextField) {
                if (placeholderText.equals(((UITextField) subview).getPlaceholder())) {
                    return subview;
                }
            }
        }
        return null;
    }

    private static void waitForBind() {
        while (true) {
            UIApplication application = UIApplication.getSharedApplication();
            if (application != null) {
                if (application.getWindows().size() >= 1) {
                    debug("Waiting for UI bind...");
                    break;
                }
            }
        }
    }
    static void debug(String logLine) {
        if (Boolean.getBoolean(DEBUG)) {
            System.err.println(" [DEBUG]: " + logLine);
        }
    }

}
