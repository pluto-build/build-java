package build.pluto.buildjava.eclipse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared execution environment.
 * 
 * @author Sebastian Erdweg <seba at informatik uni-marburg de>
 */
public class Environment {

	private File root = new File(".");

	private File binPath = new File(root, "bin");

	private File tmpDir = new File(System.getProperty("java.io.tmpdir"));

	private List<File> sourcePath = new ArrayList<>();
	private List<File> includePath = new ArrayList<>();

	private String javaComplianceLevel;


	public Environment() {
	}

	public File getRoot() {
		return root;
	}

	public void setRoot(File root) {
		this.root = root;
	}

	public void addToSourcePath(File p) {
		sourcePath.add(p);
	}

	public List<File> getSourcePath() {
		return Collections.unmodifiableList(new ArrayList<>(sourcePath));
	}

	public void setSourcePath(List<File> sourcePath) {
		this.sourcePath = sourcePath;
	}

	public File getBin() {
		return this.binPath;
	}

	public void setBin(File binPath) {
		this.binPath = binPath;
	}

	public File getTmpDir() {
		return tmpDir;
	}

	public void setTmpDir(File tmpDir) {
		this.tmpDir = tmpDir;
	}

	public void addToIncludePath(File p) {
		this.includePath.add(p);
	}

	public List<File> getIncludePath() {
		return Collections.unmodifiableList(new ArrayList<>(includePath));
	}

	public void setIncludePath(List<File> includePath) {
		this.includePath = includePath;
	}

	public File createOutPath(String relativePath) {
		return new File(getBin(), relativePath);
	}

	public String getJavaComplianceLevel() {
		return javaComplianceLevel;
	}

	public void setJavaComplianceLevel(String javaComplianceLevel) {
		this.javaComplianceLevel = javaComplianceLevel;
	}
}