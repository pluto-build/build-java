package build.pluto.buildjava.test.runner;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.sugarj.common.Exec.ExecutionError;
import org.sugarj.common.Exec.ExecutionResult;

import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.buildjava.JavaBulkCompiler;
import build.pluto.buildjava.JavaCompilerInput;
import build.pluto.buildjava.JavaRunner;
import build.pluto.buildjava.JavaRunnerInput;
import build.pluto.buildjava.compiler.JavacCompiler;
import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.TrackingBuildManager;


public class SimpleJavaRunnerTest extends ScopedBuildTest {

	@ScopedPath(value = "")
	private File sourcePath;

	@ScopedPath(value = "A.java")
	private File classAsource;
	@ScopedPath(value = "B.java")
	private File classBsource;
	@ScopedPath(value = "Echo.java")
	private File classEchosource;

	@ScopedPath(value = "bin")
	private File targetDir;

	@Before
	public void compileSources() throws Throwable {
		JavaCompilerInput input = new JavaCompilerInput
				.Builder()
				.addInputFiles(classAsource, classBsource, classEchosource)
				.setTargetDir(targetDir)
				.addSourcePaths(sourcePath)
				.setCompiler(JavacCompiler.instance)
				.get();
		BuildManagers.build(new BuildRequest<>(JavaBulkCompiler.factory, input));
	}
	
	public ExecutionResult run(JavaRunnerInput input) throws Throwable {
		return BuildManagers.build(new BuildRequest<>(JavaRunner.factory, input)).val();
	}
	
	public void assertNoRun(JavaRunnerInput input) throws Throwable {
		TrackingBuildManager m = new TrackingBuildManager();
		m.require(new BuildRequest<>(JavaRunner.factory, input));
		assertEquals(1, m.getRequiredInputs().size());
		assertEquals(0, m.getExecutedInputs().size());
	}
	
	@Test
	public void runA() throws Throwable {
		JavaRunnerInput input = new JavaRunnerInput
				.Builder()
				.setDescription("Run A")
				.setMainClass("A")
				.setWorkingDir(targetDir)
				.get();
		ExecutionResult er = run(input);

		assertEquals(0, er.errMsgs.length);
		assertEquals(1, er.outMsgs.length);
		assertEquals("Class A has run.", er.outMsgs[0]);
	}

	@Test
	public void runB() throws Throwable {
		JavaRunnerInput input = new JavaRunnerInput
				.Builder()
				.setDescription("Run B")
				.setMainClass("B")
				.setWorkingDir(targetDir)
				.get();
		ExecutionResult er = run(input);

		assertEquals(0, er.errMsgs.length);
		assertEquals(2, er.outMsgs.length);
		assertEquals("Class A has run.", er.outMsgs[0]);
		assertEquals("Class B has run.", er.outMsgs[1]);
	}
	
	@Test(expected = ExecutionError.class)
	public void runC() throws Throwable {
		JavaRunnerInput input = new JavaRunnerInput
				.Builder()
				.setDescription("Run C (does not exist)")
				.setMainClass("C")
				.setWorkingDir(targetDir)
				.get();
		ExecutionResult er = run(input);

		assertEquals(0, er.errMsgs.length);
		assertEquals(2, er.outMsgs.length);
		assertEquals("Class A has run.", er.outMsgs[0]);
		assertEquals("Class B has run.", er.outMsgs[1]);
	}
	
	@Test
	public void runEcho() throws Throwable {
		Random r = new Random(0);
		String[] args = new String[1000];
		for (int i = 0; i < args.length; i++)
			args[i] = Long.toString(r.nextLong());
		
		JavaRunnerInput input = new JavaRunnerInput
				.Builder()
				.setDescription("Run Echo")
				.setMainClass("Echo")
				.setWorkingDir(targetDir)
				.addProgramArgs(args)
				.get();
		ExecutionResult er = run(input);

		assertEquals(0, er.errMsgs.length);
		assertEquals(args.length, er.outMsgs.length);
		for (int i = 0; i < args.length; i++)
		assertEquals(args[i], er.outMsgs[i]);
		
		// test incrementality
		assertNoRun(input);
	}

}
