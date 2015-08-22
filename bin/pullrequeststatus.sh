#! /bin/bash

cut -d, -f1 armata-modules.txt | while read module; do echo -n "$(curl -s https://api.bitbucket.org/2.0/repositories/atlassian/$module/pullrequests | grep -o description | wc -l)"; echo  ":$module"; done | sort -r