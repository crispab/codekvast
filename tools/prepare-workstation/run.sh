#!/bin/bash

set -e

if [ "$(which ansible)" == "/usr/local/bin/ansible" ]; then
    echo "Removing pip-installed ansible:"
    sudo pip uninstall ansible
fi

if [ "$(grep ppa.launchpad.net/ansible/ansible /etc/apt/sources.list.d/ansible*.list)" == "" -o  "$(which ohai)" == "" ]; then
    echo "Adding APT key for ppa:ansible/ansible:"
    sudo apt-add-repository -y ppa:ansible/ansible
    sudo apt update
    sudo apt install -y software-properties-common ansible ohai libssl-dev
fi

cd $(dirname $0)/ansible
sudo chown root.root ~/.netrc
sudo ansible-playbook -i inventory playbook.yml -e "actual_username=${USER}" $*
sudo chown ${USER}.$(id -gn ${USER}) ~/.netrc
