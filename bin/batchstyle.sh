#! /bin/bash

# Colours
reset=`tput sgr0`
red=`tput setaf 1`
green=`tput setaf 2`

rm -rf build/idea/config
rm -rf build/idea/system

mkdir -p working
cd working

log=build-`date +%s`.log
exec &> >(tee -a "$log")

function emptymerge() {

  gitDir=$1
  source=$2
  target=$3

  if [[ $source == $target ]]
  then
    echo "${red}Not merging as source: $source same as target: $target${reset}"
    return
  fi

  cd $gitDir

  aheadCount=`git rev-list $source ^$target --oneline | grep -v PLATFORM-159 | wc -l | xargs`
  codeFormatCount=`git rev-list $source ^$target --oneline | grep PLATFORM-159 | wc -l | xargs`

  if [[ $aheadCount > 0 ]]; then
      echo "${red}$source has unmerged commits other than PLATFORM-159 not reachable from $target. NOT merging.${reset}"
      git rev-list $source ^$target --oneline | grep -v PLATFORM-159
  else
      if [[ $codeFormatCount < 1 ]]; then
          echo "${red}$source doesn't contain PLATFORM-159 commits not reachable from $target. NOT merging.${reset}"
      else
          echo "${green}$source is OK to merge to $target. Going ahead...${reset}"
          git checkout $target
          git merge $source -s ours --no-edit
          git push
      fi
  fi

  cd -
}

echo "Batch processing Armata platform modules."

while read line
do
    # Skip commented out modules
    [[ $line = \#* ]] && continue

    module=`echo $line | cut -d, -f1`
    repo=`echo $line | cut -d, -f2`
    previousBranch=""

    echo "-------------------------------------------------------------------------------------------------------------"
    echo "Processing module: $module with repo: $repo"

    if [ ! -d $module/.git ]; then
        git clone $repo $module
    fi

    git -C $module fetch

    if [[ $1 == "clean" ]]
    then
        git -C $module branch -r | grep PLATFORM-159 | sed 's/origin\///' | while read deleteBranch
        do
            echo "Deleting issue branch: $deleteBranch"
            git -C $module push origin --delete $deleteBranch
        done
        continue
    fi

    # Fetch the branches
    cat ../data/platform-module-branches.json |  jq '.modules[] | select(.name=="'$module'") | .branches| join(",")' | tr -d '"' | tr ',' '\n' | while read branch
    do
        # Remove everything before the last hyphen
        version=${branch##*-}

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

        git -C $module reset --hard
        git -C $module checkout $branch
        git -C $module pull
        git -C $module clean -dxf

        if [[ $1 == "merge" ]]
        then
            mergeBranch=`git -C $module branch -r | grep "$issueBranch/PLATFORM-159-code-reformat" | sed 's/origin\///'`
            behindCount=`git -C $module log $mergeBranch..$branch --oneline | wc -l | xargs`
            if [[ $behindCount > 0 ]]; then
                echo "${red}$mergeBranch is behind $branch by $behindCount commits. Not merging.${reset}"
            else
                echo "${green}$mergeBranch is OK to merge.${reset}"
                git -C $module merge $mergeBranch
                echo "Pushing merge commit."
                git -C $module push
            fi
            continue
        fi

        # This is here to marked version branches as merged
        if [[ $1 == "emptymerge" ]]
        then

            # Merge previous branch up to this if necessary (and possible)
            if [[ $previousBranch != "" ]]
            then
              emptymerge $module $previousBranch $branch
            fi

            previousBranch=$branch
            continue
        fi

        issueBranch=$issueBranch/PLATFORM-159-code-reformat-`date +%s`
        echo "Processing branch: $branch on issue branch: $issueBranch"
        git -C $module checkout -b $issueBranch

        ../bin/codestyle.sh $module

        # Check it worked, and if not log the problem and continue
        if [[ $? != 0 ]]; then
            echo "${red}*******************************************************************************************"
            echo "                Failed to format code for module: $module on branch: $branch"
            echo "*******************************************************************************************${reset}"
            continue
        fi

        git -C $module add .
        git -C $module commit --author="codeformatter <>" -m "PLATFORM-159: Code formatting only - see JIRA issue for details."
        git -C $module push --set-upstream origin $issueBranch

    done
done<../data/armata-modules.txt
