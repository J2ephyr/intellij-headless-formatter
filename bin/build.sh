#! /bin/bash
SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
BUILD_DIR=${SCRIPT_DIR}/../build

mkdir -p ${BUILD_DIR}

# make it absolute
BUILD_DIR=$(cd ${BUILD_DIR}; pwd)

if [ ! -f ${BUILD_DIR}/idea.tar.gz ]; then
    echo "Downloading IntelliJ"
    wget https://d1opms6zj7jotq.cloudfront.net/idea/ideaIC-15.0.tar.gz -O ${BUILD_DIR}/idea.tar.gz
    if [ ! $? -eq 0 ]; then
        echo "Failed to wget IntelliJ idea.tar.gz"
        exit 1
    fi
else
    echo "Already downloaded IntelliJ"
fi

if [ ! -d ${BUILD_DIR}/idea ] ; then
    echo "Unarchiving IntelliJ"
    mkdir ${BUILD_DIR}/idea
    tar xvzf ${BUILD_DIR}/idea.tar.gz -C ${BUILD_DIR}/idea --strip-components=1
else
    echo "Already unarchived IntelliJ"
fi

if [ ! -d ${BUILD_DIR}/plugins ] ; then
    echo "Linking Maven, Properties and Eclipse plugins"
    mkdir ${BUILD_DIR}/plugins
    ln -s ${BUILD_DIR}/idea/plugins/maven ${BUILD_DIR}/plugins/maven
    ln -s ${BUILD_DIR}/idea/plugins/properties ${BUILD_DIR}/plugins/properties
    ln -s ${BUILD_DIR}/idea/plugins/eclipse ${BUILD_DIR}/plugins/eclipse
else
    echo "Already linked Maven, Properties and Eclipse plugins"
fi

# xattr -d com.apple.quarantine *

echo "Compiling..."
javac -classpath "$BUILD_DIR/idea/lib/*:$BUILD_DIR/idea/plugins/maven/lib/*:$BUILD_DIR/idea/plugins/properties/lib/*:$BUILD_DIR/idea/plugins/eclipse/lib/*"  ${SCRIPT_DIR}/../src/com/terradatum/codestyle/CodeFormatApplication.java -d ${BUILD_DIR}
