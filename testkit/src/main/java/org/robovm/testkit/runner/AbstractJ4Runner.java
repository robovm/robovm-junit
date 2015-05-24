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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.robovm.compiler.Version;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.log.ConsoleLogger;
import org.robovm.compiler.log.Logger;
import org.robovm.compiler.target.ios.ProvisioningProfile;
import org.robovm.compiler.target.ios.SigningIdentity;
import org.robovm.maven.resolver.RoboVMResolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AbstractJ4Runner extends BlockJUnit4ClassRunner {

    protected final Class<?> clazz;
    protected final static String PROP_LOG_DEBUG = "robovm.test.enableDebugLogging";
    protected final static String PROP_SERVER_DEBUG = "robovm.test.enableServerLogging";
    protected final static String PROP_OS = "robovm.test.os";
    protected final static String PROP_ARCH = "robovm.test.arch";
    protected final static String PROP_CONFIG_FILE = "robovm.test.configFile";
    protected final static String PROP_PROPERTIES_FILE = "robovm.test.propertiesFile";
    protected final static String PROP_IOS_SIGNING_IDENTITY = "robovm.test.iosSignIdentity";
    protected final static String PROP_IOS_PROVISIONING_PROFILE = "robovm.test.iosProvisioningProfile";
    protected final static String PROP_IOS_SKIP_SIGNING = "robovm.test.iosSkipSigning";

    public AbstractJ4Runner(Class<?> clazz) throws InitializationError {
        super(clazz);
        this.clazz = clazz;
    }

    protected org.junit.runner.notification.RunListener getRunListener(final RunNotifier runNotifier) {
        return new org.junit.runner.notification.RunListener() {
            public void testRunStarted(Description description) throws Exception {
                runNotifier.fireTestRunStarted(description);
            }

            public void testRunFinished(Result result) throws Exception {
                runNotifier.fireTestRunFinished(result);
            }

            public void testStarted(Description description) throws Exception {
                runNotifier.fireTestStarted(description);
            }

            public void testFinished(Description description) throws Exception {
                runNotifier.fireTestFinished(description);
            }

            public void testFailure(Failure failure) throws Exception {
                runNotifier.fireTestFailure(failure);
            }

            public void testAssumptionFailure(Failure failure) {
                runNotifier.fireTestAssumptionFailed(failure);
            }

            public void testIgnored(Description description) throws Exception {
                runNotifier.fireTestIgnored(description);
            }
        };
    }

    protected Config.Builder createConfig() throws IOException {
        Config.Builder configBuilder = new Config.Builder();
        final Logger logger = new ConsoleLogger(true);
        configBuilder.logger(logger);

        RoboVMResolver roboVMResolver = new RoboVMResolver();
        roboVMResolver.setLogger(new org.robovm.maven.resolver.Logger() {
            public void info(String logLine) {
                logger.info(logLine);
            }

            public void debug(String logLine) {
                logger.debug(logLine);
            }
        });

        Config.Home home = null;
        try {
            home = Config.Home.find();
        } catch (Throwable t) {
        }
        if (home == null || !home.isDev()) {
            home = new Config.Home(roboVMResolver.resolveAndUnpackRoboVMDistArtifact(Version.getVersion()));
        }
        configBuilder.home(home);
        if (home.isDev()) {
            configBuilder.useDebugLibs(Boolean.getBoolean("robovm.useDebugLibs"));
            configBuilder.dumpIntermediates(true);
        }

        File basedir = new File(System.getProperty("user.dir"));
        if (System.getProperties().containsKey(PROP_PROPERTIES_FILE)) {
            File propertiesFile = new File(System.getProperty(PROP_PROPERTIES_FILE));
            if (!propertiesFile.exists()) {
                throw new FileNotFoundException("Failed to find specified "
                        + PROP_PROPERTIES_FILE + ": " + propertiesFile.getAbsolutePath());
            }
            logger.debug("Loading RoboVM config properties from "
                    + propertiesFile.getAbsolutePath());
            configBuilder.addProperties(propertiesFile);
        } else {
            configBuilder.readProjectProperties(basedir, true);
        }

        if (System.getProperties().containsKey(PROP_CONFIG_FILE)) {
            File configFile = new File(System.getProperty(PROP_CONFIG_FILE));
            if (!configFile.exists()) {
                throw new FileNotFoundException("Failed to find specified "
                        + PROP_CONFIG_FILE + ": " + configFile.getAbsolutePath());
            }
            logger.debug("Loading RoboVM config from " + configFile.getAbsolutePath());
            configBuilder.read(configFile);
        } else {
            configBuilder.readProjectConfig(basedir, true);
        }

        if (System.getProperty(PROP_OS) != null) {
            configBuilder.os(OS.valueOf(System.getProperty(PROP_OS)));
        }
        if (System.getProperty(PROP_ARCH) != null) {
            configBuilder.arch(Arch.valueOf(System.getProperty(PROP_ARCH)));
        }
        if (Boolean.getBoolean(PROP_IOS_SKIP_SIGNING)) {
            configBuilder.iosSkipSigning(true);
        } else {
            if (System.getProperty(PROP_IOS_SIGNING_IDENTITY) != null) {
                String iosSignIdentity = System.getProperty(PROP_IOS_SIGNING_IDENTITY);
                logger.debug("Using explicit iOS Signing identity: " + iosSignIdentity);
                configBuilder.iosSignIdentity(SigningIdentity.find(
                        SigningIdentity.list(), iosSignIdentity));
            }
            if (System.getProperty(PROP_IOS_PROVISIONING_PROFILE) != null) {
                String iosProvisioningProfile = System.getProperty(PROP_IOS_PROVISIONING_PROFILE);
                logger.debug("Using explicit iOS provisioning profile: " + iosProvisioningProfile);
                configBuilder.iosProvisioningProfile(ProvisioningProfile.find(
                        ProvisioningProfile.list(), iosProvisioningProfile));
            }
        }

        for (String clazz : getNonTestClasses(new File(System.getProperty("user.dir") + File.separator + "target"
                + File.separator + "classes"))) {
            configBuilder.addForceLinkClass(clazz);
        }

        configBuilder.addClasspathEntry(roboVMResolver.resolveArtifact(
                "org.robovm:robovm-cocoatouch:" + Version.getVersion()).asFile());
        configBuilder.addClasspathEntry(roboVMResolver
                .resolveArtifact("org.robovm:robovm-objc:" + Version.getVersion()).asFile());

        configBuilder.addClasspathEntry(new File(System.getProperty("user.dir") + File.separator + "target"
                + File.separator + "classes"));
        configBuilder.addClasspathEntry(new File(System.getProperty("user.dir") + File.separator + "target"
                + File.separator + "test-classes"));
        configBuilder.addForceLinkClass(getName());
        configBuilder.skipInstall(true);

        return configBuilder;
    }

    private List<String> getNonTestClasses(File file) {
        List<String> returnList = new ArrayList<>();
        for (File dir : FileUtils.listFilesAndDirs(file, FalseFileFilter.INSTANCE, DirectoryFileFilter.DIRECTORY)) {
            for (File file1 : dir.listFiles()) {
                if (file1.getName().endsWith("class")) {
                    String[] pkg = dir.getAbsoluteFile().toString().split("classes/");
                    if (pkg.length > 1) {
                        returnList.add(pkg[1].replaceAll(String.valueOf(File.separatorChar), ".") + ".*");
                    }
                    break;
                }
            }

        }
        return returnList;
    }

}
