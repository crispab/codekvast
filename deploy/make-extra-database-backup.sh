#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Dumps the production database to S3
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/make-extra-database-backup.yml
