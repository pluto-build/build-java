package org.sugarj.cleardep.buildjava.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.sugarj.common.CommandExecution;
import org.sugarj.common.CommandExecution.ExecutionError;
import org.sugarj.common.FileCommands;
import org.sugarj.common.StringCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
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

	/**
	 * @return list of generated class files.
	 */
	public static Pair<List<Path>, List<Path>> javac(List<Path> sourceFiles,
			List<Path> sourcePaths, Path dir, List<String> additionalArguments,
			List<Path> cp) throws IOException, SourceCodeException {
		StringBuilder cpBuilder = new StringBuilder();

		for (int i = 0; i < cp.size(); i++) {
			cpBuilder.append(FileCommands.toWindowsPath(cp.get(i)
					.getAbsolutePath()));

			if (i < cp.size() - 1)
				cpBuilder.append(File.pathSeparator);
		}

		if (cp.size() > 0)
			cpBuilder.append(File.pathSeparator);

		cpBuilder.append(dir);

		int argNum = 9
				+ (sourcePaths == null || sourcePaths.size() == 0 ? 0 : 2)
				+ (additionalArguments != null ? additionalArguments.size() : 0);
		int next = 0;
		String[] cmd = new String[argNum + sourceFiles.size()];
		cmd[next++] = "javac";
		cmd[next++] = "-cp";
		cmd[next++] = cpBuilder.toString();
		cmd[next++] = "-d";
		cmd[next++] = FileCommands.toWindowsPath(dir.getAbsolutePath());
		cmd[next++] = "-source";
		cmd[next++] = "1.5";
		cmd[next++] = "-nowarn";
		cmd[next++] = "-verbose";
		if (sourcePaths != null && sourcePaths.size() > 0) {
			StringBuilder spBuilder = new StringBuilder();
			for (Path sp : sourcePaths) {
				spBuilder.append(sp.getAbsolutePath()).append(
						File.pathSeparator);
			}
			String sp = spBuilder.toString();

			cmd[next++] = "-sourcepath";
			cmd[next++] = sp.substring(0, sp.length() - 1);
		}
		if (additionalArguments != null) {
			for (String arg : additionalArguments) {
				cmd[next++] = arg;
			}
		}

		for (int i = 0; i < sourceFiles.size(); i++)
			cmd[i + next] = FileCommands.toWindowsPath(sourceFiles.get(i)
					.getAbsolutePath());

		String stdOut;
		String errOut;
		boolean ok = false;
		try {
  		String[][] result = new CommandExecution(true).execute(cmd);
  		ok = true;
  		stdOut = StringCommands.printListSeparated(result[0], "\n");
  		errOut = StringCommands.printListSeparated(result[1], "\n");
		} catch (ExecutionError e) {
		  stdOut = StringCommands.printListSeparated(e.getOutMsgs(), "\n");
		  errOut = StringCommands.printListSeparated(e.getErrMsgs(), "\n");
		}
		
		if (!ok) {
			List<Pair<SourceLocation, String>> errors = parseJavacErrors(errOut);
			throw new SourceCodeException(errors);
		}

		List<Path> generatedFiles = extractGeneratedFiles(errOut);
		List<Path> dependentFiles = extractDependentFiles(errOut);

		return new Pair<List<Path>, List<Path>>(generatedFiles, dependentFiles);
	}

	private final static String ERR_PAT = ": error: ";
	private final static String LINE_PAT = "(at line ";
	private final static String GEN_PAT = "[wrote";
	private final static String DEP_PAT = "[loading";

	private static List<Path> extractGeneratedFiles(String errOut) {
		List<Path> generatedFiles = new LinkedList<Path>();
		int index = 0;
		while ((index = errOut.indexOf(GEN_PAT, index)) >= 0) {
			index += GEN_PAT.length();
			while (errOut.charAt(index) != '[')
				index++;
			index++;
			int to = errOut.indexOf(']', index);
			String generatedPath = errOut.substring(index, to);
			generatedFiles.add(new AbsolutePath(generatedPath));
		}
		return generatedFiles;
	}
	
	private static List<Path> extractDependentFiles(String errOut) {
		List<Path> generatedFiles = new LinkedList<Path>();
		int index = 0;
		while ((index = errOut.indexOf(DEP_PAT, index)) >= 0) {
			index += DEP_PAT.length();
			while (errOut.charAt(index) != '[')
				index++;
			index++;
			int to = errOut.indexOf(']', index);
			String generatedPath = errOut.substring(index, to);
			if (generatedPath.contains(".sym")) {
				generatedPath = generatedPath.substring(0, generatedPath.lastIndexOf(".sym")+4);
			}
			if (generatedPath.contains(".jar")) {
				generatedPath = generatedPath.substring(0, generatedPath.lastIndexOf(".jar")+4);
			}
			Path file = new AbsolutePath(generatedPath);
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
			int lineStart = index-1;
			while (s.charAt(lineStart) != ':')
				lineStart--;
			int line = Integer.parseInt(s.substring(lineStart+1, index));
			int fileStart = lineStart-1;
			while (s.charAt(fileStart) != '\n')
				fileStart--;
			String file = s.substring(fileStart+1, lineStart);
			index += ERR_PAT.length();
			int errorEnd = s.indexOf("\n", index);
			String msg = s.substring(index, errorEnd);
			
			int columnLineStart = s.indexOf("\n", s.indexOf("\n", errorEnd));
			int columnSignIndex = s.indexOf("^", columnLineStart);
			int colStart = columnSignIndex - columnLineStart;
			
			int end = s.indexOf("\n", columnSignIndex);
			
			errors.add(Pair.create(new SourceLocation(new AbsolutePath(file),
					line, line, colStart, colStart), msg));
			index = end + 1;
		}
		return errors;
	}
}
