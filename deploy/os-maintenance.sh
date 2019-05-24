#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Upgrades all the servers' operating system
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/os-maintenance.yml --limit tag_Env_staging $*
ansible-playbook playbooks/os-maintenance.yml --limit tag_Env_prod $*
