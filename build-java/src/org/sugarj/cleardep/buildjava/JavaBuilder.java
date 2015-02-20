package org.sugarj.cleardep.buildjava;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.sugarj.cleardep.CompilationUnit.State;
import org.sugarj.cleardep.SimpleCompilationUnit;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.buildjava.util.JavaCommands;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.FileCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.util.Pair;

public class JavaBuilder extends
		Builder<JavaBuildContext, JavaBuilder.Input, SimpleCompilationUnit> {

	public static BuilderFactory<JavaBuildContext, Input, SimpleCompilationUnit, JavaBuilder> factory = new BuilderFactory<JavaBuildContext, Input, SimpleCompilationUnit, JavaBuilder>() {
		@Override
		public JavaBuilder makeBuilder(JavaBuildContext context) {
			return new JavaBuilder(context);
		}
	};

	public static class Input {
		public final List<Path> inputFiles;
		public final Path targetDir;
		public final List<Path> sourcePaths;
		public final List<Path> classPaths;
		public final List<String> additionalArgs;
		public final List<RequirableCompilationUnit> requiredUnits;

		public Input(List<Path> inputFiles, Path targetDir,
				List<Path> sourcePaths, List<Path> classPaths,
				List<String> additionalArgs,
				List<RequirableCompilationUnit> requiredUnits) {
			this.inputFiles = inputFiles;
			this.targetDir = targetDir;
			this.sourcePaths = sourcePaths;
			this.classPaths = classPaths;
			this.additionalArgs = additionalArgs;
			this.requiredUnits = requiredUnits;
		}
	}

	public JavaBuilder(JavaBuildContext context) {
		super(context);
	}

	@Override
	protected String taskDescription(Input input) {
		return "Compile Java files";
	}

	@Override
	protected Path persistentPath(Input input) {
		if (input.inputFiles.size() == 1) {
			return new RelativePath(input.targetDir,
					FileCommands.fileName(input.inputFiles.get(0)) + ".dep");
		}

		int hash = Arrays.hashCode(input.inputFiles.toArray());

		return new RelativePath(input.targetDir, "javaFiles" + hash + ".dep");
	}

	@Override
	public Class<SimpleCompilationUnit> resultClass() {
		return SimpleCompilationUnit.class;
	}

	@Override
	public Stamper defaultStamper() {
		return LastModifiedStamper.instance;
	}

	@Override
	public void build(SimpleCompilationUnit result, Input input)
			throws IOException {
		try {
			if (input.requiredUnits != null) {
				for (RequirableCompilationUnit u : input.requiredUnits) {
					u.require();
				}
			}
			for (Path p : input.inputFiles) {
				result.addExternalFileDependency(p);
			}
			Pair<List<Path>, List<Path>> outFiles = JavaCommands.javac(
					input.inputFiles, input.sourcePaths, input.targetDir,
					input.additionalArgs, input.classPaths);
			for (Path p : outFiles.a) {
				result.addGeneratedFile(p);
			}
			for (Path p : outFiles.b) {
				result.addExternalFileDependency(p);
			}
			result.setState(State.finished(true));
		} catch (SourceCodeException e) {
			StringBuilder errMsg = new StringBuilder(
					"The following errors occured during compilation:\n");
			for (Pair<SourceLocation, String> error : e.getErrors()) {
				errMsg.append(FileCommands.dropDirectory(error.a.file) + "("
						+ error.a.lineStart + ":" + error.a.columnStart + "): "
						+ error.b);
			}
			throw new IOException(errMsg.toString());
		}
	}

}
