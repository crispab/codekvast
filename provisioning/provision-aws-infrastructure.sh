#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS resources for the stacks that are defined by vars/common.yml
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/infrastructure.yml $*

