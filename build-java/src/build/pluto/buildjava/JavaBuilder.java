package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sugarj.common.FileCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.util.Pair;

import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildCycleAtOnceBuilder;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CycleHandler;
import build.pluto.builder.CycleHandlerFactory;
import build.pluto.buildjava.compiler.JavaCompiler;
import build.pluto.buildjava.compiler.JavaCompiler.JavaCompilerResult;
import build.pluto.output.None;
import build.pluto.stamp.FileExistsStamper;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamper;

public class JavaBuilder extends BuildCycleAtOnceBuilder<JavaInput, None> {

	public static final BuilderFactory<ArrayList<JavaInput>, None, JavaBuilder> factory = JavaBuilderFactory.INSTANCE;
	private static final CycleHandlerFactory javaCycleSupportFactory = new CycleHandlerFactory() {
		@Override
		public CycleHandler createCycleSupport(BuildCycle cycle) {
			return new JavaCycleHandler(cycle, factory);
		}
	};

	static class JavaBuilderFactory implements BuilderFactory<ArrayList<JavaInput>, None, JavaBuilder> {
		private static final long serialVersionUID = -1714722510759449143L;

		private static final JavaBuilderFactory INSTANCE = new JavaBuilderFactory();

		@Override
		public JavaBuilder makeBuilder(ArrayList<JavaInput> input) {
			return new JavaBuilder(input);
		}

		private Object readResolve() {
			return INSTANCE;
		}
	}

	public static BuildRequest<ArrayList<JavaInput>, None, JavaBuilder, BuilderFactory<ArrayList<JavaInput>, None, JavaBuilder>> request(JavaInput input) {
		return new BuildRequest<>(factory, BuildCycleAtOnceBuilder.singletonArrayList(input));
	}

	public JavaBuilder(ArrayList<JavaInput> input) {
		super(input, factory);
	}

	@Override
	protected File singletonPersistencePath(JavaInput input) {
		return new File(input.getTargetDir(), "compile.java."+ input.getInputFiles().hashCode() +".dep");
	}

	@Override
	protected String description(ArrayList<JavaInput> input) {
		StringBuilder builder = new StringBuilder();
		for (JavaInput inp : input)
			for (File f : inp.getInputFiles())
				builder.append(f.getName()).append(", ");
		String list = builder.toString();
		if (!list.isEmpty())
			list = list.substring(0, list.length() - 2);
		
		return "Compile Java files " + list; 
	}

	@Override
	public Stamper defaultStamper() {
		return LastModifiedStamper.instance;
	}

	@Override
	protected CycleHandlerFactory getCycleSupport() {
		return javaCycleSupportFactory;
	}

	@Override
	public List<None> buildAll(ArrayList<JavaInput> inputs) throws Throwable {
		List<File> inputFiles = new ArrayList<>();
		List<File> sourcePaths = new ArrayList<>();
		List<BuildRequest<?, ?, ?, ?>> injectedDependencies = new ArrayList<>();
		File targetDir = inputs.get(0).getTargetDir();
		Collection<String> additionalArgs = inputs.get(0).getAdditionalArgs();
		List<File> classPath = new ArrayList<>();
		JavaCompiler compiler = inputs.get(0).getCompiler();
		
		for (JavaInput input : inputs) {
			for (File p : input.getInputFiles())
				if (!inputFiles.contains(p)) {
					inputFiles.add(p);
				}
			for (File p : input.getSourcePath())
				if (!sourcePaths.contains(p))
					sourcePaths.add(p);
			for (File p : input.getClassPath())
				if (!classPath.contains(p))
					classPath.add(p);
			injectedDependencies.addAll(input.getInjectedDependencies());
		}
		requireBuild(injectedDependencies);
		for (File p : inputFiles)
			require(p, FileHashStamper.instance);
		
		FileCommands.createDir(targetDir);
		JavaCompilerResult compilerResult;
		try {
			compilerResult = compiler.compile(inputFiles, targetDir, sourcePaths, classPath, additionalArgs);
		} catch (SourceCodeException e) {
			StringBuilder errMsg = new StringBuilder("The following errors occured during compilation:\n");
			for (Pair<SourceLocation, String> error : e.getErrors()) {
				errMsg.append(FileCommands.dropDirectory(error.a.file) + "(" + error.a.lineStart + ":" + error.a.columnStart + "): " + error.b);
			}
			throw new IOException(errMsg.toString(), e);
		}
		// TODO Dont register all generated files for first input
		for (Collection<File> gens : compilerResult.getGeneratedFiles().values())
			for (File gen : gens)
				provide(inputs.get(0), gen);
		
		for (File p : compilerResult.getLoadedFiles()) {
			switch (FileCommands.getExtension(p)) {
			case "jar":
				String relJar = findRelativePath(p, classPath);
				installBinaryDep(relJar, classPath);
				break;
				
			case "class":
				Path rel = FileCommands.getRelativePath(targetDir, p);
				// if class file is in target dir
				if (rel != null) {
					Path relClassSource = FileCommands.replaceExtension(rel, "java");
					installSourceDep(relClassSource.toString(), inputFiles, sourcePaths, injectedDependencies, targetDir, additionalArgs, classPath, compiler);
					require(p);
				}
				else {
					String relClass = findRelativePath(p, classPath);
					installBinaryDep(relClass, classPath);
				}
				break;
				
			case "java":
				// install source dependency
				String relSource = findRelativePath(p, sourcePaths);
				if (relSource == null)
					throw new IllegalStateException("Cannot find source file " + p + " in sourcepath " + sourcePaths.toString());
				installSourceDep(relSource, inputFiles, sourcePaths, injectedDependencies, targetDir, additionalArgs, classPath, compiler);
				require(p);
				break;
			
			default:
				require(p);
			}
		}

		List<None> result = new ArrayList<>(inputs.size());
		for (int i = 0; i < inputs.size(); i++)
			result.add(None.val);
		return result;
	}

	private void installBinaryDep(String relClass, List<File> classPath) {
		if (relClass == null)
			return;
		for (File cp : classPath) {
			File classFile = new File(cp, relClass);
			if (FileCommands.exists(classFile)) {
				require(classFile);
				break; // rest of classpath is irrelevant
			}
			else 
				require(classFile, FileExistsStamper.instance);
		}
	}

	private void installSourceDep(String rel, List<File> inputFiles,
			List<File> sourcePaths,
			List<BuildRequest<?, ?, ?, ?>> injectedDependencies,
			File targetDir, Collection<String> additionalArgs,
			List<File> classPath, JavaCompiler compiler) throws IOException {
	for (File sourcePath : sourcePaths) {
			File sourceFile = new File(sourcePath, rel);
			if (FileCommands.exists(sourceFile)) {
				if (!inputFiles.contains(sourceFile)) {
					requireBuild(JavaBuilder.request(new JavaInput(sourceFile, targetDir, sourcePaths, classPath, additionalArgs, injectedDependencies, compiler)));
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
