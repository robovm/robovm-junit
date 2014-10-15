package org.robovm.junit.protocol;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.google.gson.GsonBuilder;

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
                new Failure(Description.createSuiteDescription("test"), new RuntimeException()));

        String jsonString = new GsonBuilder()
                .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
                .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter())
                .registerTypeAdapter(Failure.class, new FailureTypeAdapter())
                .create()
                .toJson(resultObject);
        assertTrue(jsonString != null);

        ResultObject resultObject2 = new GsonBuilder()
                .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
                .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter())
                .registerTypeAdapter(Failure.class, new FailureTypeAdapter())
                .create()
                .fromJson(jsonString, ResultObject.class);

        assertTrue(resultObject2 != null);

    }
}
