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
package org.robovm.surefire.internal;

import com.google.gson.*;
import org.junit.runner.Result;

import java.lang.reflect.Type;

public class ResultTypeAdapter implements JsonSerializer<Result> {

        @Override
        public JsonElement serialize(Result result, Type type, JsonSerializationContext jsonSerializationContext) {
                JsonObject jsonObject = new JsonObject();

                jsonObject.add("fRunTime", new JsonPrimitive(result.getRunTime()));
                jsonObject.add("fIgnoreCount", new JsonPrimitive(result.getIgnoreCount()));
                jsonObject.add("fCount", new JsonPrimitive(result.getRunCount()));

                return jsonObject;
        }
}
