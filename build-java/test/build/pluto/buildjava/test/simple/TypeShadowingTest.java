package build.pluto.buildjava.test.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;

import org.junit.Test;

import build.pluto.buildjava.JavaBuilder;
import build.pluto.buildjava.JavaInput;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;


public class TypeShadowingTest extends ScopedBuildTest {

	@ScopedPath(value = "")
	private File sourcePath;

	@ScopedPath(value = "Test.java")
	private File classTestSource;

	@ScopedPath(value = "String.java")
	private File classStringSource;

	@ScopedPath(value = "bin")
	private File targetDir;

	private TrackingBuildManager build(File... inputs) throws IOException {
		TrackingBuildManager manager = new TrackingBuildManager();
		for (File input : inputs) {
			manager.require(JavaBuilder.request(new JavaInput(input, targetDir, sourcePath)));
		}
		return manager;
	}

	@Test
	public void testRebuildAfterAddedSourceFile() throws IOException {
		// First, build only Test.java.
		build(classTestSource);

		PrintStream out = new PrintStream(new FileOutputStream(classStringSource));
		out.println("public class String {}");
		out.close();

		// The class Test should be rebuilt when String is included in inputs.
		TrackingBuildManager manager = build(classTestSource, classStringSource);
		assertEquals(2, manager.getExecutedInputs().size());
	}

}
