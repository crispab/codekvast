#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------------
# Modifies an EC2 security group to enable access to certain ports from this workstation
#---------------------------------------------------------------------------------------------------------
source $(dirname $0)/.check-requirements.sh

declare region=$(grep aws_region playbooks/vars/common.yml | cut -d: -f2)
declare AWS_EC2="aws --profile codekvast --region ${region} ec2"

declare environment=${1:-staging}
declare description=${2:-$(hostname)}

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

declare fromPort=22
declare toPort=65535

echo -n "Will modify the security group $groupName to enable all ICMP access and TCP access on ports $fromPort-$toPort from $myIp with the description \"$description\". Ok? [y/N]: "
read answer
case $answer in
    ""|n|N) exit 0;;
    y|Y) ;;
esac

echo aws --profile codekvast ec2 authorize-security-group-ingress --group-id $groupId --ip-permissions "[{\"IpProtocol\": \"icmp\"\", \"FromPort\": -1, \"ToPort\": -1, \"IpRanges\": [{\"CidrIp\": \"$myIp/32\", \"Description\": \"$description\"}]}]"
aws --profile codekvast ec2 authorize-security-group-ingress --group-id $groupId --ip-permissions  "[{\"IpProtocol\": \"icmp\", \"FromPort\": -1, \"ToPort\": -1, \"IpRanges\": [{\"CidrIp\": \"$myIp/32\", \"Description\": \"$description\"}]}]"

echo aws --profile codekvast ec2 authorize-security-group-ingress --group-id $groupId --ip-permissions "[{\"IpProtocol\": \"tcp\", \"FromPort\": $fromPort, \"ToPort\": $toPort, \"IpRanges\": [{\"CidrIp\": \"$myIp/32\", \"Description\": \"$description\"}]}]"
aws --profile codekvast ec2 authorize-security-group-ingress --group-id $groupId --ip-permissions  "[{\"IpProtocol\": \"tcp\", \"FromPort\": $fromPort, \"ToPort\": $toPort, \"IpRanges\": [{\"CidrIp\": \"$myIp/32\", \"Description\": \"$description\"}]}]"
