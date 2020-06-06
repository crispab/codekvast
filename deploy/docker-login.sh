#!/usr/bin/env bash
source $(dirname $0)/.check-requirements.sh

eval $(aws ecr get-login --no-include-email --region ${AWS_REGION})
