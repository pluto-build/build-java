package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;

import org.sugarj.common.FileCommands;

import build.pluto.builder.Builder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.output.None;

public class JarManifestGenerator extends Builder<JarManifestGenerator.Input, None> {
	
	public static class Input implements Serializable {
	    private static final long serialVersionUID = 7222269122323956517L;
	    
		public final File root;
	    public final File path;
	    public final String version;
	    public final String entryPoint;
	    public final List<File> classpath;
	    public final boolean sealedPackages;
	
	    public Input(File root, File path, String version, String entryPoint, List<File> classpath, boolean sealedPackages) {
	        this.root = root;
	        this.path = path;
	        this.version = version != null ? version : "1.0";
	        this.entryPoint = entryPoint;
	        this.classpath = classpath;
	        this.sealedPackages = sealedPackages;
	    }
	}
	
	public static BuilderFactory<Input, None, JarManifestGenerator> factory = BuilderFactoryFactory.of(JarManifestGenerator.class, Input.class);
	
	public JarManifestGenerator(Input input) {
		super(input);
	}

	@Override
	protected String description(Input input) {
		return "Generate JAR manifest " + input.path;
	}
	
	@Override
	public File persistentPath(Input input) {
		return FileCommands.addExtension(input.path, "dep");
	}

    public None build(Input input) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Manifest-Version: ").append(input.version);
        sb.append("\n");
        if (input.entryPoint != null) {
            sb.append("Main-Class: ").append(input.entryPoint);
            sb.append("\n");
        }
        if (input.classpath != null && !input.classpath.isEmpty()) {
            sb.append("Class-Path: ");
            for (File f: input.classpath) {
                if(input.root != null) {
                    Path relPath = FileCommands.getRelativePath(input.root, f);
                    sb.append(relPath.toString());
                } else {
                    sb.append("file://" + f.getAbsolutePath().toString());
                }
                sb.append(" \n ");
            }
            sb.append("\n");
        }
        if(input.sealedPackages) {
            sb.append("Sealed: ").append(input.sealedPackages);
            sb.append("\n");
        }
        sb.append("\n");

        FileCommands.writeToFile(input.path.getAbsoluteFile(), sb.toString());
        provide(input.path);
        
        return null;
    }
}
