#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Removes Codekvast Admin from the production environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/admin.yml --limit tag_Env_prod $*
