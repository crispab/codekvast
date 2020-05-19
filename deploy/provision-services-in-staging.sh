#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS ECS resources in staging
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=staging ./provision-services.sh $*
