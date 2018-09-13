#!/usr/bin/env bash

declare tmpfile=$(mktemp)
trap "rm -f ${tmpfile}" exit

grep --extended-regexp --no-filename --only-matching '/opt/codekvast/codekvast-.*\.jar' /etc/systemd/system/codekvast*.service|awk '{print $1}'|sort --unique > ${tmpfile}
grep --extended-regexp --no-filename --only-matching 'jar /opt/codekvast.*codekvast-.*\.jar' /etc/systemd/system/codekvast*.service|awk '{print $2}'|sort --unique >> ${tmpfile}

find /opt/codekvast -name 'codekvast*.jar' | grep --invert-match --file ${tmpfile} | xargs --no-run-if-empty rm --force --verbose

