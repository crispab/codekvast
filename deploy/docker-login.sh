#!/usr/bin/env bash
source $(dirname $0)/.check-requirements.sh

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2 | xargs)
declare account=$(grep aws_account playbooks/vars/common.yml | cut -d: -f2 | xargs)
env AWS_PROFILE=codekvast aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account}.dkr.ecr.eu-central-1.amazonaws.com