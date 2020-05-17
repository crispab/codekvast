#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS RDS servers
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-security-groups.yml -e env=staging $*
ansible-playbook playbooks/provision-database.yml -e env=staging $*
