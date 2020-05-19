#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS EC2 security groups
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

for env in ${ENVIRONMENTS:-staging prod}; do
  ansible-playbook playbooks/provision-security-groups.yml -e env=${env} $*
done
