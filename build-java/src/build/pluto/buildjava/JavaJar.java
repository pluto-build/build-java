package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.common.Exec;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.output.None;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamper;

public class JavaJar extends Builder<JavaJar.Input, None> {

	public static BuilderFactory<Input, None, JavaJar> factory = JavaJar::new;

	public static enum Mode {
		Create, List, Extract, Update, GenIndex, CreateOrUpdate;

		public String modeForPath() {
			switch (this) {
			case Create:
				return "generate";
			case List:
				return "list";
			case Extract:
				return "extract";
			case Update:
				return "generate";
			case GenIndex:
				return "index";
			case CreateOrUpdate:
				return "generate";
			default:
				return "";
			}
		}
	}

	public String option(Mode m, Input input) {
		switch (m) {
		case Create:
			return "c";
		case List:
			return "t";
		case Extract:
			return "x";
		case Update:
			return "u";
		case GenIndex:
			return "i";
		case CreateOrUpdate:
			return FileCommands.fileExists(input.jarPath) ? option(Mode.Update, input) : option(Mode.Create, input);
		default:
			return "";
		}
	}

	public static class Input implements Serializable {
		private static final long serialVersionUID = -6951002448963322561L;
		public final Mode mode;
		public final File jarPath;
		public final File manifestPath;
		/**
		 * The files of the jar file: a map from classpath to Files to include
		 */
		public final Map<File, Set<File>> files;
		public final BuildRequest<?, ?, ?, ?>[] requiredUnits;

		public Input(Mode mode, File jarPath, File manifestPath, Map<File, Set<File>> files, BuildRequest<?, ?, ?, ?>[] requiredUnits) {
			this.mode = mode;
			this.jarPath = jarPath;
			this.manifestPath = manifestPath;
			this.files = files;
			this.requiredUnits = requiredUnits;
		}
	}

	private JavaJar(Input input) {
		super(input);
	}

	@Override
	protected String description(Input input) {
		switch (input.mode) {
		case Create:
		case Update:
			return "Generate JAR file";
		case Extract:
			return "Extract files from JAR file";
		case List:
			return "List table of contents of JAR file";
		case GenIndex:
			return "Create index information from JAR file";
		default:
			return "";
		}
	}

	@Override
	protected File persistentPath(Input input) {
		String mode = input.mode.modeForPath();
		if (input.jarPath != null)
			return FileCommands.addExtension(input.jarPath, mode + "." + "dep");
		int hash = input.files.hashCode();
		if (input.manifestPath != null)
			return FileCommands.addExtension(input.manifestPath, "jar." + mode + "." + hash + ".dep");
		return new File("./jar." + mode + "." + hash + ".dep");
	}

	@Override
	protected Stamper defaultStamper() {
		return LastModifiedStamper.instance;
	}

	@Override
	protected None build(Input input) throws IOException {
		requireBuild(input.requiredUnits);

		List<String> flags = new ArrayList<>();
		List<String> args = new ArrayList<>();

		flags.add(option(input.mode, input));

		if (input.manifestPath != null) {
			require(input.manifestPath);
			flags.add("m");
			args.add(input.manifestPath.getAbsolutePath());
		}

		if (input.jarPath != null) {
			flags.add("f");
			args.add(input.jarPath.getAbsolutePath());
		}

		for (File classpath : input.files.keySet()) {
			Set<File> files = input.files.get(classpath);

			args.add("-C");
			args.add(classpath.getAbsolutePath());
			for (File f : files) {
				args.add(FileCommands.getRelativePath(classpath, f).toString());
			}
		}

		String[] command = new String[1 + 1 + args.size()];
		command[0] = "jar";
		command[1] = StringCommands.printListSeparated(flags, "");
		int i = 2;
		for (String arg : args) {
			command[i] = arg;
			i++;
		}

		try {
			Exec.run(command);
		} finally {
			if (input.jarPath != null)
				provide(input.jarPath);
		}
		return None.val;
	}
}
