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
package org.robovm.devicebridge.internal.adapters;

import biz.source_code.base64Coder.Base64Coder;
import com.google.gson.*;
import org.robovm.apple.foundation.Foundation;
import org.robovm.devicebridge.internal.Logger;

import java.io.*;
import java.lang.reflect.Type;

/**
 * Serialization class for exceptions
 */
public class ThrowableTypeAdapter implements JsonSerializer<Throwable>, JsonDeserializer<Throwable> {

    @Override
    public JsonElement serialize(Throwable throwable, Type type,
            JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        String throwableString = null;

        Logger.log("Serializing " + throwable.getMessage());

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(byteStream);
            outputStream.writeObject(throwable);
            outputStream.flush();
            throwableString = new String(Base64Coder.encode(byteStream.toByteArray()));
        } catch (IOException e) {
            Logger.log("Error serializing JSON " + e.getMessage());
            e.printStackTrace();
        }

        jsonObject.addProperty("throwableObject", throwableString);

        return jsonObject;
    }

    @Override
    public Throwable deserialize(JsonElement jsonElement, Type type,
            JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        JsonObject jsonObject = jsonElement.getAsJsonObject();

        String throwableString = jsonObject.get("throwableObject").getAsString();

        byte bytes[] = Base64Coder.decode(throwableString);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return (Throwable) objectInputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
