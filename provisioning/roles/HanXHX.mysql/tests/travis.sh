#!/bin/sh

# Thanks to https://servercheck.in/blog/testing-ansible-roles-travis-ci-github

DIR=$( dirname $0 )
INVENTORY_FILE="localhost,"
PLAYBOOK="$DIR/travis.yml"

set -ev

# Check syntax
ansible-playbook -i $INVENTORY_FILE -c local --syntax-check -vv $PLAYBOOK

# Check role
ansible-playbook -i $INVENTORY_FILE -c local -e "{ mysql_vendor: $VENDOR, mysql_origin: $ORIGIN }" --sudo -vv $PLAYBOOK

# Check indempotence
ansible-playbook -i $INVENTORY_FILE -c local -e "{ mysql_vendor: $VENDOR, mysql_origin: $ORIGIN }" --sudo -vv $PLAYBOOK > idempot.txt
grep -q 'changed=0.*failed=0' idempot.txt \
&& (echo 'Idempotence test: pass' && exit 0) \
|| (echo 'Idempotence test: FAIL' && cat idempot.txt && exit 1)
