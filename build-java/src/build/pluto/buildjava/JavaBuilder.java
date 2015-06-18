package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sugarj.common.FileCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.util.Pair;

import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildCycleAtOnceBuilder;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CycleSupportFactory;
import build.pluto.builder.IMetaBuildingEnabled;
import build.pluto.buildjava.util.JavaCommands;
import build.pluto.buildjava.util.JavaCommands.JavacResult;
import build.pluto.output.None;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamper;
import build.pluto.util.AbsoluteComparedFile;

public class JavaBuilder extends BuildCycleAtOnceBuilder<JavaBuilder.Input, None> {

	public static final BuilderFactory<ArrayList<Input>, None, JavaBuilder> factory = JavaBuilderFactory.INSTANCE;
	private static final CycleSupportFactory javaCycleSupportFactory = (BuildCycle cycle) -> new JavaCycleSupport(cycle, factory);

	private static class JavaBuilderFactory implements BuilderFactory<ArrayList<Input>, None, JavaBuilder> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -1714722510759449143L;
		private static final JavaBuilderFactory INSTANCE = new JavaBuilderFactory();

		@Override
		public JavaBuilder makeBuilder(ArrayList<Input> input) {
			return new JavaBuilder(input);
		}

		private Object readResolve() {
			return INSTANCE;
		}

	}

	public static class Input implements Serializable, IMetaBuildingEnabled {
		private static final long serialVersionUID = -8905198283548748809L;
		public final File inputFile;
		public final File targetDir;
		public final List<File> sourcePath;
		public final List<File> classPath;
		public final String[] additionalArgs;
		public final List<BuildRequest<?, ?, ?, ?>> injectedDependencies;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Input) {
				Input other = (Input) obj;
				return AbsoluteComparedFile.equals(inputFile, other.inputFile) && AbsoluteComparedFile.equals(targetDir, other.targetDir);
			} else {
				return false;
			}
		}

		public Input(File inputFile, File targetDir, List<File> sourcePath, List<File> classPath, String[] additionalArgs,
				List<BuildRequest<?, ?, ?, ?>> requiredUnits) {
			Objects.requireNonNull(inputFile);
			this.inputFile = inputFile;
			this.targetDir = targetDir != null ? targetDir : new File(".");
			this.sourcePath = sourcePath;
			this.classPath = classPath != null ? classPath : Collections.singletonList(this.targetDir);
			this.additionalArgs = additionalArgs;
			this.injectedDependencies = requiredUnits;
		}

		public Input(File inputFile, File targetDir, File sourcePath) {
			this(inputFile, targetDir, Collections.singletonList(sourcePath), Collections.singletonList(targetDir), null, null);
		}

		@Override
		public void setMetaBuilding(boolean metaBuilding) {
			// Do nothing for now... Implementation is currently only in the
			// hotswap branch
		}

		public File getInputFile() {
			return inputFile;
		}

		public Stream<File> getSourcePathStream() {
			return sourcePath.stream();
		}

		public Stream<File> getClassPathStream() {
			return classPath.stream();
		}

		public Stream<BuildRequest<?, ?, ?, ?>> getInjectedDependenciesStream() {
			if (injectedDependencies == null)
				return Stream.empty();
			else
				return injectedDependencies.stream();
		}

	}

	public JavaBuilder(ArrayList<Input> input) {
		super(input, factory);
	}

	@Override
	protected File singletonPersistencePath(Input input) {
		return correspondingBinPath(FileCommands.replaceExtension(input.inputFile, "dep"), input);
	}

	@Override
	protected String description(ArrayList<Input> input) {
		return "Compile Java files " + input.stream().map(Input::getInputFile).map(File::getName).reduce((String f1, String f2) -> f1 + ", " + f2);
	}

	private File correspondingBinPath(File srcFile, Input input) {
		for (File sourcePath : input.sourcePath) {
			Path rel = FileCommands.getRelativePath(sourcePath, srcFile);
			if (rel != null)
				return new File(input.targetDir, rel.toString());
		}
		return input.targetDir;
	}

	@Override
	public Stamper defaultStamper() {
		return LastModifiedStamper.instance;
	}

	@Override
	protected CycleSupportFactory getCycleSupport() {
		return javaCycleSupportFactory;
	}

	@Override
	public List<None> buildAll(ArrayList<Input> inputs) throws IOException {
		List<BuildRequest<?, ?, ?, ?>> injectedDependencies = inputs.stream().flatMap(Input::getInjectedDependenciesStream).distinct()
				.collect(Collectors.toList());
		List<File> inputFiles = inputs.stream().map(Input::getInputFile).collect(Collectors.toList());
		List<File> sourcePaths = inputs.stream().flatMap(Input::getSourcePathStream).distinct().collect(Collectors.toList());
		File targetDir = inputs.get(0).targetDir;
		String[] additionalArgs = inputs.get(0).additionalArgs;
		List<File> classPath = inputs.stream().flatMap(Input::getClassPathStream).distinct().collect(Collectors.toList());

		requireBuild(injectedDependencies);
		inputFiles.forEach(this::require);
		FileCommands.createFile(targetDir);

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
		javacResult.generatedFiles.forEach((File gen) -> provide(inputs, gen));

		for (File p : javacResult.loadedFiles) {
			switch (FileCommands.getExtension(p)) {
			case "class":
				Path relTP = FileCommands.replaceExtension(FileCommands.getRelativePath(targetDir, p), "java");
				for (File sourcePath : sourcePaths) {
					File sourceFile = new File(sourcePath, relTP.toString());
					if (FileCommands.exists(sourceFile)) {
						if (!inputFiles.contains(sourceFile)) {
							requireBuild(JavaBuilder.factory, BuildCycleAtOnceBuilder.singletonArrayList(new Input(sourceFile, targetDir, sourcePaths,
									classPath, additionalArgs, injectedDependencies)));
						}
						break;
					}
				}
				break;
			case "java":
				for (File sourcePath : sourcePaths) {
					Path relSP = FileCommands.getRelativePath(sourcePath, p);
					if (relSP != null && FileCommands.exists(p)) {
						if (!inputFiles.contains(sourcePath)) {
							requireBuild(JavaBuilder.factory, BuildCycleAtOnceBuilder.singletonArrayList(new Input(p, targetDir, sourcePaths, classPath,
									additionalArgs, injectedDependencies)));
						}
						break;
					}
				}
				break;
			}
			require(p);
		}

		return Stream.iterate(None.val, UnaryOperator.identity()).limit(inputs.size()).collect(Collectors.toList());
	}

}
