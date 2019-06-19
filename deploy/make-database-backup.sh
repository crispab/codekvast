#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Dumps the production database to S3 using yesterday as backup name
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare weekday=${1:-$(env LANG=en_US date -d "yesterday 13:00" --utc +%A | tr [A-Z] [a-z])}

ansible-playbook playbooks/make-database-backup.yml -e weekday=${weekday} $*
