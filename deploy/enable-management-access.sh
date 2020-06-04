#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------------
# Modifies a Fargate security group to enable access to certain ports from this workstation
#---------------------------------------------------------------------------------------------------------
source $(dirname $0)/.check-requirements.sh

declare region=$(yq read playbooks/vars/common.yml aws_region)

declare environment=${1:-staging}
declare description=${2:-$(hostname)}
declare fromPort=22
declare toPort=65535

declare groupNames="codekvast-${environment}-backend codekvast-${environment}-database"
declare myIp=$(curl -s https://api.ipify.org)

for groupName in ${groupNames}; do
  # Find the GroupId of the security group
  declare groupId=$(aws --profile codekvast ec2 describe-security-groups --filters Name=group-name,Values=${groupName}|jq .SecurityGroups[0].GroupId|xargs)

  case "$groupId" in
      null)
          echo "No such security group: $groupName"
          exit 1;;
      sg-*)
      ;;
  esac

  echo -n "Will modify the security group $groupName to enable all ICMP access and TCP access on ports $fromPort-$toPort from $myIp with the description \"$description\". Ok? [y/N]: "
  read answer
  case ${answer} in
      ""|n|N) exit 0;;
      y|Y) ;;
  esac

  echo aws --profile codekvast ec2 authorize-security-group-ingress --group-id $groupId --ip-permissions "[{\"IpProtocol\": \"tcp\", \"FromPort\": $fromPort, \"ToPort\": $toPort, \"IpRanges\": [{\"CidrIp\": \"$myIp/32\", \"Description\": \"$description\"}]}]"
  aws --profile codekvast ec2 authorize-security-group-ingress --group-id $groupId --ip-permissions  "[{\"IpProtocol\": \"tcp\", \"FromPort\": $fromPort, \"ToPort\": $toPort, \"IpRanges\": [{\"CidrIp\": \"$myIp/32\", \"Description\": \"$description\"}]}]"
done
