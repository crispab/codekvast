#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys the latest Docker image for Codekvast Dashboard to the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

aws ecs update-service --cluster=codekvast-staging --service=dashboard --force-new-deployment | jq .service.taskDefinition | xargs
