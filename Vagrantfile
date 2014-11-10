# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = '2'

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = 'ubuntu/trusty64'

  config.vm.define 'codekvast_server' do |ck_server|
    ck_server.vm.network 'private_network', ip : '192.168.33.10'
  end

  config.vm.define 'varmfront' do |varmfront|
    varmfront.vm.network 'private_network', ip : '192.168.33.11'
  end

  # config.vm.network "forwarded_port", guest: 80, host: 8080
  # config.vm.network "private_network", ip: "192.168.33.10"
  # config.vm.network "public_network"

  # config.vm.synced_folder "../data", "/vagrant_data"
end
