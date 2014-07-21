package org.robovm;

import com.google.gson.GsonBuilder;
import org.junit.Test;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.robovm.surefire.ResultObject;
import org.robovm.surefire.RoboTestServer;
import org.robovm.surefire.internal.AtomicIntegerTypeAdapter;
import org.robovm.surefire.internal.DescriptionTypeAdapter;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ash on 16/07/2014.
 */
public class RoboTestServerTest {

    @Test
    public void testServerReceivesContent() throws Exception {

        final RoboTestServer roboTestServer = Mockito.mock(RoboTestServer.class);
        Mockito.doCallRealMethod().when(roboTestServer).startServer(8889);

        roboTestServer.startServer(8889);

        Socket socket = new Socket("localhost", 8889);
        socket.getOutputStream();
        PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);
        String jsonString = "{\"resultType\":2,\"description\":{\"display_name\":\"null\",\"sub_description\":[{\"display_name\":\"com.example.TestTest\",\"sub_description\":[{\"display_name\":\"testTest(com.example.TestTest)\",\"sub_description\":[]}]}]}}";

        writer.write(jsonString);
        writer.flush();
        writer.close();

        ResultObject resultObject = new GsonBuilder()
                .registerTypeAdapter(Description.class, new DescriptionTypeAdapter())
                .registerTypeAdapter(AtomicInteger.class, new AtomicIntegerTypeAdapter())
                .create()
                .fromJson(jsonString, ResultObject.class);

        Mockito.verify(roboTestServer).testRunStarted(resultObject.getDescription());
    }
}
