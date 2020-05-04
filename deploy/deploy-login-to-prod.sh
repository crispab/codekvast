#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys Codekvast Login to the production environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/login.yml -e rds_endpoint_address=$(get-rds-endpoint prod) --limit tag_Env_prod $*
