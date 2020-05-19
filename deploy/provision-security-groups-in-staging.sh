#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS EC2 security groups in staging
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=staging ./provision-security-groups.sh $*
