package build.pluto.buildjava.test.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.sugarj.common.FileCommands;

import build.pluto.buildjava.JavaBuilder;
import build.pluto.buildjava.JavaInput;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;


public class SimpleJavaBuildTest extends ScopedBuildTest {

	@ScopedPath(value = "")
	private File sourcePath;

	@ScopedPath(value = "A.java")
	private File classAsource;

	@ScopedPath(value = "bin")
	private File targetDir;

	private TrackingBuildManager build() throws IOException {
		TrackingBuildManager manager = new TrackingBuildManager();
		manager.require(JavaBuilder.request(new JavaInput(classAsource, targetDir, sourcePath)));
		return manager;
	}


	@Test
	public void testBuildClean() throws IOException {
		TrackingBuildManager manager = build();
		assertTrue("No class file generated", new File(targetDir, "A.class").exists());
		assertEquals(manager.getSuccessfullyExecutedInputs().size(), 1);
	}

	@Test
	public void testRebuildAfterCleanDoesNothing() throws IOException {
		build();
		TrackingBuildManager manager = build();
		assertTrue("More than nothing is executed", manager.getExecutedInputs().isEmpty());
	}

	@Test
	public void testRebuildAfterDelectedClassFile() throws IOException {
		build();
		File classFile = new File(targetDir, "A.class");
		classFile.delete();
		TrackingBuildManager manager = build();
		assertTrue("No class file generated", classFile.exists());
		assertEquals(manager.getExecutedInputs().size(), 1);
	}

	@Test
	public void testRebuildAfterChangedSourceFile() throws IOException {
		build();
		FileCommands.writeToFile(classAsource, "class A {     }");
		TrackingBuildManager manager = build();
		assertEquals(1, manager.getExecutedInputs().size());
	}

}
