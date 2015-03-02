package org.sugarj.cleardep.buildjava.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

/**
 * Shared execution environment.
 * 
 * @author Sebastian Erdweg <seba at informatik uni-marburg de>
 */
public class Environment {

	private Path root = new AbsolutePath(".");

	private Path binPath = new RelativePath(root, "bin");

	private Path tmpDir = new AbsolutePath(System.getProperty("java.io.tmpdir"));

	private List<Path> sourcePath = new LinkedList<Path>();
	private List<Path> includePath = new LinkedList<Path>();

	private String javaComplianceLevel;


	public Environment() {
	}

	public Path getRoot() {
		return root;
	}

	public void setRoot(Path root) {
		this.root = root;
	}

	public void addToSourcePath(Path p) {
		sourcePath.add(p);
	}

	public List<Path> getSourcePath() {
		return Collections.unmodifiableList(new ArrayList<>(sourcePath));
	}

	public void setSourcePath(List<Path> sourcePath) {
		this.sourcePath = sourcePath;
	}

	public Path getBin() {
		return this.binPath;
	}

	public void setBin(Path binPath) {
		this.binPath = binPath;
	}

	public Path getTmpDir() {
		return tmpDir;
	}

	public void setTmpDir(Path tmpDir) {
		this.tmpDir = tmpDir;
	}

	public void addToIncludePath(Path p) {
		this.includePath.add(p);
	}

	public List<Path> getIncludePath() {
		return Collections.unmodifiableList(new ArrayList<>(includePath));
	}

	public void setIncludePath(List<Path> includePath) {
		this.includePath = includePath;
	}

	public RelativePath createOutPath(String relativePath) {
		return new RelativePath(getBin(), relativePath);
	}

	public String getJavaComplianceLevel() {
		return javaComplianceLevel;
	}

	public void setJavaComplianceLevel(String javaComplianceLevel) {
		this.javaComplianceLevel = javaComplianceLevel;
	}
}