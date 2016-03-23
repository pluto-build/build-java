package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.sugarj.common.FileCommands;

import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildCycleAtOnceBuilder;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.CycleHandler;
import build.pluto.builder.CycleHandlerFactory;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.buildjava.compiler.IJavaCompiler;
import build.pluto.buildjava.compiler.JavaCompilerResult;
import build.pluto.dependency.Origin;
import build.pluto.executor.InputParser;
import build.pluto.output.None;
import build.pluto.stamp.FileExistsStamper;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamper;

public class JavaCompiler extends BuildCycleAtOnceBuilder<JavaCompilerInput, None> {

	public static final BuilderFactory<ArrayList<JavaCompilerInput>, None, JavaCompiler> factory = JavaBuilderFactory.INSTANCE;
	private static final CycleHandlerFactory javaCycleSupportFactory = new CycleHandlerFactory() {
		@Override
		public CycleHandler createCycleSupport(BuildCycle cycle) {
			return new JavaCycleHandler(cycle, factory);
		}
	};

	static class JavaBuilderFactory implements BuilderFactory<ArrayList<JavaCompilerInput>, None, JavaCompiler> {
		private static final long serialVersionUID = -1714722510759449143L;

		private static final JavaBuilderFactory INSTANCE = new JavaBuilderFactory();

		@Override
		public JavaCompiler makeBuilder(ArrayList<JavaCompilerInput> input) {
			return new JavaCompiler(input);
		}

		private Object readResolve() {
			return INSTANCE;
		}
		
		public boolean isOverlappingGeneratedFileCompatible(File overlap, Serializable input, BuilderFactory<?, ?, ?> otherFactory, Serializable otherInput) {
			return false;
		}

		@Override
		public InputParser<ArrayList<JavaCompilerInput>> inputParser() {
			return null;
		}

	}

	public static BuildRequest<ArrayList<JavaCompilerInput>, None, JavaCompiler, BuilderFactory<ArrayList<JavaCompilerInput>, None, JavaCompiler>> request(JavaCompilerInput input) {
		return new BuildRequest<>(factory, BuildCycleAtOnceBuilder.singletonArrayList(input));
	}

	public JavaCompiler(ArrayList<JavaCompilerInput> input) {
		super(input, factory);
	}

	@Override
	protected File singletonPersistencePath(JavaCompilerInput input) {
		return new File(input.targetDir, "compile.java."+ input.sourceFiles.hashCode() +".dep");
	}

	@Override
	protected String description(ArrayList<JavaCompilerInput> input) {
		return "Compile Java files"; 
	}

	@Override
	public Stamper defaultStamper() {
		return LastModifiedStamper.instance;
	}

	@Override
	protected CycleHandlerFactory getCycleSupport() {
		return javaCycleSupportFactory;
	}

	private Set<File> requiredJars = new HashSet<>();
	
