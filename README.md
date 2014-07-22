# RoboVM-Surefire-Provider


RoboVM surefire provider is a means of running unit tests on the iOS Simulator


## Using RoboVM Surefire Provider


### Compile and install this plugin
Compile robovm-surefire-provider and install into your local maven repository with *'mvn install'*

### Example test class:

```java
@RoboVMTest
public class TestClass {

    @Test
    public void testTest() throws Exception {
        System.err.println("Running testTest");
        assertTrue(1 == 1);
    }
}
```

### Example pom.xml

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.17</version>
                <dependencies>
                    <dependency>
                        <groupId>org.robovm</groupId>
                        <artifactId>robovm-surefire-provider</artifactId>
                        <version>1.0</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <systemProperties>
                        <robovm.home>/Users/me/robovm</robovm.home>
                        <robovm.mavenResourceHome>/Users/me/.m2</robovm.mavenResourceHome>
                        <robovm.debug>true</robovm.debug>
                        <robovm.configFile>${project.basedir}/robovm.xml</robovm.configFile>
                        <robovm.serverIP>localhost</robovm.serverIP>
                        <robovm.serverPort>8889</robovm.serverPort>
                        <robovm.installDir>/tmp/surefire/install</robovm.installDir>
                    </systemProperties>
                </configuration>
            </plugin>
        </plugins>
    </build>
```


### Running

mvn test

### Configuration:

The following system properties are configurable

* *robovm.home* robovm home directory
* *robovm.mavenResourceHome* maven resource home directory (default ~/.m2)
* *robovm.debug* enable debugging (default false)
* *robovm.configFile* specify a robovm.xml file to give fine grained configuration information. Use this for force link paths, expand the class path etc.
* *robovm.serverIP* specify the IP/host the iOS simulator bridge listens on (default localhost)
* *robovm.serverPort* specify the port the iOS simulator bridge listens on (default 8889)
* *robovm.installDir* specify the temporary installation directory for robovm files (default /tmp/surefire/install)

## How does it work?

An executable is created for every class annotated with @RoboVMTest. This, along with your java code is then compiled into bytecode.
A further process then compiles this bytecode with the RoboVM compiler (this process can be considerably slow).
Once this compilation is complete the iOSSimulatorBridge is started, which starts the simulator and communicates back your JUnit results
via a network connection using JSON serialised requests.
Your tests are then executed and the result returned just like any other surefire provider.


## What doesn't work?

Mocking frameworks -- These won't ever work as mocking requires bytecode manipulation. This is, unfortunately, a limitation of RoboVM

## Considerations

Currently this process is slow as every test class is compiled separately. In future releases some tweaking to the RoboVM cache will help this situation

## What's Inside ?

Frameworks used:

* [JavaWriter](https://github.com/square/javawriter)
* [RoboVM] (http://www.robovm.com/)
* [RxJava] (https://github.com/Netflix/RxJava)
* [Mockito] (https://code.google.com/p/mockito)
* [PowerMock] (https://code.google.com/p/powermock)
* [GSon] (https://code.google.com/p/google-gson)