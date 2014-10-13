/*
 * Copyright (C) 2014 Trillian Mobile AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.robovm.junitbridge;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.log.ConsoleLogger;
import org.robovm.compiler.target.LaunchParameters;
import org.robovm.devicebridge.RoboVMDeviceBridge;
import org.robovm.junit.protocol.ResultObject;

import rx.observables.BlockingObservable;

public class RoboVMDeviceBridgeTest {

    @Test
    public void testSuccessfulWholeClassRun() throws Exception {
        RoboVMDeviceBridge roboVMDeviceBridge = new RoboVMDeviceBridge();
        Config config = roboVMDeviceBridge.compile(createConfig());

        LaunchParameters launchParameters = config.getTarget().createLaunchParameters();
        roboVMDeviceBridge.start(config, launchParameters);
        
        BlockingObservable<ResultObject> blockingObservable = BlockingObservable.from(roboVMDeviceBridge
                .runTests(config, new String[] { RunnerClass.class.getName() }));
        List<String>  successfulTests = new ArrayList<>();
        List<String> failedTests = new ArrayList<>();
        for (ResultObject resultObject : blockingObservable.toIterable()) {
            switch (resultObject.getResultType()) {
            case ResultObject.TEST_FINISHED:
                successfulTests.add(resultObject.getDescription().toString());
                break;
            case ResultObject.TEST_FAILURE:
                failedTests.add(resultObject.getFailure().toString());
                break;
            }
        }

        assertEquals("2 successful tests expected", 2, successfulTests.size());
        assertTrue(successfulTests.contains("testSuccessfulTest1(" + RunnerClass.class.getName() + ")"));
        assertTrue(successfulTests.contains("testSuccessfulTest2(" + RunnerClass.class.getName() + ")"));
        assertEquals("1 failed test expected", 1, failedTests.size());
        assertTrue(failedTests.contains("testShouldFail(" + RunnerClass.class.getName() + "): null"));

        roboVMDeviceBridge.stop();
    }

    @Test
    public void testSuccessfulSingleMethodRun() throws Exception {
        RoboVMDeviceBridge roboVMDeviceBridge = new RoboVMDeviceBridge();
        Config config = roboVMDeviceBridge.compile(createConfig());

        LaunchParameters launchParameters = config.getTarget().createLaunchParameters();
        roboVMDeviceBridge.start(config, launchParameters);

        BlockingObservable<ResultObject> blockingObservable = BlockingObservable.from(roboVMDeviceBridge
                .runTests(config, new String[] { RunnerClass.class.getName() + "#testSuccessfulTest1" }));
        List<String>  successfulTests = new ArrayList<>();
        List<String> failedTests = new ArrayList<>();
        for (ResultObject resultObject : blockingObservable.toIterable()) {
            switch (resultObject.getResultType()) {
            case ResultObject.TEST_FINISHED:
                successfulTests.add(resultObject.getDescription().toString());
                break;
            case ResultObject.TEST_FAILURE:
                failedTests.add(resultObject.getFailure().toString());
                break;
            }
        }

        assertEquals("1 successful tests expected", 1, successfulTests.size());
        assertTrue(successfulTests.contains("testSuccessfulTest1(" + RunnerClass.class.getName() + ")"));
        assertEquals("0 failed tests expected", 0, failedTests.size());

        roboVMDeviceBridge.stop();
    }
    
    private Config.Builder createConfig() throws IOException, ClassNotFoundException {
        Config.Builder config = new Config.Builder();

        for (String p : System.getProperty("java.class.path").split(File.pathSeparator)) {
            config.addClasspathEntry(new File(p));
        }
        
        config.addForceLinkClass(RunnerClass.class.getName());
        config.logger(new ConsoleLogger(true));
        config.skipInstall(true);

        return config;
    }

}
