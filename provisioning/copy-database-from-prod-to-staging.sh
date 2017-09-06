#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Dumps the production database to S3 and restores it to staging
#---------------------------------------------------------------------------------------------------

for f in ~/.boto ~/.ssh/codekvast-amazon.pem; do
    if [ ! -f ${f} ]; then
        echo "Missing required file: $f" 1>&2
        exit 1
    fi
done

cd $(dirname $0)
ansible-playbook --private-key ~/.ssh/codekvast-amazon.pem playbooks/copy-database-from-prod-to-staging.yml

