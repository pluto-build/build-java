package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sugarj.common.FileCommands;
import org.sugarj.common.StreamCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.util.Pair;

import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildCycleAtOnceBuilder;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CycleHandlerFactory;
import build.pluto.buildjava.util.JavaCommands;
import build.pluto.buildjava.util.JavaCommands.JavacResult;
import build.pluto.output.None;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamper;

public class JavaBuilder extends BuildCycleAtOnceBuilder<JavaInput, None> {

	public static final BuilderFactory<ArrayList<JavaInput>, None, JavaBuilder> factory = JavaBuilderFactory.INSTANCE;
	private static final CycleHandlerFactory javaCycleSupportFactory = (BuildCycle cycle) -> new JavaCycleHandler(cycle, factory);

	private static class JavaBuilderFactory implements BuilderFactory<ArrayList<JavaInput>, None, JavaBuilder> {

		/**
		 * 
		 */
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
		return correspondingBinPath(FileCommands.replaceExtension(input.getInputFile(), "dep"), input);
	}

	@Override
	protected String description(ArrayList<JavaInput> input) {
		return "Compile Java files " + input.stream().map(JavaInput::getInputFile).map(File::getName).reduce((String f1, String f2) -> f1 + ", " + f2).get();
	}

	private File correspondingBinPath(File srcFile, JavaInput input) {
		Optional<File> targetFile = input.getSourcePathStream().map((File sourcePath) -> {
			Path rel = FileCommands.getRelativePath(sourcePath, srcFile);
			if (rel != null)
				return new File(input.getTargetDir(), rel.toString());
			else
				return (File) null;
		}).filter((Object o) -> o != null).findFirst();
		return targetFile.orElse(input.getTargetDir());
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
		List<BuildRequest<?, ?, ?, ?>> injectedDependencies = inputs.stream().flatMap(JavaInput::getInjectedDependenciesStream).distinct()
				.collect(Collectors.toList());
		List<File> inputFiles = inputs.stream().map(JavaInput::getInputFile).collect(Collectors.toList());
		List<File> sourcePaths = inputs.stream().flatMap(JavaInput::getSourcePathStream).distinct().collect(Collectors.toList());
		File targetDir = inputs.get(0).getTargetDir();
		String[] additionalArgs = inputs.get(0).getAdditionalArgs();
		List<File> classPath = inputs.stream().flatMap(JavaInput::getClassPathStream).distinct().collect(Collectors.toList());

		requireBuild(injectedDependencies);
		inputFiles.forEach(this::require);
		FileCommands.createDir(targetDir.toPath());

		JavacResult javacResult;
		try {
			javacResult = JavaCommands.javac(inputFiles, sourcePaths, targetDir, additionalArgs, classPath);
		} catch (SourceCodeException e) {
			StringBuilder errMsg = new StringBuilder("The following errors occured during compilation:\n");
			for (Pair<SourceLocation, String> error : e.getErrors()) {
				errMsg.append(FileCommands.dropDirectory(error.a.file) + "(" + error.a.lineStart + ":" + error.a.columnStart + "): " + error.b);
			}
			throw new IOException(errMsg.toString());
		}
		// TODO Dont register all generated files for first input
		javacResult.generatedFiles.forEach((File gen) -> provide(inputs.get(0), gen));

		StreamCommands.TrowableConsumer<File> requireJavaBuild = (File sourceFile) -> {
			if (FileCommands.exists(sourceFile)) {
				if (!inputFiles.contains(sourceFile)) {
					requireBuild(JavaBuilder.request(new JavaInput(sourceFile, targetDir, sourcePaths, classPath,
							additionalArgs, injectedDependencies)));
				}
			}
		};

		for (File p : javacResult.loadedFiles) {
			switch (FileCommands.getExtension(p)) {
			case "class":
				Path relTP = FileCommands.replaceExtension(FileCommands.getRelativePath(targetDir, p), "java");
				for (File sourcePath : sourcePaths) {
					File sourceFile = new File(sourcePath, relTP.toString());
					requireJavaBuild.action(sourceFile);
				}
				break;
			case "java":
				for (File sourcePath : sourcePaths) {
					Path relSP = FileCommands.getRelativePath(sourcePath, p);
					if (relSP != null) {
						requireJavaBuild.action(p);
					}
				}
				break;
			}
			require(p);
		}

		return Stream.iterate(None.val, UnaryOperator.identity()).limit(inputs.size()).collect(Collectors.toList());
	}

}
