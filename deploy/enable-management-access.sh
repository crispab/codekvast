#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------------
# Modifies an EC2 security group to enable access to certain ports from this workstation
#---------------------------------------------------------------------------------------------------------
source $(dirname $0)/.check-requirements.sh

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2)
declare AWS_EC2="aws --profile codekvast --region ${region} ec2"

declare environment=${1:-staging}
declare description=${2:-$USER}

declare groupName=codekvast-default-${environment}-management
declare myIp=$(curl -s https://api.ipify.org)

# Find the GroupId of the security group
declare groupId=$($AWS_EC2 describe-security-groups --filters Name=group-name,Values=${groupName}|jq .SecurityGroups[0].GroupId|xargs)

case "$groupId" in
    null)
        echo "No such security group: $groupName"
        exit 1;;
    sg-*)
    ;;
esac

echo -n "Will modify the security group $groupName to enable TCP access from $myIp to ports 22,3306,8080-8082 with the description \"$description\". Ok? [y/N]: "
read answer
case $answer in
    ""|n|N) exit 0;;
    y|Y) ;;
esac

echo "Ok, going ahead..."
# aws --profile codekvast ec2 authorize-security-group ...
