#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS RDS servers in staging
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

env ENVIRONMENTS=staging ./provision-database.sh $*
