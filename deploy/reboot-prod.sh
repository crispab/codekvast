#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------
# Reboots all prod instances using the AWS CLI
#---------------------------------------------------------------------------------------------------

source $(dirname $0)/.check-requirements.sh

echo -n "Are you sure [N/y]: "; read answer
case "$answer" in
    ""|"N"|"n") exit 0;;
    "y"|"Y") echo "Ok, here we go...";;
esac

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2)
declare AWS_EC2="aws --profile codekvast --region ${region} ec2"

${AWS_EC2} describe-instances --filter "Name=tag:Env,Values=prod" \
     | awk '/InstanceId/{print $2}' | tr -d '",' | while read instance; do
    echo "Rebooting instance ${instance}..."
    ${AWS_EC2} reboot-instances --instance-ids ${instance}
done

