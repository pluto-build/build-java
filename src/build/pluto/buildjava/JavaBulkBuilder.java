package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.sugarj.common.FileCommands;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildjava.compiler.JavaCompilerResult;
import build.pluto.output.None;
import build.pluto.stamp.FileExistsStamper;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.LastModifiedStamper;

public class JavaBulkBuilder extends Builder<JavaInput, None> {

	public JavaBulkBuilder(JavaInput input) {
		super(input);
	}

	public final static BuilderFactory<JavaInput, None, JavaBulkBuilder> factory = BuilderFactoryFactory.of(JavaBulkBuilder.class, JavaInput.class);

	@Override
	protected String description(JavaInput input) {
		return "Compile Java files";
	}

	@Override
	public File persistentPath(JavaInput input) {
		return new File(input.getTargetDir(), "compile.java."
				+ input.getInputFiles().hashCode() + ".dep");
	}

	@Override
	protected None build(JavaInput input) throws Exception {
		requireBuild(input.getFilesOrigin());
		for (File f : input.getInputFiles())
			require(f, FileHashStamper.instance);

		FileCommands.createDir(input.getTargetDir());
		JavaCompilerResult compilerResult =
				input.getCompiler().compile(
					input.getInputFiles(),
					input.getTargetDir(),
					input.getSourcePath(),
					input.getClassPath(), 
					input.getSourceRelease(),
					input.getTargetRelease(), 
					input.getAdditionalArgs());


		for (Collection<File> gens : compilerResult.getSourceTargetFiles().values())
			for (File gen : gens)
				provide(gen, LastModifiedStamper.instance);
		
		for (File source : compilerResult.getSourceTargetFiles().keySet()) {
			// install shadow dependencies for source files
			String relSource = findRelativePath(source, input.getSourcePath());
			if (relSource == null)
				throw new IllegalStateException("Cannot find source file " + source + " in sourcepath " + input.getSourcePath());
			installSourceDep(relSource, input.getSourcePath());
			require(source);
		}
		
		Set<File> requiredJars = new HashSet<>();
		
		for (File p : compilerResult.getLoadedClassFiles()) {
			Path rel = FileCommands.getRelativePath(input.getTargetDir(), p);
			// if class file is in target dir
			if (rel != null) {
				Path relClassSource = FileCommands.replaceExtension(rel, "java");
				installSourceDep(relClassSource.toString(), input.getSourcePath());
				require(p);
			}
			else {
				String relClass = findRelativePath(p, input.getClassPath());
				installBinaryDep(relClass, input.getClassPath(), requiredJars);
			}
		}

		for (Entry<File, Collection<String>> zipped : compilerResult.getLoadedFromZippedFile().entrySet())
			installZipBinaryDep(zipped.getKey(), zipped.getValue(), input.getClassPath(), input.getSourcePath(), input.getTargetDir(), requiredJars);

		for (File jar : requiredJars)
			require(jar);

		return null;
	}
	
	
	private Set<File> installBinaryDep(String relClass, List<File> classPath, Set<File> requiredJars) {
		if (relClass == null)
			return requiredJars;
		for (File cp : classPath) {
			if (cp.isFile())
				requiredJars.add(cp);
			else {
				File classFile = new File(cp, relClass);
				if (FileCommands.exists(classFile)) {
					require(classFile);
					break; // rest of classpath is irrelevant
				} else
					require(classFile, FileExistsStamper.instance);
			}
		}
		return requiredJars;
	}
	
	private void installZipBinaryDep(File zip, Collection<String> zipped, List<File> classPath, Collection<File> sourcePaths, File targetDir, Set<File> requiredJars) {
		requiredJars.add(zip);

		for (File cp : classPath) {
			if (cp.equals(zip))
				break;
			
			if (cp.isFile())
				requiredJars.add(cp);
			else {
				boolean isTarget = cp.equals(targetDir);
				for (String rel : zipped) {
					if (rel.startsWith("java/lang/")) {
						rel = rel.substring("java/lang/".length());
					}
					else if (rel.startsWith("java/"))
						continue;
					
					require(new File(cp, rel), FileExistsStamper.instance);
					if (isTarget) {
						for (File sourcePath : sourcePaths) {
							File relClassSource = FileCommands.replaceExtension(new File(sourcePath, rel), "java");
							require(relClassSource, FileExistsStamper.instance);
						}
					}
				}
			}
		}
	}

	private void installSourceDep(String rel, List<File> sourcePaths)
			throws IOException {
		for (File sourcePath : sourcePaths) {
			File sourceFile = new File(sourcePath, rel);
			if (FileCommands.exists(sourceFile))
				break; // rest of sourcepaths are irrelevant
			else
				require(sourceFile, FileExistsStamper.instance);
		}
	}

	private String findRelativePath(File full, Collection<File> bases) {
		for (File base : bases) {
			Path rel = FileCommands.getRelativePath(base, full);
			if (rel != null)
				return rel.toString();
		}
		return null;
	}
}
