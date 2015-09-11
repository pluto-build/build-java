package build.pluto.buildjava.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sugarj.common.Exec;
import org.sugarj.common.Exec.ExecutionError;
import org.sugarj.common.Exec.ExecutionResult;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.util.Pair;

/**
 * 
 * Provides methods related to processing Java. Mainly, we provide a method for
 * compiling Java code.
 * 
 * @author Manuel Weiel <weiel at st.informatik.tu-darmstadt.de
 *
 */
public class JavaCommands {

	public static class JavacResult {
		public Map<File, List<File>> generatedFiles;
		public List<File> loadedFiles;

	}

	/**
	 * @return list of generated class files + list of required class files.
	 */
	public static JavacResult javac(Collection<File> sourceFiles, List<File> sourcePaths, File dir, String[] additionalArguments, List<File> cp)
			throws IOException, SourceCodeException {
		StringBuilder cpBuilder = new StringBuilder();

		List<String> cmd = new ArrayList<>();

		cmd.add("javac");
		if (sourcePaths != null && sourcePaths.size() > 0) {
			StringBuilder sourcepath = new StringBuilder();
			for (File p : sourcePaths)
				sourcepath.append(FileCommands.toWindowsPath(p.getAbsolutePath())).append(File.pathSeparator);
			String sourcepathString = sourcepath.toString();
			sourcepathString = sourcepathString.substring(0, sourcepathString.length() - File.pathSeparator.length());

			cmd.add("-sourcepath");
			cmd.add(sourcepathString);
		}
		
		cmd.add("-cp");
		StringBuilder classpath = new StringBuilder();
		classpath.append(dir).append(File.pathSeparator);
		for (File p : cp)
			classpath.append(FileCommands.toWindowsPath(p.getAbsolutePath())).append(File.pathSeparator);
		String classpathString = classpath.toString();
		classpathString = classpathString.substring(0, classpathString.length() - File.pathSeparator.length());
		cpBuilder.append(classpath);
		cmd.add(cpBuilder.toString());
		
		cmd.add("-d");
		cmd.add(FileCommands.toWindowsPath(dir.getAbsolutePath()));
		cmd.add("-nowarn");
		cmd.add("-verbose");
		cmd.add("-implicit:none");


		if (additionalArguments != null)
			for (String arg : additionalArguments)
				cmd.add(arg);

		for (File sourceFile : sourceFiles)
			cmd.add(FileCommands.toWindowsPath(sourceFile.getAbsolutePath()));

		// String stdOut;
		String errOut;
		boolean ok = false;
		try {
			FileCommands.createDir(dir);
			
//			BatchCompiler.compile(cmd.toArray(new String[cmd.size()]), new PrintWriter("/Users/seba/tmp/ejcout"), new PrintWriter("/Users/seba/tmp/ejcerr"), null);
			ExecutionResult result = Exec.run(cmd.toArray(new String[cmd.size()]));
			ok = true;
//			 stdOut = StringCommands.printListSeparated(result.outMsgs, "\n");
			errOut = StringCommands.printListSeparated(result.errMsgs, "\n");
		} catch (ExecutionError e) {
			// stdOut = StringCommands.printListSeparated(e.outMsgs, "\n");
			errOut = StringCommands.printListSeparated(e.errMsgs, "\n");
		}

		if (!ok) {
			List<Pair<SourceLocation, String>> errors = parseJavacErrors(errOut);
			if (!errors.isEmpty())
				throw new SourceCodeException(errors);
		}

		Map<File, List<File>> generatedFiles = extractGeneratedFiles(errOut, sourcePaths);
		List<File> dependentFiles = extractDependentFiles(errOut);

		JavacResult result = new JavacResult();
		result.generatedFiles = generatedFiles;
		result.loadedFiles = dependentFiles;

		return result;
	}

	private final static String ERR_PAT = ": error: ";
	// private final static String LINE_PAT = "(at line ";
	private final static String START_PAT = "[checking";
	private final static String GEN_PAT = "[wrote";
	private final static String DEP_PAT = "[loading";
	private final static String PARSING_PAT = "[parsing started";

