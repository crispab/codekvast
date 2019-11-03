#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys Codekvast Backoffice to the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/backoffice.yml --limit tag_Env_staging $*