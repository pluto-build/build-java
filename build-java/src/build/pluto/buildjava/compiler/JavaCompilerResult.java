package build.pluto.buildjava.compiler;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class JavaCompilerResult {
	/**
	 * Maps a source file to the files generated for it.
	 */
	private final Map<File, Collection<File>> sourceTargetFiles;
	
	/**
	 * Loaded class files.
	 */
	private final Collection<File> loadedClassFiles;
	
	/**
	 * Maps a zipped file to the files loaded from it. A `null` collection means all of the zipped file was used.
	 */
	private final Map<File, Collection<String>> loadedFromZippedFile;

	public JavaCompilerResult() {
		this.sourceTargetFiles = new HashMap<>();
		this.loadedClassFiles = new HashSet<>();
		this.loadedFromZippedFile = new HashMap<>();
	}
	
	public JavaCompilerResult(Map<File, Collection<File>> generatedFiles, Collection<File> loadedFiles, Map<File, Collection<String>> loadedFromZippedFile) {
		this.sourceTargetFiles = generatedFiles;
		this.loadedClassFiles = loadedFiles;
		this.loadedFromZippedFile = loadedFromZippedFile;
	}
	
	public Map<File, Collection<File>> getSourceTargetFiles() {
		return sourceTargetFiles;
	}
	
	public Collection<File> getLoadedClassFiles() {
		return loadedClassFiles;
	}
	
	public Map<File, Collection<String>> getLoadedFromZippedFile() {
		return loadedFromZippedFile;
	}
	
	public void addSourceFile(File source) {
		Collection<File> gens = sourceTargetFiles.get(source);
		if (gens == null) {
			gens = new HashSet<>();
			sourceTargetFiles.put(source, gens);
		}
	}
	
	public void addGeneratedFile(File source, File gen) {
		Collection<File> gens = sourceTargetFiles.get(source);
		if (gens == null) {
			gens = new HashSet<>();
			sourceTargetFiles.put(source, gens);
		}
		gens.add(gen);
	}
	
	public void addLoadedClassFile(File load) {
		loadedClassFiles.add(load);
	}

	public void addLoadedFromZippedFile(File zip, String loaded) {
		Collection<String> gens = loadedFromZippedFile.get(zip);
		if (gens == null) {
			gens = new HashSet<>();
			loadedFromZippedFile.put(zip, gens);
		}
		gens.add(loaded);
	}
}
