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
package org.robovm.surefire.compile;

import com.squareup.javawriter.JavaWriter;
import org.junit.runner.JUnitCore;
import org.robovm.surefire.RoboTestListener;
import org.robovm.surefire.internal.ConfigUtils;
import org.robovm.surefire.internal.Constant;

import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * Created by williaab on 2/07/14.
 */
public class ClassGenerator {

    public static void generateSourceForClass(String className, JavaFileObject javaFileObject) {
        String host = "localhost";
        String port = "8889";

        host = ConfigUtils.getProperty(Constant.SERVER_HOST);
        port = ConfigUtils.getProperty(Constant.SERVER_PORT);


        try {
            /* clean up previous objects */
            javaFileObject.delete();
            JavaWriter javaWriter = new JavaWriter(javaFileObject.openWriter());
            javaWriter.emitPackage("org.robovm.surefire")
                .emitImports(RoboTestListener.class)
                .emitImports(JUnitCore.class)
                .emitImports(IOException.class)
                .beginType(getClassName(className) + "Runner", "class",
                    EnumSet.of(Modifier.PUBLIC, Modifier.FINAL), null)

                .beginMethod("void", "main", EnumSet.of(Modifier.PUBLIC, Modifier.STATIC),
                    Arrays.asList("String[]", "args"),
                    Arrays.asList("IOException"))
                .emitStatement("JUnitCore jUnitCore = new JUnitCore()")
                .emitStatement(
                    "jUnitCore.addListener(new RoboTestListener(null, \"" + host + "\", \"" + port
                        + "\"))")
                .emitStatement("jUnitCore.run(" + className + ".class)")
                .endMethod()
                .endType()
                .close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getClassName(String className) {
        if (className.lastIndexOf(".") != -1) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }
}
