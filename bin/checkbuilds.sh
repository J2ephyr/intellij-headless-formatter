#! /bin/bash

BAMBOO="https://ecosystem-bamboo.internal.atlassian.com"

# Colours
reset=`tput sgr0`
red=`tput setaf 1`
green=`tput setaf 2`

while read line
do
    # Skip commented out modules
    [[ $line = \#* ]] && continue

    project=`echo $line | cut -d, -f1`
    echo "Getting builds for $project"

    # Get ID of repo by name
    repoId=$(curl -s "$BAMBOO/rest/branchinator/1.0/repos?searchTerm=$project"  -H "Cookie: JSESSIONID=$JSESSIONID" | jq '.[0].id' | tr -d '"')

    if [ $repoId == "null" ]; then
        echo "${red}Module not found on $BAMBOO.${reset}"
        continue
    fi


    # Get branches for the codeformat issue
    branches=$(curl -s "$BAMBOO/rest/branchinator/1.0/branches?repoId=$repoId&searchTerm=PLATFORM-159" -H "Cookie: JSESSIONID=$JSESSIONID" | jq '.[].branchName')

    for branch in $branches; do
        branch=$(echo $branch | tr -d '"')
        echo "Branch: $branch ";

        # Get build status
        buildState=$(curl -s "$BAMBOO/rest/branchinator/1.0/builds?repoId=$repoId&branchName=$branch" -H "Cookie: JSESSIONID=$JSESSIONID" | jq 'reduce .builds[] as $build (""; . + $build.planName + ": " + $build.buildState + ",")' | tr -d '"')
        echo $buildState | tr "," "\n" | while read build; do

            if [[ $build == *"Failed"* ]]
            then
              echo -n "${red}"
            else
              echo -n "${green}"
            fi
            echo "$build${reset}"

        done;
    done;

done<data/armata-modules.txt