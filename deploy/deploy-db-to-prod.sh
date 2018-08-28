#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys MariaDB to the production environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/database.yml --limit tag_Env_prod $*
