package org.robovm.surefire.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.robovm.compiler.config.Config;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Config.Builder.class })
public class ConfigUtilsTest {

        @Test
        public void testConfigMerge() throws IOException {
                Config.Builder configBuilder = new Config.Builder();
                configBuilder.addForceLinkClass("test");
                configBuilder.addFramework("framework");

                ConfigUtils.mergeConfigs(configBuilder, "src/test/resources/config.xml");
                Config config = (Config) Whitebox.getInternalState(configBuilder, "config");

                List<String> frameworks = config.getFrameworks();
                assertTrue(frameworks.contains("framework"));
                assertTrue(frameworks.contains("UIKit"));
        }
}