#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS resources for the stacks that are defined by vars/common.yml
#---------------------------------------------------------------------------------------------------

for f in ~/.boto ~/.ssh/codekvast-amazon.pem; do
    if [ ! -f $f ]; then
        echo "Missing required file: $f" 1>&2
        exit 1
    fi
done

cd $(dirname $0)
ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/infrastructure.yml $*

