#!/bin/sh

(cd build-java; mvn install -U) && (cd build-java-eclipse; mvn install -U)
