#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys Nginx to all environments
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/nginx.yml $*

