#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys Codekvast Admin to all environments
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/admin.yml $*

