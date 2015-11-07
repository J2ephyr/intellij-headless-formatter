# Headless IntelliJ Code Formatter

We had a need to reformat projects and branches using IntelliJ's code style, from the command line.

## Prerequisites

1. JDK 1.8
3. wget (brew install wget)

## To reformat a workspace do the following:

1. git clone git@github.com:terradatum/intellij-headless-formatter.git
2. ./bin/build.sh
3. ./bin/codestyle.sh path/to/workspace/root/directory

This will simply run the formatter on what is already present there. It will not check out or commit changes. This is
useful for catching up a feature branch you're resurrecting, or for projects not yet part of the platform but following 
platform styles.
