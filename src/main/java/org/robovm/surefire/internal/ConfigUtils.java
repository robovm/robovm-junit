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
                        mergeConfigs(configBuilder, value);
                }

                return configBuilder;
        }

        public static void mergeConfigs(Config.Builder configBuilder, String value) throws IOException {
                configBuilder.read(new File(value));
        }

        public static String getSystemProperty(String property) {
                return System.getProperty(property);
        }

        public static String getProperty(String property) {
                String value;

                if ((value = getSystemProperty(property)) != null) {
                        return value;
                }
                return getDefaultValue(property);
        }

        private static String getDefaultValue(String property) {
                switch (property) {
                        case Constant.SERVER_HOST:
                                return Constant.DEFAULT_SERVER_HOST;

                        case Constant.SERVER_PORT:
                                return Constant.DEFAULT_SERVER_PORT;

                        case Constant.MAVEN_REPOSITORY_DIR:
                                return Constant.DEFAULT_MAVEN_REPOSITORY_DIR;

                        case Constant.INSTALL_DIR:
                                return Constant.DEFAULT_INSTALL_DIR;
                }
                return null;
        }
}
