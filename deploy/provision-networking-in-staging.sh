#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS networking in staging
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=staging ./provision-networking.sh $*
