#! /bin/bash

PROJECT_PATH=$1
POM_PATH=$1/pom.xml

JAVA_HOME=`/usr/libexec/java_home -v1.8`
java -classpath "$JAVA_HOME/lib/*:build:build/idea/lib/*:build/plugins/maven/lib/*:build/plugins/properties/lib/*" com.atlassian.codestyle.CodeFormatApplication $POM_PATH