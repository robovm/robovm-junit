package org.robovm.junitbridge;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.Resource;
import org.robovm.devicebridge.ResultObject;
import org.robovm.devicebridge.RoboVMDeviceBridge;
import org.robovm.devicebridge.internal.runner.TestRunner;
import rx.observables.BlockingObservable;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;

public class RoboVMDeviceBridgeTest {

    private static Thread t;

    @Test
    public void testSuccessfulJunitRun() throws IllegalAccessException, InterruptedException, IOException,
            ClassNotFoundException, MojoExecutionException {

        final RoboVMDeviceBridge roboVMDeviceBridge = new RoboVMDeviceBridge();
        mock(TestRunner.class);

        final Config.Builder config = createConfig();
        final Config conf = roboVMDeviceBridge.compile(config);

        /* start simulator */
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                roboVMDeviceBridge.run(conf);
            }
        });

        t.start();

        BlockingObservable<ResultObject> blockingObservable = BlockingObservable.from(roboVMDeviceBridge
                .runTestsOnDevice("localhost", new String[] { "org.robovm.junitbridge.RunnerClass" }));
        int successfulTests = 0;
        int failedTests = 0;
        for (ResultObject resultObject : blockingObservable.toIterable()) {
            switch (resultObject.getResultType()) {

            case ResultObject.TEST_FINISHED:
                successfulTests++;
                break;

            case ResultObject.TEST_FAILURE:
                failedTests++;
                break;
            }
        }
        ;

        assertTrue("Successful tests: " + successfulTests, successfulTests == 2);
        assertTrue("Failed tests: " + failedTests, failedTests == 1);
        Thread.sleep(2000);
        System.out.println("Sleeping for retry");

        t.join();

    }

    private Config.Builder createConfig() throws IOException, ClassNotFoundException {
        Config.Builder config = new Config.Builder();

        config.read(new File(getClass().getResource("/robovm.xml").getFile()));

        /* include test classes */
        config.addClasspathEntry(new File(getClass().getResource("/").getFile()));

        /* include non-test classes */
        config.addClasspathEntry(new File(getClass().getResource("/").getFile() + "../classes"));

        config.addForceLinkClass(RunnerClass.class.getCanonicalName());

        return config;
    }

}
