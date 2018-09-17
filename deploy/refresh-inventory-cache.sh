#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Refreshes the Ansible inventory cache
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

inventory/ec2.py --refresh-cache
