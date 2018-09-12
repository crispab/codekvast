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

declare portsToOpen="22 3306 5010 5011 8080 8081 9080 9081"

# Find the GroupId of the security group
declare groupId=$($AWS_EC2 describe-security-groups --filters Name=group-name,Values=${groupName}|jq .SecurityGroups[0].GroupId|xargs)

case "$groupId" in
    null)
        echo "No such security group: $groupName"
        exit 1;;
    sg-*)
    ;;
esac

echo -n "Will modify the security group $groupName to enable TCP access from $myIp to ports $portsToOpen with the description \"$description\". Ok? [y/N]: "
read answer
case $answer in
    ""|n|N) exit 0;;
    y|Y) ;;
esac

for port in ${portsToOpen}; do
    echo aws --profile codekvast ec2 authorize-security-group-ingress --group-id $groupId --ip-permissions "[{\"IpProtocol\": \"tcp\", \"FromPort\": $port, \"ToPort\": $port, \"IpRanges\": [{\"CidrIp\": \"$myIp/24\", \"Description\": \"Management from $description\"}]}]"
    aws --profile codekvast ec2 authorize-security-group-ingress --group-id $groupId --ip-permissions  "[{\"IpProtocol\": \"tcp\", \"FromPort\": $port, \"ToPort\": $port, \"IpRanges\": [{\"CidrIp\": \"$myIp/24\", \"Description\": \"Management from $description\"}]}]"
done
