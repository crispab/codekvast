#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys all Codekvast services to the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/services.yml --limit tag_Env_staging $*
ansible-playbook playbooks/services.yml --limit tag_Env_prod $*

