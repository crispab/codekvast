#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS RDS servers
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

for env in ${ENVIRONMENTS:-staging prod}; do
  ansible-playbook playbooks/provision-security-groups.yml -e env=${env} $*
  ansible-playbook playbooks/provision-database.yml -e env=${env} $*
done
