package org.robovm.junitbridge;

import org.junit.Test;
import org.robovm.apple.foundation.NSString;

import static org.junit.Assert.assertTrue;

public class RunnerClass {

    @Test
    public void testTest() {
        assertTrue(1 == 1);
    }

    @Test
    public void testNSClass() {
        NSString myString = new NSString("String");
        assertTrue(myString.toString().equals("String"));
    }

    @Test
    public void testShouldFail() {
        assertTrue(1 == 2);
    }
}
