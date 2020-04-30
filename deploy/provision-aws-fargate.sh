#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS Fargate resources
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/aws-fargate.yml $*

