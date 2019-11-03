#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Refreshes the Ansible inventory cache
#---------------------------------------------------------------------------------------------------

rm -fr ~/.cache/ansible-inventory/

cd $(dirname $0)
ansible-inventory --graph
