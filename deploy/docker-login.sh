#!/usr/bin/env bash
source $(dirname $0)/.check-requirements.sh

declare AWS_ACCOUNT=$(yq read playbooks/vars/common.yml aws_account)
declare AWS_REGION=$(yq read playbooks/vars/common.yml aws_region)
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin $AWS_ACCOUNT.dkr.ecr.eu-central-1.amazonaws.com
