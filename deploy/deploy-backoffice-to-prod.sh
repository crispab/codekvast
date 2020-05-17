#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys the latest Docker image for Codekvast Backoffice to the prod environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

export AWS_PROFILE=codekvast
aws ecs update-service --cluster=codekvast-prod --service=backoffice --force-new-deployment | jq .service.taskDefinition | xargs
