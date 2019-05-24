#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Upgrades the servers' operating system in prod
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/os-maintenance.yml --limit tag_Env_prod $*
