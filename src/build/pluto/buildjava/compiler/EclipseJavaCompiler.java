package build.pluto.buildjava.compiler;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.compiler.batch.BatchCompiler;
import org.sugarj.common.FileCommands;
import org.sugarj.common.errors.SourceCodeException;
import org.sugarj.common.errors.SourceLocation;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.util.Pair;

/**
 * 
 * Provides methods related to processing Java. Mainly, we provide a method for
 * compiling Java code.
 */
public class EclipseJavaCompiler implements JavaCompiler {

	private static final long serialVersionUID = 1494247105986013915L;
	
	public static final EclipseJavaCompiler instance = new EclipseJavaCompiler(); 
	private EclipseJavaCompiler() { } 
	
	public JavaCompilerResult compile(
			Collection<File> sourceFiles,
			File targetDir, 
			Collection<File> sourcePath,
			Collection<File> classPath,
			String sourceRelease,
			String targetRelease,
			Collection<String> additionalArguments) throws Exception {
		StringBuilder cpBuilder = new StringBuilder();

		List<String> cmd = new ArrayList<>();

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
		if (sourceRelease != null) {
			cmd.add("-source");
			cmd.add(sourceRelease);
		}
		if (targetRelease != null) {
			cmd.add("-target");
			cmd.add(targetRelease);
		}

		if (additionalArguments != null)
			for (String arg : additionalArguments)
				cmd.add(arg);

		for (File sourceFile : sourceFiles)
			cmd.add(FileCommands.toWindowsPath(sourceFile.getAbsolutePath()));

		StringWriter outWriter = new StringWriter();
		StringWriter errWriter = new StringWriter();

		FileCommands.createDir(targetDir);
		boolean ok = BatchCompiler.compile(
				cmd.toArray(new String[cmd.size()]), 
				new PrintWriter(outWriter), 
				new PrintWriter(errWriter),
				null);
		String outOut = outWriter.toString();
		String errOut = errWriter.toString();

		if (!ok) {
			List<Pair<SourceLocation, String>> errors = parseJavacErrors(errOut);
			if (!errors.isEmpty())
				throw new SourceCodeException(errors);
		}

		Map<File, Collection<File>> generatedFiles = extractGeneratedFiles(outOut, sourcePath, targetDir);
		List<File> requiredFiles = extractRequiredFiles(outOut);

		// TODO provided actually loaded from zipped file, last arg below.
		return new JavaCompilerResult(generatedFiles, requiredFiles, Collections.<File, Collection<String>>emptyMap());
	}

	private final static String ERR_PAT = ": error: ";
	// private final static String LINE_PAT = "(at line ";
	private final static String START_PAT = "[checking";
	private final static String GEN_PAT = "[writing";
	private final static String DEP_PAT = "[reading";
	private final static String PARSING_PAT = "[parsing";

	private static Map<File, Collection<File>> extractGeneratedFiles(String outOut, Collection<File> sourcePath, File targetDir) {
		Map<String, File> parsedFiles = new HashMap<>();
		Map<File, Collection<File>> generatedFiles = new HashMap<>();
		File currentSource = null;
		int index = 0;
		while (true) {
			int parsingIndex = outOut.indexOf(PARSING_PAT, index);
			int startIndex = outOut.indexOf(START_PAT, index);
			int genIndex = outOut.indexOf(GEN_PAT, index);
			
			if (genIndex < 0)
				break;
			
			if (parsingIndex >= 0 && (startIndex < 0 || parsingIndex < startIndex) && (genIndex < 0 || parsingIndex < genIndex)) {
				index = parsingIndex;
				index += PARSING_PAT.length();
				while (outOut.charAt(index) == ' ')
					index++;
				int to = outOut.indexOf(" - ", index);
				String parsedPath = outOut.substring(index, to);
				for (File path : sourcePath) {
					Path relPath = FileCommands.getRelativePath(path, new File(parsedPath));
					if (relPath != null)
						parsedFiles.put(FileCommands.dropExtension(relPath).toString(), new File(parsedPath));
				}
			}
//			else if (startIndex >= 0 && (genIndex < 0 || startIndex < genIndex)) {
//				index = startIndex;
//				index += START_PAT.length();
//				int to = outOut.indexOf(']', index);
//				String module = outOut.substring(index, to);
//				File source = parsedFiles.get(module.replace('.', File.separatorChar));
//				currentSource = source;
//			}
			else if (genIndex >= 0) {
				index = genIndex;
				index += GEN_PAT.length();
				while (outOut.charAt(index) == ' ')
					index++;
				int to = outOut.indexOf(" - ", index);
				String generatedPath = outOut.substring(index, to);
				String fileName = FileCommands.dropExtension(generatedPath);
				if (!parsedFiles.containsKey(fileName)) {
					int dollar = fileName.indexOf('$');
					if (dollar >= 0)
						fileName = fileName.substring(0, dollar);
				}
				if (!parsedFiles.containsKey(fileName))
					throw new IllegalStateException("Cannot associate source file to " + generatedPath);
				File source = parsedFiles.get(fileName);
				Collection<File> files = generatedFiles.get(source);
				if (files == null) {
					files = new ArrayList<>();
					generatedFiles.put(source, files);
				}
				files.add(new File(targetDir, generatedPath));
			}
			else
				break;
		}
		return generatedFiles;
	}

	private static List<File> extractRequiredFiles(String outOut) {
		// TODO extract required files
		return Collections.emptyList();
//		List<File> generatedFiles = new LinkedList<>();
//		int index = 0;
//		while ((index = outOut.indexOf(DEP_PAT, index)) >= 0) {
//			index += DEP_PAT.length();
//			while (outOut.charAt(index) == ' ')
//				index++;
//			int to = outOut.indexOf(']', index);
//			String generatedPath = outOut.substring(index, to);
//			if (generatedPath.contains(".sym")) {
//				generatedPath = generatedPath.substring(0, generatedPath.lastIndexOf(".sym") + 4);
//			}
//			if (generatedPath.contains(".jar")) {
//				generatedPath = generatedPath.substring(0, generatedPath.lastIndexOf(".jar") + 4);
//			}
//			File file = new File(generatedPath);
//			if (!generatedFiles.contains(file))
//				generatedFiles.add(file);
//		}
//		return generatedFiles;
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
