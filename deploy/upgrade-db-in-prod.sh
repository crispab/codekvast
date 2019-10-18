#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Upgrades MariaDB to the production environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/upgrade-mariadb.yml --limit tag_Env_prod $*
