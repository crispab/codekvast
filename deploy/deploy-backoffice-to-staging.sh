#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys the latest Docker image for Codekvast Backoffice to the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

aws ecs update-service --cluster=codekvast-staging --service=backoffice --force-new-deployment | jq .service.taskDefinition | xargs
