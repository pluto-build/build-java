#/bin/sh

ARGS="pluto-java build.pluto.buildjava.JavaBuilder.factory build.pluto.buildjava.JavaBuilder\$Input $@"

mvn compile exec:java -Dexec.args="$ARGS"
