#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Assigns an IAM role to all EC2 instances owned by Codekvast
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2 | tr -d '"')
declare AWS_EC2="aws --profile codekvast --region ${region} ec2"
declare IAM_ROLE="${1:-Codekvast-CloudWatch}"

${AWS_EC2} describe-instances --filter "Name=tag:Owner,Values=Codekvast" \
     | awk '/InstanceId/{print $2}' | tr -d '",' | while read instance; do
     echo "Assigning IAM role ${IAM_ROLE} to ${instance}..."
    ${AWS_EC2} associate-iam-instance-profile --iam-instance-profile Name=${IAM_ROLE} --instance-id ${instance}
done
