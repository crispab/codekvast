#!/bin/bash

set -e

if [[ "$(command -v ansible)" == "/usr/bin/ansible" ]]; then
    echo "Removing apt-installed ansible:"
    sudo apt remove ansible
fi
if [[ "$(command -v pip3)" == "" ]]; then
    echo "Installing python3-pip:"
    sudo apt install python3-pip
fi

if [[ "$(command -v ansible)" == "" ]]; then
    echo "Installing Ansible with pip3:"
    sudo pip3 install ansible
fi

cd $(dirname $0)/ansible

sudo chown root.root ~/.netrc
trap "sudo chown ${USER}.$(id -gn ${USER}) ~/.netrc" EXIT

sudo ansible-playbook -i inventory playbook.yml -e "actual_username=${USER}" $*
