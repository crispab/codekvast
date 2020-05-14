#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS Fargate resources
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-aws-secrets.yml -e env=prod $*
ansible-playbook playbooks/provision-aws-security.yml -e env=prod $*
ansible-playbook playbooks/provision-aws-fargate.yml -e env=prod $*
