package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.util.Pair;

import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.builder.bulk.BulkBuilder;
import build.pluto.buildjava.compiler.JavaCompilerResult;
import build.pluto.output.None;
import build.pluto.stamp.FileHashStamper;

public class JavaBulkBuilder extends BulkBuilder<JavaInput, None> {

	public JavaBulkBuilder(JavaInput input) {
		super(input);
	}

	public final static BuilderFactory<JavaInput, BulkOutput<None>, JavaBulkBuilder> factory = BuilderFactoryFactory
			.of(JavaBulkBuilder.class, JavaInput.class);

	@Override
	protected Collection<File> requiredFiles(JavaInput input) {
		return input.getInputFiles();
	}

	@Override
	protected String description(JavaInput input) {
		StringBuilder builder = new StringBuilder();
		for (File f : input.getInputFiles())
			builder.append(f.getName()).append(", ");
		String list = builder.toString();
		if (!list.isEmpty())
			list = list.substring(0, list.length() - 2);

		return "Compile Java files " + list;
	}

	@Override
	public File persistentPath(JavaInput input) {
		return new File(input.getTargetDir(), "compile.java."
				+ input.getInputFiles().hashCode() + ".dep");
	}

	@Override
	protected None buildBulk(JavaInput input, Set<File> changedFiles)
			throws Exception {
		Log.log.log("Rebuild Java files " + changedFiles, Log.CORE);

		requireBuild(input.getInjectedDependencies());
		JavaCompilerResult compilerResult;
		try {
			compilerResult = input.getCompiler().compile(
					changedFiles,
					input.getTargetDir(),
					input.getSourcePath(),
					input.getClassPath(), 
					input.getSourceRelease(),
					input.getTargetRelease(), 
					input.getAdditionalArgs());
		} catch (SourceCodeException e) {
			StringBuilder errMsg = new StringBuilder(
					"The following errors occured during compilation:\n");
			for (Pair<SourceLocation, String> error : e.getErrors()) {
				errMsg.append(FileCommands.dropDirectory(error.a.file) + "("
						+ error.a.lineStart + ":" + error.a.columnStart + "): "
						+ error.b);
			}
			throw new IOException(errMsg.toString(), e);
		}

		// TODO use better dependency tracking for Java source-file dependencies
		for (File source : changedFiles)
			for (File file : input.getInputFiles())
				require(source, file, FileHashStamper.instance);

		for (Entry<File, ? extends Collection<File>> e : compilerResult
				.getSourceTargetFiles().entrySet()) {
			for (File gen : e.getValue())
				provide(e.getKey(), gen);
		}
		for (File f : compilerResult.getLoadedClassFiles())
			require(f);

		return null;
	}

}
