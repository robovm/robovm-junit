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

package org.robovm.devicebridge;

import com.google.gson.GsonBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.robovm.compiler.AppCompiler;
import org.robovm.compiler.Version;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.target.LaunchParameters;
import org.robovm.compilerhelper.RoboVMResolver;
import org.robovm.devicebridge.internal.Logger;
import org.robovm.devicebridge.internal.adapters.AtomicIntegerTypeAdapter;
import org.robovm.devicebridge.internal.adapters.DescriptionTypeAdapter;
import org.robovm.devicebridge.internal.runner.TestRunner;
import rx.Observable;
import rx.Subscriber;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bridge between device and client (IDE, gradle, maven...)
 */
public class RoboVMDeviceBridge {

    private Socket socket;
    private int PORT = 8889;
    private String LOAD = "load";

    public RoboVMDeviceBridge() {
    }

    public Observable<ResultObject> runTestsOnDevice(final String hostname, final String[] testsToRun) {
        return Observable.create(new Observable.OnSubscribe<ResultObject>() {
            @Override
            public void call(Subscriber<? super ResultObject> subscriber) {

                int connectionCount = 8;

                while (true) {
                    try {
                        System.out.println("Trying to contact device...");
                        socket = new Socket(hostname, PORT);
                        break;
                    } catch (IOException e) {
                        System.out.print("sleeping for retry");
                        try {
                            if (connectionCount <= 0) {
                                subscriber.onError(new RuntimeException(
                                        "Connection to device failed, check device logs for failure reason"));
                                return;
                            }
                            connectionCount--;
                            Thread.sleep(8000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }

                int test = 0;
                try {
                    if (!subscriber.isUnsubscribed()) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));

                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                        String line = null;
                        for (int i = 0; i < testsToRun.length; i++) {
                            writer.write(LOAD + " " + testsToRun[test++] + "\n");
                            writer.flush();

                            while ((line = reader.readLine()) != null) {
                                Logger.log("Read from socket " + line);
                                ResultObject resultObject = jsonToResultObject(line);
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onNext(resultObject);
                                }
                                if (resultObject.getResultType() == ResultObject.TEST_RUN_FINISHED) {
                                    break;
                                }
                            }
                        }

                        writer.close();
                        socket.close();
                        subscriber.onCompleted();
                    }
                } catch (IOException ie) {
                    subscriber.onError(ie);
                }
            }
        });
    }

    private ResultObject jsonToResultObject(String jsonString) {
        ResultObject resultObject = new GsonBuilder()
                .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
                .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter())
                .registerTypeAdapter(Failure.class, new DescriptionTypeAdapter.FailureTypeAdapter())
                .create()
                .fromJson(jsonString, ResultObject.class);

        return resultObject;
    }

    public Config compile(Config.Builder configBuilder) throws IOException, MojoExecutionException {

        if (configBuilder == null) {
            throw new IllegalArgumentException("RoboVM configuration cannot be null");
        }

        configBuilder = mergeInJUnitDefaults(configBuilder);

        Logger.log("Building Runner");

        configBuilder.home(new Config.Home(getRoboVMHome()));
        Config config = configBuilder.build();
        new AppCompiler(config).compile();

        return config;
    }

    public void run(Config config) {
        try {
            Logger.log("Launching Simulator");
            LaunchParameters launchParameters = config.getTarget().createLaunchParameters();
            config.getTarget().launch(launchParameters).waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Config.Builder mergeInJUnitDefaults(Config.Builder configBuilder) {

        RoboVMResolver resolver = new RoboVMResolver();

        /* add classpath entries */
        configBuilder
                .addClasspathEntry(resolver.resolveArtifact("org.robovm:robovm-cocoatouch:" + Version.getVersion()));
        configBuilder.addClasspathEntry(resolver.resolveArtifact("org.robovm:robovm-objc:" + Version.getVersion()));
        configBuilder.addClasspathEntry(resolver.resolveArtifact("junit:junit:4.4"));
        configBuilder.addClasspathEntry(resolver.resolveArtifact("com.google.code.gson:gson:2.2.4"));
        configBuilder.addClasspathEntry(resolver.resolveArtifact("biz.source_code:base64coder:2010-12-19"));
        configBuilder.addClasspathEntry(resolver.resolveArtifact("org.apache.maven.surefire:surefire-api:2.17"));
        configBuilder.addClasspathEntry(resolver.resolveArtifact("com.netflix.rxjava:rxjava-core:0.18.4"));

        configBuilder.mainClass(TestRunner.class.getCanonicalName());

        return configBuilder;
    }

    public File getRoboVMHome() throws IOException, MojoExecutionException {
        RoboVMResolver resolver = new RoboVMResolver();
        File compilerFile = resolver.resolveRoboVMCompilerArtifact();
        File unpackDir = resolver.unpackInPlace(compilerFile);
        return new File(unpackDir, "robovm-" + Version.getVersion());
    }
}
