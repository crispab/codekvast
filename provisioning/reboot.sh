#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Upgrades all servers' operating system
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/reboot.yml $*
