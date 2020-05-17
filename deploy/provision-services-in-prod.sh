#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS Fargate resources
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-secrets.yml -e env=prod $*
ansible-playbook playbooks/provision-services.yml -e env=prod $*
