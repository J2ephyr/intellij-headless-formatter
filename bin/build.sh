#! /bin/bash

mkdir -p build
cd build

if [ ! -f idea.tar.gz ]; then
    echo "Downloading IntelliJ"
    wget https://d1opms6zj7jotq.cloudfront.net/idea/ideaIC-15.0.tar.gz -O idea.tar.gz
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

if [ ! -f scala.zip ]; then
    echo "Downloading Scala plugin"
    wget "https://plugins.jetbrains.com/plugin/download?pr=idea&updateId=22150" -O scala.zip
    if [ ! $? -eq 0 ]; then
        echo "Failed to wget Scala plugin scala.zip"
        exit 1
    fi
else
    echo "Already downloaded Scala plugin"
fi

if [ ! -d idea/plugins/Scala ] ; then
    echo "Unarchiving Scala plugin"
    unzip scala.zip -d idea/plugins
else
    echo "Already unarchived Scala plugin"
fi

if [ ! -d plugins ] ; then
    echo "Linking Maven, Properties, Eclipse and Scala plugins"
    mkdir plugins
    ln -s ../idea/plugins/maven plugins/maven
    ln -s ../idea/plugins/properties plugins/properties
    ln -s ../idea/plugins/eclipse plugins/eclipse
    ln -s ../idea/plugins/Scala plugins/Scala
else
    echo "Already linked Maven, Properties, Eclipse and Scala plugins"
fi

# xattr -d com.apple.quarantine *

echo "Compiling..."
javac -classpath "idea/lib/*:idea/plugins/maven/lib/*:idea/plugins/properties/lib/*:idea/plugins/eclipse/lib/*:idea/plugins/Scala/lib/*:idea/plugins/Scala/launcher/*"  ../src/com/atlassian/codestyle/CodeFormatApplication.java -d .
