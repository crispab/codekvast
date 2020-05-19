#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS RDS servers in prod
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=prod ./provision-database.sh $*
