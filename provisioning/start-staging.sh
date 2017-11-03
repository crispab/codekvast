#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Starts not running staging instances
#---------------------------------------------------------------------------------------------------

for f in ~/.boto ~/.ssh/codekvast-amazon.pem; do
    if [ ! -f ${f} ]; then
        echo "Missing required file: $f" 1>&2
        exit 1
    fi
done

aws --profile codekvast ec2 describe-instances --filter "Name=tag:Env,Values=staging" \
     | awk '/InstanceId/{print $2}' | tr -d '",' | while read instance; do
     echo "Starting instance ${instance}..."
    aws --profile codekvast ec2 start-instances --instance-ids ${instance}
done

