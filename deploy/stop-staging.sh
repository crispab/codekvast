#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Stop the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2 | xargs)
export AWS_PROFILE=codekvast
export AWS_REGION=${region}

aws rds stop-db-instance --db-instance-identifier codekvast-staging | jq "{DBInstanceIdentifier: .DBInstance.DBInstanceIdentifier, status: .DBInstance.DBInstanceStatus}"

for svc in backoffice dashboard login; do
  aws ecs update-service --cluster codekvast-staging --service ${svc} --desired-count 0 | jq "{taskDefinition: .service.taskDefinition, desiredCount: .service.desiredCount}"
done
