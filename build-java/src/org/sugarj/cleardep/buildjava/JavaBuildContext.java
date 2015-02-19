package org.sugarj.cleardep.buildjava;

import java.util.List;

import org.sugarj.common.path.Path;
import org.sugarj.cleardep.build.BuildContext;
import org.sugarj.cleardep.build.BuildManager;


public class JavaBuildContext extends BuildContext {

	public JavaBuilder java = JavaBuilder.factory.makeBuilder(this);
	
	public final Path binPath;
	public final Path baseDir;
	
	public JavaBuildContext(BuildManager manager, Path binPath, Path baseDir) {
		super(manager);
		
		this.binPath = binPath;
		this.baseDir = baseDir;
	}

}
