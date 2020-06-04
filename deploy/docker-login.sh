#!/usr/bin/env bash
source $(dirname $0)/.check-requirements.sh

declare region=$(yq read playbooks/vars/common.yml aws_region)
declare account=$(yq read playbooks/vars/common.yml aws_account)
eval $(env AWS_PROFILE=codekvast aws ecr get-login --no-include-email --region ${region})
