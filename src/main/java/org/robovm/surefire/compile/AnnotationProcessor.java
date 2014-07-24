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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes("org.robovm.surefire.compile.RoboVMTest")
public class AnnotationProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element elem : roundEnv.getElementsAnnotatedWith(RoboVMTest.class)) {
            try {
                String packageName = getPackageName(elem).getQualifiedName().toString();
                String className = elem.getSimpleName().toString();

                if (!packageName.equals("")) {
                    className = packageName + "." + className;
                }

                System.err.println("Creating test files: " + "org.robovm.surefire." + ClassGenerator
                    .getClassName(className));
                System.err.flush();
                JavaFileObject javaFileObject =
                    processingEnv.getFiler().createSourceFile(
                        "org.robovm.surefire." + ClassGenerator.getClassName(className)
                            + "Runner", elem);

                ClassGenerator.generateSourceForClass(className, javaFileObject);
            } catch (IOException e) {
                System.err.println("Failed processing annotations");
                e.printStackTrace();
            }
        }

        return true;
    }

    private PackageElement getPackageName(Element elem) {
        if (elem.getKind() != ElementKind.PACKAGE) {
            elem = elem.getEnclosingElement();
        }
        return (PackageElement) elem;
    }
}
