#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Dumps the production database to S3 using the backup name "extra"
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh
declare weekday=${1:-extra}

ansible-playbook playbooks/make-database-backup.yml -e weekday=${weekday}
