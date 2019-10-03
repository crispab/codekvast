#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Dumps the production database to S3 and restores it to staging
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare weekday=${1:-$(env LANG=en_US date -d "yesterday 13:00" --utc +%A | tr [A-Z] [a-z])}
declare srcEnv=${2:-prod}
declare targetEnv=${3:-prod}
declare appName=mariabackup

usage() {
    cat << EOF

Usage: $0 [weekday] [source-environment] [target-environment] [appName]

    Where weekday is one of monday, tuesday, wednesday, thursday, friday, saturday, sunday or extra. Defaults to yesterday's weekday.

    extra is an extra backup created by the scripts $(dirname $0)/make-extra-database-backup.sh or
    $(dirname $0)/copy-database-from-prod-to-staging.sh.

    source-environment is one of staging or prod. It is the environment that has produced the backup. Defaults to prod.
    target-environment is one of staging or prod. It is the environment that will receive the backup. Defaults to prod.
EOF
}

echo -n "About to restore the ${weekday} backup produced by ${srcEnv} into ${targetEnv} by means of ${appName}. Continue [y/N/?]: "
read answer
case ${answer} in
    '?')
        usage
        ;;

    y|yes)
        echo "OK, here we go..."
        ansible-playbook playbooks/restore-database-from-backup.yml -e srcEnv=${srcEnv} -e targetEnv=${targetEnv} -e weekday=${weekday} -e appName=${appName}
        ;;
    ''|n|no|N|NO|No)
        echo "Nothing done."
        ;;
    *)
        usage
        ;;
esac
