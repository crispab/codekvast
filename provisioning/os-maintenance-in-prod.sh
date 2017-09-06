#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Upgrades the servers' operating system in prod
#---------------------------------------------------------------------------------------------------

for f in ~/.boto ~/.ssh/codekvast-amazon.pem; do
    if [ ! -f ${f} ]; then
        echo "Missing required file: $f" 1>&2
        exit 1
    fi
done

cd $(dirname $0)
ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/os-maintenance.yml --limit tag_Env_prod $*
