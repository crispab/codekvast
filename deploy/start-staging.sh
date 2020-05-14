#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Start the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2 | xargs)
export AWS_PROFILE=codekvast
export AWS_REGION=${region}

aws ec2 describe-instances --filter Name=tag:Env,Values=staging Name=tag:role,Values=backend \
     | awk '/InstanceId/{print $2}' | tr -d '",' | while read instance; do
    aws ec2 start-instances --instance-ids ${instance} | jq ".StartingInstances[] | {instanceId: .InstanceId, currentState: .PreviousState.Name, newState: .CurrentState.Name}"
done

aws rds start-db-instance --db-instance-identifier codekvast-staging | jq "{DBInstanceIdentifier: .DBInstance.DBInstanceIdentifier, status: .DBInstance.DBInstanceStatus}"

for svc in backoffice dashboard login; do
  aws ecs update-service --cluster codekvast-staging --service ${svc} --desired-count 1 | jq "{taskDefinition: .service.taskDefinition, desiredCount: .service.desiredCount}"
done
