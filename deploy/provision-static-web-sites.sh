#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Provisions AWS static web sites in S3 and CloudFront
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

ansible-playbook playbooks/provision-static-web-sites.yml $*
