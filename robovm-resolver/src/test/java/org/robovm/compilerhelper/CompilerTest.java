package org.robovm.compilerhelper;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.robovm.compiler.Version;
import org.robovm.compiler.config.Config;
import org.robovm.compilerhelper.RoboVMResolver;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class CompilerTest {

    File unpackDir;

    @Before
    public void onStart() throws IOException {
        unpackDir = new File(System.getProperty("java.io.tmpdir") + "/unpacked");
        FileUtils.deleteDirectory(unpackDir);
    }

    @Test
    public void testRoboVMUnArchive() throws MojoExecutionException, IOException {
        RoboVMResolver resolver = new RoboVMResolver();
        FileUtils.deleteDirectory(unpackDir);

        File archive = resolver.resolveRoboVMCompilerArtifact();
        resolver.unpack(archive, unpackDir);
        assertTrue(new File(unpackDir + "/" + "robovm-" + Version.getVersion()).exists());
    }

    @Test
    public void testCompileHelloWorld() throws IOException {
        Config.Builder configuration = getConfigurationWithMainClass(HelloWorld.class);

        new org.robovm.compilerhelper.Compiler()
                .withConfiguration(configuration)
                .compile();
    }

    /**
     * Provides basic configuration for testing where 'clazz' is the main class
     * 
     * @param mainClass
     * @return configuration
     * @throws IOException
     */
    private Config.Builder getConfigurationWithMainClass(Class<HelloWorld> mainClass) throws IOException {
        Config.Builder config = new Config.Builder();
        config.addClasspathEntry(new File(getClass().getResource("/").getFile()));
        config.iosInfoPList(new File(getClass().getResource("/Info.plist.xml").getFile()));
        config.read(new File(getClass().getResource("/config.xml").getFile()));
        config.mainClass(mainClass.getCanonicalName());
        return config;
    }
}