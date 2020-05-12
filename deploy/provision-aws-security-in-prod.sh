#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS SSM secrets in production
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-aws-security.yml -e env=prod $*