	@Override
	public List<None> buildAll(ArrayList<JavaCompilerInput> inputs) throws Throwable {
		List<File> inputFiles = new ArrayList<>();
		List<File> sourcePaths = new ArrayList<>();
		Origin.Builder sourceOriginBuilder = Origin.Builder();
		Origin.Builder classOriginBuilder = Origin.Builder();
		File targetDir = inputs.get(0).targetDir;
		Collection<String> additionalArgs = inputs.get(0).additionalArgs;
		List<File> classPath = new ArrayList<>();
		String sourceRelease = inputs.get(0).sourceRelease;
		String targetRelease = inputs.get(0).targetRelease;
		IJavaCompiler compiler = inputs.get(0).compiler;
		
		for (JavaCompilerInput input : inputs) {
			for (File p : input.sourceFiles)
				if (!inputFiles.contains(p)) {
					inputFiles.add(p);
				}
			for (File p : input.sourcePath)
				if (!sourcePaths.contains(p))
					sourcePaths.add(p);
			for (File p : input.classPath)
				if (!classPath.contains(p))
					classPath.add(p);
			sourceOriginBuilder.add(input.sourceOrigin);
			classOriginBuilder.add(input.classOrigin);
		}
		
		Origin sourceOrigin = sourceOriginBuilder.get();
		Origin classOrigin = classOriginBuilder.get();
		requireBuild(sourceOrigin);
		requireBuild(classOrigin);
		
		for (File p : inputFiles)
			require(p, FileHashStamper.instance);
		
		FileCommands.createDir(targetDir);
		JavaCompilerResult compilerResult = compiler.compile(inputFiles, targetDir, sourcePaths, classPath, sourceRelease, targetRelease, additionalArgs);

		// TODO Dont register all generated files for first input
		for (Collection<File> gens : compilerResult.getSourceTargetFiles().values())
			for (File gen : gens)
				provide(inputs.get(0), gen);
		
		for (File source : compilerResult.getSourceTargetFiles().keySet()) {
			// install source dependency
			String relSource = findRelativePath(source, sourcePaths);
			if (relSource == null)
				throw new IllegalStateException("Cannot find source file " + source + " in sourcepath " + sourcePaths.toString());
			installSourceDep(relSource, inputFiles, sourcePaths, sourceOrigin, classOrigin, targetDir, additionalArgs, classPath, sourceRelease, targetRelease, compiler);
			require(source);
		}
		
		for (File p : compilerResult.getLoadedClassFiles()) {
			Path rel = FileCommands.getRelativePath(targetDir, p);
			// if class file is in target dir
			if (rel != null) {
				Path relClassSource = FileCommands.replaceExtension(rel, "java");
				installSourceDep(relClassSource.toString(), inputFiles, sourcePaths, sourceOrigin, classOrigin, targetDir, additionalArgs, classPath, sourceRelease, targetRelease, compiler);
				require(p);
			}
			else {
				String relClass = findRelativePath(p, classPath);
				installBinaryDep(relClass, classPath);
			}
		}

		for (Entry<File, Collection<String>> zipped : compilerResult.getLoadedFromZippedFile().entrySet())
			installZipBinaryDep(zipped.getKey(), zipped.getValue(), classPath, sourcePaths, targetDir);

		for (File jar : requiredJars)
			require(jar);

		List<None> result = new ArrayList<>(inputs.size());
		for (int i = 0; i < inputs.size(); i++)
			result.add(None.val);
		return result;
	}

	private void installZipBinaryDep(File zip, Collection<String> zipped, List<File> classPath, Collection<File> sourcePaths, File targetDir) {
		requiredJars.add(zip);
		for (File cp : classPath) {
			if (cp.equals(zip))
				break;
			
			boolean isTarget = cp.equals(targetDir);
			
			if (cp.isFile())
				requiredJars.add(cp);
			else
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

	private void installBinaryDep(String relClass, List<File> classPath) {
		if (relClass == null)
			return;
		for (File cp : classPath) {
			if (cp.isFile())
				requiredJars.add(cp);
			else {
				File classFile = new File(cp, relClass);
				if (FileCommands.exists(classFile)) {
					require(classFile);
					break; // rest of classpath is irrelevant
				}
				else 
					require(classFile, FileExistsStamper.instance);
			}
		}
	}

	private void installSourceDep(
			String rel, 
			List<File> inputFiles,
			List<File> sourcePaths,
			Origin sourceOrigin,
			Origin classOrigin,
			File targetDir, 
			Collection<String> additionalArgs,
			List<File> classPath,
			String sourceRelease, 
			String targetRelease, 
			IJavaCompiler compiler) throws IOException {
	for (File sourcePath : sourcePaths) {
			File sourceFile = new File(sourcePath, rel);
			if (FileCommands.exists(sourceFile)) {
				if (!inputFiles.contains(sourceFile)) {
					JavaCompilerInput input = new JavaCompilerInput
							.Builder()
							.addInputFiles(sourceFile)
							.setSourceOrigin(sourceOrigin)
							.setClassOrigin(classOrigin)
							.setTargetDir(targetDir)
							.addSourcePaths(sourcePaths)
							.addClassPaths(classPath)
							.addAdditionalArgs(additionalArgs)
							.setSourceRelease(sourceRelease)
							.setTargetRelease(targetRelease)
							.setCompiler(compiler)
							.get();
					requireBuild(JavaCompiler.factory, BuildCycleAtOnceBuilder.singletonArrayList(input));
				}
				break; // rest of sourcepaths are irrelevant
			}
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
