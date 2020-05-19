#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS ECS resources
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

for env in ${ENVIRONMENTS:-staging prod}; do
  ansible-playbook playbooks/provision-secrets.yml -e env=${env} $*
  ansible-playbook playbooks/provision-services.yml -e env=${env} $*
done
