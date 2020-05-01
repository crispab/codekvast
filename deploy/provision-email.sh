#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions infrastructure for email
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-email.yml $*
