#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Dumps the production or staging database to S3 using the backup name "extra"
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare weekday=${1:-extra}
declare srcEnv=${2:-prod}
declare appName=${3:-xtrabackup}

ansible-playbook playbooks/make-database-backup.yml -e weekday=${weekday} -e srcEnv=${srcEnv} -e appName=${appName}
