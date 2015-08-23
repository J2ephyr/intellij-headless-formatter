#! /bin/bash

PROJECT_PATH=$1
POM_PATH=$1/pom.xml

SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
BUILD_DIR=$SCRIPT_DIR/../build

# Make it absolute
BUILD_DIR=$(cd $BUILD_DIR; pwd)
PLUGINS_DIR=$BUILD_DIR/plugins

JAVA_HOME=`/usr/libexec/java_home -v1.8`
java -Didea.plugins.path=$PLUGINS_DIR -classpath "$JAVA_HOME/lib/*:$BUILD_DIR:$BUILD_DIR/idea/lib/*:$BUILD_DIR/plugins/maven/lib/*:$BUILD_DIR/plugins/properties/lib/*" com.atlassian.codestyle.CodeFormatApplication $POM_PATH