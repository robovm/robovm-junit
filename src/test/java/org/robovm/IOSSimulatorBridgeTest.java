package org.robovm;

import org.junit.Test;
import org.robovm.surefire.IOSSimulatorBridge;

import static junit.framework.Assert.assertTrue;


public class IOSSimulatorBridgeTest {

    @Test
    public void testInstantiate() {
        IOSSimulatorBridge bridge = new IOSSimulatorBridge();
        assertTrue(bridge != null);
    }
}
