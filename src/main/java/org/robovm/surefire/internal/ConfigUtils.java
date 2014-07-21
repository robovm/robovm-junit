/*
* Copyright 2014 Ashley Williams
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

import org.robovm.compiler.config.Config;

import java.io.File;
import java.io.IOException;

/**
 * Created by ash on 19/07/2014.
 */
public class ConfigUtils {


    public static Config.Builder createConfig() throws IOException {
        Config.Builder configBuilder = new Config.Builder();
        String value;

        if ((value = getSystemProperty(Constant.ROBOVM_CONFIG_FILE)) != null) {
            mergeConfigs(configBuilder,value);
        }

        return configBuilder;
    }


    public static void mergeConfigs(Config.Builder configBuilder, String value) throws IOException {

       configBuilder.read(new File(value));
    }

    public static String getSystemProperty(String property) {
        return System.getProperty(property);
    }

    public static void setSystemProperty(Object objectToSet, String propertyName) {

        String value = getSystemProperty(propertyName);
        if (value == null) {
            return;
        }

        if (objectToSet instanceof String) {
            objectToSet = getSystemProperty(propertyName);
        }
        if (objectToSet instanceof  Integer) {
            objectToSet = Integer.parseInt(getSystemProperty(propertyName));
        }

    }
}
