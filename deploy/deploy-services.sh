#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys all Codekvast services to the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/services.yml -e rds_endpoint_address=$(get-rds-endpoint staging) --limit tag_Env_staging $*
ansible-playbook playbooks/services.yml -e rds_endpoint_address=$(get-rds-endpoint prod) --limit tag_Env_prod $*

