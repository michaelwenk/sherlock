#!/bin/bash

mvn install:install-file \
   -Dfile=../lib/casekit-1.0-SNAPSHOT-jar-with-dependencies.jar \
   -DgroupId=org.openscience \
   -DartifactId=casekit \
   -Dversion=1.0 \
   -Dpackaging=jar \
   -DgeneratePom=true