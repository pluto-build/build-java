package org.sugarj.cleardep.buildjava;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.cleardep.SimpleMode;
import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.buildjava.util.FileExtensionFilter;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;


public class Main {

	public static void main(String[] args) {
		BuildManager manager = new BuildManager();
		
		Path baseDir = new AbsolutePath(args[0]);
		JavaBuildContext buildContext = new JavaBuildContext(manager, new RelativePath(baseDir, "bin"), baseDir);
		
		try {
			List<Path> files = new ArrayList<Path>();
			for (RelativePath p: FileCommands.listFiles(buildContext.baseDir, new FileExtensionFilter("java"))) {
				files.add(p);
			}
			
			buildContext.java.require(files, new SimpleMode());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
