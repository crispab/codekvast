#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Deploys the latest Docker image for Codekvast Login to the staging environment
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

aws ecs update-service --cluster=codekvast-staging --service=login --force-new-deployment | jq .service.taskDefinition | xargs
