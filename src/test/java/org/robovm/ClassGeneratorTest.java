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
