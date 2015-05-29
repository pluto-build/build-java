package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sugarj.common.FileCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.util.Pair;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.IMetaBuildingEnabled;
import build.pluto.buildjava.util.JavaCommands;
import build.pluto.output.None;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamper;

public class JavaBuilder extends Builder<JavaBuilder.Input, None> {

	public static BuilderFactory<Input, None, JavaBuilder> factory = BuilderFactory.of(JavaBuilder.class, Input.class);

	public static class Input implements Serializable, IMetaBuildingEnabled {
		private static final long serialVersionUID = -8905198283548748809L;
		public final List<File> inputFiles;
		public final File targetDir;
		public final List<File> sourcePath;
		public final List<File> classPath;
		public final String[] additionalArgs;
		public final List<BuildRequest<?, ?, ?, ?>> injectedDependencies;
		public final boolean deepRequire;

		public Input(List<File> inputFiles, File targetDir, List<File> sourcePath, List<File> classPath, String[] additionalArgs,
				List<BuildRequest<?, ?, ?, ?>> requiredUnits,
				boolean deepRequire) {
			this.inputFiles = inputFiles != null ? inputFiles : Collections.emptyList();
			this.targetDir = targetDir != null ? targetDir : new File(".");
			this.sourcePath = sourcePath;
			this.classPath = classPath != null ? classPath : Collections.singletonList(this.targetDir);
			this.additionalArgs = additionalArgs;
			this.injectedDependencies = requiredUnits;
			this.deepRequire = deepRequire;
		}

		@Override
		public void setMetaBuilding(boolean metaBuilding) {
			// Do nothing for now... Implementation is currently only in the
			// hotswap branch
		}
	}

	public JavaBuilder(Input input) {
		super(input);
	}

	@Override
	protected String description(Input input) {
		return "Compile Java files " + input.inputFiles.toString();
	}

	@Override
	protected File persistentPath(Input input) {
		if (input.inputFiles.size() == 1) {
			// return new RelativePath(input.targetDir,
			// FileCommands.fileName(input.inputFiles.get(0)) + ".dep");
			return correspondingBinPath(FileCommands.replaceExtension(input.inputFiles.get(0), "dep"), input);
		}

		int hash = input.inputFiles.hashCode();
		return new File(input.targetDir, "pluto.build.java-" + hash + ".dep");
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
	public None build(Input input) throws IOException {
		try {
			System.out.println("Ibjected dependencies " + input.injectedDependencies);
			requireBuild(input.injectedDependencies);

			for (File p : input.inputFiles)
				require(p);

			List<File> inputList = input.inputFiles;
			Pair<List<File>, List<File>> outFiles = JavaCommands.javac(input.inputFiles, input.sourcePath, input.targetDir, input.additionalArgs,
					input.classPath);

			List<File> filesToRequire = new ArrayList<>();
			for (File outFile : outFiles.a) {
				if (input.deepRequire) {
					Path relP = FileCommands.getRelativePath(input.targetDir, FileCommands.replaceExtension(outFile, "java"));

					boolean found = false;
					for (File sourcePath : input.sourcePath) {
						File relSP = new File(sourcePath, relP.toString());
						if (FileCommands.exists(relSP)) {
							found = true;
							if (!inputList.contains(relSP)) {
								found = false;
								filesToRequire.add(relSP);
							}
							break;
						}
					}
					if (found)
						provide(outFile);
				} else {
					provide(outFile);
				}
			}
			for (File p : filesToRequire) {
				Input newInput = new Input(Collections.singletonList(p), input.targetDir, input.sourcePath, input.classPath, input.additionalArgs,
						input.injectedDependencies,
						input.deepRequire);
				requireBuild(JavaBuilder.factory, newInput);
			}

			for (File p : outFiles.b) {
				Path relP = FileCommands.getRelativePath(input.targetDir, FileCommands.replaceExtension(p, "java"));

				if (input.deepRequire && relP != null) {
					boolean found = false;
					if (relP != null)
						for (File sourcePath : input.sourcePath) {
							File relSP = new File(sourcePath, relP.toString());
							if (FileCommands.exists(relSP)) {
								found = true;
								if (!inputList.contains(relSP)) {
									found = false;
									requireBuild(JavaBuilder.factory, new Input(Collections.singletonList(relSP), input.targetDir, input.sourcePath,
											input.classPath,
											input.additionalArgs, input.injectedDependencies, input.deepRequire));
								}
								break;
							}
						}
					if (found)
						require(p);
				} else {
					require(p);
				}
			}
		} catch (SourceCodeException e) {
			StringBuilder errMsg = new StringBuilder("The following errors occured during compilation:\n");
			for (Pair<SourceLocation, String> error : e.getErrors()) {
				errMsg.append(FileCommands.dropDirectory(error.a.file) + "(" + error.a.lineStart + ":" + error.a.columnStart + "): " + error.b);
			}
			throw new IOException(errMsg.toString());
		}
		return None.val;
	}
}
