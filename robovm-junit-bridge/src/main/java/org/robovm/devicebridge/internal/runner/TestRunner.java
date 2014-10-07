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

package org.robovm.devicebridge.internal.runner;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.robovm.apple.foundation.Foundation;
import org.robovm.devicebridge.internal.listener.RoboTestListener;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main TestRunner class run on the device/simulator
 */
public class TestRunner {

    private static int PORT = 8889;
    private static String LOAD = "load";
    private static RoboTestListener listener;
    private static JUnitCore jUnitCore;

    public static void main(String[] args) {
        log("Running");
        /* register global uncaught exception handler */
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    log("TestRunner threw exception " + e.getMessage());
                    printStackTrace(e);
                } catch (Exception e1) {
                    printStackTrace(e1);
                }
            }
        });

        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try {
                    log("Starting server");
                    ServerSocket serverSocket = new ServerSocket(PORT);
                    Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    OutputStream out = socket.getOutputStream();

                    listener = new RoboTestListener(out);

                    jUnitCore = new JUnitCore();
                    jUnitCore.addListener(listener);

                    String line = null;

                    while ((line = reader.readLine()) != null) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(line);
                        }
                    }

                    subscriber.onCompleted();
                } catch (IOException e) {
                    subscriber.onError(e);
                    printStackTrace(e);
                }
            }
        }).filter(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return s.length() > 0;
            }
        }).subscribe(new Action1<String>() {
            @Override
            public void call(String string) {
                processCommand(string);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                log("Error running tests");
                printStackTrace(throwable);

            }
        }, new Action0() {
            @Override
            public void call() {
                log("Finished test run");
            }
        });

        System.exit(1);
    }

    /**
     * Process command for running a JUnit test class or method
     * 
     * @param command
     */
    private static void processCommand(String command) {

        if (command.startsWith(LOAD)) {
            String classLine = command.replaceAll(LOAD + " ", "");

            if (classLine.contains("#")) {
                log("Running method");
                String classMethod[] = classLine.split("#(?=[^\\.]+$)");
                runMethodOnly(jUnitCore, classMethod[0], classMethod[1]);
            } else {
                log("Running whole class " + classLine);
                runClass(jUnitCore, classLine);
                log("done");
            }
        }
    }

    /**
     * Print stack trace using NSLog
     * 
     * @param throwable
     *            Throwable
     */
    private static void printStackTrace(Throwable throwable) {
        for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
            Foundation.log("\t" + stackTraceElement.toString());
        }
    }

    /**
     * Run a single method test
     * 
     * @param jUnitCore
     * @param className
     * @param method
     */
    private static void runMethodOnly(JUnitCore jUnitCore, String className, String method) {
        try {
            jUnitCore.run(Request.method(Class.forName(className), method));
        } catch (ClassNotFoundException e) {
            log("Class not found: " + className);
            printStackTrace(e);
        }
    }

    /**
     * Run class methods in the specified class
     * 
     * @param jUnitCore
     * @param className
     */
    private static void runClass(JUnitCore jUnitCore, String className) {
        try {
            jUnitCore.run(Class.forName(className));
        } catch (ClassNotFoundException e) {
            log("Class not found: " + className);
            printStackTrace(e);
        }
    }

    private static void log(String logLine) {
        Foundation.log(logLine);
        System.err.println(logLine);
    }

}