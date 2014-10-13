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

package org.robovm.junit.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.robovm.compiler.AppCompiler;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.plugin.LaunchPlugin;
import org.robovm.compiler.target.LaunchParameters;
import org.robovm.compiler.util.io.Fifos;
import org.robovm.compiler.util.io.OpenOnReadFileInputStream;
import org.robovm.compiler.util.io.OpenOnWriteFileOutputStream;
import org.robovm.junit.protocol.AtomicIntegerTypeAdapter;
import org.robovm.junit.protocol.DescriptionTypeAdapter;
import org.robovm.junit.protocol.ResultObject;

import rx.Observable;
import rx.Subscriber;

import com.google.gson.GsonBuilder;

/**
 * Bridge between device and client (IDE, gradle, maven...)
 */
public class RoboVMDeviceBridge {

    private static final String SERVER_CLASS_NAME = "org.robovm.junit.server.TestServer";

    private Socket socket;
    private String LOAD = "load";
    private Process process;
    private ServerPortReader serverPortReader;

    public RoboVMDeviceBridge() {
    }

    public Observable<ResultObject> runTests(final Config config, final String[] testsToRun) {
        return Observable.create(new Observable.OnSubscribe<ResultObject>() {
            @Override
            public void call(Subscriber<? super ResultObject> subscriber) {

                try {
                    config.getLogger().debug("Trying to connect to test server running on port %d", serverPortReader.port);
                    socket = new Socket("localhost", serverPortReader.port);
                } catch (IOException e) {
                    if (config.getOs() == OS.ios && config.getArch() == Arch.thumbv7) {
                        subscriber.onError(new RuntimeException(
                                "Connection to device failed, check device logs for failure reason"));
                    } else {
                        subscriber.onError(new RuntimeException(
                                "Connection to test server failed"));
                    }
                    subscriber.onCompleted();
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
                                ResultObject resultObject = jsonToResultObject(line);
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onNext(resultObject);
                                }
                                if (resultObject.getResultType() == ResultObject.TEST_RUN_FINISHED) {
                                    break;
                                }
                            }
                        }
                        subscriber.onCompleted();

                        writer.close();
                        socket.close();
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

    public Config compile(Config.Builder configBuilder) throws IOException {

        if (configBuilder == null) {
            throw new IllegalArgumentException("RoboVM configuration cannot be null");
        }

        configBuilder.mainClass(SERVER_CLASS_NAME);
        configBuilder.addForceLinkClass("com.android.org.conscrypt.OpenSSLProvider");
        configBuilder.addForceLinkClass("com.android.org.conscrypt.OpenSSLMessageDigestJDK**");
        
        Config config = configBuilder.build();
        config.getLogger().info("Building test runner");
        new AppCompiler(config).compile();

        return config;
    }

    public void start(Config config, LaunchParameters launchParameters) throws IOException {
        if (process != null) {
            throw new IllegalStateException("Already started");
        }
        ArrayList<String> args = new ArrayList<>(launchParameters.getArguments());
        args.add("-rvm:log=fatal");
        launchParameters.setArguments(args);
        
        for (LaunchPlugin plugin : config.getLaunchPlugins()) {
            plugin.beforeLaunch(config, launchParameters);
        }
        
        File oldStdErr = launchParameters.getStderrFifo();
        File newStdErr = Fifos.mkfifo("junit-err-proxy");
        launchParameters.setStderrFifo(newStdErr);
        
        try {
            process = config.getTarget().launch(launchParameters);
            serverPortReader = new ServerPortReader(config, launchParameters, process, oldStdErr, newStdErr);
            for (LaunchPlugin plugin : config.getLaunchPlugins()) {
                plugin.afterLaunch(config, launchParameters, process);
            }
            
            while (!serverPortReader.stopped && serverPortReader.port == -1) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }

            // process stopped without providing a port
            if (serverPortReader.port == -1) {
                throw new IOException("Process stopped prematurely");
            }
            
        } catch (Throwable e) {
            for (LaunchPlugin plugin : config.getLaunchPlugins()) {
                plugin.launchFailed(config, launchParameters);
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        if (process == null) {
            throw new IllegalStateException("Not started");
        }

        if (serverPortReader != null) {
            serverPortReader.running = false;
            serverPortReader.thread.interrupt();
            serverPortReader = null;            
        }
        
        try {
            process.waitFor();
            process.destroy();
            socket.close();
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
    }
    
    /**
     * Wraps the error stream of the runner and reads the port which the runner 
     * will be print to stderr. Will continue to wrap the error stream until the 
     * runner process has finished.
     */
    private static class ServerPortReader {
        volatile boolean running = false;
        volatile boolean stopped = false;
        volatile int port = -1;
        Thread thread;
        volatile boolean closeOutOnExit = true;

        public ServerPortReader(final Config config, final LaunchParameters params, final Process process,
                final File oldStdError, final File newStdError) throws IOException {

            final BufferedReader in = new BufferedReader(new InputStreamReader(new OpenOnReadFileInputStream(newStdError)));
            BufferedWriter writer = null;
            if (oldStdError != null) {
                writer = new BufferedWriter(new OutputStreamWriter(new OpenOnWriteFileOutputStream(oldStdError)));
            } else {
                writer = new BufferedWriter(new OutputStreamWriter(System.err));
                closeOutOnExit = false;
            }
            final BufferedWriter out = writer;

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        running = true;
                        while (running && !isProcessStopped(process)) {
                            String line = in.readLine();
                            if (line != null) {
                                if (line.startsWith(SERVER_CLASS_NAME + ": port=")) {
                                    port = Integer.parseInt(line.split("=")[1]);
                                    config.getLogger().debug("Test runner port: " + port);
                                } else {
                                    out.write(line + "\n");
                                    out.flush();
                                }
                            }
                        }
                    } catch (Throwable t) {
                        config.getLogger().error("Couldn't forward error stream", t.getMessage());
                    } finally {
                        if (closeOutOnExit) {
                            IOUtils.closeQuietly(out);
                        }
                        IOUtils.closeQuietly(in);
                        stopped = true;
                    }
                }

                private boolean isProcessStopped(Process process) {
                    try {
                        process.exitValue();
                        return true;
                    } catch (IllegalThreadStateException e) {
                        return false;
                    }
                }
            });
            thread.setName("JUnit Port Reader");
            thread.setDaemon(true);
            thread.start();
        }
    }
}
