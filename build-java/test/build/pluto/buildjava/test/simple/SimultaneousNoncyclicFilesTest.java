package build.pluto.buildjava.test.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.buildjava.JavaInput;
import build.pluto.buildjava.NoncyclicJavaBuilder;
import build.pluto.dependency.BuildRequirement;
import build.pluto.output.None;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;

public class SimultaneousNoncyclicFilesTest extends ScopedBuildTest {

	@ScopedPath(value = "")
	private File sourcePath;

	@ScopedPath(value = "A.java")
	private File classAsource;

	@ScopedPath(value = "B.java")
	private File classBsource;

	@ScopedPath(value = "C.java")
	private File classCsource;

	@ScopedPath(value = "D.java")
	private File classDsource;

	@ScopedPath(value = "bin/")
	private File targetDir;
	
	private BuildUnit<None> lastUnit;
	
	private File bin(File source) {
		return new File(new File(source.getParentFile(), "bin"), FileCommands.fileName(source) + ".class");
	}
	
	private TrackingBuildManager build() throws IOException {
		try {
			Thread.sleep(1200);
		} catch (InterruptedException e) {
		}
		TrackingBuildManager manager = new TrackingBuildManager();
		List<File> sourceFiles = Arrays.asList(classAsource, classBsource, classCsource, classDsource);
		BuildRequirement<None> req = manager.require(NoncyclicJavaBuilder.factory, new JavaInput(sourceFiles, targetDir, sourcePath));
		lastUnit = req.getUnit();
		return manager;
	}

	@Test
	public void testBuildClean() throws IOException {
		TrackingBuildManager manager = build();
		assertEquals(1, manager.getExecutedInputs().size());
		
		assertTrue("No class file for A generated", bin(classAsource).exists());
		assertTrue("No class file for B generated", bin(classBsource).exists());
		assertTrue("No class file for C generated", bin(classCsource).exists());
		assertTrue("No class file for D generated", bin(classDsource).exists());
	}

	@Test
	public void testCleanRebuildDoesNothing() throws IOException {
		build();
		TrackingBuildManager manager = build();
		assertEquals(0, manager.getExecutedInputs().size());
		assertTrue("Build unit is not consistent", lastUnit.isConsistent());
	}

	@Test
	public void testRebuildOnAChange() throws IOException {
		Log.log.setLoggingLevel(Log.ALWAYS);
		Log.log.log("Before1: " + FileCommands.readFileAsString(classAsource), Log.CORE);		
		build();
		
		FileCommands.writeToFile(classAsource, "class A { int x; B b; }");
		Log.log.log("Before2: " + FileCommands.readFileAsString(classAsource), Log.CORE);		
		TrackingBuildManager manager = build();
		assertEquals(1, manager.getExecutedInputs().size());
		assertTrue("Build unit is not consistent", lastUnit.isConsistent());

		assertTrue("No class file for A generated", bin(classAsource).exists());
		assertTrue("No class file for B generated", bin(classBsource).exists());
		assertTrue("No class file for C generated", bin(classCsource).exists());
		assertTrue("No class file for D generated", bin(classDsource).exists());

		// only A is recompiled
		assertTrue("A.class should be newer than B.class", bin(classAsource).lastModified() > bin(classBsource).lastModified());
		assertTrue("A.class should be newer than C.class", bin(classAsource).lastModified() > bin(classCsource).lastModified());
		assertTrue("A.class should be newer than D.class", bin(classAsource).lastModified() > bin(classDsource).lastModified());
	}

	@Test
	public void testRebuildOnBChange() throws IOException {
		build();
		FileCommands.writeToFile(classBsource, "class B { int x; }");
		TrackingBuildManager manager = build();

		assertEquals(1, manager.getExecutedInputs().size());
		assertTrue("Build unit is not consistent", lastUnit.isConsistent());

		assertTrue("No class file for A generated", bin(classAsource).exists());
		assertTrue("No class file for B generated", bin(classBsource).exists());
		assertTrue("No class file for C generated", bin(classCsource).exists());
		assertTrue("No class file for D generated", bin(classDsource).exists());

		// A and B are recompiled, C and D are old
		assertTrue("A.class should be newer than C.class", bin(classAsource).lastModified() > bin(classCsource).lastModified());
		assertTrue("A.class should be newer than D.class", bin(classAsource).lastModified() > bin(classDsource).lastModified());
		assertTrue("B.class should be newer than C.class", bin(classBsource).lastModified() > bin(classCsource).lastModified());
		assertTrue("B.class should be newer than D.class", bin(classBsource).lastModified() > bin(classDsource).lastModified());
	}
}
