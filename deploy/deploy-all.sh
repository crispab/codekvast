#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys all Codekvast components to all environments
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/servers.yml --limit tag_Env_staging $*
ansible-playbook playbooks/servers.yml --limit tag_Env_prod $*
