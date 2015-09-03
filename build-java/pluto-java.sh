#/bin/sh

ARGS="pluto-java build.pluto.buildjava.JavaBuilder.factory build.pluto.buildjava.JavaInput $@"

mvn compile exec:java -Dexec.args="$ARGS"
