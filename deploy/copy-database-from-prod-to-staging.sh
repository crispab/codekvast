#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Dumps the production database to S3 and restores it to staging
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/copy-database-from-prod-to-staging.yml
