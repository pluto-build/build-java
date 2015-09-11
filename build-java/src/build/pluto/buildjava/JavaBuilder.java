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
					require(p, FileHashStamper.instance);
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
			case "class":
				Path relTP = FileCommands.replaceExtension(FileCommands.getRelativePath(targetDir, p), "java");
				for (File sourcePath : sourcePaths) {
					File sourceFile = new File(sourcePath, relTP.toString());
					if (FileCommands.exists(sourceFile) && !inputFiles.contains(sourceFile))
						requireBuild(JavaBuilder.request(new JavaInput(sourceFile, targetDir, sourcePaths, classPath, additionalArgs, injectedDependencies, compiler)));
				}
				break;
			case "java":
				for (File sourcePath : sourcePaths) {
					Path relSP = FileCommands.getRelativePath(sourcePath, p);
					if (relSP != null && FileCommands.exists(p) && !inputFiles.contains(p))
						requireBuild(JavaBuilder.request(new JavaInput(p, targetDir, sourcePaths, classPath, additionalArgs, injectedDependencies, compiler)));
				}
				break;
			}
			require(p);
		}

		List<None> result = new ArrayList<>(inputs.size());
		for (int i = 0; i < inputs.size(); i++)
			result.add(None.val);
		return result;
	}

}
