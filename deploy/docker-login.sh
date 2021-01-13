#!/usr/bin/env bash
source $(dirname $0)/.check-requirements.sh

declare AWS_ACCOUNT=$(yq eval '.aws_account' playbooks/vars/common.yml)
declare AWS_REGION=$(yq eval '.aws_region' playbooks/vars/common.yml)
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin $AWS_ACCOUNT.dkr.ecr.eu-central-1.amazonaws.com
