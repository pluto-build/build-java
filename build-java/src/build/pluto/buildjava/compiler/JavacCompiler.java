package build.pluto.buildjava.compiler;

import java.io.File;
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
public class JavacCompiler implements JavaCompiler {

	private static final long serialVersionUID = 1494247105986013915L;
	
	public static final JavacCompiler instance = new JavacCompiler(); 
	private JavacCompiler() { } 
	
	public JavaCompilerResult compile(
			Collection<File> sourceFiles,
			File targetDir, 
			Collection<File> sourcePath,
			Collection<File> classPath,
			Collection<String> additionalArguments) throws Exception {
		StringBuilder cpBuilder = new StringBuilder();

		List<String> cmd = new ArrayList<>();

		cmd.add("javac");
		if (sourcePath != null && sourcePath.size() > 0) {
			StringBuilder sourcepath = new StringBuilder();
			for (File p : sourcePath)
				sourcepath.append(FileCommands.toWindowsPath(p.getAbsolutePath())).append(File.pathSeparator);
			String sourcePathtring = sourcepath.toString();
			sourcePathtring = sourcePathtring.substring(0, sourcePathtring.length() - File.pathSeparator.length());

			cmd.add("-sourcepath");
			cmd.add(sourcePathtring);
		}
		
		cmd.add("-cp");
		StringBuilder classpath = new StringBuilder();
		classpath.append(targetDir).append(File.pathSeparator);
		for (File p : classPath)
			classpath.append(FileCommands.toWindowsPath(p.getAbsolutePath())).append(File.pathSeparator);
		String classpathString = classpath.toString();
		classpathString = classpathString.substring(0, classpathString.length() - File.pathSeparator.length());
		cpBuilder.append(classpath);
		cmd.add(cpBuilder.toString());
		
		cmd.add("-d");
		cmd.add(FileCommands.toWindowsPath(targetDir.getAbsolutePath()));
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
			FileCommands.createDir(targetDir);
			
			ExecutionResult result = Exec.run(cmd.toArray(new String[cmd.size()]));
			ok = true;
//			 stdOut = StringCommands.printListSeparated(result.outMsgs, "\n");
			errOut = StringCommands.printListSeparated(result.errMsgs, "\n");
		} catch (ExecutionError e) {
			errOut = StringCommands.printListSeparated(e.errMsgs, "\n");
		}

		if (!ok) {
			List<Pair<SourceLocation, String>> errors = parseJavacErrors(errOut);
			if (!errors.isEmpty())
				throw new SourceCodeException(errors);
		}

		JavaCompilerResult result = new JavaCompilerResult();
		extractDependencies(errOut, sourcePath, result);
		return result;
	}

	private final static String ERR_PAT = ": error: ";
	// private final static String LINE_PAT = "(at line ";
	private final static String COMP_PAT = "[checking";
	private final static String GEN_PAT = "[wrote";
	private final static String DEP_REGULAR_PAT = "[loading RegularFileObject";
	private final static String DEP_ZIPPED_PAT = "[loading ZipFileIndexFileObject";
	private final static String PARSING_PAT = "[parsing started";

	private void extractDependencies(String errOut, Collection<File> sourcePath, JavaCompilerResult result) {
		Map<String, File> parsedFiles = new HashMap<>();
		File currentSource = null;
		
		String lines[] = errOut.split("\n");
		for (String line : lines) {
			if (line.startsWith(PARSING_PAT)) {
				int from = line.indexOf('[', PARSING_PAT.length()) + 1;
				int to = line.indexOf(']', from);
				String parsedPath = line.substring(from, to);
				for (File path : sourcePath) {
					Path relPath = FileCommands.getRelativePath(path, new File(parsedPath));
					if (relPath != null) {
						File parsedFile = new File(parsedPath);
						parsedFiles.put(FileCommands.dropExtension(relPath).toString(), parsedFile);
						result.addSourceFile(parsedFile);
						break;
					}
				}
			} 
			else if (line.startsWith(COMP_PAT)) {
				int from = COMP_PAT.length() + 1;
				int to = line.indexOf(']', from);
				String module = line.substring(from, to);
				File source = parsedFiles.get(module.replace('.', File.separatorChar));
				currentSource = source;
			}
			else if (line.startsWith(GEN_PAT)) {
				int from = line.indexOf('[', GEN_PAT.length()) + 1;
				int to = line.indexOf(']', from);
				String generatedPath = line.substring(from, to);
				result.addGeneratedFile(currentSource, new File(generatedPath));
			}
			else if (line.startsWith(DEP_REGULAR_PAT)) {
				int from = line.indexOf('[', DEP_REGULAR_PAT.length()) + 1;
				int to = line.indexOf(']', from);
				String generatedPath = line.substring(from, to);
				if (!generatedPath.endsWith(".java"))
					result.addLoadedClassFile(new File(generatedPath));
			}
			else if (line.startsWith(DEP_ZIPPED_PAT)) {
				int from = line.indexOf('[', DEP_ZIPPED_PAT.length()) + 1;
				int to = line.indexOf(']', from);
				String generatedPath = line.substring(from, to);
				int zipFrom = generatedPath.indexOf('(');
				int zipTo = generatedPath.indexOf(')', zipFrom);
				String zipPath = generatedPath.substring(0, zipFrom);
				String loaded = generatedPath.substring(zipFrom + 1, zipTo);
				
				if (zipPath.endsWith(".sym"))
					loaded = loaded.substring(loaded.indexOf(".jar") + 5);
				
				result.addLoadedFromZippedFile(new File(zipPath), loaded);
			}
		}
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
