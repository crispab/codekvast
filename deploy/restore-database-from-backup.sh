#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Dumps the production database to S3 and restores it to staging
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare weekday=${1:-$(env LANG=en_US date -d "yesterday 13:00" --utc +%A | tr [A-Z] [a-z])}
declare env=${2:-prod}

usage() {
    cat << EOF

Usage: $0 [weekday] [environment]

    Where weekday is one of monday, tuesday, wednesday, thursday, friday, saturday, sunday or extra.
    extra is an extra backup created by the script $(dirname $0)/copy-database-from-prod-to-staging.sh.

    environment is one of staging or prod. It is the environment that will receive the backup.

EOF
}

echo -n "About to restore the ${weekday} backup to ${env}. Continue [y/N/?]: "
read answer
case ${answer} in
    '?')
        usage
        ;;

    y|yes)
        echo "OK, here we go..."
        ansible-playbook playbooks/restore-database-from-backup.yml -e env=${env} -e weekday=${weekday}
        ;;
    ''|n|no|N|NO|No)
        echo "Nothing done."
        ;;
    *)
        usage
        ;;
esac
