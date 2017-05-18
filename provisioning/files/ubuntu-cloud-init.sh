#!/usr/bin/env bash
#------------------------------------------------------------------
# This script is sent as user_data to the ec2 module.
# It will execute as root immediately after the first boot
#
# NOTE: It is designed to be as quick as possible.
# Slow tasks like `apt update` and `apt upgrade` are done from a playbook.
#------------------------------------------------------------------

apt install -y python-minimal python-apt ntp aptitude
