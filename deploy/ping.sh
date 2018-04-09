#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Pings all servers
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible --private-key ~/.ssh/codekvast-amazon.pem --user=ubuntu --one-line --module-name=ping all $*

