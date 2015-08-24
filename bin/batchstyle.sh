#! /bin/bash

echo "Batch processing Armata platform modules."

mkdir -p working
cd working

while read line
do
    [[ $line = \#* ]] && continue

    module=`echo $line | cut -d, -f1`
    repo=`echo $line | cut -d, -f2`
    echo "Processing module: $module with repo: $repo"

    # Fetch the branches
    cat ../data/platform-module-branches.json |  jq '.modules[] | select(.name=="'$module'") | .branches| join(",")' | tr -d '"' | tr ',' '\n' | while read branch
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

        if [ ! -d $module/.git ]; then
            git clone $repo $module
        fi

        git -C $module checkout $branch
        git -C $module pull
        git -C $module reset --hard
        git -C $module clean -dxf
        git -C $module checkout -b $issueBranch

        ../bin/codestyle.sh $module

        # Check it worked, and if not log the problem and continue
        if [[ $? != 0 ]]; then
            echo -e "\e[31m*******************************************************************************************"
            echo -e "\e[31mFailed to format code for module: $module on branch: $branch"
            echo -e "\e[31m*******************************************************************************************"
            continue
        fi

        git -C $module add .
        git -C $module commit --author="codeformatter <>" -m "PLATFORM-159: Code formatting only - see JIRA issue for details."
        git -C $module push

    done
done<../data/armata-modules.txt