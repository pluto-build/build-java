package org.sugarj.cleardep.buildjava;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.CompilationUnit.State;
import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.build.BuildRequirement;
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

public class JavaBuilder extends Builder<JavaBuilder.Input, CompilationUnit> {

	public static BuilderFactory<Input, CompilationUnit, JavaBuilder> factory = new BuilderFactory<Input, CompilationUnit, JavaBuilder>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2193786625546374284L;

		@Override
		public JavaBuilder makeBuilder(Input input, BuildManager manager) {
			return new JavaBuilder(input, manager);
		}
	};

	public static class Input implements Serializable {
		private static final long serialVersionUID = -8905198283548748809L;
		public final List<Path> inputFiles;
		public final Path targetDir;
		public final List<Path> sourcePaths;
		public final List<Path> classPaths;
		public final List<String> additionalArgs;
		public final List<BuildRequirement<?,?,?,?>> requiredUnits;

		public Input(List<Path> inputFiles, Path targetDir,
				List<Path> sourcePaths, List<Path> classPaths,
				List<String> additionalArgs,
				List<BuildRequirement<?,?,?,?>> requiredUnits) {
			this.inputFiles = inputFiles;
			this.targetDir = targetDir;
			this.sourcePaths = sourcePaths;
			this.classPaths = classPaths;
			this.additionalArgs = additionalArgs;
			this.requiredUnits = requiredUnits;
		}
	}

	private JavaBuilder(Input input, BuildManager manager) {
		super(input, factory, manager);
	}

	@Override
	protected String taskDescription() {
		return "Compile Java files";
	}

	@Override
	protected Path persistentPath() {
		if (input.inputFiles.size() == 1) {
			return new RelativePath(input.targetDir,
					FileCommands.fileName(input.inputFiles.get(0)) + ".dep");
		}

		int hash = Arrays.hashCode(input.inputFiles.toArray());

		return new RelativePath(input.targetDir, "javaFiles" + hash + ".dep");
	}

	@Override
	public Class<CompilationUnit> resultClass() {
		return CompilationUnit.class;
	}

	@Override
	public Stamper defaultStamper() {
		return LastModifiedStamper.instance;
	}

	@Override
	public void build(CompilationUnit result) throws IOException {
		try {
			if (input.requiredUnits != null) {
				for (BuildRequirement<?,?,?,?> u : input.requiredUnits) {
					require(u);
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
