#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS networking in prod
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-vpc.yml -e env=prod $*
ansible-playbook playbooks/provision-security-groups.yml -e env=prod $*
