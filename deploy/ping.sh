#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Pings all servers
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible --user=ubuntu --one-line --module-name=ping all $*

