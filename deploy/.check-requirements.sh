#!/usr/bin/env bash
if [[ "$(which ansible)" == "" ]]; then
    echo "Ansible is not installed"
    exit 1
fi

declare ansibleVersion=$(ansible --version | awk '/^ansible/ {print $3}')
case "$ansibleVersion" in
    2.13*)
        ;;
    *)
        echo "Ansible version 2.13+ is required. Installed version is $ansibleVersion"
        exit 1;
        ;;
esac

for f in ~/.boto ~/.ssh/codekvast-amazon.pem; do
    if [[ ! -f ${f} ]]; then
        echo "Missing required file: $f" 1>&2
        exit 1
    fi
done

if [[ "$(which yq)" == "" ]]; then
    echo "yq is not installed"
    exit 1
fi

cd $(dirname $0)
export AWS_PROFILE=codekvast
export AWS_REGION=$(yq eval '.aws_region' playbooks/vars/common.yml)

declare allServices="backoffice dashboard intake login"
