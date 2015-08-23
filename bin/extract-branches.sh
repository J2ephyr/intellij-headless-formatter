#! /bin/bash

PLAN_TEMPLATES_DIR=$1
EXTRACT="$PLAN_TEMPLATES_DIR/bin/expand.sh"
PLAN_CACHE="/tmp/plancache"
OUTPUT="/tmp/branches.json"

rm $OUTPUT
mkdir -p $PLAN_CACHE

echo '{ "modules": [' >> $OUTPUT
cut -d, -f1 data/armata-modules.txt |
while read module; do
   template_file="$PLAN_TEMPLATES_DIR/templates/$module.groovy";
   if [ -f $template_file ]; then
     echo "Expanding build template for $module."
     plan_file="$PLAN_CACHE/$module.json"
     if [ ! -f $plan_file ]; then
       (cd $PLAN_TEMPLATES_DIR && $EXTRACT $template_file > $plan_file)
     else
       echo "Plan already expanded."
     fi
     echo '  { "name": "'$module'",' >> $OUTPUT
     echo -n '    "branches": [' >> $OUTPUT
     grep "key:'branch'" $plan_file | sed "s/^.*value:'\(.*\)'.*$/\1/" | sort | uniq \
     | while read branch; do echo -n '"'$branch'", '; done | echo -n "$(sed 's/, $//')" >> $OUTPUT
     echo ']' >> $OUTPUT
     echo '  },' >> $OUTPUT
   else
     echo "No plan template for $module."
   fi
done
echo ']}' >> $OUTPUT
