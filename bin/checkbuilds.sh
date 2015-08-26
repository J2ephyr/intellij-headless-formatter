#! /bin/bash

BAMBOO="https://ecosystem-bamboo.internal.atlassian.com"

if [ -z "${JSESSIONID}" ]
then
    echo "Set JSESSIONID in my environment to a current session ID for ${BAMBOO} snarfed from your browser" 1>&2
    exit 1
fi

# Colours
reset=`tput sgr0`
red=`tput setaf 1`
green=`tput setaf 2`

while read line
do
    # Skip commented out modules
    [[ $line = \#* ]] && continue

    project=`echo $line | cut -d, -f1`
    echo "-------------------------------------------------------------------------------------------------------------"
    echo "Module: $project"

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
        buildState=$(curl -s "$BAMBOO/rest/branchinator/1.0/builds?repoId=$repoId&branchName=$branch" -H "Cookie: JSESSIONID=$JSESSIONID" | jq 'reduce .builds[] as $build (""; . + $build.planName + ":" + $build.buildState +  ":" + $build.planKey + ",")' | tr -d '"' | sed 's/,$//')

        if [[ -z "$buildState" ]]
        then
            echo "${red}No builds for this branch${reset}"
            continue
        fi

        echo $buildState | tr "," "\n" | while read build; do

            planName=$(echo $build | cut -d: -f1)
            status=$(echo $build | cut -d: -f2 | tr '[:lower:]' '[:upper:]')
            planKey=$(echo $build | cut -d: -f3)

            echo -n "Build: $planName status is"

            if [[ $status == "FAILED" ]]
            then
              echo -n "${red}"
            else
              echo -n "${green}"
            fi

            echo " [$status]${reset} - $BAMBOO/browse/$planKey/latest"

        done;
    done;

done<data/armata-modules.txt