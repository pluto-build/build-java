package build.pluto.buildjava;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.sugarj.common.FileCommands;

import build.pluto.builder.Builder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.dependency.Origin;
import build.pluto.output.None;

public class JarBuilder extends Builder<JarBuilder.Input, None> {
    public static BuilderFactory<Input, None, JarBuilder> factory =
        BuilderFactoryFactory.of(JarBuilder.class, Input.class);

    public static class Entry implements Serializable {
        private static final long serialVersionUID = -5162228015108592112L;

        public final String classpath;
        public final File file;

        public Entry(String classpath, File file) {
            this.classpath = classpath;
            this.file = file;
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + classpath.hashCode();
            result = prime * result + file.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            final Entry other = (Entry) obj;
            if(!classpath.equals(other.classpath))
                return false;
            if(!file.equals(other.file))
                return false;
            return true;
        }
    }

    public static class Input implements Serializable {
        private static final long serialVersionUID = 702673807538816544L;

        /**
         * Path where jar file will be created.
         */
        public final File jarPath;

        /**
         * The files of the jar. Each entry represents the file or directory to archive, and the relative path in the
         * jar where to archive it.
         */
        public final Iterable<Entry> files;

        /**
         * Origin for the files.
         */
        public final Origin origin;

        /**
         * Optional path to store the dependency file.
         */
        public final File depPath;

        public Input(File jarPath, Iterable<Entry> files, Origin origin, File depPath) {
            this.jarPath = jarPath;
            this.origin = origin;
            this.files = files;
            this.depPath = depPath;
        }

        public Input(File jarPath, Iterable<Entry> files, Origin origin) {
            this(jarPath, files, origin, null);
        }
    }

    public JarBuilder(Input input) {
        super(input);
    }

    @Override protected String description(Input input) {
        return "Creating JAR file";
    }

    @Override public File persistentPath(Input input) {
        if(input.depPath != null) {
            return input.depPath;
        }

        // HACK: not entirely sound; hash collisions may recognize non-equal file maps as equal.
        final int filesHash = input.files.hashCode();
        return FileCommands.addExtension(input.jarPath, filesHash + ".dep");
    }

    @Override protected None build(Input input) throws Throwable {
        requireBuild(input.origin);

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try(final JarOutputStream jar = new JarOutputStream(new FileOutputStream(input.jarPath), manifest)) {
            for(Entry entry : input.files) {
                final String classpath = entry.classpath;
                final File file = entry.file;

                if(file.isFile()) {
                    require(file);
                    final JarEntry jarEntry = new JarEntry(classpath);
                    final long modified = file.lastModified();
                    if(modified != 0L) {
                        jarEntry.setTime(modified);
                    }
                    jar.putNextEntry(jarEntry);
                    Files.copy(file.toPath(), jar);
                    jar.closeEntry();
                }
            }
        } finally {
            provide(input.jarPath);
        }

        return None.val;
    }
}
