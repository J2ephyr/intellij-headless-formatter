#! /bin/bash

echo "Batch processing Armata platform modules."

while read line
do
    module=`echo $line | cut -d, -f1`
    repo=`echo $line | cut -d, -f2`
    echo "Processing module: $module with repo: $repo"

    # Fetch the branches
    cat data/platform-module-branches.json |  jq '.modules[] | select(.name=="'$module'") | .branches| join(",")' | tr -d '"' | tr ',' '\n' | while read branch
    do
        # Remove everything before the last hyphen
        version=${branch##*-![0-9]}

        # Remove the .x suffix
        version=${version%.x}

        # Remove any leading v
        version=${version#v}

        # Remove any '-stable' suffix
        version=${version%-stable}

        if [ "$version" == "master" ]; then
            issueBranch="issue"
        else
            issueBranch="issue-$version"
        fi

        issueBranch=$issueBranch/PLATFORM-159-code-reformat-`date +%s`

        echo "Processing branch: $branch on issue branch: $issueBranch"
    done
done<data/armata-modules.txt