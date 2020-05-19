#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions the complete AWS stack in prod
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=prod ./provision-all.sh $*
