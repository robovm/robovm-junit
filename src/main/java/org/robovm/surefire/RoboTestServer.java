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

import com.google.gson.GsonBuilder;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.robovm.surefire.internal.AtomicIntegerTypeAdapter;
import org.robovm.surefire.internal.DescriptionTypeAdapter;
import org.robovm.surefire.internal.FailureTypeAdapter;
import org.robovm.surefire.internal.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import static org.robovm.surefire.internal.Constant.*;

public class RoboTestServer extends org.junit.runner.notification.RunListener {

    private final RunListener reporter;
    ServerSocket serverSocket;

    public RoboTestServer(final RunListener reporter) throws IOException {
        this.reporter = reporter;
    }

    private ReportEntry createReportEntry(Description description) {
        return new SimpleReportEntry(getClassName(description), description.getDisplayName());
    }

    private ReportEntry createReportEntry(Failure failure) {
        Description description = failure.getDescription();

        return SimpleReportEntry.withException(getClassName(description),
            description.getDisplayName(),
            new LegacyPojoStackTraceWriter(getClassName(description), description.getDisplayName(),
                failure.getException()));

    }

    private String getClassName(Description description) {
        if (description == null) {
            return null;
        }
        String name = description.getDisplayName();

        if (name == null) {
            Description subDescription = description.getChildren().get(0);
            name = subDescription.getDisplayName();
        }
        return name;
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        SimpleReportEntry report = SimpleReportEntry.ignored("Test Ignored", description.getDisplayName(), "");
        reporter.testSkipped(report);
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        super.testRunStarted(description);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {

        super.testRunFinished(result);
        Logger.log("Fired finished");
        serverSocket.close();
        Logger.log("Server socket closed");
    }

    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(description);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        super.testFailure(failure);
    }

    public void startServer(final int port) {

        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try {
                    Logger.log("Starting server listener");
                    serverSocket = new ServerSocket(port);
                    Socket socket = serverSocket.accept();
                    String line;
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                    while ((line = reader.readLine()) != null) {
                        Logger.log("Read from socket " + line);
                        subscriber.onNext(line);
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.newThread())
            .observeOn(Schedulers.immediate())
            .subscribe(new Action1<String>() {
                @Override
                public void call(String jsonString) {
                    Logger.log("Processing result");

                    ResultObject resultObject = new GsonBuilder()
                        .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
                        .registerTypeAdapter(AtomicInteger.class,
                            new AtomicIntegerTypeAdapter())
                        .registerTypeAdapter(Failure.class, new FailureTypeAdapter())
                        .create()
                        .fromJson(jsonString, ResultObject.class);
                    Logger.log("de-serialized message");

                    switch (resultObject.getResultType()) {
                    case TEST_RUN_STARTED:
                        try {
                            Logger.log("Test run started");
                            testRunStarted(resultObject.getDescription());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case TEST_STARTED:
                        try {
                            Logger.log(
                                "Test " + resultObject.getDescription().getDisplayName()
                                    + " started");
                            testStarted(resultObject.getDescription());
                            reporter.testStarting(
                                createReportEntry(resultObject.getDescription()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case TEST_RUN_FINISHED:
                        try {
                            Logger.log("Test run finished");
                            testRunFinished(resultObject.getResult());
                            reporter.testSetCompleted(
                                new SimpleReportEntry("RoboVMTestRun", "RoboVMTestRun"));

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case TEST_FINISHED:
                        try {
                            Logger.log("Test finished");
                            testFinished(resultObject.getDescription());
                            reporter.testSucceeded(
                                createReportEntry(resultObject.getDescription()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case TEST_FAILURE:
                        try {
                            Logger.log("Test failed");
                            testFailure(resultObject.getFailure());
                            reporter.testFailed(
                                createReportEntry(resultObject.getFailure()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        Logger.log("Got result, but don't know what it is");
                        break;
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    System.err.println(
                        "Error receiving result from simulator " + throwable.getMessage());
                }
            }, new Action0() {
                @Override
                public void call() {
                    System.out.println("Done");
                }
            });

    }

}
