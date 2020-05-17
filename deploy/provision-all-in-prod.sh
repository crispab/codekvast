#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions the complete AWS stack in prod
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-vpc.yml -e env=prod $*
ansible-playbook playbooks/provision-security-groups.yml -e env=prod $*
ansible-playbook playbooks/provision-database.yml -e env=prod $*
ansible-playbook playbooks/provision-email.yml -e env=prod $*
ansible-playbook playbooks/provision-static-web-sites.yml -e env=prod $*
ansible-playbook playbooks/provision-secrets.yml -e env=prod $*
ansible-playbook playbooks/provision-services.yml -e env=prod $*
