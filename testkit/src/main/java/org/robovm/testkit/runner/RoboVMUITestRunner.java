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

package org.robovm.testkit.runner;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.robovm.compiler.AppCompiler;
import org.robovm.compiler.Version;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.target.LaunchParameters;
import org.robovm.junit.client.TestClient;
import org.robovm.maven.resolver.RoboVMResolver;
import org.robovm.testkit.shadow.ShadowUIApplication;

public class RoboVMUITestRunner extends AbstractJ4Runner {

    TestClient testClient;
    Result result;
    Process process = null;

    public RoboVMUITestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);

    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        if (!System.getProperty("os.name").contains("iOS")) {
            if (testClient == null) {
                testClient = new TestClient(ShadowUIApplication.class);
                RoboVMResolver roboVMResolver = new RoboVMResolver();
                result = new Result();
                notifier.addListener(result.createListener());

                try {
                    testClient.setRunListener(getRunListener(notifier));
                    Config.Builder builder = createConfig();
                    builder.addClasspathEntry(roboVMResolver.resolveArtifact(
                            "org.robovm:robovm-junit-testkit:" + Version.getVersion()).asFile());
                    builder.addClasspathEntry(roboVMResolver.resolveArtifact(
                            "org.robovm:robovm-junit-protocol:" + Version.getVersion()).asFile());
                    builder.addClasspathEntry(roboVMResolver.resolveArtifact(
                            "org.robovm:robovm-junit-server:" + Version.getVersion()).asFile());
                    builder.addClasspathEntry(roboVMResolver.resolveArtifact("junit:junit:4.12").asFile());
                    builder.addForceLinkClass("org.robovm.testkit.shadow.ShadowUIApplication");

                    builder.addFramework("UIKit");
                    builder.addFramework("Foundation");

                    Config config = testClient.configure(builder).build();
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
                    process.destroy();
                    notifier.fireTestRunFinished(result);
                    throw new RuntimeException("RoboVM test run failed", t);
                }
            }

            try {
                testClient.runTests(clazz.getName() + "#" + method.getMethod().getName()).flush();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Description description = describeChild(method);
            runLeaf(methodBlock(method), description, notifier);
        }
    }
}
