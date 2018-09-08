#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys Codekvast Admin to the production environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/admin.yml --limit tag_Env_prod $*
