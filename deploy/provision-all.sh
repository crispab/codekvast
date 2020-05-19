#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions the complete AWS stack
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

for env in ${ENVIRONMENTS:-staging prod}; do
  ansible-playbook playbooks/provision-vpc.yml -e env=${env} $*
  ansible-playbook playbooks/provision-security-groups.yml -e env=${env} $*
  ansible-playbook playbooks/provision-database.yml -e env=${env} $*
  ansible-playbook playbooks/provision-email.yml -e env=${env} $*
  ansible-playbook playbooks/provision-static-web-sites.yml -e env=${env} $*
  ansible-playbook playbooks/provision-secrets.yml -e env=${env} $*
  ansible-playbook playbooks/provision-services.yml -e env=${env} $*
done
