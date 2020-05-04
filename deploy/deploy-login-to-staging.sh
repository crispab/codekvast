#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys Codekvast Login to the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/login.yml -e rds_endpoint_address=$(get-rds-endpoint staging) --limit tag_Env_staging $*
