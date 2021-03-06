#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS SSM secrets in prod
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=prod ./provision-secrets.sh $*
