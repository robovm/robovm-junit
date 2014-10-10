/*
 * Copyright (C) 2014 Trillian Mobile AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robovm.compilerhelper;

import org.robovm.compiler.AppCompiler;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.Version;

import java.io.File;
import java.io.IOException;

public class Compiler {

    Config.Builder configuration;
    Config.Home home;

    public void compile() throws IOException {

        File unpackDir = null;
        File unpackedDistDir = null;

        if (home == null) {
            RoboVMResolver resolver = new RoboVMResolver();
            File compilerFile = null;
            try {
                compilerFile = resolver.resolveRoboVMCompilerArtifact();
                unpackDir = resolver.unpackInPlace(compilerFile);
                unpackedDistDir = new File(unpackDir, "robovm-" + Version.getVersion());
                home = new Config.Home(unpackedDistDir);
                configuration.home(home);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        AppCompiler compiler = new AppCompiler(configuration.build());
        compiler.compile();
    }

    public Compiler withHome(Config.Home home) {
        this.home = home;
        return this;
    }

    public Compiler withConfiguration(Config.Builder configuration) {
        this.configuration = configuration;

        return this;
    }

}
