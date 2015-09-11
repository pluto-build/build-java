package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.common.FileCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.util.Pair;

import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildjava.util.JavaCommands;
import build.pluto.buildjava.util.JavaCommands.JavacResult;
import build.pluto.dependency.FileRequirement;
import build.pluto.dependency.Requirement;
import build.pluto.output.None;


public class NoncyclicJavaBuilder extends Builder<JavaInput, None> {

	public final static BuilderFactory<JavaInput, None, NoncyclicJavaBuilder> factory = BuilderFactoryFactory.of(NoncyclicJavaBuilder.class, JavaInput.class);

	public NoncyclicJavaBuilder(JavaInput input) {
		super(input);
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
	protected None build(JavaInput input) throws Throwable {

		for (File f : input.getInputFiles())
			require(f);
		
		List<File> changedInputFiles = new ArrayList<>(input.getInputFiles());
		if (getPreviousBuildUnit() != null)
			for (Requirement req : getPreviousBuildUnit().getRequirements())
				if (req instanceof FileRequirement) {
					FileRequirement freq = (FileRequirement) req;
					if (changedInputFiles.contains(freq.file) && freq.isConsistent())
						changedInputFiles.remove(freq.file);
				}
		
		FileCommands.createDir(input.getTargetDir());
		JavacResult javacResult;
		try {
			javacResult = JavaCommands.javac(changedInputFiles, input.getSourcePath(), input.getTargetDir(), input.getAdditionalArgs(), input.getClassPath());
		} catch (SourceCodeException e) {
			StringBuilder errMsg = new StringBuilder("The following errors occured during compilation:\n");
			for (Pair<SourceLocation, String> error : e.getErrors()) {
				errMsg.append(FileCommands.dropDirectory(error.a.file) + "(" + error.a.lineStart + ":" + error.a.columnStart + "): " + error.b);
			}
			throw new IOException(errMsg.toString(), e);
		}

		for (List<File> gens : javacResult.generatedFiles.values())
			for (File gen : gens)
				provide(gen);
		
		List<File> additionalSourceFiles = new ArrayList<>();
		for (File p : javacResult.loadedFiles) {
			require(p);
			switch (FileCommands.getExtension(p)) {
			case "class":
				Path relTP = FileCommands.replaceExtension(FileCommands.getRelativePath(input.getTargetDir(), p), "java");
				for (File sourcePath : input.getSourcePath()) {
					File sourceFile = new File(sourcePath, relTP.toString());
					if (FileCommands.exists(sourceFile) && !input.getInputFiles().contains(sourceFile))
						additionalSourceFiles.add(sourceFile);
				}
				break;
			case "java":
				for (File sourcePath : input.getSourcePath()) {
					Path relSP = FileCommands.getRelativePath(sourcePath, p);
					if (relSP != null && FileCommands.exists(p) && !input.getInputFiles().contains(p))
						additionalSourceFiles.add(p);
				}
				break;
			}
		}
		
		return None.val;
	}
	
	
}
