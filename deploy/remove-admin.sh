#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Removes Codekvast Admin from all environments
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/admin.yml $*

