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
package org.robovm.surefire;

import com.google.gson.GsonBuilder;
import org.apache.maven.surefire.report.RunListener;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.robovm.compiler.AppCompiler;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.log.ConsoleLogger;
import org.robovm.compiler.target.LaunchParameters;
import org.robovm.surefire.internal.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.Thread;

import static org.robovm.surefire.internal.Constant.*;

public class IOSSimulatorBridge {

    private Class testToRun;

    private Socket hostSocket;
    private Config.Builder configBuilder;
    private String mavenRepositoryDir;
    private boolean debug = false;
    private String installDir;

    public void executeTestSet(Class testToRun, RunListener reporter, RunNotifier runNotifier) throws IOException {
        this.testToRun = testToRun;
        String value;
        mavenRepositoryDir = ConfigUtils.getProperty(MAVEN_REPOSITORY_DIR);
        installDir = ConfigUtils.getProperty(INSTALL_DIR);

        if ((value = ConfigUtils.getSystemProperty(Constant.DEBUG)) != null) {
            if (value.equals("true")) {
                debug = true;
            }
        }

        compileAndRunTest();
    }

    private void compileAndRunTest() throws IOException {

        if (configBuilder == null) {
            configBuilder = new Config.Builder();
        }

        configBuilder.home(RobovmHelper.findRoboHome());
        configBuilder.addClasspathEntry(new File("target/test-classes"));
        configBuilder.mainClass("org.robovm.surefire" + getClassName(testToRun.getName()) + "Runner");
        configBuilder.addClasspathEntry(new File(mavenRepositoryDir
            + "/repository/org/apache/maven/surefire/surefire-junit4/2.12.4/surefire-junit4-2.12.4.jar"));
        configBuilder.addClasspathEntry(new File("/Users/ash/Downloads/robovm-0.0.14/lib/robovm-compiler.jar"));
        configBuilder.addClasspathEntry(
            new File(mavenRepositoryDir + "/repository/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar"));
        configBuilder.addClasspathEntry(
            new File(mavenRepositoryDir + "/repository/junit/junit/4.8.2/junit-4.8.2.jar"));
        configBuilder.addClasspathEntry(new File(mavenRepositoryDir
            + "/repository/org/robovm/robovm-surefire-provider/1.0/robovm-surefire-provider-1.0.jar"));
        configBuilder.addClasspathEntry(new File(mavenRepositoryDir
            + "/repository/org/apache/maven/surefire/surefire-api/2.17/surefire-api-2.17.jar"));
        configBuilder.addClasspathEntry(new File(mavenRepositoryDir
            + "/repository/biz/source_code/base64coder/2010-12-19/base64coder-2010-12-19.jar"));
        configBuilder.addForceLinkClass("com.google.gson.GsonBuilder");
        configBuilder.addForceLinkClass("org.robovm.surefire.internal.*");
        configBuilder.addForceLinkClass("org.junit.runner.Description");
        configBuilder.addForceLinkClass("org.junit.runner.Result");
        configBuilder.addForceLinkClass("org.apache.maven.surefire.report.RunListener");
        configBuilder.addForceLinkClass("org.apache.harmony.security.provider.cert.DRLCertFactory");
        configBuilder.addForceLinkClass("com.android.org.bouncycastle.jce.provider.BouncyCastleProvider");
        configBuilder.addForceLinkClass("org.apache.harmony.security.provider.crypto.CryptoProvider");
        configBuilder.addForceLinkClass("com.android.org.conscrypt.*");
        configBuilder.installDir(new File(installDir));
        configBuilder.os(OS.ios);
        configBuilder.arch(Arch.x86);
        configBuilder.debug(false);
        if (debug) {
            configBuilder.logger(new ConsoleLogger(true));
        }

        new File(installDir).mkdirs();

        Logger.log("Building Runner");
        AppCompiler compiler = new AppCompiler(configBuilder.build());
        compiler.compile();

        try {
            Config config = configBuilder.build();

            Logger.log("Launching Simulator");
            LaunchParameters launchParameters = config.getTarget().createLaunchParameters();
            config.getTarget().launch(launchParameters).waitFor();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getClassName(String name) {
        if (name.lastIndexOf('.') != -1) {
            return name.substring(name.lastIndexOf('.'));
        }
        return name;
    }

    public void initiateConnectionToHost(String ip, String port) throws IOException {
        hostSocket = new Socket(ip, Integer.parseInt(port));
    }

    public void sendToHost(int type, ResultObject message) {

        if (type == TEST_RUN_FINISHED) {
            try {
                transmit(message);
                hostSocket.close();
                Logger.log("Socket closed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                transmit(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void transmit(ResultObject message) throws IOException, InterruptedException {

        if (hostSocket == null) {
            throw new RuntimeException("Connection to host died");
        } else {
            Logger.log("Transmitting to host");
            if (hostSocket.isConnected()) {
                Logger.log("socket connected");
            } else {
                Logger.log("socket not connected");
            }

            PrintWriter writer = null;

            String transmitMessage = new GsonBuilder()
                .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
                .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter())
                .registerTypeAdapter(Failure.class, new FailureTypeAdapter())
                .registerTypeAdapter(Throwable.class, new ThrowableTypeAdapter())
                .create().toJson(message);

            Logger.log("Client thread: " + Thread.currentThread().getId());
            Logger.log("GSON created");
            Logger.log("Sending : " + transmitMessage);
            writer = new PrintWriter(hostSocket.getOutputStream(), true);
            writer.println(transmitMessage);
            writer.flush();
            /* Give server time to process resonse */
            Thread.sleep(2000);

            Logger.log("transmitted");
        }
    }

    public void setConfig(Config.Builder config) {
        this.configBuilder = config;
    }

}
