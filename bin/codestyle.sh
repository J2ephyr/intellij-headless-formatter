#!/bin/bash -x

# Mac or Linux
PLATFORM=$(uname -s)

PROJECT_PATH=$1

SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
BUILD_DIR=${SCRIPT_DIR}/../build

# Make it absolute
BUILD_DIR=$(cd ${BUILD_DIR}; pwd)
PLUGINS_DIR=${BUILD_DIR}/plugins

# Make sure we start with a clean config and system dirname
rm -rf ${BUILD_DIR}/idea/config
rm -rf ${BUILD_DIR}/idea/system

# JAVA_HOME
[[ "$PLATFORM" == "Darwin" ]] && JHOME=$(/usr/libexec/java_home -v1.8) || JHOME=/usr/lib/jvm/java-8-jdk
JAVA_HOME="$JHOME"

# NO DEBUG
#java -Dlog4j.configuration=${SCRIPT_DIR}/log4j.xml -Dcode.style=${SCRIPT_DIR}/codeStyleSettings.xml -Didea.plugins.path=${PLUGINS_DIR} -classpath "$JAVA_HOME/lib/*:$BUILD_DIR:$BUILD_DIR/idea/lib/*:$BUILD_DIR/plugins/maven/lib/*:$BUILD_DIR/plugins/properties/lib/*:$BUILD_DIR/plugins/eclipse/lib/*" com.terradatum.codestyle.CodeFormatApplication ${PROJECT_PATH}
# DEBUG
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 -Dlog4j.configuration=${SCRIPT_DIR}/log4j.xml -Dcode.style=${SCRIPT_DIR}/codeStyleSettings.xml -Didea.plugins.path=${PLUGINS_DIR} -classpath "$JAVA_HOME/lib/*:$BUILD_DIR:$BUILD_DIR/idea/lib/*:$BUILD_DIR/plugins/maven/lib/*:$BUILD_DIR/plugins/properties/lib/*:$BUILD_DIR/plugins/eclipse/lib/*" com.terradatum.codestyle.CodeFormatApplication ${PROJECT_PATH}
