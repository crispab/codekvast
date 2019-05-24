#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys Codekvast dashboard to the production environment
#---------------------------------------------------------------------------------------------------


source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/dashboard.yml --limit tag_Env_prod $*
