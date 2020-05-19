#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS EC2 security groups in production
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=prod ./provision-security-groups.sh $*
