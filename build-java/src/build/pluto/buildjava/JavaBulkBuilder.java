package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.util.Pair;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.builder.bulk.BulkBuilder;
import build.pluto.buildjava.util.JavaCommands;
import build.pluto.buildjava.util.JavaCommands.JavacResult;
import build.pluto.output.None;
import build.pluto.output.Output;
import build.pluto.stamp.FileHashStamper;

public class JavaBulkBuilder extends BulkBuilder<JavaInput, None, JavaInput> {

	public JavaBulkBuilder(JavaInput input) {
		super(input);
	}

	public final static BuilderFactory<JavaInput, BulkOutput<None>, JavaBulkBuilder> factory = BuilderFactoryFactory.of(JavaBulkBuilder.class, JavaInput.class);
	
	@Override
	protected Collection<File> requiredFiles(JavaInput input) {
		return input.getInputFiles();
	}

	@Override
	protected Collection<JavaInput> splitInput(JavaInput input, Set<File> changedFiles) {
		List<JavaInput> inputs = new ArrayList<>();
		for (File f : changedFiles)
			inputs.add(new JavaInput(f, input.getTargetDir(), input.getSourcePath(), input.getClassPath(), input.getAdditionalArgs(), input.getInjectedDependencies()));
		return inputs;
	}

	@Override
	protected BuildRequest<
		? extends JavaInput,
		? extends Output, 
		? extends Builder<JavaInput, ? extends Output>, 
		? extends BuilderFactory<? extends JavaInput, ? extends Output, ? extends Builder<JavaInput, ? extends Output>>> 
	makeSubRequest(JavaInput subInput) {
		return new BuildRequest<>(factory, subInput);
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
		return new File(input.getTargetDir(), "compile.java."+ input.getInputFiles().hashCode() +".dep");
	}

	@Override
	protected None buildBulk(JavaInput input, Collection<JavaInput> splitInput, Set<File> changedFiles) throws IOException {
		Log.log.log("Rebuild Java files " + changedFiles, Log.CORE);
		
		JavacResult javacResult;
		try {
			javacResult = JavaCommands.javac(changedFiles, input.getSourcePath(), input.getTargetDir(), input.getAdditionalArgs(), input.getClassPath());
		} catch (SourceCodeException e) {
			StringBuilder errMsg = new StringBuilder("The following errors occured during compilation:\n");
			for (Pair<SourceLocation, String> error : e.getErrors()) {
				errMsg.append(FileCommands.dropDirectory(error.a.file) + "(" + error.a.lineStart + ":" + error.a.columnStart + "): " + error.b);
			}
			throw new IOException(errMsg.toString(), e);
		}

		// TODO use better dependency tracking for Java source-file dependencies
		for (File source : changedFiles)
			for (File file : input.getInputFiles())
				require(source, file, FileHashStamper.instance);
		
		for (Entry<File,List<File>> e : javacResult.generatedFiles.entrySet()) {
			for (File gen : e.getValue())
				provide(e.getKey(), gen);
		}
		for (File f : javacResult.loadedFiles)
			require(f);
		
		return null;
	}

}
