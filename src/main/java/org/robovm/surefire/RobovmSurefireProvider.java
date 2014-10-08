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
package org.robovm.surefire;

import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.Result;
import org.junit.runner.notification.RunNotifier;
import org.robovm.compiler.config.Config;
import org.robovm.surefire.internal.ConfigUtils;
import org.robovm.surefire.internal.Constant;

import java.io.IOException;
import java.util.Iterator;

public class RobovmSurefireProvider extends AbstractProvider {

    private final ClassLoader testClassLoader;
    private final ProviderParameters providerParameters;
    private final ScanResult scanResult;
    private final RunOrderCalculator runOrderCalculator;
    private TestsToRun testsToRun;
    RobovmTestChecker testChecker;
    private String requestedTestMethod;
    private IOSSimulatorBridge iosSimulatorBridge;
    private int serverPort = 8889;
    private String serverHost = "localhost";
    private String mavenRepositoryDir = "~/.m2";
    private Config.Builder config;
    private static int count = 0;
    RoboTestServer roboTestListener = null;

    public RobovmSurefireProvider(ProviderParameters providerParameters) throws IOException {
        this.providerParameters = providerParameters;
        this.testClassLoader = providerParameters.getTestClassLoader();
        this.scanResult = providerParameters.getScanResult();
        this.runOrderCalculator = providerParameters.getRunOrderCalculator();
        testChecker = new RobovmTestChecker(testClassLoader);
        requestedTestMethod = providerParameters.getTestRequest().getRequestedTestMethod();
        config = ConfigUtils.createConfig();
        configureSurefire();
    }

    public void configureSurefire() {
        serverPort = Integer.parseInt(ConfigUtils.getProperty(Constant.SERVER_PORT));
        serverHost = ConfigUtils.getProperty(Constant.SERVER_HOST);
        mavenRepositoryDir = ConfigUtils.getProperty(Constant.MAVEN_REPOSITORY_DIR);
    }

    @Override
    public Iterator getSuites() {
        testsToRun = scanClassPath();
        return testsToRun.iterator();
    }

    private TestsToRun scanClassPath() {
        final TestsToRun scannedClasses = scanResult.applyFilter(testChecker, testClassLoader);
        return runOrderCalculator.orderTestClasses(scannedClasses);
    }

    @Override
    public RunResult invoke(Object o) throws TestSetFailedException, ReporterException {
        final ReporterFactory reporterFactory = providerParameters.getReporterFactory();
        final RunListener reporter = reporterFactory.createReporter();
        iosSimulatorBridge = new IOSSimulatorBridge();

        try {
            roboTestListener = new RoboTestServer(reporter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Result result = new Result();
        RunNotifier runNotifier = getRunNotifier(result, roboTestListener);

        if (testsToRun == null) {
            testsToRun = scanClassPath();
        }

        for (Class testToRun : testsToRun) {
            try {
                executeTestSet(testToRun, reporter, runNotifier, config);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        runNotifier.fireTestRunFinished(result);

        return reporterFactory.close();
    }

    private void executeTestSet(Class testToRun, RunListener reporter, RunNotifier runNotifier,
        Config.Builder config) throws IOException {

        roboTestListener.startServer(serverPort);
        System.out.println("Compiling and running " + testToRun.getName());

        iosSimulatorBridge.setConfig(config);

        /* start socket listener */
        iosSimulatorBridge.executeTestSet(testToRun, reporter, runNotifier);

    }

    private RunNotifier getRunNotifier(Result result, RoboTestServer roboTestListener) {

        RunNotifier runNotifier = new RunNotifier();
        runNotifier.addListener(result.createListener());
        runNotifier.addListener(roboTestListener);
        return runNotifier;
    }
}
