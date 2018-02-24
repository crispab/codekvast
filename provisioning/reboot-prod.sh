#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Upgrades the servers' operating system in prod
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/reboot.yml --limit tag_Env_prod $*
