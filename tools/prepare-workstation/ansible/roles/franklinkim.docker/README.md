# Ansible franklinkim.docker role

[![Build Status](https://img.shields.io/travis/weareinteractive/ansible-docker.svg)](https://travis-ci.org/weareinteractive/ansible-docker)
[![Galaxy](http://img.shields.io/badge/galaxy-franklinkim.docker-blue.svg)](https://galaxy.ansible.com/list#/roles/3275)
[![GitHub Tags](https://img.shields.io/github/tag/weareinteractive/ansible-docker.svg)](https://github.com/weareinteractive/ansible-docker)
[![GitHub Stars](https://img.shields.io/github/stars/weareinteractive/ansible-docker.svg)](https://github.com/weareinteractive/ansible-docker)

> `franklinkim.docker` is an [Ansible](http://www.ansible.com) role which:
>
> * installs docker
> * configures docker
> * adds logrotate for docker container logs

## Installation

Using `ansible-galaxy`:

```shell
$ ansible-galaxy install franklinkim.docker
```

Using `requirements.yml`:

```yaml
- src: franklinkim.docker
```

Using `git`:

```shell
$ git clone https://github.com/weareinteractive/ansible-docker.git franklinkim.docker
```

## Dependencies

* Ansible >= 1.9

## Variables

Here is a list of all the default variables for this role, which are also available in `defaults/main.yml`.

```yaml
---
# For more information about handlers see:
# http://www.ansibleworks.com/docs/playbooks.html#handlers-running-operations-on-change
#
# docker_options:
#   - "--dns 8.8.4.4"
# docker_containers:
#   - image: foo/bar
#     count: ...
#     command: ...
#     env: ...
#     expose: ...
#     hostname: ...
#     links: ...
#     name: ...
#     ports: ...
#     privileged: ...
#     pull: ...
#     state: ...
#     volumes: ...
#     volumes_from: ...
#

# apt package
docker_package: docker-engine
# start on boot
docker_service_enabled: yes
# current state: started, stopped
docker_service_state: started
# docker default options
docker_options: []
# list of images (http://docs.ansible.com/docker_image_module.html)
docker_images: []
# list of containers (http://docs.ansible.com/docker_module.html)
docker_containers: []

```

## Handlers

These are the handlers that are defined in `handlers/main.yml`.

```yaml
---

- name: restart docker
  service: name=docker state=restarted
  when: docker_service_state != 'stopped'

```


## Usage

This is an example playbook:

```yaml
---

- hosts: all
  sudo: yes
  roles:
    - franklinkim.docker
  vars:
    docker_options:
      - "--bip=10.0.3.1/24"

```

## Testing

```shell
$ git clone https://github.com/weareinteractive/ansible-docker.git
$ cd ansible-docker
$ vagrant up
```

## Contributing
In lieu of a formal styleguide, take care to maintain the existing coding style. Add unit tests and examples for any new or changed functionality.

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

*Note: To update the `README.md` file please install and run `ansible-role`:*

```shell
$ gem install ansible-role
$ ansible-role docgen
```

## License
Copyright (c) We Are Interactive under the MIT license.
