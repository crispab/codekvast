#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS networking in prod
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=prod ./provision-networking.sh $*
