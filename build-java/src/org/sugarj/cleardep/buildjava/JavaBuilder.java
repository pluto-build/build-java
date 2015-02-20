package org.sugarj.cleardep.buildjava;

import java.io.IOException;
import java.util.List;

import org.sugarj.cleardep.CompilationUnit.State;
import org.sugarj.cleardep.SimpleCompilationUnit;
import org.sugarj.cleardep.build.BuildContext;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.FileCommands;
import org.sugarj.common.JavaCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.path.Path;
import org.sugarj.common.util.Pair;

public class JavaBuilder extends Builder<BuildContext, JavaBuilder.Input, SimpleCompilationUnit> {

	public static BuilderFactory<BuildContext, Input, SimpleCompilationUnit, JavaBuilder> factory = new BuilderFactory<BuildContext, Input, SimpleCompilationUnit, JavaBuilder>() {
		@Override
		public JavaBuilder makeBuilder(BuildContext context) { return new JavaBuilder(context); }
	};
	
	public static class Input {
		public final List<Path> inputFiles;
		public final Path targetDir;
		public final List<Path> sourcePaths;
		public final List<Path> classPaths;
		public final List<String> additionalArgs;
		public final List<RequirableCompilationUnit> requiredUnits;
		public Input(List<Path> inputFiles, Path targetDir, List<Path> sourcePaths, List<Path> classPaths, List<String> additionalArgs, List<RequirableCompilationUnit> requiredUnits) {
			this.inputFiles = inputFiles;
			this.targetDir = targetDir;
			this.sourcePaths = sourcePaths;
			this.classPaths = classPaths;
			this.additionalArgs = additionalArgs;
			this.requiredUnits = requiredUnits;
		}
	}
	
	public JavaBuilder(BuildContext context) {
		super(context);
	}
	
	@Override
	protected String taskDescription(Input input) {
		return "Compile Java files";
	}
	
	@Override
	protected Path persistentPath(Input input) {
		return FileCommands.addExtension(input.targetDir, "dep");
	}

	@Override
	public Class<SimpleCompilationUnit> resultClass() {
		return SimpleCompilationUnit.class;
	}

	@Override
	public Stamper defaultStamper() { return LastModifiedStamper.instance; }

	@Override
	public void build(SimpleCompilationUnit result, Input input) throws IOException {
		try {
			for (Path p: input.inputFiles) {
				result.addExternalFileDependency(p);
			}
			Path sourcePath = input.sourcePaths.get(0); // TODO support multiple source paths
			// TODO support input.additionalArgs
			List<Path> outFiles = JavaCommands.javac(input.inputFiles, input.targetDir, sourcePath, input.classPaths);
			for (Path p: outFiles) {
				result.addGeneratedFile(p);
			}
			result.setState(State.finished(true));
		} catch (SourceCodeException e) {
			for (Pair<SourceLocation, String> error: e.getErrors()) {
				System.err.println(FileCommands.dropDirectory(error.a.file) + "(" + error.a.lineStart + ":" + error.a.columnStart + "): " + error.b);
			}
			result.setState(State.finished(false));
		}
	}

}
