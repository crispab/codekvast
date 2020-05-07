#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS Fargate resources
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-aws-fargate.yml -e rds_endpoint_address=$(get-rds-endpoint staging) -e env=staging $*

