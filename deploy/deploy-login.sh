#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys Codekvast login to all environments
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/login.yml --limit tag_Env_staging $*
ansible-playbook playbooks/login.yml --limit tag_Env_prod $*
