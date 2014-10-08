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

import org.junit.Test;
import org.mockito.Mockito;
import org.robovm.surefire.compile.ClassGenerator;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertTrue;

public class ClassGeneratorTest {

    @Test
    public void testSourceGeneration() throws IOException {

        StringWriter writer = new StringWriter();
        JavaFileObject dummyJavaFileObject = Mockito.mock(JavaFileObject.class);
        Mockito.when(dummyJavaFileObject.openWriter()).thenReturn(writer);

        org.robovm.surefire.compile.ClassGenerator.generateSourceForClass(
            "TestClassName"
            , dummyJavaFileObject);

        assertJavaFile(writer, "TestClassName");
    }

    @Test
    public void testGetClassName() {
        String exampleClass = "com.example.Test";
        assertTrue(ClassGenerator.getClassName(exampleClass).equals("Test"));
    }

    private void assertJavaFile(StringWriter writer, String testClassName) {
        String expectedString =
            "package org.robovm.surefire;\n" +
                "\n" +
                "import org.robovm.surefire.RoboTestListener;\n" +
                "import org.junit.runner.JUnitCore;\n" +
                "import java.io.IOException;\n" +
                "public final class " + testClassName + "Runner {\n" +
                "  public static void main(String[] args)\n" +
                "      throws IOException {\n" +
                "    JUnitCore jUnitCore = new JUnitCore();\n" +
                "    jUnitCore.addListener(new RoboTestListener(null, \"localhost\", \"8889\"));\n" +
                "    jUnitCore.run(" + testClassName + ".class);\n" +
                "  }\n" +
                "}\n";

        assertTrue(writer.getBuffer().toString().equals(expectedString));
    }

}
