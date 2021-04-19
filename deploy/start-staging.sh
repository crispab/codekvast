#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Start the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

aws rds start-db-instance --db-instance-identifier codekvast-staging | jq "{DBInstanceIdentifier: .DBInstance.DBInstanceIdentifier, status: .DBInstance.DBInstanceStatus}"

for svc in ${allServices}; do
  aws ecs update-service --cluster codekvast-staging --service ${svc} --desired-count 1 | jq "{taskDefinition: .service.taskDefinition, desiredCount: .service.desiredCount}"
done
