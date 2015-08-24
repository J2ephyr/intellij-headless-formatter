# Headless IntelliJ Code Formatter

We had a need to reformat multiple projects and branches using IntelliJ's code style. Rather than do this manually this
little repo provides some scripts to do this automatically.

To reformat all platform modules do the follow:

1. git clone git@bitbucket.org:marcosscriven/intellij-headless-formatter.git
2. ./bin/build.sh
3. ./bin/batchstyle.sh

This will checkout all platform modules, and for each branch will format them and submit the changes on issue branches
for the given version.