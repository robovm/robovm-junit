#JUnit 4 Runner

This project aims to replace the maven surefire provider for JUnit test execution with RoboVM

 
##Examples
 
```java
package org.robovm.samples.helloworld;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robovm.apple.uikit.UITextField;
import org.robovm.apple.uikit.UIView;
import org.robovm.samples.helloworld.viewcontrollers.MyViewController;
import org.robovm.testkit.runner.RoboVMUITestRunner;

import static junit.framework.TestCase.assertTrue;
import static org.robovm.testkit.UITestKit.getChildViewWithPlaceholderText;
import static org.robovm.testkit.UITestKit.setRootView; 

@RunWith(RoboVMTestRunner.class)
public class HelloWorldTest
{

    @Test
    public void testHello() {
        String jvm = System.getProperty("java.runtime.name");
        assertTrue(jvm, "RoboVM Runtime".equals(jvm));
    }
}
```

##Notes

This is still under heavy development and may not work with all IDEs or build environments. Please report
bugs