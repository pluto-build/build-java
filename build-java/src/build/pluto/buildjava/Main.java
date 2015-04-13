//package build.pluto.buildjava;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.sugarj.common.FileCommands;
//import org.sugarj.common.path.AbsolutePath;
//import org.sugarj.common.path.Path;
//import org.sugarj.common.path.RelativePath;
//
//import build.pluto.builder.BuildManager;
//import build.pluto.builder.BuildRequest;
//import build.pluto.buildjava.util.FileExtensionFilter;
//
//
//public class Main {
//
//	public static void main(String[] args) {
//		Path baseDir = new AbsolutePath(args[0]);
//		
//		List<Path> files = new ArrayList<Path>();
//		for (RelativePath p: FileCommands.listFiles(baseDir, new FileExtensionFilter("java"))) {
//			files.add(p);
//		}
//		List<Path> paths = new ArrayList<>();
//		paths.add(baseDir);
//		
//		List<String> additionalArguments = new ArrayList<String>();
//		//additionalArguments.add("-XDignore.symbol.file");
//		
//		JavaBuilder.Input input = new JavaBuilder.Input(files, new RelativePath(baseDir, "bin"), paths, paths, additionalArguments, null, true);
//		BuildManager.build(new BuildRequest<>(JavaBuilder.factory, input));
//	}
//	
//}
