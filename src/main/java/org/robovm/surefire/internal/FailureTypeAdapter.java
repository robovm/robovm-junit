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
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import java.lang.reflect.Type;

public class FailureTypeAdapter implements JsonSerializer<Failure>, JsonDeserializer<Failure> {

    @Override
    public JsonElement serialize(Failure failure, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.add("message", new JsonPrimitive(failure.getMessage()));
        jsonObject.add("description",
            new DescriptionTypeAdapter().serialize(failure.getDescription(), null, null));
        jsonObject.add("exception", new ThrowableTypeAdapter().serialize(failure.getException(), null, null));

        return jsonObject;
    }

    @Override
    public Failure deserialize(JsonElement jsonElement, Type type,
        JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonObject jsonDescription = jsonObject.getAsJsonObject("description");
        JsonObject jsonException = jsonObject.getAsJsonObject("exception");
        Description description = new DescriptionTypeAdapter().deserialize(jsonDescription, null, null);
        Throwable throwable = new ThrowableTypeAdapter().deserialize(jsonException, null, null);

        Failure failure = new Failure(description, throwable);

        return failure;
    }
}