	private static Map<File, List<File>> extractGeneratedFiles(String errOut, List<File> sourcePaths) {
		Map<String, File> parsedFiles = new HashMap<>();
		Map<File, List<File>> generatedFiles = new HashMap<>();
		File currentSource = null;
		int index = 0;
		while (true) {
			int parsingIndex = errOut.indexOf(PARSING_PAT, index);
			int startIndex = errOut.indexOf(START_PAT, index);
			int genIndex = errOut.indexOf(GEN_PAT, index);
			
			if (genIndex < 0)
				break;
			
			if (parsingIndex >= 0 && parsingIndex < startIndex && parsingIndex < genIndex) {
				index = parsingIndex;
				index += PARSING_PAT.length();
				while (errOut.charAt(index) != '[')
					index++;
				index++;
				int to = errOut.indexOf(']', index);
				String parsedPath = errOut.substring(index, to);
				for (File path : sourcePaths) {
					Path relPath = FileCommands.getRelativePath(path, new File(parsedPath));
					if (relPath != null)
						parsedFiles.put(FileCommands.dropExtension(relPath).toString(), new File(parsedPath));
				}
			}
			else if (startIndex >= 0 && startIndex < genIndex) {
				index = startIndex;
				index += START_PAT.length();
				index++;
				int to = errOut.indexOf(']', index);
				String module = errOut.substring(index, to);
				File source = parsedFiles.get(module.replace('.', File.separatorChar));
				currentSource = source;
			}
			else if (genIndex >= 0) {
				index = genIndex;
				index += GEN_PAT.length();
				while (errOut.charAt(index) != '[')
					index++;
				index++;
				int to = errOut.indexOf(']', index);
				String generatedPath = errOut.substring(index, to);
				List<File> files = generatedFiles.get(currentSource);
				if (files == null) {
					files = new ArrayList<>();
					generatedFiles.put(currentSource, files);
				}
				files.add(new File(generatedPath));
			}
			else
				break;
		}
		return generatedFiles;
	}

	private static List<File> extractDependentFiles(String errOut) {
		List<File> generatedFiles = new LinkedList<>();
		int index = 0;
		while ((index = errOut.indexOf(DEP_PAT, index)) >= 0) {
			index += DEP_PAT.length();
			while (errOut.charAt(index) != '[')
				index++;
			index++;
			int to = errOut.indexOf(']', index);
			String generatedPath = errOut.substring(index, to);
			if (generatedPath.contains(".sym")) {
				generatedPath = generatedPath.substring(0, generatedPath.lastIndexOf(".sym") + 4);
			}
			if (generatedPath.contains(".jar")) {
				generatedPath = generatedPath.substring(0, generatedPath.lastIndexOf(".jar") + 4);
			}
			File file = new File(generatedPath);
			if (!generatedFiles.contains(file))
				generatedFiles.add(file);
		}
		return generatedFiles;
	}

	/**
	 * @param stdOut
	 */
	private static List<Pair<SourceLocation, String>> parseJavacErrors(String s) {
		List<Pair<SourceLocation, String>> errors = new LinkedList<Pair<SourceLocation, String>>();
		int index = 0;
		while ((index = s.indexOf(ERR_PAT, index)) >= 0) {
			int lineStart = index - 1;
			while (s.charAt(lineStart) != ':')
				lineStart--;
			int line = Integer.parseInt(s.substring(lineStart + 1, index));
			int fileStart = lineStart - 1;
			while (s.charAt(fileStart) != '\n')
				fileStart--;

			String file = s.substring(fileStart + 1, lineStart);
			index += ERR_PAT.length();
			int errorEnd = s.indexOf("\n", index);
			String msg = s.substring(index, errorEnd);

			int columnLineStart = s.indexOf("\n", s.indexOf("\n", errorEnd));
			int columnSignIndex = s.indexOf("^", columnLineStart);
			int colStart = columnSignIndex - columnLineStart;

			int end = s.indexOf("\n", columnSignIndex);

			errors.add(Pair.create(new SourceLocation(new AbsolutePath(file), line, line, colStart, colStart), msg));
			index = end + 1;
		}
		return errors;
	}
}
