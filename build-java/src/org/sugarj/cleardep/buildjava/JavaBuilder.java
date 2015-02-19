package org.sugarj.cleardep.buildjava;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.CompilationUnit.State;
import org.sugarj.cleardep.SimpleCompilationUnit;
import org.sugarj.cleardep.SimpleMode;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.FileCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.util.Pair;
import org.sugarj.common.JavaCommands;

public class JavaBuilder extends Builder<JavaBuildContext, List<Path>, SimpleCompilationUnit> {

	public static BuilderFactory<JavaBuildContext, List<Path>, SimpleCompilationUnit, JavaBuilder> factory = new BuilderFactory<JavaBuildContext, List<Path>, SimpleCompilationUnit, JavaBuilder>() {
		@Override
		public JavaBuilder makeBuilder(JavaBuildContext context) { return new JavaBuilder(context); }
	};
	
	public static class Input {
		public final RelativePath ppInput;
		public final RelativePath ppTermOutput;
		public Input(RelativePath ppInput, RelativePath ppTermOutput) {
			this.ppInput = ppInput;
			this.ppTermOutput = ppTermOutput;
		}
	}
	
	public JavaBuilder(JavaBuildContext context) {
		super(context);
	}
	
	@Override
	protected String taskDescription(List<Path> input) {
		return "Compile Java files";
	}
	
	@Override
	protected Path persistentPath(List<Path> input) {
		return new RelativePath(context.baseDir, FileCommands.tryGetRelativePath(context.binPath) + ".dep");
	}

	@Override
	public Class<SimpleCompilationUnit> resultClass() {
		return SimpleCompilationUnit.class;
	}

	@Override
	public Stamper defaultStamper() { return LastModifiedStamper.instance; }

	@Override
	public void build(SimpleCompilationUnit result, List<Path> input) throws IOException {
		try {
			for (Path p: input) {
				result.addSourceArtifact(FileCommands.getRelativePath(context.baseDir, p));
			}
			List<Path> outFiles = JavaCommands.javac(input, context.binPath, new ArrayList<Path>());
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
