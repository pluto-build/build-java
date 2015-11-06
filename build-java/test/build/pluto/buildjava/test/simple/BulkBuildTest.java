//package build.pluto.buildjava.test.simple;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.junit.Test;
//import org.sugarj.common.FileCommands;
//
//import build.pluto.BuildUnit;
//import build.pluto.builder.bulk.BulkBuilder.BulkOutput;
//import build.pluto.buildjava.JavaBulkBuilder;
//import build.pluto.buildjava.JavaInput;
//import build.pluto.buildjava.compiler.JavacCompiler;
//import build.pluto.dependency.BuildRequirement;
//import build.pluto.dependency.FileRequirement;
//import build.pluto.dependency.Requirement;
//import build.pluto.output.None;
//import build.pluto.test.build.ScopedBuildTest;
//import build.pluto.test.build.ScopedPath;
//import build.pluto.test.build.TrackingBuildManager;
//
//public class BulkBuildTest extends ScopedBuildTest {
//
//	@ScopedPath(value = "")
//	private File sourcePath;
//
//	@ScopedPath(value = "A.java")
//	private File classAsource;
//
//	@ScopedPath(value = "B.java")
//	private File classBsource;
//
//	@ScopedPath(value = "C.java")
//	private File classCsource;
//
//	@ScopedPath(value = "D.java")
//	private File classDsource;
//
//	@ScopedPath(value = "bin/")
//	private File targetDir;
//	
//	@Override
//    protected Collection<String> alsoCopyDirs() {
//		return Collections.singletonList("foo");
//	}
//	
//	private BuildUnit<BulkOutput<None>> lastUnit;
//	
//	private File bin(File source) {
//		return new File(new File(source.getParentFile(), "bin"), FileCommands.fileName(source) + ".class");
//	}
//	
//	private TrackingBuildManager build() throws IOException {
//		TrackingBuildManager manager = new TrackingBuildManager();
//		List<File> sourceFiles = Arrays.asList(classAsource, classBsource, classCsource, classDsource);
//		BuildRequirement<BulkOutput<None>> req = manager.require(JavaBulkBuilder.factory, new JavaInput(sourceFiles, targetDir, sourcePath, JavacCompiler.instance));
//		lastUnit = req.getUnit();
//		return manager;
//	}
//
//	private void checkCorrect() {
//		assertTrue("No class file for A generated", bin(classAsource).exists());
//		assertTrue("No class file for B generated", bin(classBsource).exists());
//		assertTrue("No class file for C generated", bin(classCsource).exists());
//		assertTrue("No class file for D generated", bin(classDsource).exists());
//		
//		Set<File> javaRequires = new HashSet<>();
//		for (Requirement req : lastUnit.getRequirements())
//			if (req instanceof FileRequirement) {
//				FileRequirement freq = (FileRequirement) req;
//				if ("java".equals(FileCommands.getExtension(freq.file)))
//					javaRequires.add(freq.file);
//			}
//		
//		assertEquals(5, javaRequires.size());
//		assertEquals(5, lastUnit.getGeneratedFiles().size());
//	}
//	
//	@Test
//	public void testBuildClean() throws IOException {
//		TrackingBuildManager manager = build();
//		assertEquals(1, manager.getExecutedInputs().size());
//		checkCorrect();
//	}
//
//	@Test
//	public void testCleanRebuildDoesNothing() throws IOException {
//		build();
//		TrackingBuildManager manager = build();
//		assertEquals(0, manager.getExecutedInputs().size());
//		checkCorrect();
//	}
//
//	@Test
//	public void testRebuildOnAChange() throws IOException {
//		build();
//		
//		FileCommands.writeToFile(classAsource, "class A { int x; B b; }");
//		TrackingBuildManager manager = build();
//		assertEquals(1, manager.getExecutedInputs().size());
//		checkCorrect();
//	}
//
//	@Test
//	public void testRebuildOnBChange() throws IOException {
//		build();
//		
//		FileCommands.writeToFile(classBsource, "class B { int x; }");
//		TrackingBuildManager manager = build();
//		assertEquals(1, manager.getExecutedInputs().size());
//		checkCorrect();
//	}
//}
