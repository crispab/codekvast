#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS SSM secrets in staging
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-security-groups.yml -e env=staging $*
