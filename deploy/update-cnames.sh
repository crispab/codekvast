#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Updates all the running EC2 instances' public CNAMEs
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/update-cnames.yml $*
