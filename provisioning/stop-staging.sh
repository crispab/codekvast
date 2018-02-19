#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Stops running staging instances
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2)
declare AWS_EC2="aws --profile codekvast --region ${region} ec2"

${AWS_EC2} describe-instances --filter "Name=tag:Env,Values=staging" \
     | awk '/InstanceId/{print $2}' | tr -d '",' | while read instance; do
     echo "Stopping instance ${instance}..."
    ${AWS_EC2} stop-instances --instance-ids ${instance}
done

