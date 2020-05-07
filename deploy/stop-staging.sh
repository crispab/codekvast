#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Stops running staging instances
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2 | xargs)
declare AWS="aws --profile codekvast --region ${region}"

${AWS} ec2 describe-instances --filter "Name=tag:Env,Values=staging" \
     | awk '/InstanceId/{print $2}' | tr -d '",' | while read instance; do
    ${AWS} ec2 stop-instances --instance-ids ${instance} | jq ".StoppingInstances[] | {instanceId: .InstanceId, currentState: .PreviousState.Name, newState: .CurrentState.Name}"
done

${AWS} rds stop-db-instance --db-instance-identifier codekvast-staging | jq "{DBInstanceIdentifier: .DBInstance.DBInstanceIdentifier, status: .DBInstance.DBInstanceStatus}"

# TODO: Stop EC2 cluster
# TODO: Stop ALB
