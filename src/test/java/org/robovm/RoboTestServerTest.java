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


public class RoboTestServerTest {

    @Test
    public void testServerReceivesContent() throws Exception {

        final RoboTestServer roboTestServer = Mockito.mock(RoboTestServer.class);
        Mockito.doCallRealMethod().when(roboTestServer).startServer(8889);

        roboTestServer.startServer(8889);

        Socket socket = new Socket("localhost", 8889);
        socket.getOutputStream();
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
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
