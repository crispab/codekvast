#!/bin/bash

set -e

cd $(dirname $0)/ansible
ansible-playbook -i inventory compilers.yml $*
