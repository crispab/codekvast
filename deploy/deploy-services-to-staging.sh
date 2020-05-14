#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys all Codekvast services to the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

export AWS_PROFILE=codekvast

for svc in backoffice dashboard login; do
  aws ecs update-service --cluster=codekvast-staging --service=${svc} --force-new-deployment | jq .service.taskDefinition | xargs
done
