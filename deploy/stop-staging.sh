#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Stops running staging instances
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2 | xargs)
declare AWS_EC2="aws --profile codekvast --region ${region} ec2"

${AWS_EC2} describe-instances --filter "Name=tag:Env,Values=staging" \
     | awk '/InstanceId/{print $2}' | tr -d '",' | while read instance; do
    ${AWS_EC2} stop-instances --instance-ids ${instance} | jq ".StoppingInstances[] | {instanceId: .InstanceId, currentState: .PreviousState.Name, newState: .CurrentState.Name}"
done

declare AWS_RDS="aws --profile codekvast --region ${region} rds"

${AWS_RDS} start-db-instance --db-instance-identifier codekvast-staging
