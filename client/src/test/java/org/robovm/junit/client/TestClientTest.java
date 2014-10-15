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
package org.robovm.junit.client;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.robovm.compiler.Version;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.Config.Home;
import org.robovm.compiler.log.ConsoleLogger;
import org.robovm.compiler.target.LaunchParameters;
import org.robovm.junit.protocol.ResultObject;
import org.robovm.junit.server.TestServer;
import org.robovm.maven.resolver.RoboVMResolver;

import rx.observables.BlockingObservable;

/**
 * Tests {@link TestClient}.
 */
public class TestClientTest {

    @Test
    public void testSuccessfulWholeClassRunOutsideOfRoboVM() throws Exception {
        final TestServer testServer = new TestServer();
        PipedOutputStream cmdStream = new PipedOutputStream();
        final PipedInputStream in = new PipedInputStream(cmdStream);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thread t = new Thread() {
            public void run() {
                testServer.run(in, out);
            }
        };
        t.start();
        
        OutputStreamWriter cmdWriter = new OutputStreamWriter(cmdStream);
        cmdWriter.write("load " + RunnerClass.class.getName() + "\n");
        cmdWriter.flush();
        cmdWriter.write("quit\n");
        cmdWriter.flush();
        
        t.join();

        String result = new String(out.toByteArray(), "ASCII");
        System.out.println(result);
        assertFalse(result.isEmpty());
    }
    
    @Test
    public void testSuccessfulWholeClassRun() throws Exception {
        TestClient client = new TestClient();
        Config config = client.compile(createConfig());

        LaunchParameters launchParameters = config.getTarget().createLaunchParameters();
        client.start(config, launchParameters);
        
        BlockingObservable<ResultObject> blockingObservable = BlockingObservable.from(client
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

        client.stop();
    }

    @Test
    public void testSuccessfulSingleMethodRun() throws Exception {
        TestClient client = new TestClient();
        Config config = client.compile(createConfig());

        LaunchParameters launchParameters = config.getTarget().createLaunchParameters();
        client.start(config, launchParameters);

        BlockingObservable<ResultObject> blockingObservable = BlockingObservable.from(client
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

        client.stop();
    }
    
    private Config.Builder createConfig() throws IOException, ClassNotFoundException {
        RoboVMResolver roboVMResolver = new RoboVMResolver();
        Home home = new Home(roboVMResolver.resolveAndUnpackRoboVMDistArtifact(Version.getVersion()));

        Config.Builder config = new Config.Builder();

        config.home(home);

        for (String p : System.getProperty("java.class.path").split(File.pathSeparator)) {
            config.addClasspathEntry(new File(p));
        }
        
        config.addForceLinkClass(RunnerClass.class.getName());
        config.logger(new ConsoleLogger(true));
        config.skipInstall(true);

        return config;
    }

}
