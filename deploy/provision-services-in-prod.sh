#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS ECS resources in production
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=prod ./provision-services.sh $*
