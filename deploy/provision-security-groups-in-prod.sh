#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS EC2 security groups in production
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-security-groups.yml -e env=prod $*
