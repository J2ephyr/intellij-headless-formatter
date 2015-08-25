#! /bin/bash

project="atlassian-core"

# Get ID of repo by name
repoId=$(curl -s "https://engservices-bamboo.internal.atlassian.com/rest/branchinator/1.0/repos?searchTerm=$project"  -H "Cookie: JSESSIONID=$JSESSIONID" | jq '.[0].id' | tr -d '"')

# Get branches for the codeformat issue
branches=$(curl -s "https://engservices-bamboo.internal.atlassian.com/rest/branchinator/1.0/branches?repoId=$repoId&searchTerm=PLATFORM-159" -H "Cookie: JSESSIONID=$JSESSIONID" | jq '.[].branchName')

for branch in $branches; do
    branch=$(echo $branch | tr -d '"')
    echo "Branch: $branch";

    # Get build status
    builds=$(curl -s "https://engservices-bamboo.internal.atlassian.com/rest/branchinator/1.0/builds?repoId=$repoId&branchName=$branch" -H "Cookie: JSESSIONID=$JSESSIONID" | jq '.builds[].buildState')
    echo $builds
done;
