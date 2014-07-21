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

import org.apache.maven.artifact.versioning.VersionRange;
import org.robovm.compiler.Version;
import org.robovm.compiler.config.Config;

import java.io.File;


public class RobovmHelper {

    public static Config.Home findRoboHome() {
        Config.Home home = new Config.Home(new File ("/Users/ash/Downloads/robovm-0.0.14"));
        return home;
    }

    protected static VersionRange getRoboVMVersion() {
        return VersionRange.createFromVersion(Version.getVersion());
    }
}
