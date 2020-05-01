#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS RDS servers
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-aws-rds.yml -e env=prod $*
