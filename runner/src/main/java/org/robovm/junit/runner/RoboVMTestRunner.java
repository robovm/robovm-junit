/*
 * Copyright (C) 2014 RoboVM AB
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

package org.robovm.junit.runner;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.robovm.compiler.AppCompiler;
import org.robovm.compiler.Version;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.target.LaunchParameters;
import org.robovm.junit.client.TestClient;
import org.robovm.maven.resolver.RoboVMResolver;

public class RoboVMTestRunner extends org.robovm.junit.runner.AbstractJ4Runner {

    TestClient testClient;
    Result result;
    Process process = null;

    public RoboVMTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        /**
         * This method gets called once in the JVM and once in RVM.
         * We need to make sure that when this method is called in the JVM
         * an RVM compilation and execution job starts
         *
         * When run in RVM it should run like any other junit4 method call and call 'runLeft'
         */
        if (System.getProperty("java.runtime.name").contains("Java")) {
          compileAndRun(method, notifier);
        } else {
           runTest(method, notifier);
        }
    }

    /**
     * Run JUnit test
     * @param method to test
     * @param notifier to report status
     */
    private void runTest(FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        runLeaf(methodBlock(method), description, notifier);
    }


    /**
     * Compile class, if required, and launch in RVM
     * @param method to run
     * @param notifier to report status
     */
    private void compileAndRun(FrameworkMethod method, RunNotifier notifier) {
        /* if testClient is null, then simulator/RVM etc hasn't been started */
        if (testClient == null) {
            testClient = new TestClient();
            RoboVMResolver roboVMResolver = new RoboVMResolver();
            result = new Result();
            notifier.addListener(result.createListener());

            try {
                testClient.setRunListener(getRunListener(notifier));
                Config.Builder builder = createConfig();
                builder.addClasspathEntry(roboVMResolver.resolveArtifact(
                        "org.robovm:robovm-junit-runner:" + Version.getVersion()).asFile());
                builder.addClasspathEntry(roboVMResolver.resolveArtifact(
                        "org.robovm:robovm-junit-protocol:" + Version.getVersion()).asFile());
                builder.addClasspathEntry(roboVMResolver.resolveArtifact(
                        "org.robovm:robovm-junit-server:" + Version.getVersion()).asFile());
                builder.addClasspathEntry(roboVMResolver.resolveArtifact("junit:junit:4.12").asFile());

                builder.addFramework("Foundation");

                builder = testClient.configure(builder);
                if (clazz.isAnnotationPresent(TargetType.class)) {
                    TargetType targetType = clazz.getAnnotation(TargetType.class);
                    builder.arch(getArch(targetType));
                    builder.os(getOS(targetType));
                    builder.targetType("console");
                } else {
                    /*default to a mac console app*/
                    builder.arch(Arch.x86_64);
                    builder.os(OS.macosx);
                    builder.targetType("console");
                }
                Config config = builder.build();


                config.getLogger().info("Building RoboVM tests for: %s (%s)", config.getOs(), config.getArch());
                config.getLogger().info("This could take a while, especially the first time round");

                AppCompiler appCompiler = new AppCompiler(config);
                appCompiler.compile();

                LaunchParameters launchParameters = config.getTarget().createLaunchParameters();
                if (Boolean.getBoolean(PROP_SERVER_DEBUG)) {
                    launchParameters.getArguments().add("-rvm:Drobovm.debug=true");
                }

                process = appCompiler.launchAsync(launchParameters);
            } catch (Throwable t) {
                if (process != null) {
                    process.destroy();
                }
                notifier.fireTestRunFinished(result);
                throw new RuntimeException("RoboVM test run failed", t);
            }
        }
        try {
            testClient.runTests(clazz.getName() + "#" + method.getMethod().getName()).flush();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private OS getOS(TargetType targetType) {
        String os = targetType.name();
        switch (os) {
            case TargetType.MAC_CONSOLE:
                return OS.macosx;
            case TargetType.LINUX_CONSOLE:
                return OS.linux;
        }
        return OS.getDefaultOS();
    }

    private Arch getArch(TargetType targetType) {
        String arch = targetType.arch();
        switch(arch) {
            case TargetType.X86_64:
                return Arch.x86_64;
            case TargetType.X86_32:
                return Arch.x86;
        }
        return Arch.getDefaultArch();
    }

}
