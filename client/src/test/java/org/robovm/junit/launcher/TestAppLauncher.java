package org.robovm.junit.launcher;

import org.robovm.junit.server.TestServer;

import java.io.IOException;


public class TestAppLauncher {
    public static void main(String[] args) throws IOException {
        /*
         * The iOS simulator seems to relaunch apps that quit too fast or don't
         * call into the UIKit event loop. The simulator doesn't preserve args
         * when relaunching. The TestClient sets a special system property on
         * the command line. If it's not set we now that we have been
         * relaunched. In that case we return immediately.
         */
        if (System.getProperty("os.name").equals("iOS Simulator")
                && !Boolean.getBoolean("robovm.launchedFromTestClient")) {
            return;
        }

        System.err.println(TestServer.class.getName() + " [DEBUG]: Running");

        new TestServer().run();
    }

}
