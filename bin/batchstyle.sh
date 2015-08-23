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
        echo "Processing branch: $branch"
    done
done<data/armata-modules.txt