#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys RabbitMQ to the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/rabbitmq.yml --limit tag_Env_staging $*
