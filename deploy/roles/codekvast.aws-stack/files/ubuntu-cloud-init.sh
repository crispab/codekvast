#!/usr/bin/env bash
#------------------------------------------------------------------
# This script is sent as user_data to the ec2 module.
# It will execute as root immediately after the first boot
#------------------------------------------------------------------

apt update
apt install -y openssh-server python3-minimal python3-apt ntp aptitude
