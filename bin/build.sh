#! /bin/bash

mkdir -p build
cd build

if [ ! -f idea.tar.gz ]; then
    echo "Downloading IntelliJ"
    wget https://download.jetbrains.com/idea/ideaIC-14.1.5.tar.gz -O idea.tar.gz
    #wget https://download.jetbrains.com/idea/ideaIU-14.1.5.tar.gz -O idea.tar.gz

    if [ ! $? -eq 0 ]; then
        echo "Failed to wget IntelliJ idea.tar.gz"
        exit 1
    fi
else
    echo "Already downloaded IntelliJ"
fi

if [ ! -d idea ] ; then
    echo "Unarchiving IntelliJ"
    mkdir idea
    tar xvzf idea.tar.gz -C idea --strip-components=1
else
    echo "Already unarchived IntelliJ"
fi

if [ ! -d plugins ] ; then
    echo "Linking Maven and Properties plugins"
    mkdir plugins
    ln -s ../idea/plugins/maven plugins/maven
    ln -s ../idea/plugins/properties plugins/properties
else
    echo "Already linked Maven and Properties plugins"
fi

# xattr -d com.apple.quarantine *

echo "Compiling..."
javac -classpath "idea/lib/*:idea/plugins/maven/lib/*"  ../src/com/atlassian/codestyle/CodeFormatApplication.java -d .
