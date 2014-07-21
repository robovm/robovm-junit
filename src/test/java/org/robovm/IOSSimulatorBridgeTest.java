package org.robovm;

import org.junit.Test;
import org.robovm.surefire.IOSSimulatorBridge;

import static junit.framework.Assert.assertTrue;

/**
 * Created by ash on 16/07/2014.
 */
public class IOSSimulatorBridgeTest {

    @Test
    public void testInstantiate() {
        IOSSimulatorBridge bridge = new IOSSimulatorBridge();
        assertTrue(bridge != null);
    }
}
