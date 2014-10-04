package org.robovm.junitbridge;

import com.google.gson.GsonBuilder;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.robovm.devicebridge.ResultObject;
import org.robovm.devicebridge.internal.adapters.AtomicIntegerTypeAdapter;
import org.robovm.devicebridge.internal.adapters.DescriptionTypeAdapter;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class GsonTest {

    @Test
    public void testResultObjectSerialization() {
        ResultObject resultObject = new ResultObject();
        Description description = Description.createSuiteDescription("test");
        Description subDescription = Description.createSuiteDescription("test2");

        description.addChild(subDescription);

        resultObject.setResult(new Result());
        resultObject.setDescription(description);
        resultObject.setFailure(
                new Failure(Description.createSuiteDescription("test"), new RuntimeException("error")));

        String jsonString = new GsonBuilder()
                .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
                .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter())
                .registerTypeAdapter(Failure.class, new DescriptionTypeAdapter.FailureTypeAdapter())
                .create()
                .toJson(resultObject);
        assertTrue(jsonString != null);

        ResultObject resultObject2 = new GsonBuilder()
                .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
                .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter())
                .registerTypeAdapter(Failure.class, new DescriptionTypeAdapter.FailureTypeAdapter())
                .create()
                .fromJson(jsonString, ResultObject.class);

        assertTrue(resultObject2 != null);

    }
}
