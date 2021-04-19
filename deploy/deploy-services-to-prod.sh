#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys all Codekvast services to the prod environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

for svc in ${allServices}; do
  aws ecs update-service --cluster=codekvast-prod --service=${svc} --force-new-deployment | jq .service.taskDefinition | xargs
done
