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
package org.robovm.junit.server;

import static org.robovm.junit.protocol.ResultObject.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.robovm.junit.deps.com.google.gson.GsonBuilder;
import org.robovm.junit.protocol.AtomicIntegerTypeAdapter;
import org.robovm.junit.protocol.DescriptionTypeAdapter;
import org.robovm.junit.protocol.FailureTypeAdapter;
import org.robovm.junit.protocol.ResultObject;
import org.robovm.junit.protocol.ThrowableTypeAdapter;

/**
 * JUnit RunListener which sends results via an output stream (eg. socket) to a
 * listening instance (eg. surefire provider)
 */
public class RoboTestListener extends org.junit.runner.notification.RunListener {

    private OutputStream out;

    private static ArrayList<String> failedTests = new ArrayList<String>();

    public RoboTestListener(OutputStream out) {
        this.out = out;
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        sendToHost(TEST_IGNORED, createDescriptionResult(description, TEST_IGNORED));
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        sendToHost(TEST_RUN_STARTED, createDescriptionResult(description, TEST_RUN_STARTED));
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        sendToHost(TEST_RUN_FINISHED, createResultResult(result, TEST_RUN_FINISHED));
    }

    @Override
    public void testStarted(Description description) throws Exception {
        sendToHost(TEST_STARTED, createDescriptionResult(description, TEST_STARTED));
    }

    @Override
    public void testFinished(Description description) throws Exception {
        for (String failedTest : failedTests) {
            if (description.getDisplayName().equals(failedTest)) {
                return;
            }
        }
        sendToHost(TEST_FINISHED, createDescriptionResult(description, TEST_FINISHED));
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        failedTests.add(failure.getDescription().getDisplayName());
        sendToHost(TEST_FAILURE, createFailureResult(failure, TEST_FAILURE));
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

    public void sendToHost(int type, ResultObject message) {

        try {
            transmit(message);
        } catch (Exception e) {
            // Lost connection to host for some reason. We cannot recover from
            // this in any way.
            throw new Error(e);
        }

    }

    private void transmit(ResultObject message) throws IOException, InterruptedException {

        PrintWriter writer;
        
        String transmitMessage = new GsonBuilder()
                .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
                .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter())
                .registerTypeAdapter(Failure.class, new FailureTypeAdapter())
                .registerTypeAdapter(Throwable.class, new ThrowableTypeAdapter())
                .create().toJson(message);

        writer = new PrintWriter(out, true);
        writer.println(transmitMessage);
        writer.flush();
    }

}
