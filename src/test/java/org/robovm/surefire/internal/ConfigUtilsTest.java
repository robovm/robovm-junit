/*
 * Copyright (C) 2014 Trillian Mobile AB
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

    @Test
    public void testSystemProperties() {
        assertTrue(ConfigUtils.getProperty(Constant.SERVER_HOST).equals("localhost"));
        System.setProperty(Constant.SERVER_HOST,"test");
        assertTrue(ConfigUtils.getProperty(Constant.SERVER_HOST).equals("test"));
    }
}