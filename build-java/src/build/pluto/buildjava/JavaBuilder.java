package build.pluto.buildjava;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sugarj.common.FileCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.common.util.Pair;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.IMetaBuildingEnabled;
import build.pluto.buildjava.util.JavaCommands;
import build.pluto.buildjava.util.ListUtils;
import build.pluto.output.None;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamper;

public class JavaBuilder extends Builder<JavaBuilder.Input, None> {

	public static BuilderFactory<Input, None, JavaBuilder> factory = new BuilderFactory<Input, None, JavaBuilder>() {
		private static final long serialVersionUID = 2193786625546374284L;

		@Override
		public JavaBuilder makeBuilder(Input input) {
			return new JavaBuilder(input);
		}
	};

	public static class Input implements Serializable, IMetaBuildingEnabled {
		private static final long serialVersionUID = -8905198283548748809L;
		public final List<Path> inputFiles;
		public final Path targetDir;
		public final List<Path> sourcePaths;
		public final List<Path> classPaths;
		public final List<String> additionalArgs;
		public final List<BuildRequest<?, ?, ?, ?>> requiredUnits;
		public final Boolean deepRequire;

		public Input(List<Path> inputFiles, Path targetDir,
				List<Path> sourcePaths, List<Path> classPaths,
				List<String> additionalArgs,
				List<BuildRequest<?, ?, ?, ?>> requiredUnits,
				Boolean deepRequire) {
			this.inputFiles = inputFiles;
			this.targetDir = targetDir;
			this.sourcePaths = sourcePaths != null ? sourcePaths : new ArrayList<Path>();
			this.classPaths = classPaths   != null ? classPaths : new ArrayList<Path>();;
			this.additionalArgs = additionalArgs;
			this.requiredUnits = requiredUnits;
			this.deepRequire = deepRequire;

		}

		@Override
		public void setMetaBuilding(boolean metaBuilding) {
			// Do nothing for now... Implementation is currently only in the hotswap branch
		}
	}

	private JavaBuilder(Input input) {
		super(input);
	}

	@Override
	protected String description() {
		return "Compile Java files " + ListUtils.printList(input.inputFiles);
	}

	@Override
	protected Path persistentPath() {
		if (input.inputFiles.size() == 1) {
			//return new RelativePath(input.targetDir,
			//		FileCommands.fileName(input.inputFiles.get(0)) + ".dep");
			return correspondingBinPath(input.inputFiles.get(0).replaceExtension("dep"));
		}

		int hash = Arrays.hashCode(input.inputFiles.toArray());

		return new RelativePath(input.targetDir, "javaFiles" + hash + ".dep");
	}
	
	private Path correspondingBinPath(Path srcFile) {
		for (Path sourcePath: input.sourcePaths) {
			RelativePath rel = FileCommands.getRelativePath(sourcePath, srcFile);
			if (rel != null)
				return new RelativePath(input.targetDir, rel.getRelativePath());
		}
		return input.targetDir;
	}

	@Override
	public Stamper defaultStamper() {
		return LastModifiedStamper.instance;
	}

	@Override
	public None build() throws IOException {
		try {
			requireBuild(input.requiredUnits);
			
			for (Path p : input.inputFiles) {
				require(p);
			}
			Pair<List<Path>, List<Path>> outFiles = JavaCommands.javac(
					input.inputFiles, input.sourcePaths, input.targetDir,
					input.additionalArgs, input.classPaths);

			List<Path> filesToRequire = new ArrayList<Path>();
			for (Path outFile : outFiles.a) {
				if (input.deepRequire) {
					RelativePath relP = FileCommands.getRelativePath(
							input.targetDir, outFile.replaceExtension("java"));

					boolean found = false;
					for (Path sourcePath : input.sourcePaths) {
						RelativePath relSP = new RelativePath(sourcePath,
								relP.getRelativePath());
						if (FileCommands.exists(relSP)) {
							found = true;
							if (!input.inputFiles.contains(relSP)) {
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
			for (Path p : filesToRequire) {
				Input newInput = new Input(Arrays.asList((Path) p),
						input.targetDir, input.sourcePaths, input.classPaths,
						input.additionalArgs, input.requiredUnits,
						input.deepRequire);
				requireBuild(JavaBuilder.factory, newInput);
			}

			for (Path p : outFiles.b) {
				RelativePath relP = FileCommands.getRelativePath(input.targetDir, p.replaceExtension("java"));
				
				if (input.deepRequire && relP != null) {
					boolean found = false;
					if (relP != null)
					for (Path sourcePath: input.sourcePaths) {
						RelativePath relSP = new RelativePath(sourcePath, relP.getRelativePath());
						if (FileCommands.exists(relSP)) {
							found = true;
							if (!input.inputFiles.contains(relSP)) {
								found = false;
								requireBuild(JavaBuilder.factory, new Input(Arrays.asList((Path)relSP), input.targetDir, input.sourcePaths, input.classPaths, input.additionalArgs, input.requiredUnits, input.deepRequire));
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
			StringBuilder errMsg = new StringBuilder(
					"The following errors occured during compilation:\n");
			for (Pair<SourceLocation, String> error : e.getErrors()) {
				errMsg.append(FileCommands.dropDirectory(error.a.file) + "("
						+ error.a.lineStart + ":" + error.a.columnStart + "): "
						+ error.b);
			}
			throw new IOException(errMsg.toString());
		}
		return None.val;
	}
}
