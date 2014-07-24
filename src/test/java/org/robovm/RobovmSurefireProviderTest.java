package org.robovm;

import com.google.gson.GsonBuilder;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.robovm.surefire.ResultObject;
import org.robovm.surefire.internal.AtomicIntegerTypeAdapter;
import org.robovm.surefire.internal.DescriptionTypeAdapter;
import org.robovm.surefire.internal.FailureTypeAdapter;

import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertTrue;

public class RobovmSurefireProviderTest {

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
            .registerTypeAdapter(Failure.class, new FailureTypeAdapter())
            .create()
            .toJson(resultObject);
        assertTrue(jsonString != null);

    }

    @Test
    public void testResultObjectDeserialization() {

        String jsonString = "{\"resultType\":2,\"description\":{\"display_name\":\"null\",\"sub_description\":[{\"display_name\":\"com.example.TestTest\",\"sub_description\":[{\"display_name\":\"testTest(com.example.TestTest)\",\"sub_description\":[]}]}]}}";

        ResultObject deserializedResultObject = new GsonBuilder()
            .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
            .create()
            .fromJson(jsonString, ResultObject.class);

        assertTrue(deserializedResultObject.getDescription().getChildren().get(0).getDisplayName()
            .equals("com.example.TestTest"));

    }

    @Test
    public void testResultObjectDeserialization2() {

        String jsonString = " {\"resultType\":3,\"result\":{\"fStartTime\":1405232992842,\"fRunTime\":6,\"fFailures\":[],\"fIgnoreCount\":0,\"fCount\":1}}";

        ResultObject deserializedResultObject = new GsonBuilder()
            .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
            .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter())
            .create()
            .fromJson(jsonString, ResultObject.class);

        assertTrue(deserializedResultObject.getResult() != null);
        assertTrue(deserializedResultObject.getResult().getRunTime() == 6);
        assertTrue(deserializedResultObject.getResult().getRunCount() == 1);
    }

    @Test
    public void testResultObjectDeserialization3() {
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
