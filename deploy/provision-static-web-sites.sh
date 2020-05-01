#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS resources for the stacks that are defined by vars/common.yml
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-static-web-sites.yml $*

