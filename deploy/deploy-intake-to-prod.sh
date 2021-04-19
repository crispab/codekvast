#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys the latest Docker image for Codekvast Intake to the prod environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

aws ecs update-service --cluster=codekvast-prod --service=intake --force-new-deployment | jq .service.taskDefinition | xargs
