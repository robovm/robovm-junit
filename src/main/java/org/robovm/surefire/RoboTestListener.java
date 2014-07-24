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

import org.apache.maven.surefire.report.RunListener;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.IOException;
import java.util.ArrayList;

import static org.robovm.surefire.internal.Constant.TEST_IGNORED;
import static org.robovm.surefire.internal.Constant.TEST_RUN_STARTED;
import static org.robovm.surefire.internal.Constant.TEST_RUN_FINISHED;
import static org.robovm.surefire.internal.Constant.TEST_STARTED;
import static org.robovm.surefire.internal.Constant.TEST_FINISHED;
import static org.robovm.surefire.internal.Constant.TEST_FAILURE;

public class RoboTestListener extends org.junit.runner.notification.RunListener {

    private final RunListener reporter;

    private IOSSimulatorBridge iosSimulatorBridge;
    private static ArrayList<String> failedTests = new ArrayList<String>();

    public RoboTestListener(RunListener reporter, String host, String port) throws IOException {
        this.reporter = reporter;
        iosSimulatorBridge = new IOSSimulatorBridge();
        iosSimulatorBridge.initiateConnectionToHost(host, port);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        iosSimulatorBridge.sendToHost(TEST_IGNORED, createDescriptionResult(description, TEST_IGNORED));
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        iosSimulatorBridge.sendToHost(TEST_RUN_STARTED, createDescriptionResult(description, TEST_RUN_STARTED));
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        iosSimulatorBridge.sendToHost(TEST_RUN_FINISHED, createResultResult(result, TEST_RUN_FINISHED));
    }

    @Override
    public void testStarted(Description description) throws Exception {
        iosSimulatorBridge.sendToHost(TEST_STARTED, createDescriptionResult(description, TEST_STARTED));
    }

    @Override
    public void testFinished(Description description) throws Exception {
        for (String failedTest : failedTests) {
            if (description.getDisplayName().equals(failedTest)) {
                return;
            }
        }
        iosSimulatorBridge.sendToHost(TEST_FINISHED, createDescriptionResult(description, TEST_FINISHED));
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        failedTests.add(failure.getDescription().getDisplayName());
        iosSimulatorBridge.sendToHost(TEST_FAILURE, createFailureResult(failure, TEST_FAILURE));
    }

    private ResultObject createFailureResult(Failure failure, int type) {
        ResultObject resultObject = new ResultObject();
        resultObject.setFailure(failure);
        resultObject.setResultType(type);
        return resultObject;
    }

    private ResultObject createDescriptionResult(Description description, int type) {
        ResultObject resultObject = new ResultObject();
        resultObject.setDescription(description);
        resultObject.setResultType(type);
        return resultObject;
    }

    private ResultObject createResultResult(Result result, int type) {
        ResultObject resultObject = new ResultObject();
        resultObject.setResult(result);
        resultObject.setResultType(type);
        return resultObject;
    }

}
