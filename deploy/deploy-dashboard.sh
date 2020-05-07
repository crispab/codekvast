#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys Codekvast Dashboard to all environments
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/dashboard.yml --limit tag_Env_staging $*
ansible-playbook playbooks/dashboard.yml --limit tag_Env_prod $*

