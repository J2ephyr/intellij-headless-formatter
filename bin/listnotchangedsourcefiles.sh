#!/bin/bash
cd $1
find . | grep "\.java" | cut -c3- | sort > temp_formatter_af
git status |  grep "\.java" | cut -d" " -f4 | sort > temp_formatter_cf
diff temp_formatter_cf temp_formatter_af
rm temp_formatter_af temp_formatter_cf

